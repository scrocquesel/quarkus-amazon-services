package io.quarkus.amazon.dynamodb.panache;

import java.util.List;
import java.util.stream.Stream;

public interface PanacheQuery<Entity> {

    <T extends Entity> List<T> list();

    <T extends Entity> Stream<T> stream();

}
