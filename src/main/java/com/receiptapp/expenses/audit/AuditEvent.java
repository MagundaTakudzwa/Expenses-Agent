package com.receiptapp.expenses.audit;

import com.receiptapp.expenses.company.Company;
import com.receiptapp.expenses.user.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/**
 * One permanent line in the audit trail: who did what, to which record, and
 * when.
 * Matches "audit_events" (V5). Append-only: there are no methods that change
 * it.
 */
@Entity
@Immutable
@Table(name = "audit_events")
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    // Empty when the system did it, e.g. the receipt reader.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private AppUser actor;

    @Column(name = "action", nullable = false, length = 50)
    private String action;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 30)
    private AuditEntityType entityType;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(name = "old_status", length = 20)
    private String oldStatus;

    @Column(name = "new_status", length = 20)
    private String newStatus;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "details")
    private String details;

    @CreationTimestamp
    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    protected AuditEvent() {
    }

    private AuditEvent(Company company, AppUser actor, String action, AuditEntityType entityType,
            Long entityId, String oldStatus, String newStatus, String details) {
        this.company = company;
        this.actor = actor;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.details = details;
    }

    /** Something a person did. */
    public static AuditEvent byUser(AppUser actor, String action, AuditEntityType entityType, Long entityId,
            String oldStatus, String newStatus, String detailsJson) {
        return new AuditEvent(actor.getCompany(), actor, action, entityType, entityId,
                oldStatus, newStatus, detailsJson);
    }

    /** Something the system did on its own, e.g. reading a receipt. */
    public static AuditEvent bySystem(Company company, String action, AuditEntityType entityType,
            Long entityId, String detailsJson) {
        return new AuditEvent(company, null, action, entityType, entityId, null, null, detailsJson);
    }

    public Long getId() {
        return id;
    }

    public Company getCompany() {
        return company;
    }

    public AppUser getActor() {
        return actor;
    }

    public String getAction() {
        return action;
    }

    public AuditEntityType getEntityType() {
        return entityType;
    }

    public Long getEntityId() {
        return entityId;
    }

    public String getOldStatus() {
        return oldStatus;
    }

    public String getNewStatus() {
        return newStatus;
    }

    public String getDetails() {
        return details;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}