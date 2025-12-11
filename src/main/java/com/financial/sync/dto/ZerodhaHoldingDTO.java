package com.financial.sync.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ZerodhaHoldingDTO {
    private Long id;
    private String tradingSymbol;
    private String exchange;
    private String isin;
    private Integer quantity;
    private BigDecimal averagePrice;
    private BigDecimal lastPrice;
    private BigDecimal pnl;
    private String product;
}