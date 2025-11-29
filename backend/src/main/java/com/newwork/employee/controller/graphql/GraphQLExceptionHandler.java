package com.newwork.employee.controller.graphql;

import com.newwork.employee.exception.ForbiddenException;
import com.newwork.employee.exception.ResourceNotFoundException;
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
        if (ex instanceof ResourceNotFoundException) {
            return buildError(env, ex.getMessage(), ErrorType.NOT_FOUND);
        }

        if (ex instanceof ForbiddenException) {
            return buildError(env, ex.getMessage(), ErrorType.FORBIDDEN);
        }

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
