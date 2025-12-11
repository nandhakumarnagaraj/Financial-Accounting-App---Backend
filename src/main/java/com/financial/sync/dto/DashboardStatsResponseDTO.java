package com.financial.sync.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsResponseDTO {
    // User Info
    private String username;
    
    // Xero Stats
    private Long totalInvoices;
    private Long totalAccounts;
    private Long totalTransactions;
    private BigDecimal totalInvoiceAmount;
    private BigDecimal totalOutstandingAmount;
    private Boolean xeroConnected;
    
    // Zerodha Stats
    private Long totalHoldings;
    private Long totalPositions;
    private Long totalOrders;
    private BigDecimal portfolioValue;
    private BigDecimal totalPnl;
    private Boolean zerodhaConnected;
    
    // Xero Data Lists
    private List<XeroInvoiceDTO> invoices;
    private List<XeroAccountDTO> accounts;
    private List<XeroTransactionDTO> transactions;
    
    // Zerodha Data Lists
    private List<ZerodhaHoldingDTO> holdings;
    private List<ZerodhaPositionDTO> positions;
    private List<ZerodhaOrderDTO> orders;
}