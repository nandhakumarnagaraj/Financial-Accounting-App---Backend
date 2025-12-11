package com.financial.sync.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "zerodha_orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ZerodhaOrder {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "order_id", unique = true, nullable = false)
    private String orderId;
    
    @Column(name = "trading_symbol")
    private String tradingSymbol;
    
    @Column(name = "exchange")
    private String exchange;
    
    @Column(name = "transaction_type")
    private String transactionType; // BUY, SELL
    
    @Column(name = "order_type")
    private String orderType; // MARKET, LIMIT
    
    @Column(name = "product")
    private String product;
    
    @Column(name = "quantity")
    private Integer quantity;
    
    @Column(name = "price", precision = 15, scale = 2)
    private BigDecimal price;
    
    @Column(name = "status")
    private String status;
    
    @Column(name = "order_timestamp")
    private LocalDateTime orderTimestamp;
    
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