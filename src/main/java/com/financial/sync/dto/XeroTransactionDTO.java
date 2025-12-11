package com.financial.sync.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class XeroTransactionDTO {
	private Long id;
	private String transactionType;
	private String contactName;
	private LocalDate transactionDate;
	private BigDecimal amount;
	private String accountCode;
	private String accountName;
	private String description;
	private String reference;
	private String status;
}

