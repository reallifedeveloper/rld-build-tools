package com.reallifedeveloper.tools.test.database.dbunit;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
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
    private TestEnum testEnum;
    private BigDecimal bd;
    private BigInteger bi;
    @ManyToOne
    @JoinColumn(name = "testentity_id")
    private TestEntity testEntity;
    @ManyToMany
    @JoinTable(name = "dbunittestentity_testentity", joinColumns = {
            @JoinColumn(name = "dbunit_test_entity_id", referencedColumnName = "id") }, inverseJoinColumns = {
                    @JoinColumn(name = "test_entity_id", referencedColumnName = "id") })
    private List<TestEntity> testEntities = new ArrayList<>();
    @OneToMany(mappedBy = "dbUnitTestEntity")
    private List<TestEntityWithoutRepository> testEntitiesWithoutRepository = new ArrayList<>();

    public enum TestEnum {
        /** The mightiest meta-variable. */
        FOO,
        /** The second mightiest meta-variable. */
        BAR
    }
}
