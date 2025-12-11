package com.financial.sync.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class XeroAccountDTO {
    private Long id;
    private String accountCode;
    private String accountName;
    private String accountType;
    private String status;
}
