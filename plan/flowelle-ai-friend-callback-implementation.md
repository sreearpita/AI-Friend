# Flowelle AI-Friend Callback Implementation Guide

## Purpose

This document describes the Flowelle-side changes needed to support AI-Friend's signed host-tool callbacks.

AI-Friend already has the client side for these tools:

- `cycle-summary`
- `user-preferences`

Flowelle should expose matching server-to-server endpoints that return bounded summaries and typed facts. Flowelle remains the owner of raw menstrual, symptom, cycle, profile, and preference data. AI-Friend should never receive full raw logs unless a later reviewed contract explicitly allows it.

## Current Flowelle Repo Shape

Observed from `https://github.com/sreearpita/Flowelle`:

- `backend/auth-service`
  - Spring Boot
  - JWT auth/profile/preferences
  - Runs at `http://localhost:8081/api`
  - Existing relevant endpoint: `GET /auth/me`
  - Existing relevant model: `UserPreferences`
- `backend/cycles-service`
  - Spring Boot
  - Cycle tracking, symptoms, predictions
  - Runs at `http://localhost:8082/api`
  - Existing relevant endpoint: `GET /api/cycles/predictions?userId={id}`
  - Existing relevant endpoint: `GET /api/cycles/current?userId={id}`
  - Existing relevant DTOs: `CyclePredictionsDto`, `CycleDataDto`

There is no checked-in API gateway and no checked-in AI-Friend server-to-server callback endpoint yet.

## Recommended Endpoint Layout

Add one endpoint to each owning service:

| Tool | Flowelle service | Endpoint |
| --- | --- | --- |
| `cycle-summary` | `backend/cycles-service` | `POST /api/aif/tools/cycle-summary` |
| `user-preferences` | `backend/auth-service` | `POST /api/aif/tools/user-preferences` |

AI-Friend can configure tenant tool callback URLs independently:

```bash
# AI-Friend local example
AIF_SEED_DEMO_TOOLS=true
AIF_DEMO_TOOL_CALLBACK_URL=http://localhost:8082/api/aif/tools
```

For `user-preferences`, AI-Friend may need a per-tool callback URL override later, because that endpoint belongs to `auth-service` on `8081`. Until AI-Friend supports different demo URLs per tool, seed configs manually in the AI-Friend DB or point both tools through a gateway/proxy.

## Request Contract

AI-Friend sends a generic signed envelope. Flowelle should verify the signature over the exact raw JSON request body before parsing business data.

```json
{
  "requestId": "9e6db9f5-2d2a-48bc-a6a9-08b1f4d0a1bd",
  "tenantSlug": "demo",
  "externalUserId": "123",
  "sessionId": "d21e5807-f989-4f01-a75d-1deec1548f17",
  "toolName": "cycle-summary",
  "scopes": ["cycle:read"],
  "locale": "en-US",
  "parameters": {
    "requestId": "9e6db9f5-2d2a-48bc-a6a9-08b1f4d0a1bd",
    "externalUserId": "123",
    "sessionId": "d21e5807-f989-4f01-a75d-1deec1548f17",
    "scopes": ["cycle:read"],
    "locale": "en-US",
    "contractVersion": "flowelle.cycle-summary.v1"
  }
}
```

### Important Mapping Decision

AI-Friend's `externalUserId` must map to a Flowelle user.

Recommended first implementation:

- Treat `externalUserId` as the Flowelle numeric `user.id` encoded as a string.
- Reject non-numeric values with `400`.

Later production implementation:

- Add a tenant/user mapping table if AI-Friend user IDs diverge from Flowelle IDs.

## Response Contract

Flowelle returns the generic response envelope that AI-Friend already understands.

```json
{
  "toolName": "cycle-summary",
  "status": "OK",
  "summary": "Predicted next period starts 2026-06-20 based on the latest logged cycle.",
  "facts": {
    "nextPeriod": "2026-06-20",
    "fertileWindowStart": "2026-06-01",
    "fertileWindowEnd": "2026-06-07",
    "ovulationDay": "2026-06-06",
    "confidence": 82,
    "basis": "Based on 4 logged cycles",
    "isPredicted": true,
    "cycleLength": 28,
    "periodLength": 5,
    "summary": "Predicted next period starts 2026-06-20.",
    "userExplanation": "I used your Flowelle cycle summary."
  },
  "userExplanation": "I used your Flowelle cycle summary."
}
```

If Flowelle cannot provide data:

```json
{
  "toolName": "cycle-summary",
  "status": "NO_DATA",
  "summary": "No cycle data is available yet.",
  "facts": {},
  "userExplanation": "I could not find enough Flowelle cycle data to answer from your history."
}
```

AI-Friend treats only `OK`, `SUCCESS`, or `COMPLETED` as completed tool calls. Other statuses become `FAILED` on the AI-Friend side.

## Signing Contract

AI-Friend signs:

```text
timestamp + "." + rawJsonRequestBody
```

Algorithm:

```text
HMAC-SHA256
```

Required headers:

- `X-AIF-Tenant`
- `X-AIF-Timestamp`
- `X-AIF-Signature`
- `X-AIF-Request-Id`
- `X-AIF-Key-Id`

Recommended validation:

- Reject missing headers with `401`.
- Reject unknown `X-AIF-Key-Id` with `401`.
- Reject timestamps older than 5 minutes with `401`.
- Compare signatures with constant-time comparison.
- Verify `X-AIF-Request-Id` matches body `requestId`.
- Verify `X-AIF-Tenant` matches body `tenantSlug`.
- Enforce required scopes per tool.

## Shared DTOs To Add In Flowelle

Add these DTOs to both services or to a shared module if Flowelle later introduces one. For now, duplication across `auth-service` and `cycles-service` is acceptable to keep the services independent.

### `AifToolRequest.java`

```java
package com.flowelle.<service>.dto;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record AifToolRequest(
        UUID requestId,
        String tenantSlug,
        String externalUserId,
        UUID sessionId,
        String toolName,
        Set<String> scopes,
        String locale,
        Map<String, Object> parameters) {
}
```

### `AifToolResponse.java`

```java
package com.flowelle.<service>.dto;

import java.util.Map;

public record AifToolResponse(
        String toolName,
        String status,
        String summary,
        Map<String, Object> facts,
        String userExplanation) {
}
```

### `FlowelleCycleSummaryResponse.java`

Add in `backend/cycles-service/src/main/java/com/flowelle/cycles/dto/FlowelleCycleSummaryResponse.java`.

```java
package com.flowelle.cycles.dto;

public record FlowelleCycleSummaryResponse(
        String nextPeriod,
        String fertileWindowStart,
        String fertileWindowEnd,
        String ovulationDay,
        Integer confidence,
        String basis,
        Boolean isPredicted,
        Integer cycleLength,
        Integer periodLength,
        String summary,
        String userExplanation) {
}
```

### `FlowelleUserPreferencesResponse.java`

Add in `backend/auth-service/src/main/java/com/flowelle/auth/dto/FlowelleUserPreferencesResponse.java`.

```java
package com.flowelle.auth.dto;

public record FlowelleUserPreferencesResponse(
        Integer cycleLength,
        Integer periodLength,
        Boolean birthControlUse,
        Boolean notificationsEnabled,
        Boolean aiCoachEnabled,
        Boolean voiceProcessingEnabled,
        Boolean analyticsOptIn,
        String summary,
        String userExplanation) {
}
```

## Signature Verification Utility

Add equivalent utility classes in both services, or extract later into a shared library.

Example package:

- cycles: `com.flowelle.cycles.security.AifCallbackVerifier`
- auth: `com.flowelle.auth.security.AifCallbackVerifier`

```java
package com.flowelle.cycles.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AifCallbackVerifier {
    public static final String TENANT_HEADER = "X-AIF-Tenant";
    public static final String TIMESTAMP_HEADER = "X-AIF-Timestamp";
    public static final String SIGNATURE_HEADER = "X-AIF-Signature";
    public static final String REQUEST_ID_HEADER = "X-AIF-Request-Id";
    public static final String KEY_ID_HEADER = "X-AIF-Key-Id";

    private final Map<String, String> secretsByKeyId;

    public AifCallbackVerifier(
            @Value("${aif.callback.key-id:dev-v1}") String keyId,
            @Value("${aif.callback.secret:dev-aif-tool-secret}") String secret) {
        this.secretsByKeyId = Map.of(keyId, secret);
    }

    public void verify(String keyId, String timestamp, String signature, String rawBody) {
        if (!StringUtils.hasText(keyId)
                || !StringUtils.hasText(timestamp)
                || !StringUtils.hasText(signature)
                || rawBody == null) {
            throw new AifCallbackUnauthorizedException("Missing AI-Friend callback signature headers");
        }

        String secret = secretsByKeyId.get(keyId);
        if (secret == null) {
            throw new AifCallbackUnauthorizedException("Unknown AI-Friend callback key id");
        }

        Instant requestTime = Instant.parse(timestamp);
        if (Duration.between(requestTime, Instant.now()).abs().toMinutes() > 5) {
            throw new AifCallbackUnauthorizedException("Expired AI-Friend callback timestamp");
        }

        String expected = sign(timestamp, rawBody, secret);
        if (!MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8))) {
            throw new AifCallbackUnauthorizedException("Invalid AI-Friend callback signature");
        }
    }

    private String sign(String timestamp, String body, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] signature = mac.doFinal((timestamp + "." + body).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(signature);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to verify AI-Friend callback signature", exception);
        }
    }
}
```

### `AifCallbackUnauthorizedException.java`

```java
package com.flowelle.cycles.security;

public class AifCallbackUnauthorizedException extends RuntimeException {
    public AifCallbackUnauthorizedException(String message) {
        super(message);
    }
}
```

Duplicate the package name for `auth-service`.

## Raw Body Handling

Signature verification needs the exact request body. The simplest implementation is to accept `String rawBody` in the controller, verify it, then parse with `ObjectMapper`.

This avoids request-body caching filters for the first implementation.

## Cycles Service Implementation

### Controller

Add:

`backend/cycles-service/src/main/java/com/flowelle/cycles/controller/AifToolController.java`

```java
package com.flowelle.cycles.controller;

import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowelle.cycles.dto.AifToolRequest;
import com.flowelle.cycles.dto.AifToolResponse;
import com.flowelle.cycles.dto.FlowelleCycleSummaryResponse;
import com.flowelle.cycles.security.AifCallbackVerifier;
import com.flowelle.cycles.service.AifCycleToolService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/aif/tools")
@RequiredArgsConstructor
public class AifToolController {
    private final AifCallbackVerifier verifier;
    private final ObjectMapper objectMapper;
    private final AifCycleToolService aifCycleToolService;

    @PostMapping("/cycle-summary")
    public ResponseEntity<AifToolResponse> cycleSummary(
            @RequestHeader(AifCallbackVerifier.KEY_ID_HEADER) String keyId,
            @RequestHeader(AifCallbackVerifier.TIMESTAMP_HEADER) String timestamp,
            @RequestHeader(AifCallbackVerifier.SIGNATURE_HEADER) String signature,
            @RequestBody String rawBody) throws Exception {
        verifier.verify(keyId, timestamp, signature, rawBody);
        AifToolRequest request = objectMapper.readValue(rawBody, AifToolRequest.class);
        validateEnvelope(request, "cycle-summary", "cycle:read");

        FlowelleCycleSummaryResponse summary = aifCycleToolService.buildCycleSummary(request);
        return ResponseEntity.ok(new AifToolResponse(
                "cycle-summary",
                "OK",
                summary.summary(),
                objectMapper.convertValue(summary, Map.class),
                summary.userExplanation()));
    }

    private void validateEnvelope(AifToolRequest request, String expectedTool, String requiredScope) {
        if (!expectedTool.equals(request.toolName())) {
            throw new IllegalArgumentException("Invalid toolName");
        }
        Set<String> scopes = request.scopes() == null ? Set.of() : request.scopes();
        if (!scopes.contains(requiredScope)) {
            throw new IllegalArgumentException("Missing required scope");
        }
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(com.flowelle.cycles.security.AifCallbackUnauthorizedException.class)
    public ResponseEntity<Map<String, String>> unauthorized(RuntimeException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", exception.getMessage()));
    }
}
```

### Service

Add:

`backend/cycles-service/src/main/java/com/flowelle/cycles/service/AifCycleToolService.java`

```java
package com.flowelle.cycles.service;

import com.flowelle.cycles.dto.AifToolRequest;
import com.flowelle.cycles.dto.CycleDataDto;
import com.flowelle.cycles.dto.CyclePredictionsDto;
import com.flowelle.cycles.dto.FlowelleCycleSummaryResponse;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AifCycleToolService {
    private final PredictionService predictionService;
    private final CycleService cycleService;

    public FlowelleCycleSummaryResponse buildCycleSummary(AifToolRequest request) {
        Long userId = parseUserId(request.externalUserId());
        CyclePredictionsDto prediction = predictionService.predictNextCycle(userId);
        CycleDataDto currentCycle = cycleService.getCurrentCycle(userId);

        Integer cycleLength = currentCycle != null ? currentCycle.getCycleLength() : null;
        Integer periodLength = currentCycle != null ? currentCycle.getPeriodLength() : null;
        String summary = "Predicted next period starts " + prediction.getNextPeriod()
                + " with confidence " + prediction.getConfidence() + "%.";

        return new FlowelleCycleSummaryResponse(
                prediction.getNextPeriod(),
                prediction.getFertileWindowStart(),
                prediction.getFertileWindowEnd(),
                prediction.getOvulationDay(),
                prediction.getConfidence(),
                prediction.getBasis(),
                prediction.getIsPredicted(),
                cycleLength,
                periodLength,
                summary,
                "I used your Flowelle cycle summary.");
    }

    private Long parseUserId(String externalUserId) {
        try {
            return Long.parseLong(externalUserId);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("externalUserId must be a Flowelle numeric user id");
        }
    }
}
```

### Security Config

`cycles-service` currently authenticates all requests except OPTIONS/docs. For AI-Friend callbacks, either:

1. Permit `/api/aif/tools/**` in `SecurityConfig` and rely on HMAC verification in the controller, or
2. Keep JWT plus HMAC.

Recommended first server-to-server implementation:

```java
.requestMatchers("/api/aif/tools/**").permitAll()
```

Place it before `.anyRequest().authenticated()`.

Rationale: AI-Friend is not acting as a user browser client and should not need a user JWT. HMAC plus scopes/consent checks form the server-to-server boundary.

## Auth Service Implementation

### Controller

Add:

`backend/auth-service/src/main/java/com/flowelle/auth/controller/AifToolController.java`

```java
package com.flowelle.auth.controller;

import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowelle.auth.dto.AifToolRequest;
import com.flowelle.auth.dto.AifToolResponse;
import com.flowelle.auth.dto.FlowelleUserPreferencesResponse;
import com.flowelle.auth.security.AifCallbackVerifier;
import com.flowelle.auth.service.AifPreferencesToolService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/aif/tools")
@RequiredArgsConstructor
public class AifToolController {
    private final AifCallbackVerifier verifier;
    private final ObjectMapper objectMapper;
    private final AifPreferencesToolService aifPreferencesToolService;

    @PostMapping("/user-preferences")
    public ResponseEntity<AifToolResponse> userPreferences(
            @RequestHeader(AifCallbackVerifier.KEY_ID_HEADER) String keyId,
            @RequestHeader(AifCallbackVerifier.TIMESTAMP_HEADER) String timestamp,
            @RequestHeader(AifCallbackVerifier.SIGNATURE_HEADER) String signature,
            @RequestBody String rawBody) throws Exception {
        verifier.verify(keyId, timestamp, signature, rawBody);
        AifToolRequest request = objectMapper.readValue(rawBody, AifToolRequest.class);
        validateEnvelope(request, "user-preferences", "preferences:read");

        FlowelleUserPreferencesResponse preferences = aifPreferencesToolService.buildPreferences(request);
        return ResponseEntity.ok(new AifToolResponse(
                "user-preferences",
                "OK",
                preferences.summary(),
                objectMapper.convertValue(preferences, Map.class),
                preferences.userExplanation()));
    }

    private void validateEnvelope(AifToolRequest request, String expectedTool, String requiredScope) {
        if (!expectedTool.equals(request.toolName())) {
            throw new IllegalArgumentException("Invalid toolName");
        }
        Set<String> scopes = request.scopes() == null ? Set.of() : request.scopes();
        if (!scopes.contains(requiredScope)) {
            throw new IllegalArgumentException("Missing required scope");
        }
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(com.flowelle.auth.security.AifCallbackUnauthorizedException.class)
    public ResponseEntity<Map<String, String>> unauthorized(RuntimeException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", exception.getMessage()));
    }
}
```

### Service

Add:

`backend/auth-service/src/main/java/com/flowelle/auth/service/AifPreferencesToolService.java`

```java
package com.flowelle.auth.service;

import com.flowelle.auth.dto.AifToolRequest;
import com.flowelle.auth.dto.FlowelleUserPreferencesResponse;
import com.flowelle.auth.model.UserPreferences;
import com.flowelle.auth.repository.UserPreferencesRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AifPreferencesToolService {
    private final UserPreferencesRepository userPreferencesRepository;

    public FlowelleUserPreferencesResponse buildPreferences(AifToolRequest request) {
        Long userId = parseUserId(request.externalUserId());
        UserPreferences preferences = userPreferencesRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User preferences not found"));

        String summary = "Flowelle preferences indicate a typical cycle length of "
                + preferences.getCycleLength()
                + " days and period length of "
                + preferences.getPeriodLength()
                + " days.";

        return new FlowelleUserPreferencesResponse(
                preferences.getCycleLength(),
                preferences.getPeriodLength(),
                preferences.getBirthControlUse(),
                preferences.getNotificationsEnabled(),
                preferences.getAiCoachEnabled(),
                preferences.getVoiceProcessingEnabled(),
                preferences.getAnalyticsOptIn(),
                summary,
                "I used your Flowelle preferences.");
    }

    private Long parseUserId(String externalUserId) {
        try {
            return Long.parseLong(externalUserId);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("externalUserId must be a Flowelle numeric user id");
        }
    }
}
```

### Security Config

Permit:

```java
.requestMatchers("/api/aif/tools/**").permitAll()
```

or, if the auth-service context path means controller paths are mounted without `/api`, permit:

```java
.requestMatchers("/aif/tools/**").permitAll()
```

Check the service's `server.servlet.context-path` / base URL behavior before finalizing the matcher.

## Application Configuration

Add these to each Flowelle service configuration.

### `cycles-service/src/main/resources/application.properties`

```properties
aif.callback.key-id=${AIF_CALLBACK_KEY_ID:dev-v1}
aif.callback.secret=${AIF_CALLBACK_SECRET:dev-aif-tool-secret}
```

### `auth-service/src/main/resources/application.yml`

```yaml
aif:
  callback:
    key-id: ${AIF_CALLBACK_KEY_ID:dev-v1}
    secret: ${AIF_CALLBACK_SECRET:dev-aif-tool-secret}
```

For production, do not use defaults. Source the secret from a secret manager or deployment secret.

## AI-Friend Configuration After Flowelle Changes

Configure AI-Friend tenant tool callback URLs:

```text
cycle-summary -> http://localhost:8082/api/aif/tools/cycle-summary
user-preferences -> http://localhost:8081/api/aif/tools/user-preferences
```

The current AI-Friend demo seeder uses one `AIF_DEMO_TOOL_CALLBACK_URL` for both tools. If no gateway exists, manually seed/update per-tool URLs in AI-Friend's `tenant_tool_configs` table until AI-Friend supports per-tool demo URL properties.

## Tests To Add In Flowelle

### Cycles Service

Add controller/service tests for:

- valid signed `cycle-summary` request returns `OK`,
- missing `cycle:read` returns `400`,
- invalid signature returns `401`,
- non-numeric `externalUserId` returns `400`,
- no cycle data returns a bounded non-raw error response.

### Auth Service

Add controller/service tests for:

- valid signed `user-preferences` request returns `OK`,
- missing `preferences:read` returns `400`,
- invalid signature returns `401`,
- non-numeric `externalUserId` returns `400`,
- missing preferences returns a bounded non-raw error response.

### Signature Unit Test

Use this known vector from AI-Friend:

```text
timestamp: 2026-06-07T00:00:00Z
body: {"toolName":"cycle-summary"}
secret: secret
signature: 61d8139ea063c314927f82377accf25429264e2bccd97503d066ae9d43b2edb2
```

## Manual Smoke Test

1. Start Flowelle auth-service on `8081`.
2. Start Flowelle cycles-service on `8082`.
3. Start AI-Friend on `8080`.
4. Ensure the same callback secret/key id are configured on both sides.
5. Configure AI-Friend `cycle-summary` callback URL:

```text
http://localhost:8082/api/aif/tools/cycle-summary
```

6. Configure AI-Friend `user-preferences` callback URL:

```text
http://localhost:8081/api/aif/tools/user-preferences
```

7. Send AI-Friend chat request:

```json
{
  "externalUserId": "1",
  "message": "When is my next period?",
  "scopes": ["cycle:read"]
}
```

8. Verify AI-Friend returns a `toolCalls` entry with:

```json
{
  "name": "cycle-summary",
  "status": "COMPLETED"
}
```

9. Send without `cycle:read`.
10. Verify AI-Friend returns `SKIPPED`.

## Production Hardening Follow-Up

- Replace plaintext callback secret config with managed secrets.
- Add key rotation support for multiple active `X-AIF-Key-Id` values.
- Add replay protection using `X-AIF-Request-Id`.
- Add tenant allow-listing.
- Add explicit consent checks in Flowelle before returning any tool summary.
- Add rate limits on `/api/aif/tools/**`.
- Add structured audit events in Flowelle for AI-Friend callbacks.
