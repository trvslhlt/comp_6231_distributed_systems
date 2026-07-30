# API Reference

All responses are JSON. Errors follow the same shape everywhere:

```json
{ 
  "error": "not_found", 
  "message": "No price found for vehicleType='Spaceship' season='Summer'"
}
```

`error` is one of `not_found` (HTTP 404) or `bad_request` (HTTP 400).

## Service B — Vehicle Season Price

Default port `8081`. Reachable directly, or indirectly through Service A.

### `GET /price`

Retrieves the daily rental price of a vehicle type for a season.

| Param | Required | Notes |
|---|---|---|
| `vehicleType` | yes | Case-insensitive, e.g. `SUV`, `suv` |
| `season` | yes | Case-insensitive, one of `spring`, `summer`, `fall`, `winter` |

**Example**

```
GET /price?vehicleType=SUV&season=Summer
```

```json
{
  "data": {
    "vehicleType": "SUV",
    "season": "summer",
    "pricePerDay": 70.00
  },
  "debug": {
    "servedByInstanceId": "f1aeb17e-874c-4cdd-8bf9-079ae8f193a0",
    "servedByPort": 8081
  }
}
```

`data` is the client-relevant answer. `debug` identifies which of the N Service B instances
handled the request — useful for observing load balancing across instances, not part of the API
contract clients should depend on.

Errors: `404 not_found` if the vehicle type or season combination doesn't exist; `400 bad_request`
if `season` isn't one of the four valid values.

## Service A — Vehicle Total Price

Default port `8080`. This is also what the load balancer proxies to.

### `GET /total`

Calls Service B for the daily rate, then multiplies by a rental period.

| Param | Required | Notes |
|---|---|---|
| `vehicleType` | yes | Forwarded to Service B as-is |
| `season` | yes | Forwarded to Service B as-is |
| `days` | yes | Integer, must be >= 1 |

**Example**

```
GET /total?vehicleType=SUV&season=Winter&days=10
```

```json
{
  "data": {
    "vehicleType": "SUV",
    "season": "winter",
    "days": 10,
    "pricePerDay": 60.00,
    "totalPrice": 600.00
  },
  "debug": {
    "servedByInstanceId": "74c58f78-ee2c-4856-98ba-ce069affbf4f",
    "servedByPort": 8080,
    "upstreamInstanceId": "f1aeb17e-874c-4cdd-8bf9-079ae8f193a0"
  }
}
```

`data` is the client-relevant answer. In `debug`, `servedByInstanceId` identifies the Service A
instance and `upstreamInstanceId` identifies which Service B instance it called — together these
show both load-balancing hops in one response.

Errors: `400 bad_request` if `days < 1`; otherwise errors from Service B are passed through
(`404 not_found` for an unknown vehicle type/season, `400 bad_request` for an invalid season).

## Load Balancer

Default port `8090` (each docker-compose instance is also mapped to its own host port — see
[SETUP.md](SETUP.md)). Proxies every path through to Service A's `/total` endpoint.

- On the current leader: behaves exactly like calling Service A's `/total` directly.
- On the non-leader (passive) instance: any request returns `503 Service Unavailable` with an
  empty body, rather than being proxied — see [DESIGN.md](DESIGN.md) for why.

## Health checks

Every service exposes Spring Boot Actuator's `GET /actuator/health` (used by the readiness/
liveness probes in the Kubernetes manifests).
