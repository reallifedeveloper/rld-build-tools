package com.reallifedeveloper.tools.test.fitnesse;

import java.util.Calendar;
import java.util.Date;

import javax.sql.DataSource;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

public class FitNesseFixtureTest extends AbstractFitNesseFixture {

    public FitNesseFixtureTest() {
        super("/META-INF/spring-context-rld-build-tools-test.xml");
    }

    @Test
    public void kommentar() {
        Assert.assertNull("Kommentar ska vara null innan den har satts", getComment());
        setComment("foo");
        Assert.assertEquals("Fel kommentar: ", "foo", getComment());
    }

    @Test
    public void getExistingBean() {
        Assert.assertNotNull("DataSource should not be null", getBean(DataSource.class));
    }

    @Test(expected = NoSuchBeanDefinitionException.class)
    public void getNonExistingBean() {
        getBean(Boolean.class);
    }

    @Test
    public void testLogger() {
        Logger logger = logger();
        Assert.assertNotNull("Logger should not be null", logger);
        logger.info("Logger method is working");
    }

    @Test
    public void testResetApplicationContext() {
        Assert.assertNotNull("DataSource should not be null before reset", getBean(DataSource.class));
        resetApplicationContext();
        Assert.assertNotNull("DataSource should not be null after reset", getBean(DataSource.class));
    }

    @Test
    public void parseCorrectDate() {
        Date date = parseDate("2016-03-26");
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        Assert.assertEquals("Wrong year: ", 2016, calendar.get(Calendar.YEAR));
        Assert.assertEquals("Wrong month: ", 3 - 1, calendar.get(Calendar.MONTH));
        Assert.assertEquals("Wrong day: ", 26, calendar.get(Calendar.DAY_OF_MONTH));
    }

    @Test(expected = IllegalArgumentException.class)
    public void parseIncorrectDate() {
        parseDate("foo");
    }

    @Test
    public void testToString() {
        Assert.assertEquals("Wrong result from toString: ", "47.11", toString(47.11));
    }

    @Test
    public void testToStringNull() {
        Assert.assertEquals("Wrong result from toString: ", null, toString(null));
    }
}
