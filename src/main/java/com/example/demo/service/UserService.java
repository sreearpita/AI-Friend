package com.example.demo.service;

import com.example.demo.model.AppUser;
import com.example.demo.repository.AppUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserService {

    private final AppUserRepository appUserRepository;

    public UserService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Transactional
    public AppUser getOrCreate(UUID userId) {
        return appUserRepository.findById(userId).orElseGet(() -> {
            AppUser user = new AppUser(userId);
            return appUserRepository.save(user);
        });
    }
}
