package com.vieterp.api;

import java.util.List;
import lombok.Builder;

/**
 * Standard paginated response envelope.
 * Must match TypeScript PagedResponse in packages/api-types/src/paged.ts
 * — must be kept in sync.
 */
@Builder
public record PagedResponse<T>(
    List<T> items,
    int page,
    int pageSize,
    long total,
    boolean hasNext
) {
    public static <T> PagedResponse<T> of(List<T> items, int page, int pageSize, long total) {
        return PagedResponse.<T>builder()
            .items(items)
            .page(page)
            .pageSize(pageSize)
            .total(total)
            .hasNext((long) (page + 1) * pageSize < total)
            .build();
    }
}
