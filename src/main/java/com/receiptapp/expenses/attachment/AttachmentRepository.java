package com.receiptapp.expenses.attachment;

import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

public interface AttachmentRepository extends Repository<Attachment, Long> {

    <S extends Attachment> S save(S attachment);

    Optional<Attachment> findByIdAndCompanyId(Long id, Long companyId);

    List<Attachment> findByCompanyIdAndExpenseIdOrderByCreatedAtAsc(Long companyId, Long expenseId);

    List<Attachment> findByCompanyIdAndPurchaseRequestIdOrderByCreatedAtAsc(Long companyId, Long purchaseRequestId);

    /** Duplicate detection: the same photo uploaded before in this company. */
    List<Attachment> findByCompanyIdAndSha256(Long companyId, String sha256);
}