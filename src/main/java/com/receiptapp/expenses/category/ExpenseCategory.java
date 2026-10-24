package com.receiptapp.expenses.category;

import com.receiptapp.expenses.common.TimestampedEntity;
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

/**
 * One company's expense category, e.g. "Fuel" or "ZINARA tolls". Matches
 * "expense_categories" (V2).
 */
@Entity
@Table(name = "expense_categories")
public class ExpenseCategory extends TimestampedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "claim_type", nullable = false, length = 20)
    private ClaimType claimType;

    @Column(name = "requires_receipt", nullable = false)
    private boolean requiresReceipt;

    @Column(name = "payroll_code", length = 30)
    private String payrollCode;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected ExpenseCategory() {
    }

    public ExpenseCategory(Company company, String name, ClaimType claimType,
            boolean requiresReceipt, int sortOrder) {
        this.company = company;
        this.name = name;
        this.claimType = claimType;
        this.requiresReceipt = requiresReceipt;
        this.sortOrder = sortOrder;
    }

    public void rename(String newName) {
        this.name = newName;
    }

    public void setPayrollCode(String payrollCode) {
        this.payrollCode = payrollCode;
    }

    /** Hidden from the picker, but old expenses keep showing it. */
    public void deactivate() {
        this.active = false;
    }

    public void reactivate() {
        this.active = true;
    }

    public Long getId() {
        return id;
    }

    public Company getCompany() {
        return company;
    }

    public String getName() {
        return name;
    }

    public ClaimType getClaimType() {
        return claimType;
    }

    public boolean isRequiresReceipt() {
        return requiresReceipt;
    }

    public String getPayrollCode() {
        return payrollCode;
    }

    public boolean isActive() {
        return active;
    }

    public int getSortOrder() {
        return sortOrder;
    }
}