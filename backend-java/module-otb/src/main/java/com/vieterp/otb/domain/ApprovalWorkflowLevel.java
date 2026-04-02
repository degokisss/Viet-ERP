package com.vieterp.otb.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "approval_workflow_level")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalWorkflowLevel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "approval_workflow_id")
    private Long approvalWorkflowId;

    @Column(name = "level_order")
    private Integer levelOrder;

    @Column(name = "level_name", length = 191)
    private String levelName;

    @Column(name = "approver_user_id")
    private Long approverUserId;

    @Column(name = "is_required")
    private Boolean isRequired;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "updated_at", nullable = true)
    private Instant updatedAt;
}
