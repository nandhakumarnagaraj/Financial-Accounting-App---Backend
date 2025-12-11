package com.financial.sync.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ZerodhaOrderDTO {
    private Long id;
    private String orderId;
    private String tradingSymbol;
    private String exchange;
    private String transactionType;
    private String orderType;
    private String product;
    private Integer quantity;
    private BigDecimal price;
    private String status;
    private LocalDateTime orderTimestamp;
}