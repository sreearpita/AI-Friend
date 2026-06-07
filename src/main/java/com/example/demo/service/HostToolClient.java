package com.example.demo.service;

import com.example.demo.dto.HostToolRequest;
import com.example.demo.dto.HostToolResponse;
import com.example.demo.model.TenantToolConfig;

public interface HostToolClient {
    HostToolResponse invoke(TenantToolConfig toolConfig, HostToolRequest request);
}
