package com.reallifedeveloper.tools.test.database.inmemory;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.NullHandling;
import org.springframework.data.domain.Sort.Order;

public class SortUtilTest {

    @SuppressWarnings("NullAway")
    private static final List<Customer> CUSTOMERS = List.of(new Customer("Bob", "Alisson", 17), new Customer("Charlie", "Charleston", null),
            new Customer("Alice", "Alisson", 35));

    @Test
    public void sortAscending() {
        List<Customer> sorted = SortUtil.sort(CUSTOMERS, Sort.by(Order.asc("firstName")));
        assertEquals("[Alice, Bob, Charlie]", sorted.stream().map(c -> c.firstName()).toList().toString());
    }

    @Test
    public void sortDescending() {
        List<Customer> sorted = SortUtil.sort(CUSTOMERS, Sort.by(Order.desc("firstName")));
        assertEquals("[Charlie, Bob, Alice]", sorted.stream().map(c -> c.firstName()).toList().toString());
    }

    @Test
    public void sortAscendingIsStable() {
        List<Customer> sorted = SortUtil.sort(CUSTOMERS, Sort.by(Order.asc("lastName")));
        assertEquals("[Bob, Alice, Charlie]", sorted.stream().map(c -> c.firstName()).toList().toString());
    }

    @Test
    public void sortDescendingIsStable() {
        List<Customer> sorted = SortUtil.sort(CUSTOMERS, Sort.by(Order.desc("lastName")));
        assertEquals("[Charlie, Bob, Alice]", sorted.stream().map(c -> c.firstName()).toList().toString());
    }

    @Test
    public void sortAscendingWithNullValuesLast() {
        List<Customer> sorted = SortUtil.sort(CUSTOMERS, Sort.by(Order.asc("age").with(NullHandling.NULLS_LAST)));
        assertEquals("[Bob, Alice, Charlie]", sorted.stream().map(c -> c.firstName()).toList().toString());
    }

    @Test
    public void sortDescendingWithNullValuesLast() {
        List<Customer> sorted = SortUtil.sort(CUSTOMERS, Sort.by(Order.desc("age").with(NullHandling.NULLS_LAST)));
        assertEquals("[Alice, Bob, Charlie]", sorted.stream().map(c -> c.firstName()).toList().toString());
    }

    @Test
    public void sortAscendingWithNullValuesFirst() {
        List<Customer> sorted = SortUtil.sort(CUSTOMERS, Sort.by(Order.asc("age").with(NullHandling.NULLS_FIRST)));
        assertEquals("[Charlie, Bob, Alice]", sorted.stream().map(c -> c.firstName()).toList().toString());
    }

    @Test
    public void sortDescendingWithNullValuesFirst() {
        List<Customer> sorted = SortUtil.sort(CUSTOMERS, Sort.by(Order.desc("age").with(NullHandling.NULLS_FIRST)));
        assertEquals("[Charlie, Alice, Bob]", sorted.stream().map(c -> c.firstName()).toList().toString());
    }

    @Test
    @SuppressWarnings("NullAway")
    public void sortAscendingOnlyNulls() {
        List<Customer> customers = List.of(new Customer("Bob", null, 17), new Customer("Alice", null, 35));
        List<Customer> sorted = SortUtil.sort(customers, Sort.by(Order.asc("lastName")));
        assertEquals("[Bob, Alice]", sorted.stream().map(c -> c.firstName()).toList().toString());
    }

    @Test
    @SuppressWarnings("NullAway")
    public void sortDescendingOnlyNulls() {
        List<Customer> customers = List.of(new Customer("Bob", null, 17), new Customer("Alice", null, 35));
        List<Customer> sorted = SortUtil.sort(customers, Sort.by(Order.desc("lastName")));
        assertEquals("[Bob, Alice]", sorted.stream().map(c -> c.firstName()).toList().toString());
    }

    private static record Customer(String firstName, String lastName, Integer age) {
    }
}
