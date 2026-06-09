package com.segula.saasgestion.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "project_work_ticket")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ProjectWorkTicket {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "ticket_id", length = 20)
    private String ticketId; // ex: UW_0001

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_type_id", nullable = false)
    private ProjectWorkType workType;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "consultant", length = 100)
    private String consultant;

    @Column(name = "assigned_date")
    private LocalDate assignedDate;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "delivery_date")
    private LocalDate deliveryDate;

    @Column(name = "first_pass")
    private Boolean firstPass; // FTR

    // TO_DO | IN_PROGRESS | DELIVERED | REJECTED
    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "comments", length = 300)
    private String comments;
}
