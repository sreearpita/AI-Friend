# Milestone 3 Flowelle Contract Hardening Plan

## Summary

Align AI-Friend's host-tool callback layer with the real Flowelle app. Flowelle remains the source of truth for raw cycle, symptom, prediction, profile, and preference data. AI-Friend consumes only signed, typed summaries that are safe to include in model prompts.

## Flowelle Repo Findings

The `sreearpita/Flowelle` repository currently contains:

- `backend/auth-service`: Spring Boot service for JWT auth, profile, privacy, and user preferences.
- `backend/cycles-service`: Spring Boot service for cycle tracking, daily logs, symptoms, and predictions.
- `frontend`: React TypeScript client.
- No checked-in API gateway implementation.
- No checked-in AI-Friend-specific server-to-server callback endpoints yet.

Relevant current Flowelle endpoints:

- Auth service: `http://localhost:8081/api`
- Cycles service: `http://localhost:8082/api`
- `GET /auth/me`: returns current user profile and preferences from auth service.
- `GET /api/cycles/current?userId={id}`: returns current cycle data.
- `GET /api/cycles/history?userId={id}`: returns cycle history.
- `GET /api/cycles/predictions?userId={id}`: returns `CyclePredictionsDto`.

Relevant current Flowelle fields:

- `CyclePredictionsDto`: `nextPeriod`, `fertileWindowStart`, `fertileWindowEnd`, `ovulationDay`, `confidence`, `basis`, `isPredicted`.
- `CycleDataDto`: `userId`, `startDate`, `endDate`, `periodLength`, `cycleLength`, `notes`.
- `UserPreferences`: `cycleLength`, `periodLength`, `birthControlUse`, `notificationsEnabled`, `aiCoachEnabled`, `voiceProcessingEnabled`, `analyticsOptIn`.

## AI-Friend Contracts

AI-Friend defines typed contracts that Flowelle can implement behind signed callback endpoints:

- `FlowelleCycleSummaryRequest`
- `FlowelleCycleSummaryResponse`
- `FlowelleUserPreferencesRequest`
- `FlowelleUserPreferencesResponse`

These contracts are wrapped by the generic `HostToolRequest` / `HostToolResponse` callback envelope for signing, transport, and public tool-call reporting.

## Signed Callback Headers

Callbacks are signed with HMAC-SHA256 over:

```text
timestamp + "." + jsonRequestBody
```

Required headers:

- `X-AIF-Tenant`
- `X-AIF-Timestamp`
- `X-AIF-Signature`
- `X-AIF-Request-Id`
- `X-AIF-Key-Id`

## Scope Rules

- `cycle-summary` should require `cycle:read`.
- `user-preferences` should require `preferences:read`.
- Missing scopes return a safe `SKIPPED` tool call.
- Flowelle failures return a safe `FAILED` tool call and AI-Friend continues with general wellness context.

## AI-Friend Implementation Status

Implemented:

- Typed Flowelle DTOs.
- `FlowelleToolClient` adapter over signed host-tool callbacks.
- Key id support via `TenantToolConfig.signingKeyId` and `X-AIF-Key-Id`.
- Strict configured-scope checks for Flowelle tools.
- Tests for typed mapping, malformed responses, signing headers, scope denial, tool failure, prompt context, and audit redaction.

## Flowelle Repo Follow-Up

Flowelle should add server-to-server endpoints that accept AI-Friend signed callbacks, verify `X-AIF-*` headers, and return the typed summary responses:

- `POST /api/aif/tools/cycle-summary`
- `POST /api/aif/tools/user-preferences`

Those endpoints should:

- authenticate via shared callback secret/key id, not user browser JWT alone,
- map AI-Friend `externalUserId` to Flowelle user id,
- enforce consent/scopes inside Flowelle,
- return summaries and bounded facts only,
- avoid returning raw health logs unless explicitly required and approved.

## Non-Goals

- Do not store raw Flowelle cycle, symptom, or preference records in AI-Friend.
- Do not add pgvector/RAG in this milestone.
- Do not require Flowelle to run for AI-Friend automated tests.
- Do not make the React demo a trusted integration boundary.

## Verification

Run:

```bash
./mvnw test
cd chat-frontend && CI=true npm test -- --watchAll=false
```

Manual integration once Flowelle callback endpoints exist:

- Configure AI-Friend callback URLs to Flowelle service endpoints.
- Send "When is my next period?" with `cycle:read`.
- Confirm `cycle-summary` returns `COMPLETED`.
- Send without `cycle:read`.
- Confirm `cycle-summary` returns `SKIPPED`.
- Confirm logs/audit metadata do not contain raw Flowelle payloads.
