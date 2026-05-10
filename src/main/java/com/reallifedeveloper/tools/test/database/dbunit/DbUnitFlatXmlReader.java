package com.reallifedeveloper.tools.test.database.dbunit;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.repository.CrudRepository;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import com.reallifedeveloper.tools.test.database.CrudRepositoryWriter;
import com.reallifedeveloper.tools.test.database.CrudRepositoryWriter.DbTableField;
import com.reallifedeveloper.tools.test.database.CrudRepositoryWriter.DbTableRow;

/**
 * A class to read a DBUnit flat XML dataset file and populate a Spring Data {@code CrudRepository} using the information in the file.
 * <p>
 * This is useful for testing in-memory repositories using the same test cases as for real repository implementations, and also for
 * populating in-memory repositories for testing services, without having to use a real database.
 *
 * @author RealLifeDeveloper
 */
@SuppressFBWarnings(value = "XXE_DOCUMENT", justification = "XML parser hardened as much as possible, see constructor")
public final class DbUnitFlatXmlReader {

    private static final Logger LOG = LoggerFactory.getLogger(DbUnitFlatXmlReader.class);

    private final DocumentBuilder documentBuilder;
    private final CrudRepositoryWriter crudRepositoryWriter = new CrudRepositoryWriter();

    /**
     * Creates a new {@code DbUnitFlatXmlReader}.
     */
    public DbUnitFlatXmlReader() {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setValidating(false);
            dbf.setNamespaceAware(true);
            dbf.setFeature("http://xml.org/sax/features/namespaces", false);
            dbf.setFeature("http://xml.org/sax/features/validation", false);
            dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
            dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            // Configuration to make the parser safe from XXE attacks while still allowing
            // DTDs.
            // See
            // https://community.veracode.com/s/article/Java-Remediation-Guidance-for-XXE
            dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
            dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            dbf.setXIncludeAware(false);
            dbf.setExpandEntityReferences(false);
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            documentBuilder = dbf.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException("Unexpected problem creating XML parser", e);
        }
    }

    /**
     * Reads a DBUnit flat XML file from the named resource, populating the given repository with entities of the given type.
     *
     * @param resourceName         the classpath resource containing a DBUnit flat XML document
     * @param repository           the repository to populate with the entities from the XML document
     * @param repositoryEntityType the class object representing {@code <T>}, i.e., the class of the entities in the repository
     * @param entityType           the class object representing {@code <E>}, i.e., the class of the eneity being created
     *
     * @param <T>                  the type of entities in the repository
     * @param <E>                  the type of entity being created
     * @param <ID>                 the type of the primary key of the entities in the repository
     *
     * @throws IOException  if reading the file failed
     * @throws SAXException if parsing the file failed
     */

    public <T, E, ID extends Serializable> void read(String resourceName, CrudRepository<T, ID> repository, Class<T> repositoryEntityType,
            Class<E> entityType) throws IOException, SAXException {
        try (InputStream in = DbUnitFlatXmlReader.class.getResourceAsStream(resourceName)) {
            if (in == null) {
                throw new FileNotFoundException(resourceName);
            }
            Document doc = documentBuilder.parse(in);
            Element dataset = doc.getDocumentElement();
            NodeList tableRows = dataset.getChildNodes();

            LOG.info("Reading from {}", resourceName.replaceAll("[\r\n]", ""));
            for (int i = 0; i < tableRows.getLength(); i++) {
                Node tableRowNode = tableRows.item(i);
                if (tableRowNode.getNodeType() == Node.ELEMENT_NODE) {
                    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
                    DbTableRow tableRow = new NodeTableRow(tableRowNode);
                    String tableName = tableRowNode.getNodeName();
                    if (crudRepositoryWriter.writeEntity(tableRow, repositoryEntityType, entityType, repository, tableName)) {
                        continue;
                    }
                    crudRepositoryWriter.addEntitiesFromJoinTable(tableRow, tableName);
                }
            }
            crudRepositoryWriter.fillReferencesBetweenEntities();
        } catch (ReflectiveOperationException | SecurityException e) {
            throw new IllegalStateException("Unexpected problem reading XML file from '" + resourceName + "'", e);
        }
    }

    /**
     * An implementation of the {@link DbTableRow} interface that gets its data from an * XML {@link Node} representing a single row in a
     * DBUnit XML dataset.
     * <p>
     * For example:
     *
     * <pre>
     * {@code
     * <dataset>
     *     ...
     *     <test_entity id="42" name="foo" />
     *     ...
     * </dataset>
     * }
     * </pre>
     */
    private static class NodeTableRow implements DbTableRow {

        private final Node tableRowNode;

        /* package-private */ NodeTableRow(Node tableRowNode) {
            this.tableRowNode = tableRowNode;
        }

        @Override
        public List<DbTableField> columns() {
            List<DbTableField> columns = new ArrayList<>();
            NamedNodeMap attributes = tableRowNode.getAttributes();
            for (int j = 0; j < attributes.getLength(); j++) {
                Node attribute = attributes.item(j);
                columns.add(new DbTableField(attribute.getNodeName(), attribute.getNodeValue()));
            }
            return columns;
        }
    }
}
