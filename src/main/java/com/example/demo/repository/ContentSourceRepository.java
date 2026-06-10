package com.example.demo.repository;

import java.util.UUID;

import com.example.demo.model.ContentSource;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ContentSourceRepository extends JpaRepository<ContentSource, UUID> {
}
