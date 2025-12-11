package com.financial.sync.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "xero_transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class XeroTransaction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "xero_transaction_id", unique = true, nullable = false)
    private String xeroTransactionId;
    
    @Column(name = "transaction_type")
    private String transactionType;
    
    @Column(name = "contact_name")
    private String contactName;
    
    @Column(name = "transaction_date")
    private LocalDate transactionDate;
    
    @Column(name = "amount", precision = 15, scale = 2)
    private BigDecimal amount;
    
    @Column(name = "account_code")
    private String accountCode;
    
    @Column(name = "account_name")
    private String accountName;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "reference")
    private String reference;
    
    @Column(name = "status")
    private String status;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    
    @Column(name = "synced_at")
    private LocalDateTime syncedAt;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        syncedAt = LocalDateTime.now();
    }
}
