package com.vieterp.otb.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "import_session")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 191)
    private String target;

    @Column(name = "file_name", length = 191)
    private String fileName;

    @Column(length = 191)
    private String status;

    @Column(name = "total_rows")
    private Integer totalRows;

    @Column(name = "success_rows")
    private Integer successRows;

    @Column(name = "error_rows")
    private Integer errorRows;

    @Column(name = "error_log", length = 191)
    private String errorLog;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at", nullable = true)
    private Instant updatedAt;
}
