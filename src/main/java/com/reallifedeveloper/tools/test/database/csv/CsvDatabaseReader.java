package com.reallifedeveloper.tools.test.database.csv;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.repository.CrudRepository;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;
import lombok.ToString;

import com.reallifedeveloper.tools.test.TestUtil;
import com.reallifedeveloper.tools.test.database.CrudRepositoryWriter;
import com.reallifedeveloper.tools.test.database.CrudRepositoryWriter.DbTableField;
import com.reallifedeveloper.tools.test.database.CrudRepositoryWriter.DbTableRow;

/**
 * A class to read a CSV file and populate a Spring Data {@code CrudRepository} using the information in the file.
 * <p>
 * This is useful for testing in-memory repositories using the same test cases as for real repository implementations, and also for
 * populating in-memory repositories for testing services, without having to use a real database.
 * <p>
 * The file is assumed to have a header containing the names of the database columns to populate, followed by the data rows. An example:
 *
 * <pre>
 *     id;name
 *     1;foo
 *     2;bar
 * </pre>
 * <p>
 *
 * @author RealLifeDeveloper
 */
@Getter
public class CsvDatabaseReader {

    private static final Logger LOG = LoggerFactory.getLogger(CsvDatabaseReader.class);

    private final char csvSeparatorCharacter;
    private final int csvSkipLines;

    private final CrudRepositoryWriter crudRepositoryWriter = new CrudRepositoryWriter();

    /**
     * Creates a new {@code CsvDatabaseReader} with the given configuration.
     *
     * @param csvSeparatorCharacter the separator character to use when reading the file, normally ',' or ';'
     * @param csvSkipLines          the number of lines to skip at the beginning of the file
     */
    public CsvDatabaseReader(char csvSeparatorCharacter, int csvSkipLines) {
        this.csvSeparatorCharacter = csvSeparatorCharacter;
        this.csvSkipLines = csvSkipLines;
    }

    /**
     * Reads a CSV file from the named resource, populating the given repository with entities of the given type.
     *
     * @param resourceName         the classpath resource containing a CSV file
     * @param repository           the repository to populate with the entities from the CSV file
     * @param repositoryEntityType the class object representing {@code <T>}, i.e., the class of entities in the repository
     * @param entityType           the class object representing {@code <E>}, i.e., the class of entity being read
     * @param tableName            the name of the database table to use; may be either the table associated with the entity, or a join
     *                             table
     * @param <T>                  the type of entities in the repository
     * @param <E>                  the type of entity being read
     * @param <ID>                 the type of the primary key of the entities in the repository
     *
     * @throws IOException  if reading the file failed
     * @throws CsvException if parsing the file failed
     */
    public <T, E, ID extends Serializable> void read(String resourceName, CrudRepository<T, ID> repository, Class<T> repositoryEntityType,
            @Nullable Class<E> entityType, String tableName) throws IOException, CsvException {
        try (InputStream in = CsvDatabaseReader.class.getResourceAsStream(resourceName)) {
            if (in == null) {
                throw new FileNotFoundException(resourceName);
            }
            LOG.info("Reading from {}", resourceName.replaceAll("[\r\n]", ""));
            try (Reader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                CSVParser parser = new CSVParserBuilder().withSeparator(csvSeparatorCharacter).build();
                try (CSVReader csvReader = new CSVReaderBuilder(reader).withSkipLines(csvSkipLines).withCSVParser(parser).build()) {
                    String[] header = csvReader.readNext();
                    String[] row;
                    while ((row = csvReader.readNext()) != null) {
                        @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
                        DbTableRow tableRow = new CsvTableRow(header, row);
                        if (crudRepositoryWriter.writeEntity(tableRow, repositoryEntityType, entityType, repository, tableName)) {
                            continue;
                        }
                        crudRepositoryWriter.addEntitiesFromJoinTable(tableRow, tableName);
                    }
                }
                crudRepositoryWriter.fillReferencesBetweenEntities();
            }
        } catch (ReflectiveOperationException | SecurityException e) {
            throw new IllegalStateException("Unexpected problem reading CSV file from '" + resourceName + "'", e);
        }
    }

    @ToString
    private static class CsvTableRow implements DbTableRow {

        private final List<String> header;
        private final List<String> row;

        @SuppressWarnings("PMD.UseVarargs")
        @SuppressFBWarnings(value = "CT_CONSTRUCTOR_THROW", justification = "Private class")
        /* package-private */ CsvTableRow(String[] header, String[] row) {
            if (header == null || row == null || header.length == 0 || row.length == 0) {
                throw new IllegalArgumentException(
                        "Arguments must not be null or empty: header=" + TestUtil.asList(header) + ", row=" + TestUtil.asList(row));
            }
            if (header.length != row.length) {
                throw new IllegalArgumentException(
                        "header and row should be of same length: header=" + Arrays.asList(header) + ", row=" + Arrays.asList(row));
            }
            this.header = Arrays.asList(header);
            this.row = Arrays.asList(row);
        }

        @Override
        public List<DbTableField> columns() {
            List<DbTableField> columns = new ArrayList<>();
            for (int i = 0; i < row.size(); i++) {
                columns.add(new DbTableField(header.get(i), row.get(i)));
            }
            return columns;
        }

    }
}
