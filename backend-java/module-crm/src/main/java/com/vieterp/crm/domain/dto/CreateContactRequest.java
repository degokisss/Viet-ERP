package com.vieterp.crm.domain.dto;

import com.vieterp.crm.domain.Contact;
import jakarta.validation.constraints.*;
import java.util.UUID;
import lombok.Builder;

@Builder
public record CreateContactRequest(
    @NotBlank(message = "First name is required") @Size(max = 100) String firstName,
    @NotBlank(message = "Last name is required") @Size(max = 100) String lastName,
    @Email(message = "Email must be valid") @Size(max = 191) String email,
    @Size(max = 50) String phone,
    @Size(max = 50) String mobile,
    @Size(max = 100) String jobTitle,
    @Size(max = 100) String department,
    @Size(max = 500) String avatarUrl,
    String notes,
    Contact.LeadSource source,
    Contact.ContactStatus status,
    UUID companyId,
    @Size(max = 10) String country,
    @Size(max = 100) String externalHrmId,
    Integer score,
    @NotBlank(message = "Owner ID is required") String ownerId
) {}
