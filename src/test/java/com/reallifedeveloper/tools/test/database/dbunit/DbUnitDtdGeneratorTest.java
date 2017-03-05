package com.reallifedeveloper.tools.test.database.dbunit;

import java.io.FileNotFoundException;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.BeanDefinitionStoreException;

public class DbUnitDtdGeneratorTest {

    private static final String EXPECTED_DTD = "<!ELEMENT dataset (\n"
            + "    DBUNITTESTENTITY*,\n"
            + "    DBUNITTESTENTITY_TESTENTITY*,\n"
            + "    TEST_ENTITY*)>\n"
            + "\n"
            + "<!ELEMENT DBUNITTESTENTITY EMPTY>\n"
            + "<!ATTLIST DBUNITTESTENTITY\n"
            + "    ID CDATA #REQUIRED\n"
            + "    B CDATA #IMPLIED\n"
            + "    BD CDATA #IMPLIED\n"
            + "    BOOL CDATA #IMPLIED\n"
            + "    C CDATA #IMPLIED\n"
            + "    D CDATA #IMPLIED\n"
            + "    DATE CDATA #IMPLIED\n"
            + "    F CDATA #IMPLIED\n"
            + "    GEOMETRY CDATA #IMPLIED\n"
            + "    L CDATA #IMPLIED\n"
            + "    S CDATA #IMPLIED\n"
            + "    STRING CDATA #IMPLIED\n"
            + "    TESTENUM CDATA #IMPLIED\n"
            + "    TESTENTITY_ID CDATA #IMPLIED\n"
            + ">\n\n"
            + "<!ELEMENT DBUNITTESTENTITY_TESTENTITY EMPTY>\n"
            + "<!ATTLIST DBUNITTESTENTITY_TESTENTITY\n"
            + "    DBUNIT_TEST_ENTITY_ID CDATA #REQUIRED\n"
            + "    TEST_ENTITY_ID CDATA #REQUIRED\n"
            + ">\n\n"
            + "<!ELEMENT TEST_ENTITY EMPTY>\n"
            + "<!ATTLIST TEST_ENTITY\n"
            + "    ID CDATA #REQUIRED\n"
            + "    NAME CDATA #IMPLIED\n"
            + ">\n\n";

    @Test
    public void generateDtd() throws Exception {
        DbUnitDtdGenerator dtdGenerator = new DbUnitDtdGenerator("/META-INF/spring-context-rld-build-tools-test.xml");
        String dtd = dtdGenerator.generateDtd();
        Assert.assertEquals("Wrong DTD: ", EXPECTED_DTD, dtd);
    }

    @Test
    public void generateDtdWithoutDataTypeFactory() throws Exception {
        DbUnitDtdGenerator dtdGenerator =
                new DbUnitDtdGenerator("/META-INF/spring-context-rld-build-tools-test-no-datatypefactory.xml");
        String dtd = dtdGenerator.generateDtd();
        Assert.assertEquals("Wrong DTD: ", EXPECTED_DTD, dtd);
    }

    @Test
    public void main() throws Exception {
        // Test that we don't get an exception.
        DbUnitDtdGenerator.main("/META-INF/spring-context-rld-build-tools-test.xml");
    }

    @Test
    public void mainIncorrectArgument() throws Exception {
        try {
            DbUnitDtdGenerator.main("foo");
            Assert.fail("Expected BeanDefinitionStoreException to be thrown");
        } catch (BeanDefinitionStoreException e) {
            Assert.assertEquals("Wrong root cause: ", FileNotFoundException.class, e.getCause().getClass());
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void mainNoArguments() throws Exception {
        DbUnitDtdGenerator.main();
    }

    @Test(expected = IllegalArgumentException.class)
    public void mainTwoArguments() throws Exception {
        DbUnitDtdGenerator.main("foo", "bar");
    }
}
