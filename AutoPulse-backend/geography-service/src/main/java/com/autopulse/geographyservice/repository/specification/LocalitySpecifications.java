package com.autopulse.geographyservice.repository.specification;

import com.autopulse.geographyservice.model.entity.Locality;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class LocalitySpecifications {

    private LocalitySpecifications() {
    }

    public static Specification<Locality> withFilters(String name,
                                                      String code,
                                                      UUID regionId,
                                                      String regionCode) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            Join<Object, Object> regionJoin = null;

            if (name != null && !name.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("name")), contains(name)));
            }
            if (code != null && !code.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("code")), contains(code)));
            }
            if (regionId != null || (regionCode != null && !regionCode.isBlank())) {
                regionJoin = root.join("region");
                query.distinct(true);
            }
            if (regionId != null && regionJoin != null) {
                predicates.add(cb.equal(regionJoin.get("id"), regionId));
            }
            if (regionCode != null && !regionCode.isBlank() && regionJoin != null) {
                predicates.add(cb.like(cb.lower(regionJoin.get("code")), contains(regionCode)));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static String contains(String value) {
        return "%" + value.trim().toLowerCase() + "%";
    }
}
