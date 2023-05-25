package io.quarkus.amazon.dynamodb.panache.runtime;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import io.quarkus.amazon.dynamodb.panache.PanacheQuery;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

public class PanacheQueryImpl<Entity> implements PanacheQuery<Entity> {

    private DynamoDbTable<Entity> table;
    private QueryConditional query;

    public PanacheQueryImpl(DynamoDbTable<Entity> table, QueryConditional query) {
        this.table = table;
        this.query = query;
    }

    @SuppressWarnings("unchecked")
    public <T extends Entity> List<T> list() {
        List<T> list = new ArrayList<>();

        PageIterable<Entity> pageIterable;
        if (query == null) {
            pageIterable = table.scan();
        } else {
            pageIterable = table.query(query);
        }

        Iterator<Entity> cursor = pageIterable.items().iterator();
        while (cursor.hasNext()) {
            T entity = (T) cursor.next();
            list.add(entity);
        }
        return list;
    }

    @SuppressWarnings("unchecked")
    public <T extends Entity> Stream<T> stream() {
        return (Stream<T>) list().stream();
    }
}
