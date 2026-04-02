package com.vieterp.otb.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.*;

@Entity
@Table(name = "allocate_header")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AllocateHeader {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "budget_id")
    private Budget budget;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    private Brand brand;

    private Integer version;

    @Column(name = "is_final_version")
    private Boolean isFinalVersion;

    @Column(name = "is_snapshot")
    private Boolean isSnapshot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id")
    private Ticket ticket;

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

    @OneToMany(mappedBy = "allocateHeader", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BudgetAllocate> budgetAllocates = new ArrayList<>();

    @OneToMany(mappedBy = "allocateHeader")
    @Builder.Default
    private List<PlanningHeader> planningHeaders = new ArrayList<>();

    @OneToMany(mappedBy = "allocateHeader")
    @Builder.Default
    private List<SKUProposalHeader> skuProposalHeaders = new ArrayList<>();
}
