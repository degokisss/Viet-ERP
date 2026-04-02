package com.vieterp.otb.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "imported_record")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportedRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id")
    private Long sessionId;

    @Column(name = "row_number")
    private Integer rowNumber;

    @Column(length = 191)
    private String data;

    @Column(length = 191)
    private String status;

    @Column(length = 191)
    private String error;

    @Column(name = "created_at")
    private Instant createdAt;
}
