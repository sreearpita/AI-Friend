package com.example.demo.service;

import com.example.demo.model.AppUser;
import com.example.demo.repository.AppUserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserService {

    private final AppUserRepository appUserRepository;

    public UserService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    /**
     * Returns the existing user or creates a new one.
     * Handles the race condition where two concurrent requests create the same user
     * by catching DataIntegrityViolationException and re-fetching.
     */
    @Transactional
    public AppUser getOrCreate(UUID userId) {
        return appUserRepository.findById(userId).orElseGet(() -> {
            try {
                AppUser user = new AppUser(userId);
                return appUserRepository.saveAndFlush(user);
            } catch (DataIntegrityViolationException e) {
                // Another thread already created the user concurrently; re-fetch it.
                return appUserRepository.findById(userId)
                        .orElseThrow(() -> new IllegalStateException("User disappeared after save conflict", e));
            }
        });
    }
}
