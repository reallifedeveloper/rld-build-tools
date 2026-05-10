package com.reallifedeveloper.tools.test.database.csv;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import com.reallifedeveloper.tools.test.database.CrudRepositoryWriter;
import com.reallifedeveloper.tools.test.database.CrudRepositoryWriter.DbTableField;
import com.reallifedeveloper.tools.test.database.CrudRepositoryWriter.DbTableRow;
import com.reallifedeveloper.tools.test.database.dbunit.DbUnitFlatXmlReaderTest;
import com.reallifedeveloper.tools.test.database.inmemory.InMemoryJpaRepository;

/**
 * Special test cases for {@link CrudRepositoryWriter} -- the majority of the class is tested by {@link CsvDatabaseReaderTest} and
 * {@link DbUnitFlatXmlReaderTest}.
 *
 * @author RealLifeDeveloper
 */
@SuppressWarnings("NullAway")
public class CrudRepositoryWriterTest {

    private CrudRepositoryWriter writer = new CrudRepositoryWriter();

    @Test
    public void writeEntityWithOneToOneWihoutMappebByOrJoinColumnThrowsException() throws Exception {
        InMemoryJpaRepository<EntityWithIncorrectMappings, Long> repository = new InMemoryJpaRepository<>();
        DbTableRow tableRow = new TestDbTableRow(new DbTableField("id", "1"), new DbTableField("s", null));
        writer.writeEntity(tableRow, EntityWithIncorrectMappings.class, EntityWithIncorrectMappings.class, repository, "table");
        Exception e = assertThrows(IllegalStateException.class, () -> writer.fillReferencesBetweenEntities());
        assertEquals("OneToOne field " + EntityWithIncorrectMappings.class.getName() + ".s has no mappedBy and no JoinColumn annotation",
                e.getMessage());
    }

    @Test
    public void writeEmbeddableThrowsException() throws Exception {
        Exception e = assertThrows(UnsupportedOperationException.class,
                () -> writer.writeEntity(null, null, TestEmbeddable.class, null, null));
        assertEquals("writeEmbeddable not yet implemented", e.getMessage());
    }

    @Entity
    @Table(name = "table")
    @AllArgsConstructor
    @NoArgsConstructor(access = AccessLevel.PACKAGE)
    private static class EntityWithIncorrectMappings {
        @Id
        private Long id;
        @OneToOne
        @SuppressWarnings("UnusedVariable")
        private String s;
    }

    @Embeddable
    private static class TestEmbeddable {
    }

    private static class TestDbTableRow implements DbTableRow {

        private List<DbTableField> columns;

        TestDbTableRow(DbTableField... columns) {
            this.columns = Arrays.asList(columns);
        }

        @Override
        public List<DbTableField> columns() {
            return columns;
        }

    }
}
