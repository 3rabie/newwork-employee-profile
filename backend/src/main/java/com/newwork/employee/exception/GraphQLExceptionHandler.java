package com.newwork.employee.exception;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Maps domain exceptions to GraphQL errors with appropriate error types and messages.
 */
@Slf4j
@Component
public class GraphQLExceptionHandler extends DataFetcherExceptionResolverAdapter {

    @Override
    protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment env) {
        String path = env.getExecutionStepInfo().getPath().toString();

        if (ex instanceof ResourceNotFoundException) {
            log.debug("GraphQL NOT_FOUND at path {}: {}", path, ex.getMessage());
            return buildError(env, ex.getMessage(), ErrorType.NOT_FOUND);
        }

        if (ex instanceof ForbiddenException) {
            log.warn("GraphQL FORBIDDEN access attempt at path {}: {}", path, ex.getMessage());
            return buildError(env, ex.getMessage(), ErrorType.FORBIDDEN);
        }

        if (ex instanceof IllegalArgumentException) {
            log.debug("GraphQL BAD_REQUEST at path {}: {}", path, ex.getMessage());
            return buildError(env, ex.getMessage(), ErrorType.BAD_REQUEST);
        }

        // Log unhandled exceptions for investigation
        log.error("Unhandled GraphQL exception at path {}: {}", path, ex.getMessage(), ex);

        // Let Spring GraphQL handle other exceptions
        return null;
    }

    private GraphQLError buildError(DataFetchingEnvironment env, String message, ErrorType type) {
        return GraphqlErrorBuilder.newError(env)
                .message(message)
                .errorType(type)
                .extensions(Map.of("errorType", type.toString()))
                .build();
    }
}
