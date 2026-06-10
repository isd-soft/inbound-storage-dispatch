package com.isd.wms.entity;

import com.isd.wms.enums.TaskStatus;
import com.isd.wms.enums.TaskType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.Hibernate;
import java.sql.Timestamp;
import java.util.Objects;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "tasks")
public class Task extends BaseTimestampEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "task_gen")
    @SequenceGenerator(name = "task_gen", sequenceName = "tasks_sequence", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supervisor_id", nullable = false)
    private User supervisor;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_type", nullable = false)
    private TaskType taskType;

    @Column(name = "requested_quantity", nullable = false)
    private Integer requestedQuantity;

    @Column(name = "completed_at")
    private Timestamp completedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        Task task = (Task) o;
        return id != null && Objects.equals(id, task.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
