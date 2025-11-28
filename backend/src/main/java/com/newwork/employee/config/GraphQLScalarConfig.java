package com.newwork.employee.config;

import graphql.language.StringValue;
import graphql.schema.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Configuration for custom GraphQL scalar types.
 * Defines how UUID and DateTime types are serialized/deserialized.
 */
@Configuration
public class GraphQLScalarConfig {

    /**
     * Configure custom scalar types for GraphQL schema.
     *
     * @return RuntimeWiringConfigurer with UUID and DateTime scalars
     */
    @Bean
    public RuntimeWiringConfigurer runtimeWiringConfigurer() {
        return wiringBuilder -> wiringBuilder
                .scalar(uuidScalar())
                .scalar(dateTimeScalar());
    }

    /**
     * UUID scalar type for GraphQL.
     * Serializes UUID to String and parses String to UUID.
     *
     * @return GraphQLScalarType for UUID
     */
    private GraphQLScalarType uuidScalar() {
        return GraphQLScalarType.newScalar()
                .name("UUID")
                .description("UUID scalar type")
                .coercing(new Coercing<UUID, String>() {
                    @Override
                    public String serialize(Object dataFetcherResult) throws CoercingSerializeException {
                        if (dataFetcherResult instanceof UUID) {
                            return dataFetcherResult.toString();
                        }
                        throw new CoercingSerializeException("Expected UUID type");
                    }

                    @Override
                    public UUID parseValue(Object input) throws CoercingParseValueException {
                        try {
                            return UUID.fromString(input.toString());
                        } catch (IllegalArgumentException e) {
                            throw new CoercingParseValueException("Invalid UUID format", e);
                        }
                    }

                    @Override
                    public UUID parseLiteral(Object input) throws CoercingParseLiteralException {
                        if (input instanceof StringValue) {
                            try {
                                return UUID.fromString(((StringValue) input).getValue());
                            } catch (IllegalArgumentException e) {
                                throw new CoercingParseLiteralException("Invalid UUID format", e);
                            }
                        }
                        throw new CoercingParseLiteralException("Expected StringValue for UUID");
                    }
                })
                .build();
    }

    /**
     * DateTime scalar type for GraphQL.
     * Serializes LocalDateTime to ISO-8601 String and parses String to LocalDateTime.
     *
     * @return GraphQLScalarType for DateTime
     */
    private GraphQLScalarType dateTimeScalar() {
        return GraphQLScalarType.newScalar()
                .name("DateTime")
                .description("DateTime scalar type (ISO-8601 format)")
                .coercing(new Coercing<LocalDateTime, String>() {
                    private final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

                    @Override
                    public String serialize(Object dataFetcherResult) throws CoercingSerializeException {
                        if (dataFetcherResult instanceof LocalDateTime) {
                            return ((LocalDateTime) dataFetcherResult).format(formatter);
                        }
                        throw new CoercingSerializeException("Expected LocalDateTime type");
                    }

                    @Override
                    public LocalDateTime parseValue(Object input) throws CoercingParseValueException {
                        try {
                            return LocalDateTime.parse(input.toString(), formatter);
                        } catch (Exception e) {
                            throw new CoercingParseValueException("Invalid DateTime format", e);
                        }
                    }

                    @Override
                    public LocalDateTime parseLiteral(Object input) throws CoercingParseLiteralException {
                        if (input instanceof StringValue) {
                            try {
                                return LocalDateTime.parse(((StringValue) input).getValue(), formatter);
                            } catch (Exception e) {
                                throw new CoercingParseLiteralException("Invalid DateTime format", e);
                            }
                        }
                        throw new CoercingParseLiteralException("Expected StringValue for DateTime");
                    }
                })
                .build();
    }
}
