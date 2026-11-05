package com.receiptapp.expenses.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.Repository;

import java.util.List;

/** Append-only: there is deliberately no delete method. */
public interface AuditEventRepository extends Repository<AuditEvent, Long> {

    <S extends AuditEvent> S save(S event);

    /** The full history of one record, oldest first. */
    @EntityGraph(attributePaths = { "actor" })
    List<AuditEvent> findByCompanyIdAndEntityTypeAndEntityIdOrderByOccurredAtAsc(
            Long companyId, AuditEntityType entityType, Long entityId);

    /** Recent activity across the company. */
    @EntityGraph(attributePaths = { "actor" })
    Page<AuditEvent> findByCompanyId(Long companyId, Pageable pageable);
}