package com.reallifedeveloper.tools.test.database.dbunit;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.Table;

import com.reallifedeveloper.tools.test.TestUtil;

/**
 * A class to read a DBUnit flat XML dataset file and populate a {@code JpaRepository} using the information in the file.
 * <p>
 * This is useful for testing in-memory repositories using the same test cases as for real repository implementations, and also for
 * populating in-memory repositories for testing services, without having to use a real database.
 * <p>
 * TODO: The current implementation only has basic support for "to many" associations (there must be a &amp;JoinTable annotation on a field,
 * with &amp;JoinColumn annotations), and for enums (an enum must be stored as a string).
 *
 * @author RealLifeDeveloper
 */
@SuppressWarnings("PMD")
@SuppressFBWarnings(value = "IMPROPER_UNICODE", justification = "equalsIgnoreCase only being used to compare table, column or field names")
public final class DbUnitFlatXmlReader {

    private static final Logger LOG = LoggerFactory.getLogger(DbUnitFlatXmlReader.class);

    private final DocumentBuilder documentBuilder;
    private final Set<Class<?>> classes = new HashSet<>();
    private final List<Object> entities = new ArrayList<>();

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
            // Configuration to make the parser safe from XXE attacks while still allowing DTDs.
            // See https://community.veracode.com/s/article/Java-Remediation-Guidance-for-XXE
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
     * @param resourceName   the classpath resource containing a DBUnit flat XML document
     * @param repository     the repository to populate with the entities from the XML document
     * @param entityType     the entity class to read
     * @param primaryKeyType the type of primary key the entities use
     * @param <T>            the type of entity to read
     * @param <ID>           the type of the primary key of the entities
     *
     * @throws IOException  if reading the file failed
     * @throws SAXException if parsing the file failed
     */
    @SuppressFBWarnings(value = "XXE_DOCUMENT", justification = "XML parser hardened as much as possible, see constructor")
    public <T, ID extends Serializable> void read(String resourceName, JpaRepository<T, ID> repository, Class<T> entityType,
            Class<ID> primaryKeyType) throws IOException, SAXException {
        try (InputStream in = DbUnitFlatXmlReader.class.getResourceAsStream(resourceName)) {
            if (in == null) {
                throw new FileNotFoundException(resourceName);
            }
            Document doc = documentBuilder.parse(in);
            Element dataset = doc.getDocumentElement();
            NodeList tableRows = dataset.getChildNodes();

            LOG.info("Reading from {}", resourceName.replaceAll("[\r\n]", ""));
            for (int i = 0; i < tableRows.getLength(); i++) {
                Node tableRowNode = (@NonNull Node) tableRows.item(i);
                if (tableRowNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element tableRow = (Element) tableRowNode;
                    String tableName = tableRow.getNodeName();
                    if (tableName.equalsIgnoreCase(getTableName(entityType))) {
                        handleTableRow(tableRow, entityType, primaryKeyType, repository);
                    } else {
                        handlePotentialJoinTable(tableRowNode, tableName);
                    }
                }
            }
        } catch (ReflectiveOperationException | SecurityException e) {
            throw new IllegalStateException("Unexpected problem reading XML file from '" + resourceName + "'", e);
        }
    }

    private <T, ID extends Serializable> void handleTableRow(Element tableRow, Class<T> entityType, Class<ID> primaryKeyType,
            JpaRepository<T, ID> repository) throws ReflectiveOperationException {
        T entity = createEntity(entityType);
        NamedNodeMap attributes = (@NonNull NamedNodeMap) tableRow.getAttributes();
        for (int j = 0; j < attributes.getLength(); j++) {
            Node attribute = (@NonNull Node) attributes.item(j);
            String fieldName = getFieldName(attribute.getNodeName(), entityType);
            String attributeValue = attribute.getNodeValue();
            setField(entity, fieldName, attributeValue, primaryKeyType);
        }
        entity = repository.save(entity);
        entities.add(entity);
        classes.add(entity.getClass());
    }

    private void handlePotentialJoinTable(Node tableRow, String tableName)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        joinTableField(tableName).ifPresent(joinTableField -> {
            joinTableField.setAccessible(true);
            ParameterizedType parameterizedType = (ParameterizedType) joinTableField.getGenericType();
            Class<?> targetType = (Class<?>) parameterizedType.getActualTypeArguments()[0];
            JoinTable joinTable = joinTableField.getAnnotation(JoinTable.class);
            assert joinTable != null : "JoinTable annotation should be present when the joinTableField method returns a non-empty value";
            for (JoinColumn joinColumn : joinTable.joinColumns()) {
                for (JoinColumn inverseJoinColumn : joinTable.inverseJoinColumns()) {
                    addEntityFromJoinTable(tableRow, joinTableField, targetType, joinColumn, inverseJoinColumn);
                }
            }
        });
    }

    private Optional<Field> joinTableField(String tableName) {
        for (Class<?> c : classes) {
            for (Field field : c.getDeclaredFields()) {
                JoinTable joinTable = field.getAnnotation(JoinTable.class);
                if (joinTable != null && tableName.equalsIgnoreCase(joinTable.name())) {
                    return Optional.of(field);
                }
            }
        }
        return Optional.empty();
    }

    private void addEntityFromJoinTable(Node tableRow, Field joinTableField, Class<?> targetType, JoinColumn joinColumn,
            JoinColumn inverseJoinColumn) {
        NamedNodeMap attributes = tableRow.getAttributes();
        String lhsPrimaryKey = null;
        String rhsPrimaryKey = null;
        for (int j = 0; j < attributes.getLength(); j++) {
            Node attribute = attributes.item(j);
            if (attribute.getNodeName().equalsIgnoreCase(joinColumn.name())) {
                lhsPrimaryKey = attribute.getNodeValue();
            } else if (attribute.getNodeName().equalsIgnoreCase(inverseJoinColumn.name())) {
                rhsPrimaryKey = attribute.getNodeValue();
            }
        }
        Object lhs = findEntity(lhsPrimaryKey, joinTableField.getDeclaringClass());
        Object rhs = findEntity(rhsPrimaryKey, targetType);
        try {
            Method add = joinTableField.getType().getMethod("add", Object.class);
            add.invoke(joinTableField.get(lhs), rhs);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Method 'add' not found -- @JoinTable annotation should be on a Collection", e);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("Unexpected problem", e);
        }
    }

    private <T> String getTableName(Class<T> entityType) {
        Table table = entityType.getAnnotation(Table.class);
        if (table == null) {
            return entityType.getSimpleName();
        } else {
            return table.name();
        }
    }

    private <T> String getFieldName(String attributeName, Class<T> entityType) {
        for (Field field : entityType.getDeclaredFields()) {
            if (checkFieldName(attributeName, field)) {
                return field.getName();
            }
        }
        if (entityType.getSuperclass() == null) {
            throw new IllegalArgumentException("Cannot find any field matching attribute '" + attributeName + "' for " + entityType);
        } else {
            return getFieldName(attributeName, entityType.getSuperclass());
        }
    }

    private boolean checkFieldName(String attributeName, Field field) {
        Column column = field.getAnnotation(Column.class);
        if (column == null || column.name() == null) {
            JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
            if (joinColumn == null || joinColumn.name() == null) {
                return field.getName().equalsIgnoreCase(attributeName);
            } else {
                return joinColumn.name().equalsIgnoreCase(attributeName);
            }
        } else {
            return column.name().equalsIgnoreCase(attributeName);
        }
    }

    private <T> T createEntity(Class<T> entityType) throws ReflectiveOperationException {
        Constructor<T> constructor = entityType.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }

    private <T, ID> void setField(T entity, String fieldName, @Nullable String attributeValue, Class<ID> primaryKeyType)
            throws ReflectiveOperationException {
        Field field = getField(entity, fieldName);
        field.setAccessible(true);
        Object fieldValue = createObjectFromString(attributeValue, field, primaryKeyType);
        field.set(entity, fieldValue);
    }

    private Field getField(Object entity, String fieldName) throws NoSuchFieldException {
        Class<?> entityType = entity.getClass();
        while (entityType != null) {
            for (Field field : entityType.getDeclaredFields()) {
                if (field.getName().equalsIgnoreCase(fieldName)) {
                    return field;
                }
            }
            entityType = entityType.getSuperclass();
        }
        throw new NoSuchFieldException(fieldName);
    }

    private Object createObjectFromString(String s, Field field, Class<?> primaryKeyType) {
        Class<?> type;
        if (field.getAnnotation(Id.class) != null) {
            type = primaryKeyType;
        } else {
            type = field.getType();
        }
        return createObjectFromString(s, type);
    }

    private Object createObjectFromString(String s, Class<?> type) {
        if (type == Byte.class) {
            return Byte.parseByte(s);
        } else if (type == Short.class) {
            return Short.parseShort(s);
        } else if (type == Integer.class) {
            return Integer.parseInt(s);
        } else if (type == Long.class) {
            return Long.parseLong(s);
        } else if (type == Float.class) {
            return Float.parseFloat(s);
        } else if (type == Double.class) {
            return Double.parseDouble(s);
        } else if (type == Boolean.class) {
            return Boolean.parseBoolean(s);
        } else if (type == Character.class) {
            return s.charAt(0);
        } else if (type == String.class) {
            return s;
        } else if (type == Date.class) {
            return TestUtil.parseDate(s);
        } else if (type == BigDecimal.class) {
            return new BigDecimal(s);
        } else if (type == BigInteger.class) {
            return new BigInteger(s);
        } else {
            return findEntity(s, type);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private Object findEntity(String strId, Class<?> entityType) {
        if (entityType.isEnum()) {
            Class<? extends Enum> enumType = (Class<? extends Enum>) entityType;
            return Enum.valueOf(enumType, strId);
        }
        for (Object entity : entities) {
            if (entity.getClass().equals(entityType)) {
                Field idField = getIdField(entity);
                idField.setAccessible(true);
                try {
                    Object id = idField.get(entity);
                    if (id != null && id.equals(createObjectFromString(strId, id.getClass()))) {
                        return entity;
                    }
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException("Unexpected problem looking up entity of " + entityType + " with primary key " + strId,
                            e);
                }
            }
        }
        throw new IllegalArgumentException("Entity of " + entityType + " with primary key " + strId + " not found");
    }

    private Field getIdField(Object entity) {
        Class<?> entityType = entity.getClass();
        while (entityType != null) {
            for (Field field : entityType.getDeclaredFields()) {
                if (field.getDeclaredAnnotation(Id.class) != null) {
                    return field;
                }
            }
            entityType = entityType.getSuperclass();
        }
        throw new IllegalStateException("Id field not found for entity " + entity);
    }

}
