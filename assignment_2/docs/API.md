# API Reference

All responses are JSON. Errors follow the same shape everywhere:

```json
{ 
  "error": "not_found", 
  "message": "No price found for vehicleType='Spaceship' season='Summer'"
}
```

## Vehicle Season Price

### `GET /price`

Retrieves the daily rental price of a vehicle type for a season.

| Param         | Required | Type | Notes |
|---            |---|---|---|
| `vehicleType` | yes | string | {`Compact`, `Sedan`, `SUV`, `Convertible`, `Truck`} |
| `season`      | yes | string | {`spring`, `summer`, `fall`, `winter`} |

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

`data` is the client-relevant answer. `debug` identifies which of the service instances handled the request. This is useful for observing load balancing across instances, not part of the API contract.

## Vehicle Total Price

### `GET /total`

Calls `Vehicle Season Price` for the daily rate, then multiplies by a rental period.

| Param         | Required | Type | Notes |
|---            |---|---|---|
| `vehicleType` | yes | string | unvalidated {`Compact`, `Sedan`, `SUV`, `Convertible`, `Truck`} |
| `season`      | yes | string | unvalidated {`spring`, `summer`, `fall`, `winter`} |
| `days`        | yes | integer | Must be >= 1 |

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

Errors: `400 bad_request` if `days < 1`.

## Load Balancer

- On the current leader: behaves exactly like calling Service A's `/total` directly.
- On the non-leader (passive) instance: any request returns `503 Service Unavailable` with an
  empty body, rather than being proxied — see [DESIGN.md](DESIGN.md) for why.

## Health checks

Every service exposes Spring Boot Actuator's `GET /actuator/health` (used by the readiness/
liveness probes in the Kubernetes manifests).
