package com.receiptapp.expenses.purchase;

import com.receiptapp.expenses.category.ExpenseCategory;
import com.receiptapp.expenses.common.BusinessRuleException;
import com.receiptapp.expenses.common.CurrencyCode;
import com.receiptapp.expenses.common.MeasurementUnit;
import com.receiptapp.expenses.common.TimestampedEntity;
import com.receiptapp.expenses.company.Company;
import com.receiptapp.expenses.user.AppUser;
import com.receiptapp.expenses.user.Role;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A worker asking permission BEFORE buying. Matches "purchase_requests" (V3).
 */
@Entity
@Table(name = "purchase_requests")
public class PurchaseRequest extends TimestampedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by_id", nullable = false)
    private AppUser requestedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private ExpenseCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "quote_source", nullable = false, length = 20)
    private QuoteSource quoteSource;

    @Column(name = "purpose", nullable = false, length = 255)
    private String purpose;

    @Column(name = "supplier_name", length = 150)
    private String supplierName;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false, length = 3)
    private CurrencyCode currency;

    @Column(name = "estimated_total", nullable = false, precision = 14, scale = 2)
    private BigDecimal estimatedTotal;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PurchaseRequestStatus status = PurchaseRequestStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decided_by_id")
    private AppUser decidedBy;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "decision_note", length = 500)
    private String decisionNote;

    // Optimistic locking: if two managers decide at the same moment, the second
    // save fails.
    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @CreationTimestamp
    @Column(name = "submitted_at", nullable = false, updatable = false)
    private Instant submittedAt;

    @OneToMany(mappedBy = "purchaseRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNumber ASC")
    private List<PurchaseRequestItem> items = new ArrayList<>();

    protected PurchaseRequest() {
    }

    public PurchaseRequest(AppUser requestedBy, ExpenseCategory category, QuoteSource quoteSource,
            String purpose, String supplierName, CurrencyCode currency,
            BigDecimal estimatedTotal, String notes) {
        this.requestedBy = requestedBy;
        this.company = requestedBy.getCompany();
        this.category = category;
        this.quoteSource = quoteSource;
        this.purpose = purpose;
        this.supplierName = supplierName;
        this.currency = currency;
        this.estimatedTotal = estimatedTotal;
        this.notes = notes;
    }

    // ---------------------------------------------------------------- items

    /**
     * Adds a line. For a worker's own estimate, the total is kept equal to the sum
     * of the lines.
     */
    public PurchaseRequestItem addItem(String description, BigDecimal quantity,
            MeasurementUnit unit, BigDecimal estimatedUnitPrice) {
        requireStatus(PurchaseRequestStatus.PENDING, "add items to");
        PurchaseRequestItem item = new PurchaseRequestItem(
                this, items.size() + 1, description, quantity, unit, estimatedUnitPrice);
        items.add(item);
        if (quoteSource == QuoteSource.WORKER_ESTIMATE) {
            estimatedTotal = items.stream()
                    .map(PurchaseRequestItem::calculateLineTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        return item;
    }

    // ---------------------------------------------------------------- decisions

    public void approve(AppUser decider, String note) {
        decide(decider, PurchaseRequestStatus.APPROVED, note);
    }

    public void reject(AppUser decider, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BusinessRuleException("A rejection must include a reason for the worker.");
        }
        decide(decider, PurchaseRequestStatus.REJECTED, reason);
    }

    /** Approved requests stay open for receipts until the worker submits them. */
    public boolean isOpenForReceipts() {
        return status == PurchaseRequestStatus.APPROVED;
    }

    /**
     * The worker decided not to buy. Allowed before or after approval. The service
     * must first check no receipts are attached (draft receipts are discarded).
     */
    public void cancel() {
        if (status != PurchaseRequestStatus.PENDING && status != PurchaseRequestStatus.APPROVED) {
            throw new BusinessRuleException("Only pending or approved requests can be cancelled.");
        }
        status = PurchaseRequestStatus.CANCELLED;
    }

    /**
     * Called by the service once the worker submits all receipts for this request.
     */
    public void markCompleted() {
        requireStatus(PurchaseRequestStatus.APPROVED, "complete");
        status = PurchaseRequestStatus.COMPLETED;
    }

    private void decide(AppUser decider, PurchaseRequestStatus outcome, String note) {
        requireStatus(PurchaseRequestStatus.PENDING, "decide on");
        if (!Objects.equals(decider.getCompany().getId(), company.getId())) {
            throw new BusinessRuleException("You can only decide on requests in your own company.");
        }
        if (decider.getRole() != Role.MANAGER && decider.getRole() != Role.ADMIN) {
            throw new BusinessRuleException("Only managers and admins can approve or reject requests.");
        }
        if (Objects.equals(decider.getId(), requestedBy.getId())) {
            throw new BusinessRuleException("You can't approve or reject your own request.");
        }
        this.status = outcome;
        this.decidedBy = decider;
        this.decidedAt = Instant.now();
        this.decisionNote = note;
    }

    private void requireStatus(PurchaseRequestStatus expected, String action) {
        if (status != expected) {
            throw new BusinessRuleException(
                    "Can't " + action + " a request that is " + status + ".");
        }
    }

    // ---------------------------------------------------------------- getters

    public Long getId() {
        return id;
    }

    public Company getCompany() {
        return company;
    }

    public AppUser getRequestedBy() {
        return requestedBy;
    }

    public ExpenseCategory getCategory() {
        return category;
    }

    public QuoteSource getQuoteSource() {
        return quoteSource;
    }

    public String getPurpose() {
        return purpose;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public CurrencyCode getCurrency() {
        return currency;
    }

    public BigDecimal getEstimatedTotal() {
        return estimatedTotal;
    }

    public String getNotes() {
        return notes;
    }

    public PurchaseRequestStatus getStatus() {
        return status;
    }

    public AppUser getDecidedBy() {
        return decidedBy;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }

    public String getDecisionNote() {
        return decisionNote;
    }

    public long getVersion() {
        return version;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public List<PurchaseRequestItem> getItems() {
        return Collections.unmodifiableList(items);
    }
}