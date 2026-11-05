package com.receiptapp.expenses.purchase;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.Repository;

import java.util.Optional;

public interface PurchaseRequestRepository extends Repository<PurchaseRequest, Long> {

    <S extends PurchaseRequest> S save(S request);

    @EntityGraph(attributePaths = { "requestedBy", "category" })
    Optional<PurchaseRequest> findByIdAndCompanyId(Long id, Long companyId);

    /** A worker's own requests. */
    @EntityGraph(attributePaths = { "category" })
    Page<PurchaseRequest> findByCompanyIdAndRequestedById(Long companyId, Long requestedById, Pageable pageable);

    /** A manager's list, e.g. everything PENDING in the company. */
    @EntityGraph(attributePaths = { "requestedBy", "category" })
    Page<PurchaseRequest> findByCompanyIdAndStatus(Long companyId, PurchaseRequestStatus status, Pageable pageable);
}