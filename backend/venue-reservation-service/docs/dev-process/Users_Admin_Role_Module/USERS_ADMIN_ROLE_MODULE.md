# Users & Admin Role Module

## Module Goal

This module connects OSA Gateway / NCU Portal login data with the venue reservation backend.

- Portal `identifier` is stored as `users.user_id`.
- Booking ownership continues to use `bookings.user_id`.
- `users.deleted_at` is the account ban flag.
- `admin_role` is the only source for review/admin permissions.
- `.env` reviewer identifier settings are no longer used for authorization.

## Tables

### users

| Column | Meaning |
| --- | --- |
| `id` | Internal auto-increment id |
| `user_id` | Portal identifier and booking FK target |
| `name` | Portal `chinese-name` / `chineseName` |
| `role` | Runtime display role: `USER` or `ADMIN` |
| `email` | Portal email |
| `unit_id` | Reserved for future unit-based permission logic |
| `login_at` | Last successful login time |
| `deleted_at` | Ban flag; non-null users are denied at login |
| `created_at` | Creation time |

### admin_role

| Column | Meaning |
| --- | --- |
| `id` | Internal auto-increment id |
| `user_id` | Portal identifier |
| `level` | `0`: review admin, `1`: review admin plus admin-role CRUD |
| `deleted_at` | Soft-delete flag; non-null admin roles are inactive |
| `created_at` | Creation time |
| `updated_at` | Last update time |

`admin_role.user_id` is intentionally not required to have a matching `users.user_id`, so a level 1 admin can grant access to a portal user before that person logs in for the first time.

## Login Data Flow

1. `GatewaySessionAuthInterceptor` reads the session cookie.
2. It loads the Gateway profile JSON from Redis.
3. It extracts `identifier`, `chinese-name` / `chineseName`, and `email`.
4. It loads `users` by `identifier`.
5. If `users.deleted_at` is non-null, the request is rejected with `403`.
6. It loads active `admin_role` by `identifier`.
7. If the user does not exist, it inserts a `users` row with the portal profile and computed role.
8. If the user already exists, it only updates `login_at`.
9. `UserContext` receives the current user with role `ADMIN` when an active admin role exists, otherwise `USER`.

Existing user `name` and `email` are not overwritten during later logins.

## Permission Flow

- `/api/reviews/**` requires `UserContext.role == ADMIN`.
- `/api/admin-roles/**` requires an active `admin_role` row with `level = 1`.
- Level 0 admins can access review APIs but cannot manage admin roles.
- Level 1 admins can access review APIs and manage admin roles.

## Admin Role APIs

All APIs are under `/api/admin-roles` and require an active level 1 admin.

| Method | Path | Behavior |
| --- | --- | --- |
| `GET` | `/api/admin-roles?includeDeleted=false` | List admin roles. Defaults to active roles only. |
| `POST` | `/api/admin-roles` | Create a role or restore a soft-deleted role for `{ userId, level }`. |
| `PUT` | `/api/admin-roles/{id}` | Update active role level. |
| `DELETE` | `/api/admin-roles/{id}` | Soft-delete an active role. |

The service blocks deleting or downgrading the last active level 1 admin.

## Booking Compatibility

The following booking APIs keep their existing contracts and continue to use portal identifier through `UserContext.getUser().getUserId()`:

- `POST /api/bookings`
- `GET /api/bookings/my`
- `POST /api/bookings/query`
- Calendar APIs that show the current user's own bookings

The SQL in `BookingMapper.xml` still filters by `bookings.user_id = #{userId}` and `bookings.user_id` still references `users.user_id`.

## Static Verification Checklist

- Confirm `AUTH_REVIEWER_IDENTIFIER`, `reviewerIdentifier`, and `AuthProperties.isReviewer()` are no longer authorization sources.
- Confirm `UserContext.setUser` is only assigned in `GatewaySessionAuthInterceptor`.
- Confirm `/api/reviews/**` checks `ADMIN`.
- Confirm `/api/admin-roles/**` checks active `admin_role.level = 1`.
- Confirm `User.java` matches the new `users` schema.
- Confirm `AdminRoleMapper.xml` uses `deleted_at IS NULL` for active-role queries and soft-delete updates.
- Confirm booking insert/history APIs still use `user_id` and keep existing endpoints.

## Verification Notes

Backend service startup is not required for this module verification. Use static data-flow inspection and mapper/API checks first.

Static checks performed:

- `AUTH_REVIEWER_IDENTIFIER`, `reviewerIdentifier`, and `AuthProperties.isReviewer()` no longer appear in backend source as authorization sources.
- `UserContext.setUser` is assigned in `GatewaySessionAuthInterceptor`.
- Gateway login flow checks `users.deleted_at`, updates or inserts `users`, and then loads active `admin_role`.
- `/api/reviews/**` checks `ADMIN`.
- `/api/admin-roles/**` checks active `admin_role.level = 1`.
- `BookingMapper.xml` still inserts and queries bookings by `user_id`.
- `AdminRoleMapper.xml` uses `deleted_at IS NULL` for active role queries and soft-delete guards.

Command attempted:

```powershell
.\mvnw.cmd test
```

Result: Maven wrapper failed before compilation with `icm : Cannot index into a null array` and `Cannot start maven from wrapper`. No backend service, Docker stack, or Spring Boot runtime was started.
