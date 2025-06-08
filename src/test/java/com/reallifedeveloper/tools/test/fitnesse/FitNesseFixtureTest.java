package com.reallifedeveloper.tools.test.fitnesse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Calendar;
import java.util.Date;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

public class FitNesseFixtureTest extends BaseFitNesseFixture {

    public FitNesseFixtureTest() {
        super("/META-INF/spring-context-rld-build-tools-test.xml");
    }

    @Test
    public void comment() {
        assertNull(getComment(), "Comment should be null before it has been set");
        setComment("foo");
        assertEquals("foo", getComment(), "Wrong comment: ");
    }

    @Test
    public void getExistingBean() {
        assertNotNull(getBean(DataSource.class), "DataSource should not be null");
    }

    @Test
    public void getNonExistingBean() {
        assertThrows(NoSuchBeanDefinitionException.class, () -> getBean(Boolean.class));
    }

    @Test
    public void testLogger() {
        Logger logger = logger();
        assertNotNull(logger, "Logger should not be null");
        logger.info("Logger method is working");
    }

    @Test
    public void testResetApplicationContext() {
        assertNotNull(getBean(DataSource.class), "DataSource should not be null before reset");
        resetApplicationContext();
        assertNotNull(getBean(DataSource.class), "DataSource should not be null after reset");
    }

    @Test
    public void parseCorrectDate() {
        Date date = parseDate("2016-03-26");
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        assertEquals(2016, calendar.get(Calendar.YEAR), "Wrong year: ");
        assertEquals(3 - 1, calendar.get(Calendar.MONTH), "Wrong month: ");
        assertEquals(26, calendar.get(Calendar.DAY_OF_MONTH), "Wrong day: ");
    }

    @Test
    public void parseIncorrectDate() {
        assertThrows(IllegalArgumentException.class, () -> parseDate("foo"));
    }

    @Test
    public void testToString() {
        assertEquals("47.11", toString(47.11), "Wrong result from toString: ");
    }

    @Test
    public void testToStringNull() {
        assertEquals(null, toString(null), "Wrong result from toString: ");
    }

    @Test
    public void baseFitnNesseFixtureWithNullSpringConfigResourceName() {
        Exception e = assertThrows(IllegalArgumentException.class, () -> new BaseFitNesseFixture((String) null));
        assertEquals("springConfigurationResourceName must not be null", e.getMessage());
    }
}
