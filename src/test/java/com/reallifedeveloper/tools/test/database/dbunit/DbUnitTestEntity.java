package com.reallifedeveloper.tools.test.database.dbunit;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKey;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Entity
@Getter
@Accessors(fluent = true)
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PACKAGE)
@SuppressWarnings("NullAway")
public class DbUnitTestEntity {
    private Byte b;
    private Short s;
    @Id
    private Integer id;
    private Long l;
    private Float f;
    private Double d;
    private Boolean bool;
    private Character c;
    private String string;
    private Date date;
    private LocalDate localDate;
    private LocalDateTime localDateTime;
    private ZonedDateTime zonedDateTime;
    private TestEnum testEnum;
    private BigDecimal bd;
    private BigInteger bi;
    private List<String> strings;
    @ManyToOne
    @JoinColumn(name = "testentity_id")
    private TestEntity testEntity;
    @ManyToMany
    @JoinTable(name = "dbunittestentity_testentity", joinColumns = {
            @JoinColumn(name = "dbunit_test_entity_id", referencedColumnName = "id") }, inverseJoinColumns = {
                    @JoinColumn(name = "test_entity_id", referencedColumnName = "id") })
    private List<TestEntity> testEntities = new ArrayList<>();
    @OneToOne
    @JoinColumn(name = "one_to_one_db_unit_test_entity_id")
    private TestEntityWithoutRepository testEntityWithoutRepository;
    @OneToMany(mappedBy = "dbUnitTestEntity")
    private List<TestEntityWithoutRepository> testEntitiesWithoutRepository = new ArrayList<>();
    @OneToMany
    @JoinColumn(name = "db_unit_test_entity_id")
    @MapKey(name = "id")
    private Map<UUID, TestEntityWithoutRepository> mappedEntitiesWithoutRepository = new HashMap<>();

    public enum TestEnum {
        /** The mightiest meta-variable. */
        FOO,
        /** The second mightiest meta-variable. */
        BAR
    }
}
