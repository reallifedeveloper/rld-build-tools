package com.reallifedeveloper.tools.test.database.dbunit;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;

import com.vividsolutions.jts.geom.Geometry;

@Entity
public class DbUnitTestEntity {
    private Byte b;
    private Short s;
    @Id
    private Integer id;
    private Long l;
    private Float f;
    private Double d;
    private BigDecimal bd;
    private Boolean bool;
    private Character c;
    private String string;
    private Date date;
    private TestEnum testEnum;
    private Geometry geometry;
    @ManyToOne
    @JoinColumn(name = "testentity_id")
    private TestEntity testEntity;
    @ManyToMany
    @JoinTable(name = "dbunittestentity_testentity",
            joinColumns = { @JoinColumn(name = "dbunit_test_entity_id", referencedColumnName = "id") },
            inverseJoinColumns = { @JoinColumn(name = "test_entity_id", referencedColumnName = "id") })
    private List<TestEntity> testEntities = new ArrayList<>();

    // CHECKSTYLE:OFF
    public DbUnitTestEntity(byte b, Short s, Integer id, Long l, Float f, Double d, BigDecimal bd, Boolean bool,
            Character c, String string, Date date, TestEnum testEnum, Geometry geometry, TestEntity testEntity,
            Collection<TestEntity> testEntities) {
        // CHECKSTYLE:on
        this.b = b;
        this.s = s;
        this.id = id;
        this.l = l;
        this.f = f;
        this.d = d;
        this.bd = bd;
        this.bool = bool;
        this.c = c;
        this.string = string;
        this.date = date;
        this.testEnum = testEnum;
        this.geometry = geometry;
        this.testEntity = testEntity;
        this.testEntities = new ArrayList<>(testEntities);
    }

    DbUnitTestEntity() {
    }

    public Byte b() {
        return b;
    }

    public Short s() {
        return s;
    }

    public Integer id() {
        return id;
    }

    public Long l() {
        return l;
    }

    public Float f() {
        return f;
    }

    public Double d() {
        return d;
    }

    public BigDecimal bd() {
        return bd;
    }

    public Boolean bool() {
        return bool;
    }

    public Character c() {
        return c;
    }

    public String string() {
        return string;
    }

    public Date date() {
        return date;
    }

    public TestEnum testEnum() {
        return testEnum;
    }

    public Geometry geometry() {
        return geometry;
    }

    public TestEntity testEntity() {
        return testEntity;
    }

    public List<TestEntity> testEntities() {
        return testEntities;
    }

    public enum TestEnum {
        FOO, BAR
    }
}
