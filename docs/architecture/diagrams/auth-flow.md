# Authentication Flow Diagrams

## Overview

This document details the authentication and authorization flows in QAWave.

## Login Flow (PKCE)

```
┌─────────┐                                    ┌─────────────┐                    ┌─────────┐
│ Browser │                                    │  Keycloak   │                    │ Frontend│
└────┬────┘                                    └──────┬──────┘                    └────┬────┘
     │                                                │                                 │
     │ 1. User visits /packages                       │                                 │
     │───────────────────────────────────────────────────────────────────────────────►│
     │                                                │                                 │
     │                                                │ 2. AuthProvider checks state   │
     │                                                │◄────────────────────────────────│
     │                                                │                                 │
     │                                                │ 3. No token, init PKCE         │
     │                                                │    Generate code_verifier      │
     │                                                │    hash → code_challenge       │
     │                                                │────────────────────────────────►
     │                                                │                                 │
     │ 4. Redirect to Keycloak /auth?                 │                                 │
     │    response_type=code&                         │                                 │
     │    client_id=qawave-frontend&                  │                                 │
     │    code_challenge=abc123&                      │                                 │
     │    code_challenge_method=S256                  │                                 │
     │◄──────────────────────────────────────────────────────────────────────────────────
     │                                                │                                 │
     │ 5. User enters credentials                     │                                 │
     │────────────────────────────────────────────────►                                 │
     │                                                │                                 │
     │                                                │ 6. Validate credentials        │
     │                                                │─────────┐                       │
     │                                                │         │                       │
     │                                                │◄────────┘                       │
     │                                                │                                 │
     │ 7. Redirect with auth code                     │                                 │
     │    /callback?code=xyz789                       │                                 │
     │◄───────────────────────────────────────────────│                                 │
     │                                                │                                 │
     │───────────────────────────────────────────────────────────────────────────────►│
     │                                                │                                 │
     │                                                │ 8. Exchange code for tokens    │
     │                                                │    POST /token                 │
     │                                                │    code=xyz789&                │
     │                                                │    code_verifier=original      │
     │                                                │◄────────────────────────────────│
     │                                                │                                 │
     │                                                │ 9. Validate code_verifier     │
     │                                                │    matches code_challenge      │
     │                                                │─────────┐                       │
     │                                                │         │                       │
     │                                                │◄────────┘                       │
     │                                                │                                 │
     │                                                │ 10. Return tokens              │
     │                                                │     {                          │
     │                                                │       access_token: "...",     │
     │                                                │       refresh_token: "...",    │
     │                                                │       expires_in: 300          │
     │                                                │     }                          │
     │                                                │─────────────────────────────────►
     │                                                │                                 │
     │                                                │ 11. Store token in memory,     │
     │                                                │     set auth state             │
     │◄──────────────────────────────────────────────────────────────────────────────────
     │                                                │                                 │
     │ 12. Render /packages                           │                                 │
     │◄──────────────────────────────────────────────────────────────────────────────────
     │                                                │                                 │
```

## API Request Flow

```
┌─────────┐       ┌─────────┐       ┌─────────┐       ┌───────────┐
│ Frontend│       │ Backend │       │Keycloak │       │ Database  │
└────┬────┘       └────┬────┘       └────┬────┘       └─────┬─────┘
     │                 │                 │                   │
     │ 1. GET /api/qa/packages           │                   │
     │    Authorization: Bearer {token}  │                   │
     │────────────────►│                 │                   │
     │                 │                 │                   │
     │                 │ 2. Extract JWT from header          │
     │                 │─────────┐       │                   │
     │                 │         │       │                   │
     │                 │◄────────┘       │                   │
     │                 │                 │                   │
     │                 │ 3. Validate JWT signature           │
     │                 │    using JWKS   │                   │
     │                 │────────────────►│                   │
     │                 │                 │                   │
     │                 │ 4. Return public keys               │
     │                 │◄────────────────│                   │
     │                 │                 │                   │
     │                 │ 5. Verify signature, expiry, issuer │
     │                 │─────────┐       │                   │
     │                 │         │       │                   │
     │                 │◄────────┘       │                   │
     │                 │                 │                   │
     │                 │ 6. Extract roles from JWT           │
     │                 │    realm_access.roles               │
     │                 │─────────┐       │                   │
     │                 │         │       │                   │
     │                 │◄────────┘       │                   │
     │                 │                 │                   │
     │                 │ 7. Check authorization              │
     │                 │    (role-based)  │                   │
     │                 │─────────┐       │                   │
     │                 │         │       │                   │
     │                 │◄────────┘       │                   │
     │                 │                 │                   │
     │                 │ 8. Execute business logic           │
     │                 │─────────────────────────────────────►
     │                 │                 │                   │
     │                 │ 9. Return data  │                   │
     │                 │◄─────────────────────────────────────
     │                 │                 │                   │
     │ 10. Response    │                 │                   │
     │◄────────────────│                 │                   │
     │                 │                 │                   │
```

## Token Refresh Flow

```
┌─────────┐       ┌─────────────┐
│ Frontend│       │  Keycloak   │
└────┬────┘       └──────┬──────┘
     │                   │
     │ 1. Token expires soon (< 5 min)
     │─────────┐         │
     │         │         │
     │◄────────┘         │
     │                   │
     │ 2. POST /token    │
     │    grant_type=refresh_token
     │    refresh_token=abc123
     │──────────────────►│
     │                   │
     │ 3. Validate refresh token
     │                   │─────────┐
     │                   │         │
     │                   │◄────────┘
     │                   │
     │ 4. Issue new tokens
     │    {               │
     │      access_token, │
     │      refresh_token │
     │    }               │
     │◄──────────────────│
     │                   │
     │ 5. Update in-memory token
     │─────────┐         │
     │         │         │
     │◄────────┘         │
     │                   │
```

## Logout Flow

```
┌─────────┐       ┌─────────┐       ┌─────────────┐
│ Browser │       │ Frontend│       │  Keycloak   │
└────┬────┘       └────┬────┘       └──────┬──────┘
     │                 │                   │
     │ 1. Click logout │                   │
     │────────────────►│                   │
     │                 │                   │
     │                 │ 2. Clear tokens   │
     │                 │─────────┐         │
     │                 │         │         │
     │                 │◄────────┘         │
     │                 │                   │
     │                 │ 3. keycloak.logout()
     │                 │──────────────────►│
     │                 │                   │
     │                 │ 4. Invalidate session
     │                 │                   │─────────┐
     │                 │                   │         │
     │                 │                   │◄────────┘
     │                 │                   │
     │ 5. Redirect to login page           │
     │◄────────────────────────────────────│
     │                 │                   │
```

## Role-Based Authorization

```
┌─────────────────────────────────────────────────────────────────┐
│                     ROLE HIERARCHY                               │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│                        ┌─────────┐                              │
│                        │  admin  │                              │
│                        └────┬────┘                              │
│                             │ includes                          │
│                             ▼                                   │
│                        ┌─────────┐                              │
│                        │  tester │                              │
│                        └────┬────┘                              │
│                             │ includes                          │
│                             ▼                                   │
│                        ┌─────────┐                              │
│                        │  viewer │                              │
│                        └─────────┘                              │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                     PERMISSION MATRIX                            │
├────────────────────┬──────────┬──────────┬──────────────────────┤
│ Operation          │  admin   │  tester  │  viewer              │
├────────────────────┼──────────┼──────────┼──────────────────────┤
│ View packages      │    ✅    │    ✅    │    ✅               │
│ View scenarios     │    ✅    │    ✅    │    ✅               │
│ View run results   │    ✅    │    ✅    │    ✅               │
├────────────────────┼──────────┼──────────┼──────────────────────┤
│ Create package     │    ✅    │    ✅    │    ❌               │
│ Run package        │    ✅    │    ✅    │    ❌               │
│ Edit scenario      │    ✅    │    ✅    │    ❌               │
│ Export results     │    ✅    │    ✅    │    ❌               │
├────────────────────┼──────────┼──────────┼──────────────────────┤
│ Delete package     │    ✅    │    ❌    │    ❌               │
│ Manage users       │    ✅    │    ❌    │    ❌               │
│ System settings    │    ✅    │    ❌    │    ❌               │
└────────────────────┴──────────┴──────────┴──────────────────────┘
```

## JWT Token Structure

```json
{
  "header": {
    "alg": "RS256",
    "typ": "JWT",
    "kid": "key-id-from-keycloak"
  },
  "payload": {
    "exp": 1706648400,
    "iat": 1706648100,
    "jti": "unique-token-id",
    "iss": "https://auth.qawave.local/realms/qawave",
    "aud": "account",
    "sub": "user-uuid",
    "typ": "Bearer",
    "azp": "qawave-frontend",
    "session_state": "session-id",
    "acr": "1",
    "realm_access": {
      "roles": ["tester", "default-roles-qawave"]
    },
    "resource_access": {
      "qawave-backend": {
        "roles": ["service-user"]
      }
    },
    "scope": "openid email profile",
    "email_verified": true,
    "name": "John Doe",
    "preferred_username": "johnd",
    "email": "john.doe@example.com"
  },
  "signature": "..."
}
```

## References

- [ADR-007: Authentication with Keycloak](../decisions/ADR-007-authentication-keycloak.md)
- [Keycloak PKCE Documentation](https://www.keycloak.org/docs/latest/securing_apps/#authorization-code-grant)
