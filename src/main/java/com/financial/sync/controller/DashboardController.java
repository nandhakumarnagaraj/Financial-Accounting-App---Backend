package com.financial.sync.controller;

import com.financial.sync.dto.DashboardStatsResponseDTO;
import com.financial.sync.entity.User;
import com.financial.sync.service.AuthService;
import com.financial.sync.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@CrossOrigin(origins = { "http://localhost:4200", "http://localhost:3000" })
public class DashboardController {

	private final AuthService authService;
	private final DashboardService dashboardService;

	/**
	 * Get complete dashboard statistics including Xero and Zerodha data
	 * @return Complete dashboard stats with all financial data
	 */
	@GetMapping("/stats")
	public ResponseEntity<DashboardStatsResponseDTO> getDashboardStats() {
		try {
			log.info("Fetching dashboard stats for current user");
			User currentUser = authService.getCurrentUser();
			DashboardStatsResponseDTO stats = dashboardService.getCompleteDashboardStats(currentUser);
			log.info("Dashboard stats fetched successfully for user: {}", currentUser.getUsername());
			return ResponseEntity.ok(stats);
		} catch (Exception e) {
			log.error("Error fetching dashboard stats: {}", e.getMessage(), e);
			throw e;
		}
	}

	/**
	 * Get monthly revenue for a specific month
	 * @param year Year (e.g., 2024)
	 * @param month Month (1-12)
	 * @return Monthly revenue total
	 */
	@GetMapping("/revenue/monthly")
	public ResponseEntity<Map<String, Object>> getMonthlyRevenue(
			@RequestParam int year,
			@RequestParam int month) {
		try {
			User currentUser = authService.getCurrentUser();
			YearMonth yearMonth = YearMonth.of(year, month);
			BigDecimal revenue = dashboardService.getMonthlyRevenueTotal(currentUser, yearMonth);
			
			Map<String, Object> response = new HashMap<>();
			response.put("year", year);
			response.put("month", month);
			response.put("revenue", revenue);
			response.put("currency", "USD");
			
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			log.error("Error fetching monthly revenue: {}", e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	/**
	 * Get total expenses from transactions
	 * @return Total expenses
	 */
	@GetMapping("/expenses/total")
	public ResponseEntity<Map<String, Object>> getTotalExpenses() {
		try {
			User currentUser = authService.getCurrentUser();
			BigDecimal expenses = dashboardService.getTotalExpenses(currentUser);
			
			Map<String, Object> response = new HashMap<>();
			response.put("totalExpenses", expenses);
			response.put("currency", "USD");
			
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			log.error("Error fetching total expenses: {}", e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	/**
	 * Get summary statistics (lightweight version without full data lists)
	 * @return Summary statistics only
	 */
	@GetMapping("/summary")
	public ResponseEntity<Map<String, Object>> getDashboardSummary() {
		try {
			User currentUser = authService.getCurrentUser();
			DashboardStatsResponseDTO stats = dashboardService.getCompleteDashboardStats(currentUser);
			
			// Create lightweight summary without full data lists
			Map<String, Object> summary = new HashMap<>();
			summary.put("username", stats.getUsername());
			
			// Xero summary
			Map<String, Object> xeroSummary = new HashMap<>();
			xeroSummary.put("connected", stats.getXeroConnected());
			xeroSummary.put("totalInvoices", stats.getTotalInvoices());
			xeroSummary.put("totalAccounts", stats.getTotalAccounts());
			xeroSummary.put("totalTransactions", stats.getTotalTransactions());
			xeroSummary.put("totalInvoiceAmount", stats.getTotalInvoiceAmount());
			xeroSummary.put("totalOutstandingAmount", stats.getTotalOutstandingAmount());
			summary.put("xero", xeroSummary);
			
			// Zerodha summary
			Map<String, Object> zerodhaSummary = new HashMap<>();
			zerodhaSummary.put("connected", stats.getZerodhaConnected());
			zerodhaSummary.put("totalHoldings", stats.getTotalHoldings());
			zerodhaSummary.put("totalPositions", stats.getTotalPositions());
			zerodhaSummary.put("totalOrders", stats.getTotalOrders());
			zerodhaSummary.put("portfolioValue", stats.getPortfolioValue());
			zerodhaSummary.put("totalPnl", stats.getTotalPnl());
			summary.put("zerodha", zerodhaSummary);
			
			return ResponseEntity.ok(summary);
		} catch (Exception e) {
			log.error("Error fetching dashboard summary: {}", e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	/**
	 * Get connection status for integrations
	 * @return Connection status for Xero and Zerodha
	 */
	@GetMapping("/connections")
	public ResponseEntity<Map<String, Object>> getConnectionStatus() {
		try {
			User currentUser = authService.getCurrentUser();
			
			Map<String, Object> connections = new HashMap<>();
			connections.put("xeroConnected", currentUser.getXeroAccessToken() != null);
			connections.put("zerodhaConnected", currentUser.getZerodhaAccessToken() != null);
			connections.put("xeroTenantId", currentUser.getXeroTenantId());
			connections.put("zerodhaUserId", currentUser.getZerodhaUserId());
			
			if (currentUser.getTokenExpiry() != null) {
				connections.put("tokenExpiry", currentUser.getTokenExpiry());
				connections.put("tokenValid", currentUser.getTokenExpiry().isAfter(java.time.LocalDateTime.now()));
			}
			
			return ResponseEntity.ok(connections);
		} catch (Exception e) {
			log.error("Error fetching connection status: {}", e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	/**
	 * Health check endpoint for dashboard service
	 * @return Service health status
	 */
	@GetMapping("/health")
	public ResponseEntity<Map<String, String>> healthCheck() {
		Map<String, String> health = new HashMap<>();
		health.put("status", "UP");
		health.put("service", "Dashboard Service");
		health.put("timestamp", java.time.LocalDateTime.now().toString());
		return ResponseEntity.ok(health);
	}
}