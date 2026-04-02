package com.vieterp.otb.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

@Entity
@Table(name = "s_k_u_proposal_header")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SKUProposalHeader {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "allocate_header_id")
    private AllocateHeader allocateHeader;

    private Integer version;

    @Column(length = 191)
    private String status;

    @Column(name = "is_final_version")
    private Boolean isFinalVersion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User creator;

    @Column(name = "created_at")
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updater;

    @Column(name = "updated_at", nullable = true)
    private Instant updatedAt;
}
