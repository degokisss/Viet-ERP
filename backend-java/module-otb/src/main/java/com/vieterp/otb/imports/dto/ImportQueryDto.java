package com.vieterp.otb.imports.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportQueryDto {
    private ImportTargetEnum target;
    private Integer page;
    private Integer pageSize;
    private String search;
    private String sortBy;
    private String sortOrder;

    public Integer getPage() {
        return page != null ? page : 1;
    }

    public Integer getPageSize() {
        return pageSize != null ? pageSize : 20;
    }

    public String getSortBy() {
        return sortBy != null ? sortBy : "createdAt";
    }

    public String getSortOrder() {
        return sortOrder != null ? sortOrder : "DESC";
    }
}
