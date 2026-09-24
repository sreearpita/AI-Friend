package com.example.demo.controller;

import java.util.List;
import java.util.UUID;

import com.example.demo.dto.AdminApiKeyCreateRequest;
import com.example.demo.dto.AdminApiKeyCreateResponse;
import com.example.demo.dto.AdminApiKeyMetadataResponse;
import com.example.demo.dto.AdminCapabilityRequest;
import com.example.demo.dto.AdminCapabilityResponse;
import com.example.demo.dto.AdminTenantRequest;
import com.example.demo.dto.AdminTenantResponse;
import com.example.demo.dto.AdminToolConfigRequest;
import com.example.demo.dto.AdminToolConfigResponse;
import com.example.demo.dto.AdminUserAuthConfigRequest;
import com.example.demo.security.AdminAuthInterceptor;
import com.example.demo.service.AdminManagementService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminController {
    private final AdminManagementService adminManagementService;

    public AdminController(AdminManagementService adminManagementService) {
        this.adminManagementService = adminManagementService;
    }

    @PutMapping("/internal/admin/tenants/{tenantSlug}")
    public AdminTenantResponse upsertTenant(
            @PathVariable String tenantSlug,
            @RequestAttribute(AdminAuthInterceptor.ADMIN_ACTOR_ATTRIBUTE) String actor,
            @Valid @RequestBody AdminTenantRequest request) {
        return adminManagementService.upsertTenant(new AdminTenantRequest(tenantSlug, request.displayName(), request.active()), actor);
    }

    @PutMapping("/internal/admin/tenants/{tenantSlug}/user-auth")
    public AdminUserAuthConfigRequest configureUserAuth(
            @PathVariable String tenantSlug,
            @RequestAttribute(AdminAuthInterceptor.ADMIN_ACTOR_ATTRIBUTE) String actor,
            @Valid @RequestBody AdminUserAuthConfigRequest request) {
        return adminManagementService.configureUserAuth(tenantSlug, request, actor);
    }

    @PutMapping("/internal/admin/tenants/{tenantSlug}/tools/{toolName}")
    public AdminToolConfigResponse configureTool(
            @PathVariable String tenantSlug,
            @PathVariable String toolName,
            @RequestAttribute(AdminAuthInterceptor.ADMIN_ACTOR_ATTRIBUTE) String actor,
            @Valid @RequestBody AdminToolConfigRequest request) {
        return adminManagementService.configureTool(
                tenantSlug,
                new AdminToolConfigRequest(toolName, request.callbackUrl(), request.secretRef(), request.signingKeyId(), request.contractVersion(), request.allowedScopes(), request.active()),
                actor);
    }

    @PutMapping("/internal/admin/tenants/{tenantSlug}/capabilities/{capabilityKey}")
    public AdminCapabilityResponse configureCapability(
            @PathVariable String tenantSlug,
            @PathVariable String capabilityKey,
            @RequestAttribute(AdminAuthInterceptor.ADMIN_ACTOR_ATTRIBUTE) String actor,
            @Valid @RequestBody AdminCapabilityRequest request) {
        return adminManagementService.configureCapability(tenantSlug, capabilityKey, request, actor);
    }

    @org.springframework.web.bind.annotation.GetMapping("/internal/admin/tenants/{tenantSlug}/capabilities")
    public List<AdminCapabilityResponse> listCapabilities(@PathVariable String tenantSlug) {
        return adminManagementService.listCapabilities(tenantSlug);
    }

    @PostMapping("/internal/admin/tenants/{tenantSlug}/api-keys")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminApiKeyCreateResponse createApiKey(
            @PathVariable String tenantSlug,
            @RequestAttribute(AdminAuthInterceptor.ADMIN_ACTOR_ATTRIBUTE) String actor,
            @Valid @RequestBody AdminApiKeyCreateRequest request) {
        return adminManagementService.createApiKey(tenantSlug, request, actor);
    }

    @GetMapping("/internal/admin/tenants/{tenantSlug}/api-keys")
    public List<AdminApiKeyMetadataResponse> listApiKeys(@PathVariable String tenantSlug) {
        return adminManagementService.listApiKeys(tenantSlug);
    }

    @DeleteMapping("/internal/admin/tenants/{tenantSlug}/api-keys/{keyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokeApiKey(
            @PathVariable String tenantSlug,
            @PathVariable UUID keyId,
            @RequestAttribute(AdminAuthInterceptor.ADMIN_ACTOR_ATTRIBUTE) String actor) {
        adminManagementService.revokeApiKey(tenantSlug, keyId, actor);
    }
}
