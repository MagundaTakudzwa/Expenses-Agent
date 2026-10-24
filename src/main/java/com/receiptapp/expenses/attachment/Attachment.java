package com.receiptapp.expenses.attachment;

import com.receiptapp.expenses.common.BusinessRuleException;
import com.receiptapp.expenses.company.Company;
import com.receiptapp.expenses.expense.Expense;
import com.receiptapp.expenses.purchase.PurchaseRequest;
import com.receiptapp.expenses.user.AppUser;
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
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * The record of an uploaded file. The file itself lives in file storage under
 * storageKey.
 * Matches "attachments" (V5). Evidence is never edited, so the entity is
 * immutable.
 */
@Entity
@Immutable
@Table(name = "attachments")
public class Attachment {

    public static final long MAX_SIZE_BYTES = 20L * 1024 * 1024; // 20 MB, same as the database rule
    public static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp", "application/pdf");
    private static final Pattern SHA256 = Pattern.compile("^[0-9a-f]{64}$");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_by_id", nullable = false)
    private AppUser uploadedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_id")
    private Expense expense;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_request_id")
    private PurchaseRequest purchaseRequest;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 20)
    private AttachmentKind kind;

    @Column(name = "storage_key", nullable = false, length = 500)
    private String storageKey;

    @Column(name = "original_filename", length = 255)
    private String originalFilename;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    // CHAR(64) in the database, so tell Hibernate it's fixed-length text.
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "sha256", nullable = false, length = 64)
    private String sha256;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Attachment() {
    }

    private Attachment(AppUser uploadedBy, Company owningCompany, AttachmentKind kind, String storageKey,
            String originalFilename, String contentType, long sizeBytes, String sha256) {
        if (!Objects.equals(uploadedBy.getCompany().getId(), owningCompany.getId())) {
            throw new BusinessRuleException("You can only attach files in your own company.");
        }
        if (!ALLOWED_TYPES.contains(contentType)) {
            throw new BusinessRuleException("Only photos (JPEG, PNG, WebP) and PDFs can be attached.");
        }
        if (sizeBytes <= 0 || sizeBytes > MAX_SIZE_BYTES) {
            throw new BusinessRuleException("Files must be smaller than 20 MB.");
        }
        if (sha256 == null || !SHA256.matcher(sha256).matches()) {
            throw new IllegalArgumentException("The SHA-256 fingerprint must be 64 lowercase hex characters.");
        }
        this.uploadedBy = uploadedBy;
        this.company = owningCompany;
        this.kind = kind;
        this.storageKey = storageKey;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.sha256 = sha256;
    }

    public static Attachment forExpense(Expense expense, AppUser uploadedBy, AttachmentKind kind,
            String storageKey, String originalFilename, String contentType,
            long sizeBytes, String sha256) {
        Attachment a = new Attachment(uploadedBy, expense.getCompany(), kind, storageKey,
                originalFilename, contentType, sizeBytes, sha256);
        a.expense = expense;
        return a;
    }

    public static Attachment forPurchaseRequest(PurchaseRequest request, AppUser uploadedBy, AttachmentKind kind,
            String storageKey, String originalFilename, String contentType,
            long sizeBytes, String sha256) {
        Attachment a = new Attachment(uploadedBy, request.getCompany(), kind, storageKey,
                originalFilename, contentType, sizeBytes, sha256);
        a.purchaseRequest = request;
        return a;
    }

    public Long getId() {
        return id;
    }

    public Company getCompany() {
        return company;
    }

    public AppUser getUploadedBy() {
        return uploadedBy;
    }

    public Expense getExpense() {
        return expense;
    }

    public PurchaseRequest getPurchaseRequest() {
        return purchaseRequest;
    }

    public AttachmentKind getKind() {
        return kind;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public String getSha256() {
        return sha256;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}