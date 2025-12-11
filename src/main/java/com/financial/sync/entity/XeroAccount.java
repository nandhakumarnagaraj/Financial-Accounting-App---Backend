package com.financial.sync.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "xero_accounts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class XeroAccount {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "xero_account_id", unique = true, nullable = false)
    private String xeroAccountId;
    
    @Column(name = "account_code")
    private String accountCode;
    
    @Column(name = "account_name")
    private String accountName;
    
    @Column(name = "account_type")
    private String accountType;
    
    @Column(name = "account_class")
    private String accountClass;
    
    @Column(name = "status")
    private String status;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "tax_type")
    private String taxType;
    
    @Column(name = "enable_payments_to_account")
    private Boolean enablePaymentsToAccount;
    
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