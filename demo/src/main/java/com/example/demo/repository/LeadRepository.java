package com.example.demo.repository;

import com.example.demo.model.Lead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface LeadRepository extends JpaRepository<Lead, Long>, JpaSpecificationExecutor<Lead> {
    @Query("select l.status, count(l) from Lead l group by l.status")
    List<Object[]> countGroupByStatus();

    @Query("select l.source, count(l) from Lead l group by l.source")
    List<Object[]> countGroupBySource();

    @Query("select l.owner, count(l) from Lead l where l.status = '已转学员' and l.owner is not null and l.owner <> '' group by l.owner")
    List<Object[]> conversionByOwner();
}
