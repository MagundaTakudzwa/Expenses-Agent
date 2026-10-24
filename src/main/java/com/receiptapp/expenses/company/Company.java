package com.receiptapp.expenses.company;

import com.receiptapp.expenses.common.CurrencyCode;
import com.receiptapp.expenses.common.TimestampedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/** A business using the system. Matches the "companies" table from V1. */
@Entity
@Table(name = "companies")
public class Company extends TimestampedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "base_currency", nullable = false, length = 3)
    private CurrencyCode baseCurrency = CurrencyCode.USD;

    @Column(name = "variance_tolerance_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal varianceTolerancePercent = new BigDecimal("10.00");

    @Column(name = "allow_claims_without_approval", nullable = false)
    private boolean allowClaimsWithoutApproval = true;

    @Column(name = "max_amount_without_approval", precision = 12, scale = 2)
    private BigDecimal maxAmountWithoutApproval;

    @Column(name = "mileage_rate_per_km", precision = 10, scale = 4)
    private BigDecimal mileageRatePerKm;

    @Column(name = "daily_subsistence_rate", precision = 12, scale = 2)
    private BigDecimal dailySubsistenceRate;

    /**
     * Required by JPA. Protected so the rest of our code can't create an empty
     * company.
     */
    protected Company() {
    }

    public Company(String name, CurrencyCode baseCurrency) {
        this.name = name;
        this.baseCurrency = baseCurrency;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public CurrencyCode getBaseCurrency() {
        return baseCurrency;
    }

    public BigDecimal getVarianceTolerancePercent() {
        return varianceTolerancePercent;
    }

    public boolean isAllowClaimsWithoutApproval() {
        return allowClaimsWithoutApproval;
    }

    public BigDecimal getMaxAmountWithoutApproval() {
        return maxAmountWithoutApproval;
    }

    public BigDecimal getMileageRatePerKm() {
        return mileageRatePerKm;
    }

    public BigDecimal getDailySubsistenceRate() {
        return dailySubsistenceRate;
    }
}