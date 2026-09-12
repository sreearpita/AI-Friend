# Milestone 6: Production Security and Tenant Controls

## Summary

Milestone 6 moves AI-Friend from a trusted-demo chat API to a production-oriented tenant integration surface. The backend now has a `/v2` chat contract where tenant authentication and user authorization are separated: tenants authenticate with an API key, while each chat request carries a short-lived RS256 user-context JWT issued by the host backend.

Flowelle remains the owner of raw cycle and preference data. The intended Flowelle production path is browser to Flowelle backend, then Flowelle backend to AI-Friend `/v2`.

## Implemented in AI-Friend

- Added `POST /v2/chat/messages` with `X-AIF-Tenant-Key` and `X-AIF-User-Context`.
- Added `ChatV2MessageRequest`, `AuthenticatedUserContext`, and `ChatCommand` so v2 derives user ID and scopes only from verified token claims.
- Added RS256 JWT verification against tenant-specific JWKS configuration.
- Required user-context claims: `iss`, `sub`, `aud`, `iat`, `exp`, `jti`, `tenant`, and `scope`.
- Enforced default `aud=ai-friend-chat`, maximum 60-second tenant token lifetime, tenant claim matching, and required `wellness:chat` scope.
- Added single-use `jti` replay protection before chat persistence/model/tool execution.
- Added user-per-minute, tenant-per-minute, and tenant-per-day guardrails with structured `429` responses and retry headers.
- Added a Redis-compatible state-store abstraction with local in-memory behavior by default and a simple Redis RESP client when `AIF_REDIS_ENABLED=true`.
- Restricted `/v1/chat/messages` and `/chat` to the Spring `local` profile.
- Added `requestId` to chat responses and audit metadata for correlation.
- Added `authorizationJti` to host tool callback payloads for cross-system audit correlation without forwarding the JWT.
- Replaced runtime use of plaintext tool secrets with `TenantToolConfig.secretRef`; supported production `env://VARIABLE_NAME` references.
- Added an OIDC-style admin guard for `/internal/admin/**` using RS256 issuer/audience/JWKS/role validation.
- Added management endpoints for tenant upsert, tenant user-auth config, tool config, API-key creation/listing/revocation.
- Added API-key prefix, expiry, revocation, last-used, and creator metadata.
- Added Flyway migration `V4__production_security_controls.sql`.
- Repaired local Mockito test execution with subclass mock maker and removed duplicate `org.json` test classpath conflict.

## Production Configuration

Required for v2 production traffic:

- `AIF_REDIS_ENABLED=true`
- `AIF_REDIS_HOST`, `AIF_REDIS_PORT`, and optional `AIF_REDIS_PASSWORD`
- tenant API keys created through the admin API
- tenant user auth config with issuer, audience, JWKS URI, RS256 algorithm, and token lifetime
- host tool configs using `env://` secret references

Admin API configuration:

- `AIF_ADMIN_ISSUER`
- `AIF_ADMIN_AUDIENCE=ai-friend-admin`
- `AIF_ADMIN_JWKS_URI`
- `AIF_ADMIN_REQUIRED_ROLE=aif-admin`

## Flowelle Work Still Needed

- Add Flowelle `POST /aif/chat/messages`.
- Authenticate the normal Flowelle user JWT.
- Require `aiCoachEnabled` before issuing AI-Friend context.
- Issue a one-request RS256 JWT with `aud=ai-friend-chat`, `tenant=flowelle`, `sub=<flowelle-user-id>`, `jti`, `iat`, `exp`, and scopes `wellness:chat cycle:read preferences:read`.
- Proxy the request to AI-Friend `/v2/chat/messages` from the Flowelle backend only.
- Keep the AI-Friend tenant API key server-side.
- Publish Flowelle JWKS and configure that JWKS URI in AI-Friend.
- Re-check `aiCoachEnabled` in Flowelle tool callbacks before returning cycle or preference facts.

## Verification

Run in AI-Friend:

```bash
./mvnw test
cd chat-frontend && CI=true npm test -- --watchAll=false
```

Docker-backed PostgreSQL migration tests are expected to skip when Docker is not available locally.

## Acceptance Criteria

- `/v2/chat/messages` rejects missing, forged, expired, wrong-audience, wrong-issuer, wrong-tenant, underscoped, and replayed user-context JWTs.
- `/v2/chat/messages` ignores any request-body identity or scope fields.
- Tenant/user/session isolation remains intact.
- Rate-limit and quota failures return structured `429` responses with retry headers.
- Tool callbacks use resolved `env://` secrets and include request correlation fields.
- Admin API never returns stored key hashes or resolved secrets and reveals raw API keys only at creation time.
