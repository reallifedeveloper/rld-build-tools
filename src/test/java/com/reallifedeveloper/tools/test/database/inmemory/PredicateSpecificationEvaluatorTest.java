package com.reallifedeveloper.tools.test.database.inmemory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.PredicateSpecification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import edu.umd.cs.findbugs.annotations.Nullable;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.criteria.Join;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;

import com.reallifedeveloper.tools.test.database.inmemory.PredicateSpecificationEvaluatorTest.TestEntities.Customer;
import com.reallifedeveloper.tools.test.database.inmemory.PredicateSpecificationEvaluatorTest.TestEntities.Order;
import com.reallifedeveloper.tools.test.database.inmemory.PredicateSpecificationEvaluatorTest.TestEntities.Order.OrderStatus;
import com.reallifedeveloper.tools.test.database.inmemory.PredicateSpecificationEvaluatorTest.TestEntities.OrderLine;
import com.reallifedeveloper.tools.test.database.inmemory.PredicateSpecificationEvaluatorTest.TestEntities.OrderSpecifications;
import com.reallifedeveloper.tools.test.database.inmemory.PredicateSpecificationEvaluatorTest.TestEntities.Product;

public class PredicateSpecificationEvaluatorTest {
    private static final Product PRODUCT_BOOK = new Product(1L, "War and Peace", "BOOK");
    private static final Product PRODUCT_TV = new Product(2L, "Samsung TV", "ELECTRONICS");
    private static final Customer CUSTOMER_1 = new Customer(1L, "Alice", "US", null);
    private static final Customer CUSTOMER_2 = new Customer(2L, "Bob", "UK", null);

    private static final Clock CLOCK = Clock.systemUTC();

    private final PredicateSpecificationEvaluator<Order> evaluator = new PredicateSpecificationEvaluator<>();

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

        PredicateSpecification<Order> spec = OrderSpecifications
                .hasLineMatching(OrderSpecifications.lineMatches("BOOK", new BigDecimal("100")));

        assertFalse(evaluator.matches(spec, order));
    }

    @Test
    void matchesWhenOneJoinedRowSatisfiesBothConditions() {
        Order order = new Order(1L, OrderStatus.PAID, Instant.now(CLOCK), CUSTOMER_1);
        order.addOrderLine(new OrderLine(1l, order, PRODUCT_BOOK, 15, new BigDecimal(10)));
        order.addOrderLine(new OrderLine(2L, order, PRODUCT_TV, 1, new BigDecimal(500)));

        PredicateSpecification<Order> spec = OrderSpecifications
                .hasLineMatching(OrderSpecifications.lineMatches("BOOK", new BigDecimal("100")));

        assertTrue(evaluator.matches(spec, order));
    }

    @Test
    void separateJoinsAreIndependent() {
        Order order = new Order(1L, OrderStatus.PAID, Instant.now(CLOCK), CUSTOMER_1);
        order.addOrderLine(new OrderLine(1l, order, PRODUCT_BOOK, 1, new BigDecimal("10")));
        order.addOrderLine(new OrderLine(2L, order, PRODUCT_TV, 1, new BigDecimal(500)));

        PredicateSpecification<Order> spec = (root, cb) -> {
            var lineForCategory = root.join("lines");
            var lineForPrice = root.join("lines");
            return cb.and(cb.equal(lineForCategory.join("product").get("category"), "BOOK"),
                    cb.greaterThanOrEqualTo(lineForPrice.get("unitPrice"), new BigDecimal("100")));
        };

        assertTrue(evaluator.matches(spec, order));
    }

    @Test
    void filterWithAndWithoutNot() {
        Order order1 = new Order(1L, OrderStatus.PAID, Instant.now(CLOCK), CUSTOMER_1);
        order1.addOrderLine(new OrderLine(1l, order1, PRODUCT_BOOK, 1, new BigDecimal("10")));
        Order order2 = new Order(2L, OrderStatus.NEW, Instant.now(CLOCK), CUSTOMER_2);
        order2.addOrderLine(new OrderLine(2L, order2, PRODUCT_TV, 1, new BigDecimal(500)));

        PredicateSpecification<Order> paidSpec = OrderSpecifications.isPaid();

        List<Order> paidOrders = evaluator.filter(paidSpec, List.of(order1, order2));
        assertEquals(1, paidOrders.size());
        assertEquals(order1, paidOrders.get(0));

        PredicateSpecification<Order> unpaidSpec = PredicateSpecification.not(OrderSpecifications.isPaid());

        List<Order> unpaidOrders = evaluator.filter(unpaidSpec, List.of(order1, order2));
        assertEquals(1, unpaidOrders.size());
        assertEquals(order2, unpaidOrders.get(0));
    }

    @Test
    public void trueAndFalseAndNotMatchAsExpected() {
        Order order = new Order(1L, OrderStatus.PAID, Instant.now(CLOCK), CUSTOMER_1);
        order.addOrderLine(new OrderLine(1l, order, PRODUCT_BOOK, 1, new BigDecimal("10")));
        order.addOrderLine(new OrderLine(2L, order, PRODUCT_TV, 1, new BigDecimal(500)));

        PredicateSpecification<Order> alwaysTrue = (from, cb) -> cb.conjunction();
        PredicateSpecification<Order> alwaysFalse = (from, cb) -> cb.disjunction();

        assertTrue(evaluator.matches(alwaysTrue, order));
        assertFalse(evaluator.matches(PredicateSpecification.not(alwaysTrue), order));

        assertFalse(evaluator.matches(alwaysFalse, order));
        assertTrue(evaluator.matches(PredicateSpecification.not(alwaysFalse), order));
    }

    public static class TestEntities {

        public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {
        }

        @Entity
        @Getter
        @Accessors(fluent = true)
        @AllArgsConstructor
        @ToString
        public static class Order {

            public enum OrderStatus {
                NEW, PAID
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

            public void addOrderLine(OrderLine orderLine) {
                lines.add(orderLine);
            }
        }

        @Entity
        @Getter
        @Accessors(fluent = true)
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
        }

        @Entity
        @Getter
        @Accessors(fluent = true)
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
        }

        public static class OrderSpecifications {

            public static PredicateSpecification<Order> isPaid() {
                return (order, cb) -> cb.equal(order.get("status"), OrderStatus.PAID);
            }

            public static PredicateSpecification<Order> customerCountryIs(String country) {
                return (order, cb) -> {
                    var customer = order.join("customer");
                    return cb.equal(customer.get("country"), country);
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
        }
    }
}
