package io.quarkus.amazon.dynamodb.panache;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;
import java.util.UUID;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class DynamoDbEntityTest {
    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(
                            DynamoDbEntityEntity.class,
                            DynamoDbEntityRepository.class)
                    .addAsResource("application.properties"));

    @Inject
    DynamoDbEntityRepository dynamoDbEntityRepository;

    @Test
    public void testDynamoDbEntity() {

        DynamoDbEntityEntity.table().createTable();

        assertEquals("DynamoDbEntityEntity", DynamoDbEntityEntity.table().describeTable().table().tableName());
        assertEquals("DynamoDbEntityEntity", dynamoDbEntityRepository.table().describeTable().table().tableName());

        String partitionKeyAsString = UUID.randomUUID().toString();
        DynamoDbEntityEntity entity = new DynamoDbEntityEntity();
        entity.setId(partitionKeyAsString);
        entity.persist();

        assertEquals(partitionKeyAsString,
                ((DynamoDbEntityEntity) DynamoDbEntityEntity.findById(partitionKeyAsString)).getId());

        assertEquals(1, DynamoDbEntityEntity.listAll().size());
        assertEquals(1, DynamoDbEntityEntity.list("id", partitionKeyAsString).size());

        entity.delete();
        assertEquals(Optional.empty(), DynamoDbEntityEntity.findByIdOptional(partitionKeyAsString));

        // todo
        assertEquals(0, DynamoDbEntityEntity.list("id = ?1", partitionKeyAsString).size());
    }
}
