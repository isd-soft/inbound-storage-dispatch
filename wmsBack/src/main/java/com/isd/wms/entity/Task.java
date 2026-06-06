package com.isd.wms.entity;

import com.isd.wms.enums.TaskStatus;
import com.isd.wms.enums.TaskType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Timestamp;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "tasks")
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "task_seq")
    @SequenceGenerator(name = "task_seq", sequenceName = "tasks_sequence", allocationSize = 1)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "supervisor_id")
    private User supervisor;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_type", nullable = false, length = 20)
    private TaskType taskType;

    @NonNull
    private Integer requestedQuantity;

    @CreationTimestamp
    @Column(updatable = false, nullable = false)
    private Timestamp createdAt;

    private Timestamp completedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_status", nullable = false, length = 30)
    private TaskStatus taskStatus;
}
