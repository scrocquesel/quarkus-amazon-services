package io.quarkus.amazon.dynamodb.panache;

import static io.quarkus.amazon.dynamodb.panache.runtime.JavaDynamoDbOperations.INSTANCE;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import io.quarkus.panache.common.impl.GenerateBridge;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

public abstract class PanacheDynamoDbEntityBase {
    /**
     * Persist this entity in the database.
     * This will set its ID field if not already set.
     *
     * @see #persist(Iterable)
     * @see #persist(Stream)
     * @see #persist(Object, Object...)
     */
    public void persist() {
        INSTANCE.persist(this);
    }

    /**
     * Update this entity in the database.
     *
     * @see #update(Iterable)
     * @see #update(Stream)
     * @see #update(Object, Object...)
     */
    public void update() {
        INSTANCE.update(this);
    }

    /**
     * Persist this entity in the database or update it if it already exists.
     *
     * @see #persistOrUpdate(Iterable)
     * @see #persistOrUpdate(Stream)
     * @see #persistOrUpdate(Object, Object...)
     */
    public void persistOrUpdate() {
        INSTANCE.persistOrUpdate(this);
    }

    /**
     * Delete this entity from the database, if it is already persisted.
     *
     * @see #delete(String, Object...)
     * @see #delete(String, Map)
     * @see #delete(String, Parameters)
     * @see #deleteAll()
     */
    public void delete() {
        INSTANCE.delete(this);
    }

    // Queries

    /**
     * Find an entity of this type by ID.
     *
     * @param id the ID of the entity to find.
     * @return the entity found, or <code>null</code> if not found.
     */
    @GenerateBridge(targetReturnTypeErased = true)
    public static <T extends PanacheDynamoDbEntityBase> T findById(Object id) {
        throw INSTANCE.implementationInjectionMissing();
    }

    /**
     * Find an entity of this type by ID.
     *
     * @param id the ID of the entity to find.
     * @return if found, an optional containing the entity, else <code>Optional.empty()</code>.
     */
    @GenerateBridge
    public static <T extends PanacheDynamoDbEntityBase> Optional<T> findByIdOptional(Object id) {
        throw INSTANCE.implementationInjectionMissing();
    }

    /**
     * Find entities using a query, with optional indexed parameters.
     *
     * @param query a {@link io.quarkus.amazon.dynamodb.panache query string}
     * @param params optional sequence of indexed parameters
     * @return a new {@link PanacheQuery} instance for the given query
     * @see #find(String, Sort, Object...)
     * @see #find(String, Map)
     * @see #find(String, Parameters)
     * @see #list(String, Object...)
     * @see #stream(String, Object...)
     */
    @GenerateBridge
    public static <T extends PanacheDynamoDbEntityBase> PanacheQuery<T> find(String query, Object... params) {
        throw INSTANCE.implementationInjectionMissing();
    }

    /**
     * Find entities using a query and the given sort options, with optional indexed parameters.
     *
     * @param query a {@link io.quarkus.amazon.dynamodb.panache query string}
     * @param sort the sort strategy to use
     * @param params optional sequence of indexed parameters
     * @return a new {@link PanacheQuery} instance for the given query
     * @see #find(String, Object...)
     * @see #find(String, Sort, Map)
     * @see #find(String, Sort, Parameters)
     * @see #list(String, Sort, Object...)
     * @see #stream(String, Sort, Object...)
     */
    @GenerateBridge
    public static <T extends PanacheDynamoDbEntityBase> PanacheQuery<T> find(String query, Sort sort, Object... params) {
        throw INSTANCE.implementationInjectionMissing();
    }

    /**
     * Find entities using a query, with named parameters.
     *
     * @param query a {@link io.quarkus.amazon.dynamodb.panache query string}
     * @param params {@link Map} of named parameters
     * @return a new {@link PanacheQuery} instance for the given query
     * @see #find(String, Sort, Map)
     * @see #find(String, Object...)
     * @see #find(String, Parameters)
     * @see #list(String, Map)
     * @see #stream(String, Map)
     */
    @GenerateBridge
    public static <T extends PanacheDynamoDbEntityBase> PanacheQuery<T> find(String query, Map<String, Object> params) {
        throw INSTANCE.implementationInjectionMissing();
    }

    /**
     * Find entities using a query and the given sort options, with named parameters.
     *
     * @param query a {@link io.quarkus.amazon.dynamodb.panache query string}
     * @param sort the sort strategy to use
     * @param params {@link Map} of indexed parameters
     * @return a new {@link PanacheQuery} instance for the given query
     * @see #find(String, Map)
     * @see #find(String, Sort, Object...)
     * @see #find(String, Sort, Parameters)
     * @see #list(String, Sort, Map)
     * @see #stream(String, Sort, Map)
     */
    @GenerateBridge
    public static <T extends PanacheDynamoDbEntityBase> PanacheQuery<T> find(String query, Sort sort,
            Map<String, Object> params) {
        throw INSTANCE.implementationInjectionMissing();
    }

    /**
     * Find entities using a query, with named parameters.
     *
     * @param query a {@link io.quarkus.amazon.dynamodb.panache query string}
     * @param params {@link Parameters} of named parameters
     * @return a new {@link PanacheQuery} instance for the given query
     * @see #find(String, Sort, Parameters)
     * @see #find(String, Map)
     * @see #find(String, Parameters)
     * @see #list(String, Parameters)
     * @see #stream(String, Parameters)
     */
    @GenerateBridge
    public static <T extends PanacheDynamoDbEntityBase> PanacheQuery<T> find(String query, Parameters params) {
        throw INSTANCE.implementationInjectionMissing();
    }

    /**
     * Find entities using a query and the given sort options, with named parameters.
     *
     * @param query a {@link io.quarkus.amazon.dynamodb.panache query string}
     * @param sort the sort strategy to use
     * @param params {@link Parameters} of indexed parameters
     * @return a new {@link PanacheQuery} instance for the given query
     * @see #find(String, Parameters)
     * @see #find(String, Sort, Map)
     * @see #find(String, Sort, Parameters)
     * @see #list(String, Sort, Parameters)
     * @see #stream(String, Sort, Parameters)
     */
    @GenerateBridge
    public static <T extends PanacheDynamoDbEntityBase> PanacheQuery<T> find(String query, Sort sort, Parameters params) {
        throw INSTANCE.implementationInjectionMissing();
    }

    /**
     * Find entities using a BSON query.
     *
     * @param query a {@link org.bson.Document} query
     * @return a new {@link PanacheQuery} instance for the given query
     * @see #find(Document, Document)
     * @see #list(Document)
     * @see #list(Document, Document)
     * @see #stream(Document)
     * @see #stream(Document, Document)
     */
    @GenerateBridge
    public static <T extends PanacheDynamoDbEntityBase> PanacheQuery<T> find(QueryConditional query) {
        throw INSTANCE.implementationInjectionMissing();
    }

    /**
     * Find all entities of this type.
     *
     * @return a new {@link PanacheQuery} instance to find all entities of this type.
     * @see #findAll(Sort)
     * @see #listAll()
     * @see #streamAll()
     */
    @GenerateBridge
    public static <T extends PanacheDynamoDbEntityBase> PanacheQuery<T> findAll() {
        throw INSTANCE.implementationInjectionMissing();
    }

    /**
     * Find all entities of this type, in the given order.
     *
     * @param sort the sort order to use
     * @return a new {@link PanacheQuery} instance to find all entities of this type.
     * @see #findAll()
     * @see #listAll(Sort)
     * @see #streamAll(Sort)
     */
    @GenerateBridge
    public static <T extends PanacheDynamoDbEntityBase> PanacheQuery<T> findAll(Sort sort) {
        throw INSTANCE.implementationInjectionMissing();
    }

    /**
     * Find entities matching a query, with optional indexed parameters.
     * This method is a shortcut for <code>find(query, params).list()</code>.
     *
     * @param query a {@link io.quarkus.amazon.dynamodb.panache query string}
     * @param params optional sequence of indexed parameters
     * @return a {@link List} containing all results, without paging
     * @see #list(String, Sort, Object...)
     * @see #list(String, Map)
     * @see #list(String, Parameters)
     * @see #find(String, Object...)
     * @see #stream(String, Object...)
     */
    @GenerateBridge
    public static <T extends PanacheDynamoDbEntityBase> List<T> list(String query, Object... params) {
        throw INSTANCE.implementationInjectionMissing();
    }

    /**
     * Find entities matching a query and the given sort options, with optional indexed parameters.
     * This method is a shortcut for <code>find(query, sort, params).list()</code>.
     *
     * @param query a {@link io.quarkus.amazon.dynamodb.panache query string}
     * @param sort the sort strategy to use
     * @param params optional sequence of indexed parameters
     * @return a {@link List} containing all results, without paging
     * @see #list(String, Object...)
     * @see #list(String, Sort, Map)
     * @see #list(String, Sort, Parameters)
     * @see #find(String, Sort, Object...)
     * @see #stream(String, Sort, Object...)
     */
    @GenerateBridge
    public static <T extends PanacheDynamoDbEntityBase> List<T> list(String query, Sort sort, Object... params) {
        throw INSTANCE.implementationInjectionMissing();
    }

    /**
     * Find entities matching a query, with named parameters.
     * This method is a shortcut for <code>find(query, params).list()</code>.
     *
     * @param query a {@link io.quarkus.amazon.dynamodb.panache query string}
     * @param params {@link Map} of named parameters
     * @return a {@link List} containing all results, without paging
     * @see #list(String, Sort, Map)
     * @see #list(String, Object...)
     * @see #list(String, Parameters)
     * @see #find(String, Map)
     * @see #stream(String, Map)
     */
    @GenerateBridge
    public static <T extends PanacheDynamoDbEntityBase> List<T> list(String query, Map<String, Object> params) {
        throw INSTANCE.implementationInjectionMissing();
    }

    /**
     * Find entities matching a query and the given sort options, with named parameters.
     * This method is a shortcut for <code>find(query, sort, params).list()</code>.
     *
     * @param query a {@link io.quarkus.amazon.dynamodb.panache query string}
     * @param sort the sort strategy to use
     * @param params {@link Map} of indexed parameters
     * @return a {@link List} containing all results, without paging
     * @see #list(String, Map)
     * @see #list(String, Sort, Object...)
     * @see #list(String, Sort, Parameters)
     * @see #find(String, Sort, Map)
     * @see #stream(String, Sort, Map)
     */
    @GenerateBridge
    public static <T extends PanacheDynamoDbEntityBase> List<T> list(String query, Sort sort, Map<String, Object> params) {
        throw INSTANCE.implementationInjectionMissing();
    }

    /**
     * Find entities matching a query, with named parameters.
     * This method is a shortcut for <code>find(query, params).list()</code>.
     *
     * @param query a {@link io.quarkus.amazon.dynamodb.panache query string}
     * @param params {@link Parameters} of named parameters
     * @return a {@link List} containing all results, without paging
     * @see #list(String, Sort, Parameters)
     * @see #list(String, Object...)
     * @see #list(String, Map)
     * @see #find(String, Parameters)
     * @see #stream(String, Parameters)
     */
    @GenerateBridge
    public static <T extends PanacheDynamoDbEntityBase> List<T> list(String query, Parameters params) {
        throw INSTANCE.implementationInjectionMissing();
    }

    /**
     * Find entities using a BSON query.
     * This method is a shortcut for <code>find(query).list()</code>.
     *
     * @param query a {@link org.bson.Document} query
     * @return a {@link List} containing all results, without paging
     * @see #find(Document)
     * @see #find(Document, Document)
     * @see #list(Document, Document)
     * @see #stream(Document)
     * @see #stream(Document, Document)
     */
    @GenerateBridge
    public static <T extends PanacheDynamoDbEntityBase> List<T> list(QueryConditional query) {
        throw INSTANCE.implementationInjectionMissing();
    }

    /**
     * Find all entities of this type.
     * This method is a shortcut for <code>findAll().list()</code>.
     *
     * @return a {@link List} containing all results, without paging
     * @see #listAll(Sort)
     * @see #findAll()
     * @see #streamAll()
     */
    @GenerateBridge
    public static <T extends PanacheDynamoDbEntityBase> List<T> listAll() {
        throw INSTANCE.implementationInjectionMissing();
    }

    /**
     * Find entities matching a query, with optional indexed parameters.
     * This method is a shortcut for <code>find(query, params).stream()</code>.
     *
     * @param query a {@link io.quarkus.amazon.dynamodb.panache query string}
     * @param params optional sequence of indexed parameters
     * @return a {@link Stream} containing all results, without paging
     * @see #stream(String, Sort, Object...)
     * @see #stream(String, Map)
     * @see #stream(String, Parameters)
     * @see #find(String, Object...)
     * @see #list(String, Object...)
     */
    @GenerateBridge
    public static <T extends PanacheDynamoDbEntityBase> Stream<T> stream(String query, Object... params) {
        throw INSTANCE.implementationInjectionMissing();
    }

    /**
     * Find entities matching a query, with named parameters.
     * This method is a shortcut for <code>find(query, params).stream()</code>.
     *
     * @param query a {@link io.quarkus.amazon.dynamodb.panache query string}
     * @param params {@link Map} of named parameters
     * @return a {@link Stream} containing all results, without paging
     * @see #stream(String, Sort, Map)
     * @see #stream(String, Object...)
     * @see #stream(String, Parameters)
     * @see #find(String, Map)
     * @see #list(String, Map)
     */
    @GenerateBridge
    public static <T extends PanacheDynamoDbEntityBase> Stream<T> stream(String query, Map<String, Object> params) {
        throw INSTANCE.implementationInjectionMissing();
    }

    /**
     * Find entities matching a query, with named parameters.
     * This method is a shortcut for <code>find(query, params).stream()</code>.
     *
     * @param query a {@link io.quarkus.amazon.dynamodb.panache query string}
     * @param params {@link Parameters} of named parameters
     * @return a {@link Stream} containing all results, without paging
     * @see #stream(String, Sort, Parameters)
     * @see #stream(String, Object...)
     * @see #stream(String, Map)
     * @see #find(String, Parameters)
     * @see #list(String, Parameters)
     */
    @GenerateBridge
    public static <T extends PanacheDynamoDbEntityBase> Stream<T> stream(String query, Parameters params) {
        throw INSTANCE.implementationInjectionMissing();
    }

    /**
     * Find entities using a BSON query.
     * This method is a shortcut for <code>find(query).stream()</code>.
     *
     * @param query a {@link org.bson.Document} query
     * @return a {@link Stream} containing all results, without paging
     * @see #find(Document)
     * @see #find(Document, Document)
     * @see #list(Document)
     * @see #list(Document, Document)
     * @see #stream(Document, Document)
     */
    @GenerateBridge
    public static <T extends PanacheDynamoDbEntityBase> Stream<T> stream(QueryConditional query) {
        throw INSTANCE.implementationInjectionMissing();
    }

    /**
     * Find all entities of this type.
     * This method is a shortcut for <code>findAll().stream()</code>.
     *
     * @return a {@link Stream} containing all results, without paging
     * @see #streamAll(Sort)
     * @see #findAll()
     * @see #listAll()
     */
    @GenerateBridge
    public static <T extends PanacheDynamoDbEntityBase> Stream<T> streamAll() {
        throw INSTANCE.implementationInjectionMissing();
    }

    /**
     * Insert all given entities.
     *
     * @param entities the entities to insert
     * @see #persist()
     * @see #persist(Stream)
     * @see #persist(Object,Object...)
     */
    @GenerateBridge(callSuperMethod = true)
    public static void persist(Iterable<?> entities) {
        INSTANCE.persist(entities);
    }

    /**
     * Insert all given entities.
     *
     * @param entities the entities to insert
     * @see #persist()
     * @see #persist(Iterable)
     * @see #persist(Object,Object...)
     */
    @GenerateBridge(callSuperMethod = true)
    public static void persist(Stream<?> entities) {
        INSTANCE.persist(entities);
    }

    /**
     * Insert all given entities.
     *
     * @param entities the entities to insert
     * @see #persist()
     * @see #persist(Stream)
     * @see #persist(Iterable)
     */
    @GenerateBridge(callSuperMethod = true)
    public static void persist(Object firstEntity, Object... entities) {
        INSTANCE.persist(firstEntity, entities);
    }

    /**
     * Update all given entities.
     *
     * @param entities the entities to update
     * @see #update()
     * @see #update(Stream)
     * @see #update(Object,Object...)
     */
    @GenerateBridge(callSuperMethod = true)
    public static void update(Iterable<?> entities) {
        INSTANCE.update(entities);
    }

    /**
     * Update all given entities.
     *
     * @param entities the entities to insert
     * @see #update()
     * @see #update(Iterable)
     * @see #update(Object,Object...)
     */
    @GenerateBridge(callSuperMethod = true)
    public static void update(Stream<?> entities) {
        INSTANCE.update(entities);
    }

    /**
     * Update all given entities.
     *
     * @param entities the entities to update
     * @see #update()
     * @see #update(Stream)
     * @see #update(Iterable)
     */
    @GenerateBridge(callSuperMethod = true)
    public static void update(Object firstEntity, Object... entities) {
        INSTANCE.update(firstEntity, entities);
    }

    /**
     * Persist all given entities or update them if they already exist.
     *
     * @param entities the entities to update
     * @see #persistOrUpdate()
     * @see #persistOrUpdate(Stream)
     * @see #persistOrUpdate(Object,Object...)
     */
    @GenerateBridge(callSuperMethod = true)
    public static void persistOrUpdate(Iterable<?> entities) {
        INSTANCE.persistOrUpdate(entities);
    }

    /**
     * Persist all given entities.
     *
     * @param entities the entities to insert
     * @see #persistOrUpdate()
     * @see #persistOrUpdate(Iterable)
     * @see #persistOrUpdate(Object,Object...)
     */
    @GenerateBridge(callSuperMethod = true)
    public static void persistOrUpdate(Stream<?> entities) {
        INSTANCE.persistOrUpdate(entities);
    }

    /**
     * Persist all given entities.
     *
     * @param entities the entities to update
     * @see #persistOrUpdate()
     * @see #persistOrUpdate(Stream)
     * @see #persistOrUpdate(Iterable)
     */
    @GenerateBridge(callSuperMethod = true)
    public static void persistOrUpdate(Object firstEntity, Object... entities) {
        INSTANCE.persistOrUpdate(firstEntity, entities);
    }

    /**
     * Allow to access the underlying DynamoDB table.
     */
    @GenerateBridge
    public static <T extends PanacheDynamoDbEntityBase> DynamoDbTable<T> table() {
        throw INSTANCE.implementationInjectionMissing();
    }

    /**
     * Allow to access the underlying DynamoDB client.
     */
    @GenerateBridge
    public static DynamoDbEnhancedClient dynamoDbEnhancedClient() {
        throw INSTANCE.implementationInjectionMissing();
    }
}
