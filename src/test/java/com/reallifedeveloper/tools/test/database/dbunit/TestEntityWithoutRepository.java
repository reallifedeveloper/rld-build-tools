package com.reallifedeveloper.tools.test.database.dbunit;

import java.util.UUID;

import jakarta.annotation.Nullable;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

@Entity
@Table(name = "TEST_ENTITY_WITHOUT_REPOSITORY")
@Getter
@Setter
@Accessors(fluent = true)
@ToString
public class TestEntityWithoutRepository {

    @Id
    private UUID id;

    private String name;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "db_unit_test_entity_id")
    @Nullable
    private DbUnitTestEntity dbUnitTestEntity;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "one_to_one_db_unit_test_entity_id")
    @Nullable
    private DbUnitTestEntity oneToOneDbUnitTestEntity;

    public TestEntityWithoutRepository(UUID id, String name) {
        this.id = id;
        this.name = name;
    }

    /**
     * Required by JPA.
     */
    @SuppressWarnings("NullAway")
    /* package-private */ TestEntityWithoutRepository() {
    }

}
