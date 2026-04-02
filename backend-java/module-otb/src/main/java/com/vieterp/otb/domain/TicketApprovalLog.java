package com.vieterp.otb.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "ticket_approval_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketApprovalLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_id")
    private Long ticketId;

    @Column(name = "approval_workflow_level_id")
    private Long approvalWorkflowLevelId;

    @Column(name = "approver_user_id")
    private Long approverUserId;

    @Column(name = "is_approved")
    private Boolean isApproved;

    @Column(length = 191)
    private String comment;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "updated_at", nullable = true)
    private Instant updatedAt;
}
