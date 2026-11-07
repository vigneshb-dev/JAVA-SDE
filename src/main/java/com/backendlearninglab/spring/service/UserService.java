package com.backendlearninglab.spring.service;

import com.backendlearninglab.model.UserRole;
import com.backendlearninglab.spring.dto.request.CreateUserRequest;
import com.backendlearninglab.spring.dto.response.UserResponse;
import com.backendlearninglab.spring.entity.UserEntity;
import com.backendlearninglab.spring.exception.ResourceNotFoundException;
import com.backendlearninglab.spring.repository.SpringDataUserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service Layer for User Management.
 * <p>
 * TEACHING FOCUS: Why the Service Layer Exists:
 * Controllers handle HTTP (request mapping, path variables, status codes).
 * Repositories handle database storage.
 * The SERVICE LAYER is where business domain logic, validation, transformations,
 * and transaction boundaries live!
 */
@Service
@Transactional(readOnly = true)
public class UserService {

    private final SpringDataUserRepository userRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    public UserService(SpringDataUserRepository userRepository,
                       org.springframework.security.crypto.password.PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DataIntegrityViolationException("Email already registered: " + request.email());
        }

        // Production-grade BCrypt key derivation with random salt and 12 work factor rounds
        String hashedPassword = passwordEncoder.encode(request.password());

        UserRole role = (request.role() != null) ? request.role() : UserRole.USER;
        UserEntity user = new UserEntity(request.email(), hashedPassword, request.fullName(), role);

        UserEntity saved = userRepository.save(user);
        return UserResponse.fromEntity(saved);
    }

    public UserResponse getUserById(Long id) {
        return userRepository.findById(id)
                .map(UserResponse::fromEntity)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserResponse::fromEntity)
                .toList();
    }

    @Transactional
    public UserResponse updateUserStatus(Long id, String newStatus) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));

        // DIRTY CHECKING: Notice we NEVER call userRepository.save(user)!
        // Because this method is @Transactional, modifying a managed entity's field
        // triggers Hibernate's dirty checking to automatically issue an UPDATE SQL statement!
        user.setStatus(newStatus);
        return UserResponse.fromEntity(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User", id);
        }
        userRepository.deleteById(id);
    }
}
