package com.financial.sync.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ZerodhaPositionDTO {
    private Long id;
    private String tradingSymbol;
    private String exchange;
    private String product;
    private Integer quantity;
    private Integer buyQuantity;
    private Integer sellQuantity;
    private BigDecimal averagePrice;
    private BigDecimal lastPrice;
    private BigDecimal pnl;
    private BigDecimal unrealisedPnl;
    private BigDecimal realisedPnl;
}