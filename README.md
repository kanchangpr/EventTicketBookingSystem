# Ticket Booking System (Java / Spring Boot)

Backend API implementing:
- Event CRUD
- Hold-then-confirm ticket booking workflow
- Automatic expiration release for holds (5 minutes)
- Booking view and soft-cancel
- Availability endpoint (total - held - confirmed)

## Run

Prerequisite: Java 22

```bash
gradle bootRun
```

Base URL:

```text
http://localhost:8080
```


## Mandatory Request Headers

All endpoints support `X-Correlation-Id` (optional):
- If provided, it is reused for logs/traces and echoed in the response.
- If missing, server generates one and returns it in response header.

All `POST` endpoints require:
- `X-Idempotency-Key`: idempotency key to prevent accidental duplicate processing.

Optional tracing headers (if not provided, server generates values):
- `X-Trace-Id`
- `X-Span-Id`

## API Endpoints with Request Body

### 1) Create Event(s) (Single API for one or many)
- **Method**: `POST`
- **URL**: `/api/events`
- **Request Body (single event object)**:

```json
{
  "name": "Rock Fest",
  "eventDate": "2026-12-25T18:30:00",
  "location": "Main Hall",
  "totalSeats": 100
}
```

- **Request Body (multiple events array)**:

```json
[
  {
    "name": "Tech Expo",
    "eventDate": "2026-12-26T10:00:00",
    "location": "Hall A",
    "totalSeats": 80
  },
  {
    "name": "Food Fest",
    "eventDate": "2026-12-27T11:00:00",
    "location": "Hall B",
    "totalSeats": 60
  }
]
```

### 2) List Events
- **Method**: `GET`
- **URL**: `/api/events`
- **Request Body**: _None_

### 3) Get Event by ID
- **Method**: `GET`
- **URL**: `/api/events/{eventId}`
- **Request Body**: _None_

### 4) Update Event
- **Method**: `PUT`
- **URL**: `/api/events/{eventId}`
- **Request Body**:

```json
{
  "name": "Rock Fest - Updated",
  "eventDate": "2026-12-25T20:00:00",
  "location": "Main Arena",
  "totalSeats": 120
}
```

### 5) Delete Event
- **Method**: `DELETE`
- **URL**: `/api/events/{eventId}`
- **Request Body**: _None_

### 6) Hold Seats
- **Method**: `POST`
- **URL**: `/api/events/{eventId}/holds`
- **Request Body**:

```json
{
  "userId": "user-1",
  "seatNumbers": [1, 2, 3]
}
```

### 7) Confirm Booking
- **Method**: `POST`
- **URL**: `/api/bookings/confirm`
- **Request Body**:

```json
{
  "holdId": "b7d9b2c7-6b0c-4d2d-9f95-3d8f8b8d1a4e"
}
```

### 8) View Booking(s) (Single API for both list and details)
- **Method**: `GET`
- **URL**: `/api/bookings` (returns all bookings)
- **URL**: `/api/bookings/{bookingId}` (returns one booking)
- **Request Body**: _None_

### 9) Cancel Booking
- **Method**: `POST`
- **URL**: `/api/bookings/{bookingId}/cancel`
- **Request Body**: _None_

### 10) Event Availability (Single API family for one or all)
- **Method**: `GET`
- **URL**: `/api/events/{eventId}/availability` (returns one event availability)
- **URL**: `/api/events/availability` (returns all events availability)
- **Request Body**: _None_

## Notes

- Holds remain `ACTIVE` for 5 minutes and are automatically marked `EXPIRED` by a scheduled cleanup.
- Confirming a hold creates a permanent booking and marks hold as `CONFIRMED`.
- Cancellation is soft delete by status transition to `CANCELED`.


## Senior-level Design Considerations (Non-functional)

- **Duplicate event prevention**:
  - Event creation is now idempotent by business key (`name + eventDate + location`).
  - Posting the same event payload again returns the existing event instead of creating a duplicate row.
  - A database-level unique constraint also protects against accidental duplicates under concurrent requests.

- **Booking behavior per user**:
  - Same user can create multiple confirmed bookings for the same event as long as requested seats are available and not already held/booked.

- **Concurrent seat holds / overbooking prevention**:
  - Hold and confirm flows lock the event row with **pessimistic write lock**, serializing seat-allocation critical sections per event.
  - Seat availability checks include both currently confirmed seats and still-active holds.
  - This prevents two users from successfully holding the same seat at the same time.

- **Idempotency and retry safety**:
  - Event create endpoint supports safe retries for duplicate payloads.
  - Booking confirmation validates hold state and seat ownership before creating final booking records.

- **Data integrity**:
  - Soft cancel is preserved for booking audit trail.
  - Expired holds are cleaned by scheduler so stale reservations are released.


## Error Response Semantics

- `400 Bad Request`: malformed JSON / invalid payload format.
- `404 Not Found`: resource does not exist.
- `409 Conflict`: business conflicts (seat already held/booked, duplicate booking, reused idempotency key, duplicate event key on update).
- `410 Gone`: hold expired.
- `422 Unprocessable Entity`: semantic validation errors (invalid seat range, duplicate seat numbers in request, etc.).
- `428 Precondition Required`: required request headers missing (`X-Idempotency-Key` for POST).
- `500 Internal Server Error`: unexpected server failures.



## Downstream propagation

- A `RestTemplate` bean is configured to automatically propagate `X-Correlation-Id`, `X-Trace-Id`, and `X-Span-Id` from MDC to downstream HTTP calls.
- This ensures cross-service request tracking in microservice chains.
