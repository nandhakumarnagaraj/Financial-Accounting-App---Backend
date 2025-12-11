package com.financial.sync.controller;

import com.financial.sync.dto.DashboardStatsResponseDTO;
import com.financial.sync.entity.User;
import com.financial.sync.service.AuthService;
import com.financial.sync.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@CrossOrigin(origins = { "http://localhost:4200", "http://localhost:3000" })
public class DashboardController {

	private final AuthService authService;
	private final DashboardService dashboardService;

	@GetMapping("/stats")
	public ResponseEntity<DashboardStatsResponseDTO> getDashboardStats() {
		User currentUser = authService.getCurrentUser();
		DashboardStatsResponseDTO stats = dashboardService.getCompleteDashboardStats(currentUser);
		return ResponseEntity.ok(stats);
	}
}
