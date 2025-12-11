package com.financial.sync.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "xero_invoices")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class XeroInvoice {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "xero_invoice_id", unique = true, nullable = false)
    private String xeroInvoiceId;
    
    @Column(name = "invoice_number")
    private String invoiceNumber;
    
    @Column(name = "contact_name")
    private String contactName;
    
    @Column(name = "invoice_date")
    private LocalDate invoiceDate;
    
    @Column(name = "due_date")
    private LocalDate dueDate;
    
    @Column(name = "status")
    private String status;
    
    @Column(name = "type")
    private String type;
    
    @Column(name = "sub_total", precision = 15, scale = 2)
    private BigDecimal subTotal;
    
    @Column(name = "total_tax", precision = 15, scale = 2)
    private BigDecimal totalTax;
    
    @Column(name = "total", precision = 15, scale = 2)
    private BigDecimal total;
    
    @Column(name = "amount_due", precision = 15, scale = 2)
    private BigDecimal amountDue;
    
    @Column(name = "amount_paid", precision = 15, scale = 2)
    private BigDecimal amountPaid;
    
    @Column(name = "currency_code")
    private String currencyCode;
    
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
