package com.financial.sync.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String username;

	@Column(nullable = false, unique = true)
	private String email;

	@Column(nullable = false)
	private String password;

	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
	private Set<Role> roles = new HashSet<>();

	// Xero Integration Fields
	@Column(name = "xero_access_token", length = 2048)
	private String xeroAccessToken;

	@Column(name = "xero_refresh_token", length = 2048)
	private String xeroRefreshToken;

	@Column(name = "xero_tenant_id", length = 255)
	private String xeroTenantId;

	// Zerodha Integration Fields
	@Column(name = "zerodha_access_token", length = 2048)
	private String zerodhaAccessToken;

	@Column(name = "zerodha_user_id", length = 255)
	private String zerodhaUserId;

	@Column(name = "token_expiry")
	private LocalDateTime tokenExpiry;

	@Column(name = "created_at")
	private LocalDateTime createdAt;

	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

	@PrePersist
	protected void onCreate() {
		createdAt = LocalDateTime.now();
		updatedAt = LocalDateTime.now();
	}

	@PreUpdate
	protected void onUpdate() {
		updatedAt = LocalDateTime.now();
	}
}