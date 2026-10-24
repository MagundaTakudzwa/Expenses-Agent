package com.receiptapp.expenses.purchase;

import com.receiptapp.expenses.common.MeasurementUnit;
import com.receiptapp.expenses.company.Company;
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
import org.hibernate.annotations.Generated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

/**
 * One line of a quote, e.g. "Cooking oil 2L x 2 @ $4.50". Matches
 * "purchase_request_items" (V3).
 */
@Entity
@Table(name = "purchase_request_items")
public class PurchaseRequestItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_request_id", nullable = false)
    private PurchaseRequest purchaseRequest;

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

    @Column(name = "estimated_unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal estimatedUnitPrice;

    // Calculated by PostgreSQL. Hibernate never writes it, and reads it back after
    // saving.
    @Generated
    @Column(name = "line_total", insertable = false, updatable = false, precision = 14, scale = 2)
    private BigDecimal lineTotal;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected PurchaseRequestItem() {
    }

    /**
     * Only PurchaseRequest creates items, so it can number them and keep its total
     * right.
     */
    PurchaseRequestItem(PurchaseRequest purchaseRequest, int lineNumber, String description,
            BigDecimal quantity, MeasurementUnit unit, BigDecimal estimatedUnitPrice) {
        this.purchaseRequest = purchaseRequest;
        this.company = purchaseRequest.getCompany();
        this.lineNumber = lineNumber;
        this.description = description;
        this.quantity = quantity;
        this.unit = unit;
        this.estimatedUnitPrice = estimatedUnitPrice;
    }

    /** The same sum the database calculates, available before the row is saved. */
    public BigDecimal calculateLineTotal() {
        return quantity.multiply(estimatedUnitPrice).setScale(2, RoundingMode.HALF_UP);
    }

    public void setNormalizedName(String normalizedName) {
        this.normalizedName = normalizedName;
    }

    public Long getId() {
        return id;
    }

    public PurchaseRequest getPurchaseRequest() {
        return purchaseRequest;
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

    public BigDecimal getEstimatedUnitPrice() {
        return estimatedUnitPrice;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}