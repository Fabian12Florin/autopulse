package com.autopulse.geographyservice.repository.specification;

import com.autopulse.geographyservice.model.entity.Region;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class RegionSpecifications {

    private RegionSpecifications() {
    }

    public static Specification<Region> withFilters(String name, String code) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (name != null && !name.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("name")), contains(name)));
            }
            if (code != null && !code.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("code")), contains(code)));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static String contains(String value) {
        return "%" + value.trim().toLowerCase() + "%";
    }
}
