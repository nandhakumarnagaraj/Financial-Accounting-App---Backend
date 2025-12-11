package com.financial.sync.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "xero_state_mapping")
@Data
public class XeroStateMapping {

    @Id
    private String state;

    private Long userId;
}
