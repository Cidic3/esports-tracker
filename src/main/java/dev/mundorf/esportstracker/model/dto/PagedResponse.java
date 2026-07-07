package dev.mundorf.esportstracker.model.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Stable pagination envelope. We wrap Spring Data's {@link Page} rather than returning it directly:
 * serializing Page exposes its internal structure as an unstable JSON contract (Spring even warns
 * about this), so we expose only the fields a client needs.
 */
public record PagedResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last
) {
    public static <E, T> PagedResponse<T> from(Page<E> page, Function<E, T> mapper) {
        return new PagedResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast());
    }
}
