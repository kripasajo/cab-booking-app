# How RideFlow works (explained simply)

Use this to walk someone through the project in an interview.

## The 30-second pitch

> RideFlow is an Uber-like app split into five Spring Boot microservices that talk to each other through Kafka events.
> When a rider books, the booking service publishes a "ride requested" event; the driver service finds the nearest
> free driver with a Redis GEO search in under a millisecond and publishes the match. Driver GPS updates also flow
> through Kafka to a notification service, which pushes them over WebSockets to the React app, so the rider sees
> the car move live on Google Maps. Prices use surge pricing based on how many free drivers and active rides are
> nearby, also measured with Redis GEO. Everything is secured with JWT, runs with one `docker compose up`, and
> GitHub Actions builds, tests and publishes the Docker images.

## Where each résumé claim lives in the code

| Claim | File(s) |
|---|---|
| Event-driven Spring Boot microservices | `backend/*-service`, wired only by Kafka + one REST call |
| User, booking, driver, pricing, notification services | one folder each in `backend/` |
| Kafka ride lifecycle event streaming | `common/event/RideEvent.java`, `booking-service/.../RideEventPublisher.java`, the `@KafkaListener` classes |
| React + TypeScript frontend | `frontend/src` |
| WebSocket live driver tracking | `notification-service/.../NotificationListener.java` (server), `frontend/src/ws.ts` + `RiderDashboard.tsx` (client) |
| Google Maps SDK | `frontend/src/components/GoogleMapView.tsx` |
| Redis GEO sub-second nearest-driver lookup | `driver-service/.../store/DriverStore.java` → `findNearestAvailable` |
| Dynamic surge pricing | `pricing-service/.../PricingCalculator.java` + `DemandSupplyService.java` |
| JWT authentication | `common/security/*`, issued in `user-service/.../AuthService.java` |
| Docker | `backend/Dockerfile`, `frontend/Dockerfile`, `docker-compose.yml` |
| GitHub Actions CI/CD | `.github/workflows/ci.yml` |

## One ride, step by step

1. **Rider logs in.** `user-service` checks the BCrypt password and returns a JWT containing the user id, name and role.
   The browser keeps it and sends it as `Authorization: Bearer ...` on every call.
2. **Rider picks pickup and drop.** The app calls `pricing-service`, which counts free drivers and active rides within
   3 km (Redis `GEOSEARCH`) and returns the fare with surge.
3. **Rider books.** `booking-service` asks pricing for the final fare (REST, because it needs the answer right away),
   saves the booking as `REQUESTED` in Postgres and publishes `RIDE_REQUESTED` to Kafka.
4. **Matching.** `driver-service` consumes the event and runs `GEOSEARCH drivers:available ... ASC COUNT 1`, which
   returns the closest free driver. It marks them `BUSY` (and removes them from the available set so nobody else can
   get them), then publishes the result on `driver-assignments`.
5. **Assignment saved.** `booking-service` consumes the result, saves `DRIVER_ASSIGNED`, and publishes it on `ride-events`.
6. **Push to the browser.** `notification-service` consumes `DRIVER_ASSIGNED` and sends it to WebSocket topic
   `/topic/rides/{id}` (rider) and `/topic/drivers/{driverId}` (driver).
7. **Live tracking.** Every second the driver's position goes to `POST /api/drivers/location` → Redis GEO + Kafka
   `driver-locations` → notification-service → `/topic/rides/{id}/location` → the marker moves on the map.
8. **Start / complete.** The driver presses the buttons; booking-service publishes `RIDE_STARTED` and `RIDE_COMPLETED`.
   driver-service puts the driver back into `drivers:available`; pricing-service removes the ride from demand.

## Design decisions worth mentioning

- **One owner for ride state.** Only booking-service changes a booking. It saves first, then publishes, so any event
  another service sees is already in the database. Other services react to events instead of calling it.
- **Events carry a full snapshot.** A `RideEvent` contains everything about the ride, so consumers never need to call
  back to booking-service.
- **Ordering.** The Kafka key is the booking id, so a ride's events stay on one partition and arrive in order.
- **No double-booking of drivers.** Matching happens in a single Kafka consumer, one ride at a time, and the matched
  driver is removed from `drivers:available` immediately.
- **Race: rider cancels while matching.** If an assignment arrives for a cancelled ride, booking-service publishes
  `DRIVER_RELEASED` so the driver is freed.
- **Optimistic locking** (`@Version` on `Booking`) stops two updates from silently overwriting each other.
- **Database per service.** `userdb` and `bookingdb` are separate; Redis holds fast-changing location data.
- **Stateless JWT.** Every service validates the token itself with the shared secret, so there is no session store
  and no call to user-service on each request. The WebSocket checks the same JWT on STOMP `CONNECT`.
- **Why Redis GEO?** It stores positions as geohashes in a sorted set, so "closest N within R km" is an
  O(log N + M) query in memory. That is why the lookup takes well under a millisecond.
- **Fallback.** The frontend also re-syncs ride status every 5 seconds, so a missed WebSocket message never leaves the
  UI stuck.

## Likely interview questions

**Why Kafka instead of services calling each other?**
Services stay independent: booking-service doesn't need to know who is interested in a ride. Adding a new consumer
(say, an analytics service) needs no change to the producer. Kafka also keeps events if a consumer is down, and it
replays them when it comes back.

**Why is the pricing call REST and not an event?**
The rider needs the price in the booking response, so it's a synchronous question-and-answer. Events fit
"something happened, react to it"; REST fits "I need an answer now".

**How would you scale it?**
Run more instances of each service. Kafka spreads partitions across the instances of a consumer group. For matching
at scale you would partition by city or geohash so each instance matches a region, and use a Redis lock
(`SET NX`) when reserving a driver.

**What would you add next?**
An API gateway (Spring Cloud Gateway), real road routing and ETA (Google Directions API), payments, a dead-letter topic
for failed events, Testcontainers integration tests, and Kubernetes manifests.

**How is the simulator built?**
`BotDriverSimulator` puts a few bot drivers online at startup. Every second it moves any busy bot one step towards its
pickup or drop by calling the same `updateLocation` method real drivers use, and it calls booking-service's start and
complete endpoints with a JWT for the bot. So the bots exercise the exact production flow.
