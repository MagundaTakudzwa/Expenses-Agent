package com.receiptapp.expenses.expense;

import com.receiptapp.expenses.category.ClaimType;
import com.receiptapp.expenses.category.ExpenseCategory;
import com.receiptapp.expenses.common.BusinessRuleException;
import com.receiptapp.expenses.common.CurrencyCode;
import com.receiptapp.expenses.common.MeasurementUnit;
import com.receiptapp.expenses.common.TimestampedEntity;
import com.receiptapp.expenses.company.Company;
import com.receiptapp.expenses.fuel.FuelType;
import com.receiptapp.expenses.purchase.PurchaseRequest;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A claim for money already spent (or a mileage or subsistence claim). Matches
 * "expenses" (V4, changed by V6). Every claim starts as a DRAFT until the
 * worker submits it.
 */
@Entity
@Table(name = "expenses")
public class Expense extends TimestampedEntity {

    private static final ZoneId ZIMBABWE = ZoneId.of("Africa/Harare");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submitted_by_id", nullable = false)
    private AppUser submittedBy;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private ExpenseCategory category;

    // A snapshot of the category's type when the claim was created.
    @Enumerated(EnumType.STRING)
    @Column(name = "claim_type", nullable = false, length = 20)
    private ClaimType claimType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_request_id")
    private PurchaseRequest purchaseRequest;

    @Column(name = "skip_approval_reason", length = 500)
    private String skipApprovalReason;

    // ---- what, where, when, how much
    @Column(name = "description", nullable = false, length = 255)
    private String description;

    @Column(name = "supplier_name", length = 150)
    private String supplierName;

    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false, length = 3)
    private CurrencyCode currency;

    @Column(name = "amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "paid_with", nullable = false, length = 20)
    private PaidWith paidWith;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 20)
    private PaymentMethod paymentMethod;

    @Column(name = "fiscal_invoice_number", length = 50)
    private String fiscalInvoiceNumber;

    // ---- fuel claims
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fuel_type_id")
    private FuelType fuelType;

    @Column(name = "fuel_litres", precision = 10, scale = 3)
    private BigDecimal fuelLitres;

    @Column(name = "fuel_price_per_litre", precision = 10, scale = 4)
    private BigDecimal fuelPricePerLitre;

    @Column(name = "vehicle_registration", length = 20)
    private String vehicleRegistration;

    @Column(name = "odometer_km")
    private Integer odometerKm;

    // ---- mileage claims (rate is a snapshot of the company's rate on the day)
    @Column(name = "trip_from", length = 150)
    private String tripFrom;

    @Column(name = "trip_to", length = 150)
    private String tripTo;

    @Column(name = "distance_km", precision = 10, scale = 1)
    private BigDecimal distanceKm;

    @Column(name = "mileage_rate", precision = 10, scale = 4)
    private BigDecimal mileageRate;

    // ---- subsistence claims (rate is a snapshot too)
    @Column(name = "trip_location", length = 150)
    private String tripLocation;

    @Column(name = "subsistence_days", precision = 5, scale = 1)
    private BigDecimal subsistenceDays;

    @Column(name = "daily_rate", precision = 12, scale = 2)
    private BigDecimal dailyRate;

    // ---- receipt reading
    @Enumerated(EnumType.STRING)
    @Column(name = "extraction_status", nullable = false, length = 20)
    private ExtractionStatus extractionStatus = ExtractionStatus.NOT_APPLICABLE;

    // Stored as PostgreSQL JSONB: whatever the receipt reader found.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "extracted_data")
    private String extractedData;

    // ---- review
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ExpenseStatus status = ExpenseStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_id")
    private AppUser reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "review_note", length = 500)
    private String reviewNote;

    // ---- reimbursement
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reimbursed_by_id")
    private AppUser reimbursedBy;

    @Column(name = "reimbursed_at")
    private Instant reimbursedAt;

    @Column(name = "reimbursement_reference", length = 100)
    private String reimbursementReference;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    // Empty while the claim is a draft; set when the worker submits it.
    @Column(name = "submitted_at")
    private Instant submittedAt;

    @OneToMany(mappedBy = "expense", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNumber ASC")
    private List<ExpenseItem> items = new ArrayList<>();

    protected Expense() {
    }

    /** Checks shared by every kind of claim. */
    private Expense(AppUser submitter, ExpenseCategory category, ClaimType expectedType,
            String description, LocalDate expenseDate, CurrencyCode currency,
            BigDecimal amount, PaidWith paidWith) {
        if (!sameCompany(category.getCompany(), submitter.getCompany())) {
            throw new BusinessRuleException("That category belongs to a different company.");
        }
        if (!category.isActive()) {
            throw new BusinessRuleException("The category '" + category.getName() + "' is no longer in use.");
        }
        if (category.getClaimType() != expectedType) {
            throw new BusinessRuleException("The category '" + category.getName() + "' is for "
                    + category.getClaimType() + " claims, not " + expectedType + " claims.");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessRuleException("The amount must be more than zero.");
        }
        if (expenseDate.isAfter(LocalDate.now(ZIMBABWE))) {
            throw new BusinessRuleException("The expense date can't be in the future.");
        }
        this.submittedBy = submitter;
        this.company = submitter.getCompany();
        this.category = category;
        this.claimType = expectedType;
        this.description = description;
        this.expenseDate = expenseDate;
        this.currency = currency;
        this.amount = amount;
        this.paidWith = paidWith;
    }

    // ================================================================ creating
    // claims

    public static Expense receiptClaim(AppUser submitter, ExpenseCategory category, String description,
            PurchaseDetails details, PurchaseRequest request,
            String skipApprovalReason) {
        Expense e = new Expense(submitter, category, ClaimType.RECEIPT, description,
                details.expenseDate(), details.currency(), details.amount(), details.paidWith());
        e.applyPurchaseDetails(details);
        e.linkApproval(request, skipApprovalReason);
        e.extractionStatus = ExtractionStatus.PENDING;
        return e;
    }

    public static Expense fuelClaim(AppUser submitter, ExpenseCategory category, String description,
            PurchaseDetails details, PurchaseRequest request, String skipApprovalReason,
            FuelType fuelType, BigDecimal litres, BigDecimal pricePerLitre,
            String vehicleRegistration, Integer odometerKm) {
        Expense e = new Expense(submitter, category, ClaimType.FUEL, description,
                details.expenseDate(), details.currency(), details.amount(), details.paidWith());
        if (!sameCompany(fuelType.getCompany(), submitter.getCompany()) || !fuelType.isActive()) {
            throw new BusinessRuleException("Please choose one of your company's fuel types.");
        }
        if (litres == null || litres.signum() <= 0 || pricePerLitre == null || pricePerLitre.signum() <= 0) {
            throw new BusinessRuleException("Litres and price per litre must both be more than zero.");
        }
        if (odometerKm != null && odometerKm < 0) {
            throw new BusinessRuleException("The odometer reading can't be negative.");
        }
        e.applyPurchaseDetails(details);
        e.linkApproval(request, skipApprovalReason);
        e.fuelType = fuelType;
        e.fuelLitres = litres;
        e.fuelPricePerLitre = pricePerLitre;
        e.vehicleRegistration = vehicleRegistration;
        e.odometerKm = odometerKm;
        e.extractionStatus = ExtractionStatus.PENDING;
        return e;
    }

    /**
     * Own vehicle: the amount is always distance x the company's current rate per
     * km.
     */
    public static Expense mileageClaim(AppUser submitter, ExpenseCategory category, String purpose,
            LocalDate tripDate, String from, String to, BigDecimal distanceKm) {
        Company company = submitter.getCompany();
        BigDecimal rate = company.getMileageRatePerKm();
        if (rate == null) {
            throw new BusinessRuleException("Your company hasn't set a mileage rate yet.");
        }
        if (distanceKm == null || distanceKm.signum() <= 0) {
            throw new BusinessRuleException("The distance must be more than zero.");
        }
        // Round to what the database stores BEFORE calculating, so both agree to the
        // cent.
        BigDecimal km = distanceKm.setScale(1, RoundingMode.HALF_UP);
        BigDecimal amount = km.multiply(rate).setScale(2, RoundingMode.HALF_UP);
        Expense e = new Expense(submitter, category, ClaimType.MILEAGE, purpose, tripDate,
                company.getBaseCurrency(), amount, PaidWith.WORKER_OWN_MONEY);
        e.tripFrom = from;
        e.tripTo = to;
        e.distanceKm = km;
        e.mileageRate = rate;
        return e;
    }

    /** Per diem: the amount is always days x the company's current daily rate. */
    public static Expense subsistenceClaim(AppUser submitter, ExpenseCategory category, String purpose,
            LocalDate startDate, String location, BigDecimal days) {
        Company company = submitter.getCompany();
        BigDecimal rate = company.getDailySubsistenceRate();
        if (rate == null) {
            throw new BusinessRuleException("Your company hasn't set a daily subsistence rate yet.");
        }
        if (days == null || days.signum() <= 0) {
            throw new BusinessRuleException("The number of days must be more than zero.");
        }
        BigDecimal d = days.setScale(1, RoundingMode.HALF_UP);
        BigDecimal amount = d.multiply(rate).setScale(2, RoundingMode.HALF_UP);
        Expense e = new Expense(submitter, category, ClaimType.SUBSISTENCE, purpose, startDate,
                company.getBaseCurrency(), amount, PaidWith.WORKER_OWN_MONEY);
        e.tripLocation = location;
        e.subsistenceDays = d;
        e.dailyRate = rate;
        return e;
    }

    private void applyPurchaseDetails(PurchaseDetails details) {
        this.supplierName = details.supplierName();
        this.paymentMethod = details.paymentMethod();
        this.fiscalInvoiceNumber = details.fiscalInvoiceNumber();
    }

    /**
     * Links an approved request, or checks the company allows skipping approval.
     */
    private void linkApproval(PurchaseRequest request, String skipReason) {
        if (request != null) {
            if (!sameCompany(request.getCompany(), company)
                    || !Objects.equals(request.getRequestedBy().getId(), submittedBy.getId())) {
                throw new BusinessRuleException("You can only claim against your own purchase requests.");
            }
            if (!request.isOpenForReceipts()) {
                throw new BusinessRuleException("Receipts can only be added to an approved request that is "
                        + "still open (this one is " + request.getStatus() + ").");
            }
            if (request.getCurrency() != currency) {
                throw new BusinessRuleException("The receipt currency must match the approved request.");
            }
            // The request stays open: the worker may add more receipts before submitting
            // them all.
            this.purchaseRequest = request;
            return;
        }
        if (!company.isAllowClaimsWithoutApproval()) {
            throw new BusinessRuleException("Your company requires an approved purchase request first.");
        }
        BigDecimal limit = company.getMaxAmountWithoutApproval();
        if (limit != null && (currency != company.getBaseCurrency() || amount.compareTo(limit) > 0)) {
            throw new BusinessRuleException("Claims over " + limit + " " + company.getBaseCurrency()
                    + " need an approved purchase request first.");
        }
        requireText(skipReason, "Please explain why this was bought without approval.");
        this.skipApprovalReason = skipReason.trim();
    }

    // ================================================================ receipt
    // lines and reading

    public ExpenseItem addItem(String description, BigDecimal quantity, MeasurementUnit unit,
            BigDecimal unitPrice, BigDecimal lineTotal) {
        if (claimType != ClaimType.RECEIPT) {
            throw new BusinessRuleException("Only receipt claims have item rows.");
        }
        requireStatus(ExpenseStatus.DRAFT, "add items to");
        ExpenseItem item = new ExpenseItem(this, items.size() + 1, description, quantity,
                unit, unitPrice, lineTotal);
        items.add(item);
        return item;
    }

    /**
     * Records what the receipt reader found. UNREADABLE sends it to the manager to
     * check by eye.
     */
    public void recordExtraction(ExtractionStatus result, String extractedJson) {
        if (claimType != ClaimType.RECEIPT && claimType != ClaimType.FUEL) {
            throw new BusinessRuleException("Only receipt and fuel claims have receipts to read.");
        }
        if (result == ExtractionStatus.NOT_APPLICABLE) {
            throw new BusinessRuleException("A receipt claim always has a reading result.");
        }
        this.extractionStatus = result;
        this.extractedData = extractedJson;
    }

    // ================================================================ submitting

    /**
     * The worker sends the claim to their manager. Until then it's a draft they can
     * still change. Receipts for a purchase request are submitted together by the
     * service, which then completes the request.
     */
    public void submit(AppUser by) {
        requireStatus(ExpenseStatus.DRAFT, "submit");
        requireSubmitter(by, "submit");
        if (purchaseRequest != null && !purchaseRequest.isOpenForReceipts()) {
            throw new BusinessRuleException("The purchase request for this receipt is no longer open.");
        }
        this.status = ExpenseStatus.SUBMITTED;
        this.submittedAt = Instant.now();
    }

    public boolean isDraft() {
        return status == ExpenseStatus.DRAFT;
    }

    // ================================================================ review

    public void verify(AppUser reviewer) {
        review(reviewer, ExpenseStatus.VERIFIED, null);
    }

    public void query(AppUser reviewer, String question) {
        requireText(question, "A query must say what information is needed.");
        review(reviewer, ExpenseStatus.QUERIED, question);
    }

    public void reject(AppUser reviewer, String reason) {
        requireText(reason, "A rejection must include a reason for the worker.");
        review(reviewer, ExpenseStatus.REJECTED, reason);
    }

    /** The worker has answered a query, so the claim goes back for review. */
    public void resubmit(AppUser by) {
        requireStatus(ExpenseStatus.QUERIED, "resubmit");
        requireSubmitter(by, "resubmit");
        // The question and answer stay in the audit trail.
        this.status = ExpenseStatus.SUBMITTED;
        this.reviewedBy = null;
        this.reviewedAt = null;
        this.reviewNote = null;
    }

    private void review(AppUser reviewer, ExpenseStatus outcome, String note) {
        requireStatus(ExpenseStatus.SUBMITTED, "review");
        requireSameCompanyOtherPerson(reviewer, "review");
        if (reviewer.getRole() != Role.MANAGER && reviewer.getRole() != Role.ADMIN) {
            throw new BusinessRuleException("Only managers and admins can review claims.");
        }
        this.status = outcome;
        this.reviewedBy = reviewer;
        this.reviewedAt = Instant.now();
        this.reviewNote = note;
    }

    // ================================================================
    // reimbursement

    /** Finance pays the worker back, e.g. with an EcoCash transaction reference. */
    public void reimburse(AppUser payer, String reference) {
        requireStatus(ExpenseStatus.VERIFIED, "reimburse");
        if (paidWith != PaidWith.WORKER_OWN_MONEY) {
            throw new BusinessRuleException("This was paid with company money, so nothing is owed to the worker.");
        }
        requireSameCompanyOtherPerson(payer, "reimburse");
        if (payer.getRole() != Role.FINANCE && payer.getRole() != Role.ADMIN) {
            throw new BusinessRuleException("Only finance or admin users can record reimbursements.");
        }
        this.status = ExpenseStatus.REIMBURSED;
        this.reimbursedBy = payer;
        this.reimbursedAt = Instant.now();
        this.reimbursementReference = reference;
    }

    // ================================================================ questions
    // the app asks

    /** Counts towards "owed to workers" in the finance totals. */
    public boolean isOwedToWorker() {
        return status == ExpenseStatus.VERIFIED && paidWith == PaidWith.WORKER_OWN_MONEY;
    }

    /** Shown as the "No prior approval" badge for managers. */
    public boolean skippedApproval() {
        return (claimType == ClaimType.RECEIPT || claimType == ClaimType.FUEL) && purchaseRequest == null;
    }

    /**
     * For fuel claims: receipt amount minus litres x price. A big gap is worth a
     * manager's look.
     */
    public BigDecimal fuelAmountDifference() {
        if (claimType != ClaimType.FUEL) {
            return BigDecimal.ZERO;
        }
        return amount.subtract(fuelLitres.multiply(fuelPricePerLitre).setScale(2, RoundingMode.HALF_UP));
    }

    // ================================================================ helpers

    private void requireStatus(ExpenseStatus expected, String action) {
        if (status != expected) {
            throw new BusinessRuleException("Can't " + action + " a claim that is " + status + ".");
        }
    }

    private void requireSameCompanyOtherPerson(AppUser user, String action) {
        if (!sameCompany(user.getCompany(), company)) {
            throw new BusinessRuleException("You can only " + action + " claims in your own company.");
        }
        if (Objects.equals(user.getId(), submittedBy.getId())) {
            throw new BusinessRuleException("You can't " + action + " your own claim.");
        }
    }

    private void requireSubmitter(AppUser user, String action) {
        if (!Objects.equals(user.getId(), submittedBy.getId())) {
            throw new BusinessRuleException("Only the person who created a claim can " + action + " it.");
        }
    }

    private static void requireText(String text, String message) {
        if (text == null || text.isBlank()) {
            throw new BusinessRuleException(message);
        }
    }

    private static boolean sameCompany(Company a, Company b) {
        return Objects.equals(a.getId(), b.getId());
    }

    // ================================================================ getters

    public Long getId() {
        return id;
    }

    public Company getCompany() {
        return company;
    }

    public AppUser getSubmittedBy() {
        return submittedBy;
    }

    public ExpenseCategory getCategory() {
        return category;
    }

    public ClaimType getClaimType() {
        return claimType;
    }

    public PurchaseRequest getPurchaseRequest() {
        return purchaseRequest;
    }

    public String getSkipApprovalReason() {
        return skipApprovalReason;
    }

    public String getDescription() {
        return description;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public LocalDate getExpenseDate() {
        return expenseDate;
    }

    public CurrencyCode getCurrency() {
        return currency;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public PaidWith getPaidWith() {
        return paidWith;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public String getFiscalInvoiceNumber() {
        return fiscalInvoiceNumber;
    }

    public FuelType getFuelType() {
        return fuelType;
    }

    public BigDecimal getFuelLitres() {
        return fuelLitres;
    }

    public BigDecimal getFuelPricePerLitre() {
        return fuelPricePerLitre;
    }

    public String getVehicleRegistration() {
        return vehicleRegistration;
    }

    public Integer getOdometerKm() {
        return odometerKm;
    }

    public String getTripFrom() {
        return tripFrom;
    }

    public String getTripTo() {
        return tripTo;
    }

    public BigDecimal getDistanceKm() {
        return distanceKm;
    }

    public BigDecimal getMileageRate() {
        return mileageRate;
    }

    public String getTripLocation() {
        return tripLocation;
    }

    public BigDecimal getSubsistenceDays() {
        return subsistenceDays;
    }

    public BigDecimal getDailyRate() {
        return dailyRate;
    }

    public ExtractionStatus getExtractionStatus() {
        return extractionStatus;
    }

    public String getExtractedData() {
        return extractedData;
    }

    public ExpenseStatus getStatus() {
        return status;
    }

    public AppUser getReviewedBy() {
        return reviewedBy;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public String getReviewNote() {
        return reviewNote;
    }

    public AppUser getReimbursedBy() {
        return reimbursedBy;
    }

    public Instant getReimbursedAt() {
        return reimbursedAt;
    }

    public String getReimbursementReference() {
        return reimbursementReference;
    }

    public long getVersion() {
        return version;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public List<ExpenseItem> getItems() {
        return Collections.unmodifiableList(items);
    }
}