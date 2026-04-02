package com.vieterp.crm.domain.dto;

import com.vieterp.crm.domain.Company;
import com.vieterp.crm.domain.Contact;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record ContactResponse(
    UUID id,
    String firstName,
    String lastName,
    String email,
    String phone,
    String mobile,
    String jobTitle,
    String department,
    String avatarUrl,
    String notes,
    Contact.LeadSource source,
    Contact.ContactStatus status,
    UUID companyId,
    String companyName,
    String country,
    String externalHrmId,
    Integer score,
    Instant lastActivityAt,
    String ownerId,
    Instant createdAt,
    Instant updatedAt
) {}
