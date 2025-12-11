package com.financial.sync.controller;

import com.financial.sync.dto.JwtResponse;
import com.financial.sync.dto.LoginRequest;
import com.financial.sync.dto.MessageResponse;
import com.financial.sync.dto.SignupRequest;
import com.financial.sync.entity.User;
import com.financial.sync.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = { "http://localhost:4200", "http://localhost:3000" })
public class AuthController {

	private final AuthService authService;

	@PostMapping("/login")
	public ResponseEntity<JwtResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
		JwtResponse jwtResponse = authService.login(loginRequest);
		return ResponseEntity.ok(jwtResponse);
	}

	@PostMapping("/signup")
	public ResponseEntity<MessageResponse> signup(@Valid @RequestBody SignupRequest signupRequest) {
		User user = authService.signup(signupRequest);
		return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
	}
}