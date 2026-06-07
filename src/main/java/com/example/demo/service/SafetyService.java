package com.example.demo.service;

import java.util.List;
import java.util.Locale;

import com.example.demo.model.SafetyDecision;
import com.example.demo.model.SafetyStatus;

import org.springframework.stereotype.Service;

@Service
public class SafetyService {
    private static final List<String> RED_FLAG_TERMS = List.of(
            "severe pain",
            "unbearable pain",
            "heavy bleeding",
            "soaking pads",
            "fainting",
            "dizzy",
            "pregnant and bleeding",
            "chest pain",
            "suicidal",
            "self harm",
            "kill myself");

    public SafetyDecision evaluate(String message) {
        String normalized = message.toLowerCase(Locale.ROOT);
        boolean redFlag = RED_FLAG_TERMS.stream().anyMatch(normalized::contains);
        if (redFlag) {
            return new SafetyDecision(
                    SafetyStatus.RED_FLAG_ESCALATION,
                    false,
                    "I can't safely assess urgent or severe symptoms in chat. If you have severe pain, heavy bleeding, fainting, pregnancy-related bleeding, chest pain, or thoughts of self-harm, please seek urgent medical help now or contact local emergency services. If symptoms are not urgent, a clinician can help evaluate what is going on.");
        }
        return new SafetyDecision(SafetyStatus.OK, true, null);
    }

    public String modelFallbackResponse() {
        return "I’m having trouble reaching the AI service right now. For general wellness questions, try again in a moment. For urgent symptoms or safety concerns, please contact a medical professional or local emergency services.";
    }
}
