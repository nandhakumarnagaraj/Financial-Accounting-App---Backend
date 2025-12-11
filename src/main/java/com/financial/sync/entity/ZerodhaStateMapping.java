package com.financial.sync.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "zerodha_state_mapping")
@Data
public class ZerodhaStateMapping {
	@Id
	private String state;
	private Long userId;
}