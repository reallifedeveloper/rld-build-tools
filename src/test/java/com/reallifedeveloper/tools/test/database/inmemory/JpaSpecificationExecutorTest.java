package com.reallifedeveloper.tools.test.database.inmemory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.DeleteSpecification;
import org.springframework.data.jpa.domain.PredicateSpecification;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.domain.UpdateSpecification;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

public class JpaSpecificationExecutorTest {

    private static final Specification<User> UNRESTRICTED = Specification.unrestricted();
    private static final DeleteSpecification<User> UNRESTRICTED_DELETE = DeleteSpecification.unrestricted();
    private static final UpdateSpecification<User> UNRESTRICTED_UPDATE = UpdateSpecification.unrestricted();
    private static final Pageable UNPAGED = Pageable.unpaged();
    private static final Sort UNSORTED = Sort.unsorted();

    private final InMemoryJpaRepository<User, Long> repository = new InMemoryJpaRepository<>();

    @BeforeEach
    public void init() {
        repository.save(new User(1L, "Alice", "Alisson", "alice@email.com", 35, Status.ACTIVE));
        repository.save(new User(2L, "Bob", "Bobson", "bob@email.com", 17, Status.ACTIVE));
        repository.save(new User(3L, "Charlie", "Charleston", "charlie@email.com", 49, Status.INACTIVE));
        repository.save(new User(4L, "Eve", "Eavesdropper", "eve@email.com", 42, Status.ACTIVE));
        repository.save(new User(5L, "Mallory", "Malign", "mallory@email.com", 26, Status.INACTIVE));
        repository.save(new User(6L, "John", "Doe", "john@doe.com", 22, Status.INACTIVE));
        repository.save(new User(7L, "Tom", "Doe", "tom@doe.com", 26, Status.ACTIVE));
    }

    @Test
    public void count() {
        assertEquals(2, repository.count(UserSpecifications.lastNameIs("Doe")));
    }

    public static enum Status {
        ACTIVE, INACTIVE
    }

    @Entity
    @Getter
    @AllArgsConstructor
    @ToString
    public static class User {
        @Id
        @GeneratedValue(strategy = GenerationType.AUTO)
        private Long id;

        private String firstName;
        private String lastName;
        private String email;

        private int age;

        private Status status;
    }

    public static class UserSpecifications {
        public static PredicateSpecification<User> active() {
            return (from, cb) -> cb.equal(from.get("status"), Status.ACTIVE);
        }

        public static PredicateSpecification<User> lastNameIs(String lastName) {
            return (from, cb) -> cb.equal(from.get("lastName"), lastName);
        }

        public static PredicateSpecification<User> olderThan(int age) {
            return (from, cb) -> cb.greaterThan(from.get("age"), age);
        }
    }

    @Test
    public void countWithSpecificationThrowsUnsupportedOperationException() {
        Exception e = assertThrows(UnsupportedOperationException.class, () -> repository.count(UNRESTRICTED));
        assertEquals("count(Specification)", e.getMessage());
    }

    @Test
    public void deleteWithDeleteSpecificationThrowsUnsupportedOperationException() {
        Exception e = assertThrows(UnsupportedOperationException.class, () -> repository.delete(UNRESTRICTED_DELETE));
        assertEquals("delete(DeleteSpecification)", e.getMessage());
    }

    @Test
    public void existsWithSpecificationThrowsUnsupportedOperationException() {
        Exception e = assertThrows(UnsupportedOperationException.class, () -> repository.exists(UNRESTRICTED));
        assertEquals("exists(Specification)", e.getMessage());
    }

    @Test
    public void finaAllWithSpecificationThrowsUnsupportedOperationException() {
        Exception e = assertThrows(UnsupportedOperationException.class, () -> repository.findAll(UNRESTRICTED));
        assertEquals("findAll(Specification)", e.getMessage());
    }

    @Test
    public void finaAllWithSpecificationAndPageableThrowsUnsupportedOperationException() {
        Exception e = assertThrows(UnsupportedOperationException.class, () -> repository.findAll(UNRESTRICTED, UNPAGED));
        assertEquals("findAll(Specification, Pageable)", e.getMessage());
    }

    @Test
    public void finaAllWithSpecificationAndSortThrowsUnsupportedOperationException() {
        Exception e = assertThrows(UnsupportedOperationException.class, () -> repository.findAll(UNRESTRICTED, UNSORTED));
        assertEquals("findAll(Specification, Sort)", e.getMessage());
    }

    @Test
    public void finaAllWithTwoSpecificationsAndPageableThrowsUnsupportedOperationException() {
        Exception e = assertThrows(UnsupportedOperationException.class, () -> repository.findAll(UNRESTRICTED, UNRESTRICTED, UNPAGED));
        assertEquals("findAll(Specification, Specification, Pageable)", e.getMessage());
    }

    @Test
    public void finaAllWithSpecificationAndSpecificationFluentQueryThrowsUnsupportedOperationException() {
        Exception e = assertThrows(UnsupportedOperationException.class, () -> repository.findBy(UNRESTRICTED, (q) -> null));
        assertEquals("findBy(Specification, Function<SpecificationFluentQuery>)", e.getMessage());
    }

    @Test
    public void finaOneWithSpecificationThrowsUnsupportedOperationException() {
        Exception e = assertThrows(UnsupportedOperationException.class, () -> repository.findOne(UNRESTRICTED));
        assertEquals("findOne(Specification)", e.getMessage());
    }

    @Test
    public void updateWithUpdateSpecificationThrowsUnsupportedOperationException() {
        Exception e = assertThrows(UnsupportedOperationException.class, () -> repository.update(UNRESTRICTED_UPDATE));
        assertEquals("update(UpdateSpecification)", e.getMessage());
    }
}
