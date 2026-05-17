package com.autopulse.routingservice.repository.specification;

import com.autopulse.routingservice.model.RoutingJob;
import com.autopulse.routingservice.web.dto.RoutingJobQueryRequest;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class RoutingJobSpecification {

    private RoutingJobSpecification() {
    }

    public static Specification<RoutingJob> withFilters(RoutingJobQueryRequest request) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (request.depotCode() != null) {
                predicates.add(criteriaBuilder.equal(root.get("depotCode"), request.depotCode()));
            }

            if (request.routeDate() != null) {
                predicates.add(criteriaBuilder.equal(root.get("routeDate"), request.routeDate()));
            }

            if (request.routeType() != null) {
                predicates.add(criteriaBuilder.equal(root.get("routeType"), request.routeType()));
            }

            if (request.status() != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), request.status()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
