package com.newwork.employee.config;

import com.newwork.employee.entity.EmployeeProfile;
import com.newwork.employee.entity.User;
import com.newwork.employee.repository.EmployeeProfileRepository;
import com.newwork.employee.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.BatchLoaderRegistry;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Configuration for GraphQL DataLoaders to solve N+1 query problems.
 * DataLoaders batch multiple individual requests into a single database query.
 */
@Configuration
@RequiredArgsConstructor
public class GraphQLDataLoaderConfig {

    private final UserRepository userRepository;
    private final EmployeeProfileRepository profileRepository;

    /**
     * Register batch loaders to reduce N+1 query problems.
     * Spring for GraphQL will automatically use these for @SchemaMapping fields.
     *
     * @param registry automatically provided by Spring for GraphQL
     * @return ApplicationRunner that registers loaders on startup
     */
    @Bean
    public ApplicationRunner registerDataLoaders(BatchLoaderRegistry registry) {
        return args -> {
            // Batch loader for User entities by ID
            registry.forTypePair(UUID.class, User.class)
                    .registerBatchLoader((userIds, batchEnvironment) -> {
                        List<User> users = userRepository.findAllById(userIds);
                        Map<UUID, User> userMap = users.stream()
                                .collect(Collectors.toMap(User::getId, Function.identity()));

                        // Return users in the same order as requested IDs
                        return Flux.fromIterable(userIds)
                                .map(userMap::get);
                    });

            // Batch loader for EmployeeProfile entities by user ID
            registry.forTypePair(UUID.class, EmployeeProfile.class)
                    .registerBatchLoader((userIds, batchEnvironment) -> {
                        List<EmployeeProfile> profiles = profileRepository.findAllByUserIdIn(userIds);
                        Map<UUID, EmployeeProfile> profileMap = profiles.stream()
                                .collect(Collectors.toMap(
                                    profile -> profile.getUser().getId(),
                                    Function.identity()
                                ));

                        // Return profiles in the same order as requested IDs (null for missing profiles)
                        return Flux.fromIterable(userIds)
                                .map(profileMap::get);
                    });
        };
    }
}
