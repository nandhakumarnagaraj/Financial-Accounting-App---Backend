package com.financial.sync.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "zerodha_holdings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ZerodhaHolding {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "trading_symbol", nullable = false)
    private String tradingSymbol;
    
    @Column(name = "exchange")
    private String exchange;
    
    @Column(name = "isin")
    private String isin;
    
    @Column(name = "quantity")
    private Integer quantity;
    
    @Column(name = "average_price", precision = 15, scale = 2)
    private BigDecimal averagePrice;
    
    @Column(name = "last_price", precision = 15, scale = 2)
    private BigDecimal lastPrice;
    
    @Column(name = "pnl", precision = 15, scale = 2)
    private BigDecimal pnl;
    
    @Column(name = "product")
    private String product;
    
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