package com.example.kursinisbackend.service;

import com.example.kursinisbackend.model.BasicUser;
import com.example.kursinisbackend.repos.BasicUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final PasswordEncoder passwordEncoder;
    private final BasicUserRepository basicUserRepository;

    public void registerUser(BasicUser user) {
        // 1. Hash the password
        String encodedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encodedPassword);

        // 2. Save to DB
        basicUserRepository.save(user);
    }

    public String encodePassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    public boolean verifyUser(String rawPassword, String storedHash) {
        // Use matches() to check. Do NOT compare strings directly.
        // BCrypt handles extracting the salt from the storedHash.
        return passwordEncoder.matches(rawPassword, storedHash);
    }
}