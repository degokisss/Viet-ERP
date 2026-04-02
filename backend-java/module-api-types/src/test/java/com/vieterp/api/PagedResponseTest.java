package com.vieterp.api;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class PagedResponseTest {

    @Test
    void pagedResponseBuilderWorks() {
        var resp = PagedResponse.<String>builder()
            .items(List.of("a", "b", "c"))
            .page(1)
            .pageSize(10)
            .total(25)
            .hasNext(true)
            .build();

        assertEquals(3, resp.items().size());
        assertEquals(1, resp.page());
        assertEquals(10, resp.pageSize());
        assertEquals(25, resp.total());
        assertTrue(resp.hasNext());
    }

    @Test
    void ofFactoryComputesHasNext() {
        var resp = PagedResponse.of(List.of("a", "b"), 0, 10, 25);
        assertTrue(resp.hasNext());
        assertEquals(25, resp.total());
    }

    @Test
    void ofFactoryFalseWhenLastPage() {
        var resp = PagedResponse.of(List.of("a", "b"), 2, 10, 25);
        assertFalse(resp.hasNext());
    }

    @Test
    void errorResponseBuilderWorks() {
        var err = ErrorResponse.builder()
            .type("https://vieterp.com/errors/not-found")
            .title("Not Found")
            .status(404)
            .detail("Employee not found")
            .build();

        assertEquals(404, err.status());
        assertEquals("Not Found", err.title());
        assertNotNull(err.timestamp());
    }

    }
