package com.example.demo.security;

public interface SecretResolver {
    String resolve(String secretRef);
}
