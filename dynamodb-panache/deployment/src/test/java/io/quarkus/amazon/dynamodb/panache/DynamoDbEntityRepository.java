package io.quarkus.amazon.dynamodb.panache;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.amazon.dynamodb.panache.runtime.PanacheDynamoDbRepository;

@ApplicationScoped
public class DynamoDbEntityRepository implements PanacheDynamoDbRepository<DynamoDbEntityEntity> {

}
