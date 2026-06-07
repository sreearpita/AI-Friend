package com.example.demo.service;

import java.util.List;

import com.example.demo.dto.CitationResponse;

import org.springframework.stereotype.Service;

@Service
public class RetrievalService {
    public List<CitationResponse> findRelevantCitations(String message) {
        return List.of();
    }
}
