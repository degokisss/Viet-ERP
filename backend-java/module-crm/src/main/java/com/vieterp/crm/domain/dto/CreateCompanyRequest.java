package com.vieterp.crm.domain.dto;

import com.vieterp.crm.domain.Company;
import jakarta.validation.constraints.*;
import lombok.Builder;

@Builder
public record CreateCompanyRequest(
    @NotBlank(message = "Name is required") @Size(max = 255) String name,
    @Size(max = 255) String domain,
    @Size(max = 100) String industry,
    Company.CompanySize size,
    @Size(max = 50) String phone,
    @Email(message = "Email must be valid") @Size(max = 191) String email,
    @Size(max = 255) String website,
    @Size(max = 500) String address,
    @Size(max = 100) String city,
    @Size(max = 100) String province,
    @Size(max = 10) String country,
    @Size(max = 50) String taxCode,
    String notes,
    @Size(max = 500) String logoUrl,
    @NotBlank(message = "Owner ID is required") String ownerId
) {}
