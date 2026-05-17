package com.autopulse.geographyservice.repository;

import com.autopulse.geographyservice.model.entity.Locality;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LocalityRepository extends JpaRepository<Locality, UUID>, JpaSpecificationExecutor<Locality> {

    @Override
    @EntityGraph(attributePaths = "region")
    Optional<Locality> findById(UUID id);

    @Override
    @EntityGraph(attributePaths = "region")
    Page<Locality> findAll(Specification<Locality> specification, Pageable pageable);

    @EntityGraph(attributePaths = "region")
    Optional<Locality> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, UUID id);
}
