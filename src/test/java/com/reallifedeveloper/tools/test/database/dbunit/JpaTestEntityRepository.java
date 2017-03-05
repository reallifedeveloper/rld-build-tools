package com.reallifedeveloper.tools.test.database.dbunit;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaTestEntityRepository extends JpaRepository<TestEntity, Long> {

}
