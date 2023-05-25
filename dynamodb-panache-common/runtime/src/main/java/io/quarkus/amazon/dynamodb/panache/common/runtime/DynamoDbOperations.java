package io.quarkus.amazon.dynamodb.panache.common.runtime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.logging.Logger;

import io.quarkus.amazon.dynamodb.panache.common.DynamoDbEntity;
import io.quarkus.amazon.dynamodb.panache.common.binder.PanacheQlQueryBinder;
import io.quarkus.panache.common.Parameters;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.WriteBatch;
import software.amazon.awssdk.enhanced.dynamodb.model.WriteBatch.Builder;

public abstract class DynamoDbOperations<QueryType> {

    private static final Logger LOGGER = Logger.getLogger(DynamoDbOperations.class);

    private final Map<String, String> defaultTableName = new ConcurrentHashMap<>();

    protected abstract QueryType createQuery(DynamoDbTable<?> table, QueryConditional query);

    protected abstract List<?> list(QueryType queryType);

    protected abstract Stream<?> stream(QueryType queryType);

    public void persist(Object entity) {
        DynamoDbTable table = table(entity);
        persist(table, entity);
    }

    public void persist(Iterable<?> entities) {
        // not all iterables are re-traversal, so we traverse it once for copying inside a list
        List<Object> objects = new ArrayList<>();
        for (Object entity : entities) {
            objects.add(entity);
        }

        persist(objects);
    }

    public void persist(Object firstEntity, Object... entities) {
        if (entities == null || entities.length == 0) {
            DynamoDbTable table = table(firstEntity);
            persist(table, firstEntity);
        } else {
            List<Object> entityList = new ArrayList<>();
            entityList.add(firstEntity);
            entityList.addAll(Arrays.asList(entities));
            persist(entityList);
        }
    }

    public void persist(Stream<?> entities) {
        List<Object> objects = entities.collect(Collectors.toList());
        persist(objects);
    }

    public void update(Object entity) {
        DynamoDbTable table = table(entity);
        update(table, entity);
    }

    public void update(Iterable<?> entities) {
        // not all iterables are re-traversal, so we traverse it once for copying inside a list
        List<Object> objects = new ArrayList<>();
        for (Object entity : entities) {
            objects.add(entity);
        }

        if (!objects.isEmpty()) {
            // get the first entity to be able to retrieve the table with it
            Object firstEntity = objects.get(0);
            DynamoDbTable table = table(firstEntity);
            update(table, objects);
        }
    }

    public void update(Object firstEntity, Object... entities) {
        DynamoDbTable table = table(firstEntity);
        if (entities == null || entities.length == 0) {
            update(table, firstEntity);
        } else {
            List<Object> entityList = new ArrayList<>();
            entityList.add(firstEntity);
            entityList.addAll(Arrays.asList(entities));
            update(table, entityList);
        }
    }

    public void update(Stream<?> entities) {
        List<Object> objects = entities.collect(Collectors.toList());
        if (!objects.isEmpty()) {
            // get the first entity to be able to retrieve the table with it
            Object firstEntity = objects.get(0);
            DynamoDbTable table = table(firstEntity);
            update(table, objects);
        }
    }

    public void persistOrUpdate(Object entity) {
        DynamoDbTable table = table(entity);
        persistOrUpdate(table, entity);
    }

    public void persistOrUpdate(Iterable<?> entities) {
        // not all iterables are re-traversal, so we traverse it once for copying inside a list
        List<Object> objects = new ArrayList<>();
        for (Object entity : entities) {
            objects.add(entity);
        }

        persistOrUpdate(objects);
    }

    public void persistOrUpdate(Object firstEntity, Object... entities) {
        DynamoDbTable table = table(firstEntity);
        if (entities == null || entities.length == 0) {
            persistOrUpdate(table, firstEntity);
        } else {
            List<Object> entityList = new ArrayList<>();
            entityList.add(firstEntity);
            entityList.addAll(Arrays.asList(entities));
            persistOrUpdate(entityList);
        }
    }

    public void persistOrUpdate(Stream<?> entities) {
        List<Object> objects = entities.collect(Collectors.toList());
        persistOrUpdate(objects);
    }

    public void delete(Object entity) {
        DynamoDbTable table = table(entity);

        table.deleteItem(entity);
    }

    public DynamoDbTable table(Class<?> entityClass) {
        DynamoDbEntity dynamoDbEntity = entityClass.getAnnotation(DynamoDbEntity.class);
        DynamoDbEnhancedClient dynamoEnhancedClient = dynamoDbEnhancedClient(dynamoDbEntity);
        if (dynamoDbEntity != null) {
            DynamoDbTable table = dynamoDbEntity.table().isEmpty()
                    ? dynamoEnhancedClient.table(entityClass.getSimpleName(), TableSchema.fromClass(entityClass))
                    : dynamoEnhancedClient.table(dynamoDbEntity.table(), TableSchema.fromClass(entityClass));
            return table;
        }
        return dynamoEnhancedClient.table(entityClass.getSimpleName(), TableSchema.fromClass(entityClass));
    }

    public DynamoDbEnhancedClient dynamoDbEnhancedClient(Class<?> entityClass) {
        DynamoDbEntity dynamoDbEntity = entityClass.getAnnotation(DynamoDbEntity.class);
        return dynamoDbEnhancedClient(dynamoDbEntity);
    }

    //
    // Private stuff

    private void persist(DynamoDbTable table, Object entity) {
        table.putItem(entity);
    }

    private void persist(List<Object> entities) {
        if (!entities.isEmpty()) {
            // get the first entity to be able to retrieve the table with it
            Object firstEntity = entities.get(0);
            DynamoDbEnhancedClient client = dynamoDbEnhancedClient(firstEntity.getClass());

            Builder batch = WriteBatch.builder(firstEntity.getClass()).mappedTableResource(table(firstEntity.getClass()));
            for (Object entity : entities) {
                batch.addPutItem(entity);
            }

            client.batchWriteItem(builder -> builder.writeBatches(batch.build()));
        }
    }

    private void update(DynamoDbTable table, Object entity) {
        table.updateItem(entity);
    }

    private void update(DynamoDbTable table, List<Object> entities) {
        for (Object entity : entities) {
            update(table, entity);
        }
    }

    private void persistOrUpdate(DynamoDbTable table, Object entity) {
        update(table, entity);
    }

    private void persistOrUpdate(List<Object> entities) {
        if (entities.isEmpty()) {
            return;
        }

        // get the first entity to be able to retrieve the table with it
        Object firstEntity = entities.get(0);
        DynamoDbTable table = table(firstEntity);

        for (Object entity : entities) {
            update(table, entity);
        }
    }

    private DynamoDbTable table(Object entity) {
        Class<?> entityClass = entity.getClass();
        return table(entityClass);
    }

    private DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbEntity dynamoDbEntity) {
        return BeanUtils.clientFromArc(dynamoDbEntity, DynamoDbEnhancedClient.class);
    }

    //
    // Queries

    public Object findById(Class<?> entityClass, Object id) {
        DynamoDbTable table = table(entityClass);

        Key partitionKey;

        if (id instanceof String) {
            partitionKey = Key.builder().partitionValue((String) id).build();
        } else if (id instanceof Number) {
            partitionKey = Key.builder().partitionValue((Number) id).build();
        } else if (id instanceof SdkBytes) {
            partitionKey = Key.builder().partitionValue((SdkBytes) id).build();
        } else {
            return null; // invalid key type will not match any object in dynamodb table
        }

        return table.getItem(partitionKey);
    }

    public Optional findByIdOptional(Class<?> entityClass, Object id) {
        return Optional.ofNullable(findById(entityClass, id));
    }

    public QueryType find(Class<?> entityClass, String query, Object... params) {
        QueryConditional bindQuery = bindFilter(entityClass, query, params);
        DynamoDbTable table = table(entityClass);
        return createQuery(table, bindQuery);
    }

    /**
     * We should have a query like <code>{'firstname': ?1, 'lastname': ?2}</code> for native one
     * and like <code>firstname = ?1</code> for PanacheQL one.
     */
    public QueryConditional bindFilter(Class<?> clazz, String query, Object[] params) {
        QueryConditional bindQuery = bindQuery(clazz, query, params);
        LOGGER.debug(bindQuery);
        return bindQuery;
    }

    /**
     * We should have a query like <code>{'firstname': :firstname, 'lastname': :lastname}</code> for native one
     * and like <code>firstname = :firstname and lastname = :lastname</code> for PanacheQL one.
     */
    public QueryConditional bindFilter(Class<?> clazz, String query, Map<String, Object> params) {
        QueryConditional bindQuery = bindQuery(clazz, query, params);
        LOGGER.debug(bindQuery);
        return bindQuery;
    }

    private QueryConditional bindQuery(Class<?> clazz, String query, Object[] params) {
        QueryConditional bindQuery = PanacheQlQueryBinder.bindQuery(clazz, query, params);
        return bindQuery;
    }

    private QueryConditional bindQuery(Class<?> clazz, String query, Map<String, Object> params) {
        QueryConditional bindQuery = PanacheQlQueryBinder.bindQuery(clazz, query, params);
        return bindQuery;
    }

    public QueryType find(Class<?> entityClass, String query, Map<String, Object> params) {
        QueryConditional bindQuery = bindFilter(entityClass, query, params);
        DynamoDbTable table = table(entityClass);
        return createQuery(table, bindQuery);
    }

    public QueryType find(Class<?> entityClass, String query, Parameters params) {
        return find(entityClass, query, params.map());
    }

    public QueryType find(Class<?> entityClass, QueryConditional query) {
        DynamoDbTable table = table(entityClass);
        return createQuery(table, query);
    }

    public List<?> list(Class<?> entityClass, String query, Object... params) {
        return list(find(entityClass, query, params));
    }

    public List<?> list(Class<?> entityClass, String query, Map<String, Object> params) {
        return list(find(entityClass, query, params));
    }

    public List<?> list(Class<?> entityClass, String query, Parameters params) {
        return list(find(entityClass, query, params));
    }

    //specific DynamoDB query
    public List<?> list(Class<?> entityClass, QueryConditional query) {
        return list(find(entityClass, query));
    }

    public Stream<?> stream(Class<?> entityClass, String query, Object... params) {
        return stream(find(entityClass, query, params));
    }

    public Stream<?> stream(Class<?> entityClass, String query, Map<String, Object> params) {
        return stream(find(entityClass, query, params));
    }

    public Stream<?> stream(Class<?> entityClass, String query, Parameters params) {
        return stream(find(entityClass, query, params));
    }

    //specific DynamoDB query
    public Stream<?> stream(Class<?> entityClass, QueryConditional query) {
        return stream(find(entityClass, query));
    }

    public QueryType findAll(Class<?> entityClass) {
        DynamoDbTable table = table(entityClass);
        return createQuery(table, null);
    }

    public List<?> listAll(Class<?> entityClass) {
        return list(findAll(entityClass));
    }

    public Stream<?> streamAll(Class<?> entityClass) {
        return stream(findAll(entityClass));
    }

    public IllegalStateException implementationInjectionMissing() {
        return new IllegalStateException(
                "This method is normally automatically overridden in subclasses");
    }
}
