package com.vieterp.otb.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.*;

@Entity
@Table(name = "budget")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Budget {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 191)
    private String name;

    private BigDecimal amount;

    @Column(length = 191)
    private String description;

    @Column(length = 191)
    private String status;

    @Column(name = "fiscal_year")
    private Integer fiscalYear;

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

    @OneToMany(mappedBy = "budget", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AllocateHeader> allocateHeaders = new ArrayList<>();

    @OneToMany(mappedBy = "budget")
    @Builder.Default
    private List<Ticket> tickets = new ArrayList<>();
}
