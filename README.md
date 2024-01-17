# RSVPLANER Backend

A REST API backend for serving and handling data for the RSVPLANER frontend based on Spring Boot 3.
Requires Java version 21 and corresponding Maven version.

# Steps to start

## Start the database containers

### Podman

```bash
podman play kube dev-pod.yaml
```

### Docker

```bash
docker-compose -f docker-compose-dev.yaml up -d
```

## Starting the application

### For windows
```bash
./mvnw clean compile
./mvnw spring-boot:run
```

### For linux
```bash
mvw clean compile
mvn spring-boot:run
```

## Routes

The API is available at `http://localhost:8080/api/v1/` per default. Endpoint definition are declared
in the `swagger.yaml` file.
