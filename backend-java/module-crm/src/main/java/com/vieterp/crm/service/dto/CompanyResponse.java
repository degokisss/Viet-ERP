package com.vieterp.crm.service.dto;

import com.vieterp.crm.domain.Company;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record CompanyResponse(
    UUID id,
    String name,
    String domain,
    String industry,
    Company.CompanySize size,
    String phone,
    String email,
    String website,
    String address,
    String city,
    String province,
    String country,
    String taxCode,
    String notes,
    String logoUrl,
    String ownerId,
    Instant createdAt,
    Instant updatedAt
) {}
