package com.reallifedeveloper.tools.test.database.dbunit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.reallifedeveloper.tools.test.LogbackTestUtil;

public class DbUnitDtdGeneratorTest {

    private static final String EXPECTED_DTD = """
            <!ELEMENT dataset (
                DBUNITTESTENTITY*,
                DBUNITTESTENTITY_TESTENTITY*,
                TEST_ENTITY*)>

            <!ELEMENT DBUNITTESTENTITY EMPTY>
            <!ATTLIST DBUNITTESTENTITY
                ID CDATA #REQUIRED
                B CDATA #IMPLIED
                BD CDATA #IMPLIED
                BI CDATA #IMPLIED
                BOOL CDATA #IMPLIED
                C CDATA #IMPLIED
                D CDATA #IMPLIED
                DATE CDATA #IMPLIED
                F CDATA #IMPLIED
                L CDATA #IMPLIED
                S CDATA #IMPLIED
                STRING CDATA #IMPLIED
                TESTENUM CDATA #IMPLIED
                TESTENTITY_ID CDATA #IMPLIED
            >

            <!ELEMENT DBUNITTESTENTITY_TESTENTITY EMPTY>
            <!ATTLIST DBUNITTESTENTITY_TESTENTITY
                DBUNIT_TEST_ENTITY_ID CDATA #REQUIRED
                TEST_ENTITY_ID CDATA #REQUIRED
            >

            <!ELEMENT TEST_ENTITY EMPTY>
            <!ATTLIST TEST_ENTITY
                ID CDATA #REQUIRED
                NAME CDATA #IMPLIED
            >

            """;

    @Test
    public void generateDtd() throws Exception {
        ApplicationContext applicationContext = new AnnotationConfigApplicationContext(DatabaseTestConfiguration.class);
        DbUnitDtdGenerator dtdGenerator = new DbUnitDtdGenerator(applicationContext);
        String dtd = dtdGenerator.generateDtd();
        assertDtd(EXPECTED_DTD, dtd);
    }

    @Test
    public void generateDtdWithoutDataTypeFactory() throws Exception {
        ApplicationContext applicationContext = new ClassPathXmlApplicationContext(
                "/META-INF/spring-context-rld-build-tools-test-no-datatypefactory.xml");
        DbUnitDtdGenerator dtdGenerator = new DbUnitDtdGenerator(applicationContext);
        String dtd = dtdGenerator.generateDtd();
        assertDtd(EXPECTED_DTD, dtd);
    }

    @Test
    public void mainWithConfigurationFile() throws Exception {
        LogbackTestUtil.clearLoggingEvents();
        DbUnitDtdGenerator.main("/META-INF/spring-context-rld-build-tools-test.xml");
        assertDtd(EXPECTED_DTD, LogbackTestUtil.getLoggingEvents().get(0).getFormattedMessage());
    }

    @Test
    public void mainWithConfigurationClass() throws Exception {
        LogbackTestUtil.clearLoggingEvents();
        DbUnitDtdGenerator.main("com.reallifedeveloper.tools.test.database.dbunit.DatabaseTestConfiguration");
        assertDtd(EXPECTED_DTD, LogbackTestUtil.getLoggingEvents().get(0).getFormattedMessage());
    }

    /**
     * For some reason, the order of the attribues in thd DTD differs depending on if the application context is created using
     * an @Configuration class or an XML file. We therefore sort the lines of the strings and assert that those are equal. This may not be
     * perfect since some differences in order can also mean semantic differences, but I think it is good enough.
     *
     * @param expectedDtd the expected DTD
     * @param actualDtd   the actual DTD
     */
    private static void assertDtd(String expectedDtd, String actualDtd) {
        List<String> sortedExpectedDtd = splitAndSortString(expectedDtd);
        List<String> sortedActualDtd = splitAndSortString(actualDtd);
        assertEquals(sortedExpectedDtd, sortedActualDtd);
    }

    private static List<String> splitAndSortString(String s) {
        return Arrays.asList(s.split("\n")).stream().sorted().toList();
    }

    @Test
    @SuppressWarnings("NullAway")
    public void mainIncorrectArgument() throws Exception {
        Exception e = assertThrows(BeanDefinitionStoreException.class, () -> DbUnitDtdGenerator.main("foo"));
        assertEquals(FileNotFoundException.class, e.getCause().getClass(), "Wrong root cause: ");
    }

    @Test
    public void mainNoArguments() throws Exception {
        Exception e = assertThrows(IllegalArgumentException.class, () -> DbUnitDtdGenerator.main());
        assertEquals(usage(), e.getMessage());
    }

    @Test
    public void mainTwoArguments() throws Exception {
        Exception e = assertThrows(IllegalArgumentException.class, () -> DbUnitDtdGenerator.main("foo", "bar"));
        assertEquals(usage(), e.getMessage());
    }

    private static String usage() {
        return "Usage: java com.reallifedeveloper.tools.test.database.dbunit.DbUnitDtdGenerator "
                + "<classpath Spring config class or XML file>";
    }
}
