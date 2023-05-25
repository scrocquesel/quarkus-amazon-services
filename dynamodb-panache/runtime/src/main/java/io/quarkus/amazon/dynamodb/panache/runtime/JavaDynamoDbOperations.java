package io.quarkus.amazon.dynamodb.panache.runtime;

import java.util.List;
import java.util.stream.Stream;

import io.quarkus.amazon.dynamodb.panache.PanacheQuery;
import io.quarkus.amazon.dynamodb.panache.common.runtime.DynamoDbOperations;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

public class JavaDynamoDbOperations extends DynamoDbOperations<PanacheQuery<?>> {

    /**
     * Provides the default implementations for quarkus to wire up. Should not be used by third party developers.
     */
    public static final JavaDynamoDbOperations INSTANCE = new JavaDynamoDbOperations();

    @Override
    protected PanacheQuery<?> createQuery(DynamoDbTable table, QueryConditional query) {
        return new PanacheQueryImpl(table, query);
    }

    @Override
    protected List<?> list(PanacheQuery<?> query) {
        return query.list();
    }

    @Override
    protected Stream<?> stream(PanacheQuery<?> query) {
        return query.stream();
    }

}
