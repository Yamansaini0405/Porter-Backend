# Porter Clone — Backend (Spring Boot)

A working starting point for a Porter-like delivery platform: JWT auth, rider onboarding
state machine, vehicle management, Redis-based nearby-rider matching, real-time location
tracking (WebSocket), fare calculation with waiting/holding charges, and a commission engine.

## Quick start

```bash
# 1. Start MySQL + Redis
docker-compose up -d

# 2. Run the app (Flyway migrates the schema automatically on boot)
mvn spring-boot:run

# 3. Explore the API
open http://localhost:8080/swagger-ui.html
```

Default local credentials are in `application.yml` (`porter_user` / `porter_pass`,
matching `docker-compose.yml`). Override with env vars (`DB_HOST`, `DB_USER`, `JWT_SECRET`, etc.)
for anything beyond local dev — **do not ship the default `JWT_SECRET` to production.**

## What's implemented

| Area | Status |
|---|---|
| JWT auth (register, OTP login, refresh token rotation) | Done |
| Rider onboarding state machine (REGISTERED -> ... -> ACTIVE) | Done — `RiderOnboardingService` |
| Rider availability (online/offline/on-trip) + Redis heartbeat | Done — `RiderAvailabilityService` |
| Nearby rider matching (Redis GEO + distributed lock) | Done — `RiderMatchingService`, `RiderLocationService` |
| Delivery/trip lifecycle state machine | Done — `DeliveryService` |
| Fare calculation incl. waiting/holding charges | Done — `FareCalculationService` |
| Commission model (versioned, per-vehicle-type or global) | Done — `CommissionService` |
| Real-time location broadcast (WebSocket/STOMP) | Done — `WebSocketConfig`, `LocationBroadcastService` |
| Admin: rider approval, vehicle verification, pricing, commission config, disputes, dashboard | Done |
| Vehicle management | Done |
| Payments (Razorpay/Stripe integration) | Not implemented — `payments` table exists, service layer doesn't |

## Important things to know before you build on this

**This has not been compiled in this environment.** The sandbox here has no access to Maven
Central, so I could not run `mvn compile` to catch every last typo. I checked brace-balance
and cross-references carefully by hand across all 89 files, but **please run `mvn compile`
yourself as the first step** and treat any errors as normal first-run friction, not a sign
the architecture is wrong.

**Distance/duration is currently Haversine (straight-line), not road distance.**
`GeoUtils.haversineDistanceKm()` is a placeholder — swap it for Google Distance Matrix API
or a self-hosted OSRM instance before this fare-calculates anything real. This affects both
the fare estimate and the final fare.

**Rider dispatch is sequential and synchronous, not broadcast-with-timeout.**
`DeliveryService.dispatch()` tries the nearest candidate, then the next, immediately —
it doesn't push to a rider and wait 15s for them to accept/reject like the design doc
describes. Wiring that up needs: FCM/WebSocket push to the candidate, a timeout job
(e.g. Spring `@Scheduled` polling or a delayed queue message), and moving `dispatch()`
off the request thread entirely (publish a `DeliveryRequestedEvent`, handle it async).

**No ownership checks on rider/customer path variables yet.** e.g. `POST /riders/{riderId}/online`
doesn't verify the JWT's user actually owns `riderId`. Add a check comparing
`@AuthenticationPrincipal UserPrincipal.userId()` against the resource before this goes anywhere
near production — right now any authenticated rider could theoretically hit another rider's endpoints.

**OTP send is console-logged, not SMS.** `OtpService.sendOtp()` prints the OTP to the server
console (`[DEV-ONLY] OTP for ... is ...`) instead of calling a real SMS gateway. Swap in
Twilio/MSG91/SNS when you're ready.

**Payments module isn't built yet.** Schema exists (`payments` table), but there's no
`PaymentService`, no gateway integration, no webhook handler. This was the one major
piece from the original design doc left out to keep this response's scope sane —
happy to build it next if useful.

## Project layout

```
com.porterclone
 |- security/ , user/       JWT, register, OTP login
 |- customer/
 |- rider/                  onboarding state machine, availability
 |- vehicle/
 |- delivery/                trip lifecycle state machine
 |- matching/                 Redis geo matching + distributed-lock assignment
 |- pricing/                  pure fare calculation engine
 |- commission/               commission config + rider earnings ledger
 |- dispute/
 |- admin/                    admin-only controllers (rider approval, pricing, dashboard)
 `- websocket/                 live location broadcast
```

## Suggested next steps, in order

1. `mvn compile` locally, fix whatever surfaces (should be minor).
2. Wire the ownership checks mentioned above.
3. Replace Haversine with a real routing API.
4. Build the async dispatch-with-timeout flow for matching.
5. Build the payments module.
6. Add Testcontainers-based integration tests for the state machines —
   the unit test included (`FareCalculationServiceTest`) only covers the pure fare logic;
   the onboarding/trip state machines and matching engine need integration-level tests
   against a real MySQL/Redis since they're the highest-risk components.
