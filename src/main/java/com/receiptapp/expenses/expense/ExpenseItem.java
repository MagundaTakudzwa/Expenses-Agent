package com.receiptapp.expenses.expense;

import com.receiptapp.expenses.common.BusinessRuleException;
import com.receiptapp.expenses.common.MeasurementUnit;
import com.receiptapp.expenses.company.Company;
import com.receiptapp.expenses.purchase.PurchaseRequestItem;
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

import java.math.BigDecimal;
import java.time.Instant;

/** One line from a receipt. Matches "expense_items" (V4). */
@Entity
@Table(name = "expense_items")
public class ExpenseItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "expense_id", nullable = false)
    private Expense expense;

    @Column(name = "line_number", nullable = false)
    private int lineNumber;

    @Column(name = "description", nullable = false, length = 200)
    private String description;

    @Column(name = "normalized_name", length = 200)
    private String normalizedName;

    @Column(name = "quantity", nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "unit", nullable = false, length = 10)
    private MeasurementUnit unit;

    // Many till slips only print the line total, so the unit price is optional.
    @Column(name = "unit_price", precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "line_total", nullable = false, precision = 14, scale = 2)
    private BigDecimal lineTotal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_request_item_id")
    private PurchaseRequestItem matchedRequestItem;

    @Enumerated(EnumType.STRING)
    @Column(name = "match_method", length = 20)
    private MatchMethod matchMethod;

    @Column(name = "match_confidence", precision = 4, scale = 3)
    private BigDecimal matchConfidence;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ExpenseItem() {
    }

    /** Only Expense creates items, so it can number them. */
    ExpenseItem(Expense expense, int lineNumber, String description, BigDecimal quantity,
            MeasurementUnit unit, BigDecimal unitPrice, BigDecimal lineTotal) {
        this.expense = expense;
        this.company = expense.getCompany();
        this.lineNumber = lineNumber;
        this.description = description;
        this.quantity = quantity;
        this.unit = unit;
        this.unitPrice = unitPrice;
        this.lineTotal = lineTotal;
    }

    /** Links this receipt line to a line of the approved quote. */
    public void matchTo(PurchaseRequestItem requestItem, MatchMethod method, BigDecimal confidence) {
        if (expense.getPurchaseRequest() == null
                || requestItem.getPurchaseRequest() != expense.getPurchaseRequest()) {
            throw new BusinessRuleException("A receipt line can only match an item from its own approved request.");
        }
        if (confidence != null
                && (confidence.compareTo(BigDecimal.ZERO) < 0 || confidence.compareTo(BigDecimal.ONE) > 0)) {
            throw new BusinessRuleException("Match confidence must be between 0 and 1.");
        }
        this.matchedRequestItem = requestItem;
        this.matchMethod = method;
        this.matchConfidence = confidence;
    }

    public void clearMatch() {
        this.matchedRequestItem = null;
        this.matchMethod = null;
        this.matchConfidence = null;
    }

    public void setNormalizedName(String normalizedName) {
        this.normalizedName = normalizedName;
    }

    public Long getId() {
        return id;
    }

    public Expense getExpense() {
        return expense;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public String getDescription() {
        return description;
    }

    public String getNormalizedName() {
        return normalizedName;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public MeasurementUnit getUnit() {
        return unit;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }

    public PurchaseRequestItem getMatchedRequestItem() {
        return matchedRequestItem;
    }

    public MatchMethod getMatchMethod() {
        return matchMethod;
    }

    public BigDecimal getMatchConfidence() {
        return matchConfidence;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}