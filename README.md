# Movie Ticket Booking System

A production-quality **Spring Boot 3.x** REST API for a multi-city, multi-theater movie ticket booking system with seat-level concurrency control, time-bound holds, configurable pricing & discounts, payment processing, and async notifications.

---

## Tech Stack

| Concern | Choice |
|---|---|
| Framework | Spring Boot 3.2.5 |
| Language | Java 21 |
| Database | PostgreSQL 15 |
| ORM | Spring Data JPA + Hibernate |
| Schema Migration | Flyway |
| Auth | Spring Security + JWT (JJWT 0.12) |
| Async | Spring `@Async` + `ApplicationEventPublisher` |
| Scheduling | Spring `@Scheduled` |
| Testing | JUnit 5, Mockito, Testcontainers (PostgreSQL) |
| API Docs | SpringDoc OpenAPI 3 / Swagger UI |
| Build | Maven 3.9+ |

---

## Architecture Overview

```
Request → JWT Filter → Controller → Service → Repository → PostgreSQL
                                         ↓
                              ApplicationEventPublisher
                                         ↓
                              @Async Listener → NotificationService
```

### Key Design Decisions

#### 1. Two-Phase Seat Booking (Hold → Confirm)
1. **Hold** (`POST /api/bookings`): Acquires `SELECT … FOR UPDATE` on the `show_seats` rows, flips them `HELD`, records `holdExpiresAt = now + 10 min`, creates a `PENDING` booking.
2. **Confirm** (`POST /api/bookings/{id}/confirm`): Verifies hold hasn't expired, processes mock payment, promotes seats to `BOOKED`, booking to `CONFIRMED`.

A `@Scheduled` sweeper runs every 60 s to release expired holds back to `AVAILABLE`.

#### 2. Concurrency Correctness
- **Pessimistic write lock** (`@Lock(PESSIMISTIC_WRITE)`) on `ShowSeat` rows — prevents double-allocation at the DB level.
- Seats are locked in ascending ID order to prevent deadlocks.
- The hold-expiry sweeper uses a simple `findExpiredHolds` query that is safe to run concurrently with booking transactions.

#### 3. Pricing
`effectivePrice = basePrice × weekendMultiplier (if enabled & weekend) × (1 − discountPercent)`

Weekend = Friday, Saturday, Sunday (configurable per `PricingTier`).

#### 4. Refund Policy
Priority chain: **show-level** > **theater-level** > **global default**.
Each tier is a bracket: "cancel ≥ N hours before show → refund X%".

#### 5. Async Notifications
`ApplicationEventPublisher` fires domain events after booking confirmation/cancellation.
`@Async` listeners store a `Notification` record and log it (no real email/SMS).
A `@Scheduled` cron job sends reminders 24 h before shows.

---

## Project Structure

```
src/main/java/com/moviebooking/
├── config/           SecurityConfig, AsyncConfig, OpenApiConfig, JpaAuditingConfig
├── controller/       AuthController, Admin*, Customer*
├── domain/
│   ├── entity/       City, Theater, Screen, Seat, Movie, Show, ShowSeat,
│   │                 User, Booking, BookingItem, Payment, PricingTier,
│   │                 DiscountCode, RefundPolicy, Notification
│   └── enums/        Role, SeatType, ShowSeatStatus, BookingStatus, …
├── dto/
│   ├── request/      RegisterRequest, LoginRequest, CreateCityRequest, …
│   └── response/     ApiResponse, AuthResponse, ShowResponse, BookingResponse, …
├── event/            BookingConfirmedEvent, BookingCancelledEvent, BookingEventListener
├── exception/        GlobalExceptionHandler + custom exceptions
├── repository/       All Spring Data repos with custom JPQL
├── scheduler/        HoldExpirySweeper, ReminderScheduler
├── security/         JwtUtils, JwtAuthenticationFilter, UserDetailsServiceImpl
└── service/          AuthService, CityTheaterService, MovieShowService,
                      PricingService, BookingService, PaymentService,
                      RefundPolicyService, NotificationService
```

---

## Prerequisites

- Java 21+
- Maven 3.9+
- PostgreSQL 15 running locally (or Docker)
- Docker (for integration tests via Testcontainers — pulled automatically)

---

## Running the Application

### 1. Create the database

```sql
CREATE DATABASE moviebooking;
```

### 2. Configure credentials (optional — defaults shown)

```bash
export DB_USERNAME=postgres
export DB_PASSWORD=postgres
export JWT_SECRET=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
```

### 3. Start the application

```bash
cd movie-booking
mvn spring-boot:run
```

The app starts on **http://localhost:8080**.
Flyway runs migrations automatically on startup, including a seeded admin user.

### 4. Swagger UI

Open **http://localhost:8080/swagger-ui.html** to explore all endpoints interactively.

---

## Default Credentials

| Role | Email | Password |
|------|-------|----------|
| Admin | `admin@moviebooking.com` | `Admin@123` |

---

## API Overview

### Auth (Public)
| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/auth/register` | Register a customer |
| POST | `/api/auth/login` | Get JWT |

### Admin
| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/admin/cities` | Create city |
| GET | `/api/admin/cities` | List cities |
| POST | `/api/admin/theaters` | Create theater |
| PUT | `/api/admin/theaters/{id}` | Update theater |
| GET | `/api/admin/theaters?cityId=` | List theaters by city |
| POST | `/api/admin/theaters/{id}/screens` | Add screen + seat layout |
| POST | `/api/admin/movies` | Add movie |
| GET | `/api/admin/movies` | List movies |
| POST | `/api/admin/shows` | Schedule show |
| DELETE | `/api/admin/shows/{id}` | Cancel show |
| POST | `/api/admin/shows/{id}/pricing` | Set pricing tiers |
| POST | `/api/admin/discount-codes` | Create discount code |
| GET | `/api/admin/discount-codes` | List discount codes |
| POST | `/api/admin/refund-policies` | Create refund policy |
| GET | `/api/admin/refund-policies` | List refund policies |

### Customer
| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/shows?cityId=&movieId=&date=` | Browse shows |
| GET | `/api/shows/{id}` | Show details |
| GET | `/api/shows/{id}/seats` | Live seat map + prices |
| POST | `/api/bookings` | Hold seats (Phase 1) |
| POST | `/api/bookings/{id}/confirm` | Confirm + pay (Phase 2) |
| GET | `/api/bookings/{id}` | Get booking |
| GET | `/api/bookings` | My booking history |
| DELETE | `/api/bookings/{id}` | Cancel booking |

---

## Running Tests

### Unit tests only (no Docker needed)
```bash
mvn test -Dtest="com.moviebooking.unit.*"
```

### All tests including integration (requires Docker)
```bash
mvn verify
```

Integration tests use Testcontainers which automatically pulls and starts a PostgreSQL 15 container.

---

## Assumptions

1. A show maps to exactly one screen; the screen's seat layout is fixed at creation time.
2. Hold TTL defaults to 10 minutes (configurable via `app.booking.hold-ttl-minutes`).
3. Payment is simulated by a mock gateway. Set `app.payment.mock-failure-rate=0.1` for 10% random failures.
4. Notifications are stored in the `notifications` table and logged — no real email/SMS provider.
5. Weekend pricing applies Friday–Sunday, configurable per `PricingTier`.
6. Discount codes can be global (any show) or show-scoped.
7. Cancellation is supported for both `PENDING` and `CONFIRMED` bookings; all seats in a booking are cancelled together.
8. JWT expiry is 24 h. No refresh token flow (out of scope).
9. Refund policy priority: show-level > theater-level > global default.
10. Seat layout is grid-based (rows × seats); rows A, B, C… The first N rows are PREMIUM, the rest REGULAR.
11. A default admin account is created by Flyway migration V4 with password `Admin@123`.
12. The seeded admin password hash corresponds to BCrypt of `Admin@123`. Change it in production.

---

## Configuration Reference

| Property | Default | Description |
|----------|---------|-------------|
| `app.booking.hold-ttl-minutes` | `10` | Seat hold TTL |
| `app.booking.sweeper-interval-ms` | `60000` | Hold expiry sweeper interval |
| `app.payment.mock-failure-rate` | `0.0` | Payment failure probability (0.0–1.0) |
| `app.reminder.hours-before-show` | `24` | Hours before show to send reminder |
| `app.jwt.expiration-ms` | `86400000` | JWT validity (ms) |
