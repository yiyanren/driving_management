package com.example.demo.common;

import org.springframework.data.domain.Page;

import java.util.List;

public record PageResult<T>(List<T> content, long totalElements, int totalPages, int page, int size) {
    public static <T> PageResult<T> from(Page<T> p) {
        return new PageResult<>(p.getContent(), p.getTotalElements(), p.getTotalPages(), p.getNumber(), p.getSize());
    }
}
