package com.financial.sync.service;

import com.financial.sync.entity.User;
import java.util.List;
import java.util.Optional;

public interface UserService {

	User createUser(User user);

	Optional<User> getUserById(Long id);

	Optional<User> getUserByUsername(String username);

	Optional<User> getUserByEmail(String email);

	List<User> getAllUsers();

	User updateUser(User user);

	void deleteUser(Long id);

	boolean existsByUsername(String username);

	boolean existsByEmail(String email);

	void updateXeroTokens(Long userId, String accessToken, String refreshToken, String tenantId);
}
