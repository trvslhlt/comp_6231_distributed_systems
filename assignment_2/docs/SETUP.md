# Setup & Running

## Prerequisites

- Java 21+
- Maven 3.9+
- Docker
- `kubectl`
- `kind`

Build everything once from `assignment_2/`:

```bash
mvn -N install # install the parent POM
mvn install -DskipTests # build child modules
```

## API development

```bash
docker run -d --name postgres -p 5432:5432 \
  -e POSTGRES_DB=vehicle_rental -e POSTGRES_USER=vehicle_rental -e POSTGRES_PASSWORD=vehicle_rental \
  postgres:16-alpine

cd service-vehicle-season-price
DB_HOST=localhost mvn spring-boot:run   # port 8081

cd ../service-vehicle-total-price
mvn spring-boot:run                     # port 8080, calls http://localhost:8081

cd ../load-balancer
mvn spring-boot:run                     # port 8090, forwards to http://localhost:8080
```

```bash
curl "http://localhost:8080/total?vehicleType=SUV&season=Winter&days=10"   # Service A directly
curl "http://localhost:8090/total?vehicleType=SUV&season=Winter&days=10"   # through the LB
```

### Hot reload

All three services carry `spring-boot-devtools`. Run `mvn compile` after editing a file; the
  running `spring-boot:run` process picks up the change and restarts on its own.

### Running the tests

```bash
mvn test                                          # run all unit test suites
mvn test -pl service-vehicle-season-price -am     # run one
```

## docker-compose

```bash
docker compose up --build
```

The two load-balancer instances are each on their own host port, since compose has no
k8s-style service selector to route only to the leader, check both. The leader answers
200, the other answers 503:

```bash
curl -s -o /dev/null -w "%{http_code}\n" "http://localhost:8090/total?vehicleType=SUV&season=Summer&days=2"
curl -s -o /dev/null -w "%{http_code}\n" "http://localhost:8091/total?vehicleType=SUV&season=Summer&days=2"
```

Tear down: `docker compose down` (add `-v` to drop the Postgres/etcd data volumes).

## Kubernetes / kind

See the root [README.md](../README.md#kubernetes-resource-topology) for a diagram of how this
gets wired up inside the cluster.

### Setup
```bash
# Create k8s cluster container
kind create cluster --name vehicle-rental

# Build the application images
docker build -t vehicle-rental/postgres-patroni:latest ./db/patroni
docker build -t vehicle-rental/service-vehicle-season-price:latest -f service-vehicle-season-price/Dockerfile .
docker build -t vehicle-rental/service-vehicle-total-price:latest -f service-vehicle-total-price/Dockerfile .
docker build -t vehicle-rental/load-balancer:latest -f load-balancer/Dockerfile .

# Make the images available to the k8s cluster
kind load docker-image vehicle-rental/postgres-patroni:latest vehicle-rental/service-vehicle-season-price:latest vehicle-rental/service-vehicle-total-price:latest vehicle-rental/load-balancer:latest --name vehicle-rental

# Apply the k8s manifests
kubectl apply -f k8s/
kubectl -n vehicle-rental get pods -w   # wait for everything to reach Running/Ready
```

### Usage
```bash
# Forward LB port to localhost
kubectl -n vehicle-rental port-forward svc/load-balancer 8090:80
curl "http://localhost:8090/total?vehicleType=SUV&season=Winter&days=10"
```


### Tear down
```bash
kubectl delete namespace vehicle-rental
kind delete cluster --name vehicle-rental   # if you're done with the cluster entirely
```

## Configuration reference

All settings are environment variables. See each module's `application.yml` for details and defaults.
