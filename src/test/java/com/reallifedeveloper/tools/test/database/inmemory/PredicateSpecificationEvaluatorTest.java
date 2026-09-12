
package com.reallifedeveloper.tools.test.database.inmemory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.PredicateSpecification;

import edu.umd.cs.findbugs.annotations.Nullable;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKey;
import jakarta.persistence.OneToMany;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.MapJoin;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;

import com.reallifedeveloper.tools.test.database.inmemory.PredicateSpecificationEvaluatorTest.TestEntities.Customer;
import com.reallifedeveloper.tools.test.database.inmemory.PredicateSpecificationEvaluatorTest.TestEntities.CustomerGroup;
import com.reallifedeveloper.tools.test.database.inmemory.PredicateSpecificationEvaluatorTest.TestEntities.Order;
import com.reallifedeveloper.tools.test.database.inmemory.PredicateSpecificationEvaluatorTest.TestEntities.Order.OrderStatus;
import com.reallifedeveloper.tools.test.database.inmemory.PredicateSpecificationEvaluatorTest.TestEntities.OrderLine;
import com.reallifedeveloper.tools.test.database.inmemory.PredicateSpecificationEvaluatorTest.TestEntities.OrderSpecifications;
import com.reallifedeveloper.tools.test.database.inmemory.PredicateSpecificationEvaluatorTest.TestEntities.Product;

@SuppressWarnings("NullAway")
public class PredicateSpecificationEvaluatorTest {
    private static final Product PRODUCT_BOOK = new Product(1L, "War and Peace", "BOOK", "BOOK-001");
    private static final Product PRODUCT_TV = new Product(2L, "Samsung TV", "ELECTRONICS", "TV-001");
    private static final Customer CUSTOMER_1 = new Customer(1L, "Alice", "US", null);
    private static final Customer CUSTOMER_2 = new Customer(2L, "Bob", "UK", null);

    private static final Clock CLOCK = Clock.systemUTC();

    private final PredicateSpecificationEvaluator<Order> evaluator = new PredicateSpecificationEvaluator<>();

    @BeforeEach
    public void init() {
        CUSTOMER_1.clearOrders();
        CUSTOMER_2.clearOrders();
    }

    private void sanityCheck(Order... orders) {
        for (Order order : orders) {
            assertTrue(evaluator.matches(OrderSpecifications.sanityCheck(), order), "Test data not correctly set up");
        }
    }

    @Test
    void evaluatesSimpleProperties() {
        Order order = new Order(1L, OrderStatus.PAID, Instant.now(CLOCK), CUSTOMER_1);

        PredicateSpecification<Order> spec = OrderSpecifications.isPaid().and(OrderSpecifications.customerCountryIs("US"));

        assertTrue(evaluator.matches(spec, order));
    }

    @Test
    void bothConditionsMustMatchTheSameJoinedRow() {
        Order order = new Order(1L, OrderStatus.PAID, Instant.now(CLOCK), CUSTOMER_1);
        order.addOrderLine(new OrderLine(1l, order, PRODUCT_BOOK, 1, new BigDecimal("10")));
        order.addOrderLine(new OrderLine(2L, order, PRODUCT_TV, 1, new BigDecimal(500)));
        sanityCheck(order);

        PredicateSpecification<Order> spec = OrderSpecifications
                .hasLineMatching(OrderSpecifications.lineMatches("BOOK", new BigDecimal("100")));

        assertFalse(evaluator.matches(spec, order));
    }

    @Test
    void matchesWhenOneJoinedRowSatisfiesBothConditions() {
        Order order = new Order(1L, OrderStatus.PAID, Instant.now(CLOCK), CUSTOMER_1);
        order.addOrderLine(new OrderLine(1l, order, PRODUCT_BOOK, 15, new BigDecimal(10)));
        order.addOrderLine(new OrderLine(2L, order, PRODUCT_TV, 1, new BigDecimal(500)));
        sanityCheck(order);

        PredicateSpecification<Order> spec = OrderSpecifications
                .hasLineMatching(OrderSpecifications.lineMatches("BOOK", new BigDecimal("100")));

        assertTrue(evaluator.matches(spec, order));
    }

    @Test
    void separateJoinsAreIndependent() {
        Order order = new Order(1L, OrderStatus.PAID, Instant.now(CLOCK), CUSTOMER_1);
        order.addOrderLine(new OrderLine(1l, order, PRODUCT_BOOK, 1, new BigDecimal("10")));
        order.addOrderLine(new OrderLine(2L, order, PRODUCT_TV, 1, new BigDecimal(500)));
        sanityCheck(order);

        PredicateSpecification<Order> spec = (root, cb) -> {
            var lineForCategory = root.join("lines");
            var lineForPrice = root.join("lines");
            return cb.and(cb.equal(lineForCategory.join("product").get("category"), "BOOK"),
                    cb.greaterThanOrEqualTo(lineForPrice.get("unitPrice"), new BigDecimal("100")));
        };

        assertTrue(evaluator.matches(spec, order));
    }

    @Test
    public void leftJoin() {
        PredicateSpecificationEvaluator<Customer> customerEvaluator = new PredicateSpecificationEvaluator<>();
        Order order = new Order(2L, OrderStatus.NEW, Instant.now(CLOCK), CUSTOMER_2);
        order.addOrderLine(new OrderLine(2L, order, PRODUCT_TV, 1, new BigDecimal(500)));
        sanityCheck(order);
        CUSTOMER_2.addOrder(order);

        PredicateSpecification<Customer> newOrderOrNoOrder = (from, cb) -> {
            Join<Customer, Order> orders = from.join("orders", JoinType.LEFT);
            return cb.or(cb.equal(orders.get("status"), OrderStatus.NEW), cb.isNull(orders.get("id")));
        };

        // CUSTOMER_1 has no orders, CUSTOMER_2 has an unpaid order.
        assertEquals(2, customerEvaluator.filter(newOrderOrNoOrder, List.of(CUSTOMER_1, CUSTOMER_2)).size());

        order.setStatus(OrderStatus.PAID);

        // Now that CUSTOMER_2's order is paid, only CUSTOMER_1 remains.
        List<Customer> customers = customerEvaluator.filter(newOrderOrNoOrder, List.of(CUSTOMER_1, CUSTOMER_2));
        assertEquals(1, customers.size());
        assertEquals(CUSTOMER_1, customers.getFirst());
    }

    @Test
    void filterWithAndWithoutNot() {
        Order order1 = new Order(1L, OrderStatus.PAID, Instant.now(CLOCK), CUSTOMER_1);
        order1.addOrderLine(new OrderLine(1l, order1, PRODUCT_BOOK, 1, new BigDecimal("10")));
        Order order2 = new Order(2L, OrderStatus.NEW, Instant.now(CLOCK), CUSTOMER_2);
        order2.addOrderLine(new OrderLine(2L, order2, PRODUCT_TV, 1, new BigDecimal(500)));
        sanityCheck(order1, order2);

        PredicateSpecification<Order> paidSpec = OrderSpecifications.isPaid();

        List<Order> paidOrders = evaluator.filter(paidSpec, List.of(order1, order2));
        assertEquals(1, paidOrders.size());
        assertEquals(order1, paidOrders.getFirst());

        PredicateSpecification<Order> unpaidSpec = PredicateSpecification.not(OrderSpecifications.isPaid());

        List<Order> unpaidOrders = evaluator.filter(unpaidSpec, List.of(order1, order2));
        assertEquals(1, unpaidOrders.size());
        assertEquals(order2, unpaidOrders.getFirst());
    }

    @Test
    public void trueAndFalseAndNotMatchAsExpected() {
        Order order = new Order(1L, OrderStatus.PAID, Instant.now(CLOCK), CUSTOMER_1);
        order.addOrderLine(new OrderLine(1l, order, PRODUCT_BOOK, 1, new BigDecimal("10")));
        order.addOrderLine(new OrderLine(2L, order, PRODUCT_TV, 1, new BigDecimal(500)));
        sanityCheck(order);

        PredicateSpecification<Order> alwaysTrue = (from, cb) -> cb.conjunction();
        PredicateSpecification<Order> alwaysFalse = (from, cb) -> cb.disjunction();

        assertTrue(evaluator.matches(alwaysTrue, order));
        assertFalse(evaluator.matches(PredicateSpecification.not(alwaysTrue), order));

        assertFalse(evaluator.matches(alwaysFalse, order));
        assertTrue(evaluator.matches(PredicateSpecification.not(alwaysFalse), order));
    }

    @Test
    public void comparisonsMatchAsExpected() {
        PredicateSpecificationEvaluator<OrderLine> lineEvaluator = new PredicateSpecificationEvaluator<>();
        OrderLine orderLine = new OrderLine(1l, null, PRODUCT_BOOK, 15, new BigDecimal("10"));

        assertFalse(lineEvaluator.matches(OrderSpecifications.lineAmountIsLessThan(new BigDecimal("149")), orderLine));
        assertFalse(lineEvaluator.matches(OrderSpecifications.lineAmountIsLessThan(new BigDecimal("150")), orderLine));
        assertTrue(lineEvaluator.matches(OrderSpecifications.lineAmountIsLessThan(new BigDecimal("151")), orderLine));

        assertFalse(lineEvaluator.matches(OrderSpecifications.lineAmountIsLessThanOrEqualTo(new BigDecimal("149")), orderLine));
        assertTrue(lineEvaluator.matches(OrderSpecifications.lineAmountIsLessThanOrEqualTo(new BigDecimal("150")), orderLine));
        assertTrue(lineEvaluator.matches(OrderSpecifications.lineAmountIsLessThanOrEqualTo(new BigDecimal("151")), orderLine));

        assertTrue(lineEvaluator.matches(OrderSpecifications.lineAmountIsGreaterThan(new BigDecimal("149")), orderLine));
        assertFalse(lineEvaluator.matches(OrderSpecifications.lineAmountIsGreaterThan(new BigDecimal("150")), orderLine));
        assertFalse(lineEvaluator.matches(OrderSpecifications.lineAmountIsGreaterThan(new BigDecimal("151")), orderLine));

        assertTrue(lineEvaluator.matches(OrderSpecifications.lineAmountIsGreaterThanOrEqualTo(new BigDecimal("149")), orderLine));
        assertTrue(lineEvaluator.matches(OrderSpecifications.lineAmountIsGreaterThanOrEqualTo(new BigDecimal("150")), orderLine));
        assertFalse(lineEvaluator.matches(OrderSpecifications.lineAmountIsGreaterThanOrEqualTo(new BigDecimal("151")), orderLine));
    }

    @Test
    public void inMatchesAsExpected() {
        Order order1 = new Order(1L, OrderStatus.SHIPPED, Instant.now(CLOCK), CUSTOMER_1);
        Order order2 = new Order(2L, OrderStatus.NEW, Instant.now(CLOCK), CUSTOMER_2);

        PredicateSpecification<Order> unshippedOrders = (o, cb) -> o.get("status").in(List.of(OrderStatus.NEW, OrderStatus.PAID));

        assertFalse(evaluator.matches(unshippedOrders, order1));
        assertTrue(evaluator.matches(unshippedOrders, order2));
    }

    @Test
    public void nullDoesNotMatchIn() {
        Order order = new Order(1L, null, Instant.now(CLOCK), CUSTOMER_1);

        PredicateSpecification<Order> unshippedOrders = (o, cb) -> o.get("status").in(List.of(OrderStatus.NEW, OrderStatus.PAID));

        assertFalse(evaluator.matches(unshippedOrders, order));
    }

    @Test
    public void inDoesNotFailIfGivenASingleValue() {
        Order order = new Order(1L, null, Instant.now(CLOCK), CUSTOMER_1);

        PredicateSpecification<Order> spec = (c, cb) -> c.get("id").in(cb.literal(1L));

        assertTrue(evaluator.matches(spec, order));
    }

    @Test
    void criteriaBuilderInMatchesAsExpected() {
        Order order1 = new Order(1L, OrderStatus.SHIPPED, Instant.now(CLOCK), CUSTOMER_1);
        Order order2 = new Order(2L, OrderStatus.NEW, Instant.now(CLOCK), CUSTOMER_2);

        PredicateSpecification<Order> spec = (root, cb) -> {
            CriteriaBuilder.In<OrderStatus> in = cb.in(root.get("status"));
            in.value(OrderStatus.PAID);
            in.value(OrderStatus.SHIPPED);
            return in;
        };

        assertTrue(evaluator.matches(spec, order1));
        assertFalse(evaluator.matches(spec, order2));
    }

    @Test
    void evaluatesMapJoinKey() {
        Order order1 = new Order(1L, OrderStatus.PAID, Instant.now(CLOCK), CUSTOMER_1);
        order1.addOrderLine(new OrderLine(1l, order1, PRODUCT_BOOK, 1, new BigDecimal("10")));
        Order order2 = new Order(2L, OrderStatus.NEW, Instant.now(CLOCK), CUSTOMER_2);
        order2.addOrderLine(new OrderLine(2L, order2, PRODUCT_TV, 1, new BigDecimal(500)));
        sanityCheck(order1, order2);

        PredicateSpecification<Order> spec = (root, cb) -> {
            MapJoin<Order, String, OrderLine> line = root.joinMap("linesBySku");
            return cb.equal(line.key(), "TV-001");
        };

        assertFalse(evaluator.matches(spec, order1));
        assertTrue(evaluator.matches(spec, order2));
    }

    @Test
    void evaluatesMapJoinValue() {
        Order order1 = new Order(1L, OrderStatus.PAID, Instant.now(CLOCK), CUSTOMER_1);
        order1.addOrderLine(new OrderLine(1l, order1, PRODUCT_BOOK, 1, new BigDecimal("10")));
        Order order2 = new Order(2L, OrderStatus.NEW, Instant.now(CLOCK), CUSTOMER_2);
        order2.addOrderLine(new OrderLine(2L, order2, PRODUCT_TV, 1, new BigDecimal(500)));
        sanityCheck(order1, order2);

        PredicateSpecification<Order> spec = (root, cb) -> {
            MapJoin<Order, String, OrderLine> line = root.joinMap("linesBySku");
            return cb.equal(line.value().get("product").get("sku"), line.key());
        };

        assertTrue(evaluator.matches(spec, order1));
        assertTrue(evaluator.matches(spec, order2));
    }

    @Test
    public void mapJoinCanBeJoined() {
        Order order1 = new Order(1L, OrderStatus.PAID, Instant.now(CLOCK), CUSTOMER_1);
        order1.addOrderLine(new OrderLine(1l, order1, PRODUCT_BOOK, 1, new BigDecimal("10")));
        Order order2 = new Order(2L, OrderStatus.NEW, Instant.now(CLOCK), CUSTOMER_2);
        order2.addOrderLine(new OrderLine(2L, order2, PRODUCT_TV, 1, new BigDecimal(500)));
        sanityCheck(order1, order2);

        PredicateSpecification<Order> spec = (o, cb) -> {
            MapJoin<Order, String, OrderLine> line = o.joinMap("linesBySku");
            return cb.equal(line.join("order").get("status"), OrderStatus.PAID);
        };

        assertTrue(evaluator.matches(spec, order1));
        assertFalse(evaluator.matches(spec, order2));
    }

    @Test
    public void comparisonsUsingComparableMatchAsExpected() {
        PredicateSpecificationEvaluator<Customer> customerEvaluator = new PredicateSpecificationEvaluator<>();

        PredicateSpecification<Customer> nameLessThanB = (c, cb) -> cb.lessThan(c.get("name"), "B");

        assertTrue(customerEvaluator.matches(nameLessThanB, CUSTOMER_1));
        assertFalse(customerEvaluator.matches(nameLessThanB, CUSTOMER_2));
    }

    @Test
    public void orMatchesAsExpected() {
        PredicateSpecificationEvaluator<OrderLine> lineEvaluator = new PredicateSpecificationEvaluator<>();

        // GT or LT = NE
        PredicateSpecification<OrderLine> spec = OrderSpecifications.lineAmountIsLessThan(new BigDecimal("150"))
                .or(OrderSpecifications.lineAmountIsGreaterThan(new BigDecimal("150")));

        assertTrue(lineEvaluator.matches(spec, new OrderLine(1l, null, PRODUCT_BOOK, 1, new BigDecimal("149"))));
        assertFalse(lineEvaluator.matches(spec, new OrderLine(1l, null, PRODUCT_BOOK, 1, new BigDecimal("150"))));
        assertTrue(lineEvaluator.matches(spec, new OrderLine(1l, null, PRODUCT_BOOK, 1, new BigDecimal("151"))));
    }

    @Test
    public void nullChecksMatchAsExpected() {
        PredicateSpecificationEvaluator<OrderLine> lineEvaluator = new PredicateSpecificationEvaluator<>();
        OrderLine orderLine1 = new OrderLine(1l, null, PRODUCT_BOOK, 15, new BigDecimal("10"));
        OrderLine orderLine2 = new OrderLine(null, null, PRODUCT_TV, 1, new BigDecimal(500));

        PredicateSpecification<OrderLine> idIsNull = (line, cb) -> cb.isNull(line.get("id"));
        PredicateSpecification<OrderLine> idIsNotNull = (line, cb) -> cb.isNotNull(line.get("id"));

        assertFalse(lineEvaluator.matches(idIsNull, orderLine1));
        assertTrue(lineEvaluator.matches(idIsNull, orderLine2));

        assertTrue(lineEvaluator.matches(idIsNotNull, orderLine1));
        assertFalse(lineEvaluator.matches(idIsNotNull, orderLine2));
    }

    @Test
    public void equalityChecksMatchAsExpected() {
        PredicateSpecificationEvaluator<OrderLine> lineEvaluator = new PredicateSpecificationEvaluator<>();
        OrderLine orderLine1 = new OrderLine(1l, null, PRODUCT_BOOK, 15, new BigDecimal("10"));
        OrderLine orderLine2 = new OrderLine(2L, null, PRODUCT_TV, 1, new BigDecimal(500));

        PredicateSpecification<OrderLine> isIsOne = (line, cb) -> cb.equal(line.get("id"), 1L);
        PredicateSpecification<OrderLine> isIsNotOne = (line, cb) -> cb.notEqual(line.get("id"), 1L);

        assertTrue(lineEvaluator.matches(isIsOne, orderLine1));
        assertFalse(lineEvaluator.matches(isIsOne, orderLine2));

        assertFalse(lineEvaluator.matches(isIsNotOne, orderLine1));
        assertTrue(lineEvaluator.matches(isIsNotOne, orderLine2));
    }

    @Test
    public void unrestrictedMatchesEverything() {
        Order order = new Order(1L, OrderStatus.PAID, Instant.now(CLOCK), CUSTOMER_1);

        assertTrue(evaluator.matches(PredicateSpecification.unrestricted(), order));
    }

    @Test
    public void isEmptyMatchesAsExpected() {
        Order order1 = new Order(1L, OrderStatus.PAID, Instant.now(CLOCK), CUSTOMER_1);
        order1.addOrderLine(new OrderLine(1l, order1, PRODUCT_BOOK, 1, new BigDecimal("10")));
        Order order2 = new Order(2L, OrderStatus.NEW, Instant.now(CLOCK), CUSTOMER_2);

        PredicateSpecification<Order> noLines = (o, cb) -> cb.isEmpty(o.get("lines"));

        assertFalse(evaluator.matches(noLines, order1));
        assertTrue(evaluator.matches(noLines, order2));
    }

    @Test
    public void isEmptyFailsIfNotGivenACollection() {
        Order order = new Order(1L, null, Instant.now(CLOCK), CUSTOMER_1);

        PredicateSpecification<Order> spec = (o, cb) -> cb.isEmpty(o.get("id"));

        Exception e = assertThrows(IllegalArgumentException.class, () -> evaluator.matches(spec, order));
        assertEquals("isEmpty() requires a Collection, but got java.lang.Long", e.getMessage());
    }

    @Test
    public void isEmptyConsidersNullAsUnknownWhichDoesNotMatch() {
        Order order = new Order(1L, null, Instant.now(CLOCK), CUSTOMER_1);

        PredicateSpecification<Order> spec = (o, cb) -> cb.isEmpty(o.get("status"));

        assertFalse(evaluator.matches(spec, order));

    }

    @Test
    public void isNotEmptyMatchesAsExpected() {
        Order order1 = new Order(1L, OrderStatus.PAID, Instant.now(CLOCK), CUSTOMER_1);
        order1.addOrderLine(new OrderLine(1l, order1, PRODUCT_BOOK, 1, new BigDecimal("10")));
        Order order2 = new Order(2L, OrderStatus.NEW, Instant.now(CLOCK), CUSTOMER_2);

        PredicateSpecification<Order> hasLines = (o, cb) -> cb.isNotEmpty(o.get("lines"));

        assertTrue(evaluator.matches(hasLines, order1));
        assertFalse(evaluator.matches(hasLines, order2));
    }

    @Test
    public void isNotEmptyFailsIfNotGivenACollection() {
        Order order = new Order(1L, null, Instant.now(CLOCK), CUSTOMER_1);

        PredicateSpecification<Order> spec = (o, cb) -> cb.isNotEmpty(o.get("id"));

        Exception e = assertThrows(IllegalArgumentException.class, () -> evaluator.matches(spec, order));
        assertEquals("isNotEmpty() requires a Collection, but got java.lang.Long", e.getMessage());
    }

    @Test
    public void isNotEmptyConsidersNullAsUnknownWhichDoesNotMatch() {
        Order order = new Order(1L, null, Instant.now(CLOCK), CUSTOMER_1);

        PredicateSpecification<Order> spec = (o, cb) -> cb.isNotEmpty(o.get("status"));

        assertFalse(evaluator.matches(spec, order));

    }

    @Test
    void evaluatesRegisteredFunction() {
        PredicateSpecificationEvaluator<Order> evaluatorWithFunction = new PredicateSpecificationEvaluator<Order>().registerFunction(
                "normalize", String.class, String.class, value -> value == null ? null : value.trim().toLowerCase(Locale.ROOT));

        Customer customer = new Customer(1L, "Alice", "     US   ", null);
        Order order = new Order(1L, OrderStatus.NEW, Instant.now(CLOCK), customer);

        PredicateSpecification<Order> spec = (root, cb) -> {
            Join<Order, Customer> c = root.join("customer");
            Expression<String> country = cb.function("normalize", String.class, c.get("country"));
            return cb.equal(country, "us");
        };

        assertTrue(evaluatorWithFunction.matches(spec, order));
    }

    @Test
    void failsForUnregisteredFunction() {
        Order order = new Order(1L, OrderStatus.NEW, Instant.now(CLOCK), CUSTOMER_1);
        PredicateSpecification<Order> spec = (root, cb) -> cb.equal(cb.function("unknown_function", String.class, root.get("status")),
                "foo");

        Exception e = assertThrows(UnsupportedOperationException.class, () -> evaluator.matches(spec, order));

        assertEquals("No evaluator function registered for CriteriaBuilder.function(unknown_function, java.lang.String)", e.getMessage());
    }

    @Test
    public void functionFailsIfGivenTheWrongNumberOfArguments() {
        PredicateSpecificationEvaluator<Customer> evaluatorWithFunction = new PredicateSpecificationEvaluator<Customer>()
                .registerFunction("toLower", String.class, String.class, s -> s.toLowerCase(Locale.ROOT));

        PredicateSpecification<Customer> spec = (c, cb) -> cb.equal(cb.function("toLower", String.class, c.get("name"), cb.literal("foo")),
                cb.literal("alice"));

        Exception e = assertThrows(IllegalArgumentException.class, () -> evaluatorWithFunction.matches(spec, CUSTOMER_1));
        assertEquals("Function 'toLower' expected 1 arguments but received 2", e.getMessage());
    }

    @Test
    public void functionFailsIfGivenArgumentOfWrongType() {
        PredicateSpecificationEvaluator<Customer> evaluatorWithFunction = new PredicateSpecificationEvaluator<Customer>()
                .registerFunction("toLower", String.class, String.class, s -> s.toLowerCase(Locale.ROOT));

        PredicateSpecification<Customer> spec = (c, cb) -> cb.equal(cb.function("toLower", String.class, cb.literal(42)),
                cb.literal("alice"));

        Exception e = assertThrows(IllegalArgumentException.class, () -> evaluatorWithFunction.matches(spec, CUSTOMER_1));
        assertEquals("Argument 0 to function 'toLower' was java.lang.Integer, expected java.lang.String", e.getMessage());
    }

    @Test
    public void functionFailsIfCalledWithWrongResultType() {
        PredicateSpecificationEvaluator<Customer> evaluatorWithFunction = new PredicateSpecificationEvaluator<Customer>()
                .registerFunction("toLower", String.class, String.class, s -> s.toLowerCase(Locale.ROOT));

        PredicateSpecification<Customer> spec = (c, cb) -> cb.equal(cb.function("toLower", Integer.class, cb.literal("foo")),
                cb.literal("alice"));

        Exception e = assertThrows(IllegalArgumentException.class, () -> evaluatorWithFunction.matches(spec, CUSTOMER_1));
        assertEquals(
                "Function 'toLower' was requested with result type java.lang.Integer, but is registered with result type java.lang.String",
                e.getMessage());
    }

    @Test
    public void functionFailsIfReturningWrongResultType() {
        PredicateSpecificationEvaluator<Customer> evaluatorWithFunction = new PredicateSpecificationEvaluator<Customer>()
                .registerFunction("foo", String.class, List.of(String.class), s -> 42);

        PredicateSpecification<Customer> spec = (c, cb) -> cb.equal(cb.function("foo", String.class, cb.literal("foo")),
                cb.literal("alice"));

        Exception e = assertThrows(IllegalArgumentException.class, () -> evaluatorWithFunction.matches(spec, CUSTOMER_1));
        assertEquals("Function 'foo' returned java.lang.Integer, but CriteriaBuilder.function() declared java.lang.String", e.getMessage());
    }

    @Test
    public void functionsCanHandlePrimitiveReturnTypes() {
        PredicateSpecificationEvaluator<Customer> evaluatorWithFunction = new PredicateSpecificationEvaluator<Customer>()
                .registerFunction("intFunction", int.class, List.of(long.class), args -> 42)
                .registerFunction("longFunction", long.class, List.of(float.class), args -> 42L)
                .registerFunction("floatFunction", float.class, List.of(double.class), args -> 42.0f)
                .registerFunction("doubleFunction", double.class, List.of(short.class), args -> 42.0)
                .registerFunction("shortFunction", short.class, List.of(byte.class), args -> (short) 42)
                .registerFunction("byteFunction", byte.class, List.of(boolean.class), args -> (byte) 42)
                .registerFunction("booleanFunction", boolean.class, List.of(char.class), args -> true)
                .registerFunction("charFunction", char.class, List.of(int.class), args -> '*');

        PredicateSpecification<Customer> spec = (from,
                cb) -> cb.equal(
                        cb.function("intFunction", int.class,
                                cb.function("longFunction", long.class,
                                        cb.function("floatFunction", float.class,
                                                cb.function("doubleFunction", double.class,
                                                        cb.function("shortFunction", short.class, cb.function("byteFunction", byte.class,
                                                                cb.function("booleanFunction", boolean.class,
                                                                        cb.function("charFunction", char.class, cb.literal(0))))))))),
                        cb.literal(42));

        assertTrue(evaluatorWithFunction.matches(spec, CUSTOMER_1));
    }

    @Test
    public void propertyAccessUsingFieldsAndNestedGets() {
        PredicateSpecificationEvaluator<Customer> customerEvaluator = new PredicateSpecificationEvaluator<>();
        CustomerGroup customerGroup = new CustomerGroup(1L, "My Group"); // The CustomerGroup class has no getters
        Customer customer = new Customer(1L, "Charlie", "NZ", customerGroup);

        // We use "c.get" instead of "c.join" to verify that the nested get("name") call works as epxected.
        PredicateSpecification<Customer> spec = (c, cb) -> cb.equal(c.get("group").get("name"), "My Group");

        assertTrue(customerEvaluator.matches(spec, customer));
    }

    @Test
    public void negatedPredicateMatchesAsExpected() {
        Order order1 = new Order(1L, OrderStatus.PAID, Instant.now(CLOCK), CUSTOMER_1);
        Order order2 = new Order(2L, OrderStatus.NEW, Instant.now(CLOCK), CUSTOMER_2);

        PredicateSpecification<Order> unpaidSpec = (o, cb) -> cb.equal(o.get("status"), OrderStatus.PAID).not();

        assertFalse(evaluator.matches(unpaidSpec, order1));
        assertTrue(evaluator.matches(unpaidSpec, order2));
    }

    @Test
    public void notConsidersNullAsUnknownWhichDoesNotMatch() {
        Order order = new Order(1L, null, Instant.now(CLOCK), CUSTOMER_1);

        PredicateSpecification<Order> spec = (o, cb) -> cb.not(cb.equal(o.get("status"), cb.literal(OrderStatus.PAID)));

        assertFalse(evaluator.matches(spec, order));
    }

    @Test
    public void usingUnimplementedMethodThrowsUnsupportedOperationException() throws NoSuchMethodException {
        Order order = new Order(1L, OrderStatus.PAID, Instant.now(CLOCK), CUSTOMER_1);
        Method unimplementedMethod = Expression.class.getMethod("isNotNull");

        PredicateSpecification<Order> spec = (o, cb) -> cb.isTrue(o.get("status").isNotNull());

        Exception e = assertThrows(UnsupportedOperationException.class, () -> evaluator.matches(spec, order));
        assertEquals("Unsupported Criteria API operation: " + unimplementedMethod, e.getMessage());
    }

    @Test
    public void accessingUnknownPropertyThrowsIllegalArgumentException() {
        Order order = new Order(1L, OrderStatus.PAID, Instant.now(CLOCK), CUSTOMER_1);
        PredicateSpecification<Order> spec = (o, cb) -> cb.equal(o.get("no_such_field"), "foo");

        Exception e = assertThrows(IllegalArgumentException.class, () -> evaluator.matches(spec, order));
        assertEquals("No readable property 'no_such_field' on " + Order.class.getName(), e.getMessage());
    }

    @Test
    public void foo() {
        PredicateSpecificationEvaluator<Customer> customerEvaluator = new PredicateSpecificationEvaluator<>();
        PredicateSpecification<Customer> spec = (c, cb) -> {
            System.out.println("===== " + c.get("name").toString());
            System.out.println("===== " + c.get("name").hashCode());
            // System.out.println("===== " + c.get("name").equals(c.get("name")));
            return cb.conjunction();
        };
        assertTrue(customerEvaluator.matches(spec, CUSTOMER_1));
    }

    public static class TestEntities {

        @Entity
        @Getter
        @Accessors(fluent = true)
        @AllArgsConstructor
        @ToString
        public static class Order {

            public enum OrderStatus {
                NEW, PAID, SHIPPED
            }

            @Id
            private Long id;

            @Enumerated(EnumType.STRING)
            private OrderStatus status;

            private Instant createdAt;

            @ManyToOne(fetch = FetchType.LAZY)
            private Customer customer;

            @OneToMany(mappedBy = "order")
            private final Set<OrderLine> lines = new HashSet<>();

            @OneToMany
            @MapKey(name = "sku")
            private final Map<String, OrderLine> linesBySku = new HashMap<>();

            public void setStatus(OrderStatus status) {
                this.status = status;
            }

            public void addOrderLine(OrderLine orderLine) {
                lines.add(orderLine);
                linesBySku.put(orderLine.product().sku(), orderLine);
            }
        }

        @Entity
        @Getter
        @Accessors(fluent = false) // We use "getNnn" here in order to test that part of the logic in PredicationSpecificationEvaluator
        @AllArgsConstructor
        @ToString
        public static class Customer {

            @Id
            private Long id;

            private String name;

            private String country;

            @ManyToOne(fetch = FetchType.LAZY)
            @Nullable
            private CustomerGroup group;

            @OneToMany
            @ToString.Exclude
            private final Set<Order> orders = new HashSet<>();

            public void addOrder(Order order) {
                orders.add(order);
            }

            public void clearOrders() {
                orders.clear();
            }
        }

        @Entity
        // @Getter // No getters in this class, we want to test field access in PredicateSpecificationEvaluator
        // @Accessors(fluent = true)
        @AllArgsConstructor
        @ToString
        public static class CustomerGroup {

            @Id
            private Long id;

            private String name;
        }

        @Entity
        @Getter
        @Accessors(fluent = true)
        @AllArgsConstructor
        @ToString
        public static class OrderLine {

            @Id
            private Long id;

            @ManyToOne(fetch = FetchType.LAZY)
            @ToString.Exclude
            private Order order;

            @ManyToOne(fetch = FetchType.LAZY)
            private Product product;

            private int quantity;

            private BigDecimal unitPrice;
        }

        @Entity
        @Getter
        @Accessors(fluent = true)
        @AllArgsConstructor
        @ToString
        public static class Product {

            @Id
            private Long id;

            private String name;

            private String category;

            private String sku;
        }

        public static class OrderSpecifications {

            // Verifies that Orders have been correctly constructed, with "linesBySku" matching their Product.
            public static PredicateSpecification<Order> sanityCheck() {
                return (o, cb) -> {
                    MapJoin<Order, String, OrderLine> line = o.joinMap("linesBySku");
                    return cb.or(cb.isEmpty(o.get("lines")), cb.equal(line.key(), line.get("product").get("sku")));
                };
            }

            public static PredicateSpecification<Order> isPaid() {
                return (order, cb) -> cb.equal(order.get("status"), OrderStatus.PAID);
            }

            public static PredicateSpecification<Order> customerCountryIs(String country) {
                return (order, cb) -> {
                    var customer = order.join("customer");
                    return cb.equal(customer.get("country"), country);
                };
            }

            public static PredicateSpecification<Order> hasSku(String sku) {
                return (order, cb) -> {
                    MapJoin<Order, String, OrderLine> lines = order.joinMap("linesBySku");
                    return cb.equal(lines.key(), sku);
                };
            }

            public static PredicateSpecification<Order> hasExpensiveSku(String sku, BigDecimal minimumPrice) {
                return (order, cb) -> {
                    MapJoin<Order, String, OrderLine> lines = order.joinMap("linesBySku");
                    return cb.and(cb.equal(lines.key(), sku), cb.greaterThanOrEqualTo(lines.value().get("unitPrice"), minimumPrice));
                };
            }

            public static PredicateSpecification<OrderLine> lineMatches(String category, BigDecimal minimumValue) {
                return (line, cb) -> cb.and(cb.equal(line.join("product").get("category"), category),
                        cb.greaterThanOrEqualTo(cb.prod(line.get("unitPrice"), line.get("quantity")), minimumValue));
            }

            public static PredicateSpecification<Order> hasLineMatching(PredicateSpecification<OrderLine> lineSpecification) {
                return (order, cb) -> {
                    Join<Order, OrderLine> line = order.join("lines");
                    return lineSpecification.toPredicate(line, cb);
                };
            }

            public static PredicateSpecification<OrderLine> lineAmountIsLessThan(BigDecimal amount) {
                return (line, cb) -> cb.lessThan(cb.prod(line.get("unitPrice"), line.get("quantity")), amount);
            }

            public static PredicateSpecification<OrderLine> lineAmountIsLessThanOrEqualTo(BigDecimal amount) {
                return (line, cb) -> cb.lessThanOrEqualTo(cb.prod(line.get("unitPrice"), line.get("quantity")), amount);
            }

            public static PredicateSpecification<OrderLine> lineAmountIsGreaterThan(BigDecimal amount) {
                return (line, cb) -> cb.greaterThan(cb.prod(line.get("unitPrice"), line.get("quantity")), amount);
            }

            public static PredicateSpecification<OrderLine> lineAmountIsGreaterThanOrEqualTo(BigDecimal amount) {
                return (line, cb) -> cb.greaterThanOrEqualTo(cb.prod(line.get("unitPrice"), line.get("quantity")), amount);
            }

        }
    }
}
