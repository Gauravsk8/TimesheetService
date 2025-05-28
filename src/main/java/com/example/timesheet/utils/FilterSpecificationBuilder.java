package com.example.timesheet.utils;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import com.example.timesheet.dto.paginationdto.FilterRequest;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import org.springframework.data.jpa.domain.Specification;
import java.util.ArrayList;
import java.util.List;

public class FilterSpecificationBuilder<T> {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final List<String> DATE_FIELDS = List.of("endDate", "startDate"); // Add all timestamp fields here

    public Specification<T> build(List<FilterRequest> filters) {
        return (root, query, criteriaBuilder) -> {
            if (filters == null || filters.isEmpty()) {
                return null;
            }

            List<Predicate> predicates = new ArrayList<>();

            for (FilterRequest filter : filters) {
                String field = filter.getField();
                String operator = filter.getOperator().toLowerCase();
                String value = filter.getValue();

                if (DATE_FIELDS.contains(field)) {
                    // Handle timestamp fields
                    handleTimestampField(predicates, root, criteriaBuilder, field, operator, value);
                } else {
                    // Handle string fields
                    handleStringField(predicates, root, criteriaBuilder, field, operator, value);
                }
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private void handleTimestampField(List<Predicate> predicates, Root<?> root, CriteriaBuilder cb,
                                      String field, String operator, String value) {
        try {
            if (operator.equals("like")) {
                // For 'like' on dates, we'll check if the date string contains the pattern
                Expression<String> dateStringExpr = cb.function("to_char", String.class,
                        root.get(field), cb.literal("YYYY-MM-DD"));
                predicates.add(cb.like(dateStringExpr, "%" + value + "%"));
            } else {
                // For other operators, parse as date and compare
                LocalDate localDate = LocalDate.parse(value, DATE_FORMATTER);
                LocalDateTime startOfDay = localDate.atStartOfDay();
                LocalDateTime endOfDay = localDate.plusDays(1).atStartOfDay();

                switch (operator) {
                    case "eq":
                        // Equal means the date falls on this day (any time)
                        predicates.add(cb.between(root.get(field),
                                Timestamp.valueOf(startOfDay),
                                Timestamp.valueOf(endOfDay.minusNanos(1))));
                        break;
                    case "gt":
                        predicates.add(cb.greaterThan(root.get(field), Timestamp.valueOf(endOfDay.minusNanos(1))));
                        break;
                    case "lt":
                        predicates.add(cb.lessThan(root.get(field), Timestamp.valueOf(startOfDay)));
                        break;
                    case "gte":
                        predicates.add(cb.greaterThanOrEqualTo(root.get(field), Timestamp.valueOf(startOfDay)));
                        break;
                    case "lte":
                        predicates.add(cb.lessThanOrEqualTo(root.get(field), Timestamp.valueOf(endOfDay.minusNanos(1))));
                        break;
                    default:
                        // Default to equal if operator not recognized
                        predicates.add(cb.between(root.get(field),
                                Timestamp.valueOf(startOfDay),
                                Timestamp.valueOf(endOfDay.minusNanos(1))));
                }
            }
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format for field " + field +
                    ". Expected format: yyyy-MM-dd");
        }
    }

    private void handleStringField(List<Predicate> predicates, Root<?> root, CriteriaBuilder cb,
                                   String field, String operator, String value) {
        Path<String> path = root.get(field);

        switch (operator) {
            case "eq":
                predicates.add(cb.equal(cb.lower(path), value.toLowerCase()));
                break;
            case "like":
                predicates.add(cb.like(cb.lower(path), "%" + value.toLowerCase() + "%"));
                break;
            case "gt":
                predicates.add(cb.greaterThan(cb.lower(path), value.toLowerCase()));
                break;
            case "lt":
                predicates.add(cb.lessThan(cb.lower(path), value.toLowerCase()));
                break;
            case "gte":
                predicates.add(cb.greaterThanOrEqualTo(cb.lower(path), value.toLowerCase()));
                break;
            case "lte":
                predicates.add(cb.lessThanOrEqualTo(cb.lower(path), value.toLowerCase()));
                break;
            default:
                predicates.add(cb.equal(cb.lower(path), value.toLowerCase()));
        }
    }
}

