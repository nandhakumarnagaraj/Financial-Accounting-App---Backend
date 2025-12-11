package com.financial.sync.service;

import com.financial.sync.dto.JwtResponse;
import com.financial.sync.dto.LoginRequest;
import com.financial.sync.dto.SignupRequest;
import com.financial.sync.entity.User;

public interface AuthService {

	JwtResponse login(LoginRequest loginRequest);

	User signup(SignupRequest signupRequest);

	User getCurrentUser();
}