package com.receiptapp.expenses.expense;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ExpenseRepository extends Repository<Expense, Long> {

    <S extends Expense> S save(S expense);

    /**
     * Only for discarding a worker's own draft. The service checks it's a draft
     * first.
     */
    void delete(Expense expense);

    @EntityGraph(attributePaths = { "submittedBy", "category" })
    Optional<Expense> findByIdAndCompanyId(Long id, Long companyId);

    /** A worker's own claims, including their drafts. */
    @EntityGraph(attributePaths = { "category" })
    Page<Expense> findByCompanyIdAndSubmittedById(Long companyId, Long submittedById, Pageable pageable);

    /**
     * Managers' and finance's lists. Drafts are excluded inside the query itself,
     * so no caller can ever show a worker's unfinished claims to their boss.
     */
    @EntityGraph(attributePaths = { "submittedBy", "category" })
    @Query("""
            select e from Expense e
            where e.company.id = :companyId
              and e.status in :statuses
              and e.status <> com.receiptapp.expenses.expense.ExpenseStatus.DRAFT
            """)
    Page<Expense> findForReview(@Param("companyId") Long companyId,
            @Param("statuses") Collection<ExpenseStatus> statuses,
            Pageable pageable);

    /**
     * All receipts attached to one purchase request, in the order they were added.
     */
    List<Expense> findByCompanyIdAndPurchaseRequestIdOrderByIdAsc(Long companyId, Long purchaseRequestId);

    /** The drafts to submit together when the worker taps "Submit receipts". */
    List<Expense> findByCompanyIdAndPurchaseRequestIdAndStatus(Long companyId, Long purchaseRequestId,
            ExpenseStatus status);

    /** Has this ZIMRA fiscal invoice number been claimed before? */
    List<Expense> findByCompanyIdAndFiscalInvoiceNumberIgnoreCase(Long companyId, String fiscalInvoiceNumber);

    @Query("""
            select new com.receiptapp.expenses.expense.CurrencyTotal(e.currency, sum(e.amount), count(e))
            from Expense e
            where e.company.id = :companyId
              and e.status = :status
              and e.paidWith = :paidWith
            group by e.currency
            order by e.currency
            """)
    List<CurrencyTotal> sumByCurrency(@Param("companyId") Long companyId,
            @Param("status") ExpenseStatus status,
            @Param("paidWith") PaidWith paidWith);

    /**
     * What the company owes its workers right now, per currency. Same rule as
     * Expense.isOwedToWorker().
     */
    default List<CurrencyTotal> owedToWorkers(Long companyId) {
        return sumByCurrency(companyId, ExpenseStatus.VERIFIED, PaidWith.WORKER_OWN_MONEY);
    }
}