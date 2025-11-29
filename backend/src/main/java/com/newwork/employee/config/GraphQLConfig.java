package com.newwork.employee.config;

import com.newwork.employee.entity.EmployeeProfile;
import com.newwork.employee.entity.User;
import com.newwork.employee.repository.EmployeeProfileRepository;
import com.newwork.employee.repository.UserRepository;
import graphql.scalars.ExtendedScalars;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.BatchLoaderRegistry;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Central GraphQL configuration for scalars and DataLoaders.
 */
@Configuration
@RequiredArgsConstructor
public class GraphQLConfig {

    private final UserRepository userRepository;
    private final EmployeeProfileRepository profileRepository;

    @Bean
    public RuntimeWiringConfigurer runtimeWiringConfigurer() {
        return wiringBuilder -> wiringBuilder
                .scalar(ExtendedScalars.UUID)
                .scalar(ExtendedScalars.DateTime);
    }

    @Bean
    public ApplicationRunner registerDataLoaders(BatchLoaderRegistry registry) {
        return args -> {
            registry.forTypePair(UUID.class, User.class)
                    .registerBatchLoader((userIds, env) -> {
                        List<User> users = userRepository.findAllById(userIds);
                        Map<UUID, User> userMap = users.stream()
                                .collect(Collectors.toMap(User::getId, Function.identity()));

                        return Flux.fromIterable(userIds)
                                .map(userMap::get);
                    });

            registry.forTypePair(UUID.class, EmployeeProfile.class)
                    .registerBatchLoader((userIds, env) -> {
                        List<EmployeeProfile> profiles = profileRepository.findAllByUserIdIn(userIds);
                        Map<UUID, EmployeeProfile> profileMap = profiles.stream()
                                .collect(Collectors.toMap(
                                        profile -> profile.getUser().getId(),
                                        Function.identity()
                                ));

                        return Flux.fromIterable(userIds)
                                .map(profileMap::get);
                    });
        };
    }
}
