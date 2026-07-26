# psych-api

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: <https://quarkus.io/>.

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:

```shell script
mvn quarkus:dev
```

> **_NOTE:_** Quarkus now ships with a Dev UI, which is available in dev mode only at <http://localhost:8080/q/dev/>.

## Packaging and running the application

The application can be packaged using:

```shell script
mvn package
```

It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:

```shell script
mvn package -Dquarkus.package.jar.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Creating a native executable

You can create a native executable using:

```shell script
mvn package -Dnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using:

```shell script
mvn package -Dnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/psych-api-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult <https://quarkus.io/guides/maven-tooling>.

## Related Guides

- SmallRye Health ([guide](https://quarkus.io/guides/smallrye-health)): Monitor service health
- Hibernate Validator ([guide](https://quarkus.io/guides/validation)): Bean validation using Hibernate Validator and Jakarta Validation annotations
- SmallRye OpenAPI ([guide](https://quarkus.io/guides/openapi-swaggerui)): Generate OpenAPI schemas and serve Swagger UI for REST API documentation
- MongoDB with Panache ([guide](https://quarkus.io/guides/mongodb-panache)): Simplify your persistence code for MongoDB via the active record or the repository pattern
- REST Jackson ([guide](https://quarkus.io/guides/rest#json-serialisation)): Jackson serialization support for Quarkus REST. This extension is not compatible with the quarkus-resteasy extension, or any of the extensions that depend on it
- YAML Configuration ([guide](https://quarkus.io/guides/config-yaml)): Use YAML to configure your Quarkus application
- Micrometer Registry Prometheus ([guide](https://quarkus.io/guides/micrometer)): Enable Prometheus support for Micrometer

## Provided Code

### YAML Config

Configure your application with YAML

[Related guide section...](https://quarkus.io/guides/config-reference#configuration-examples)

The Quarkus application configuration is located in `src/main/resources/application.yml`.

### REST

Easily start your REST Web Services

[Related guide section...](https://quarkus.io/guides/getting-started-reactive#reactive-jax-rs-resources)

### SmallRye Health

Monitor your application's health using SmallRye Health

[Related guide section...](https://quarkus.io/guides/smallrye-health)

## Database Seeder

This project includes a database seeder for populating sample data during development and testing.

### Configuration

The seeder is controlled via environment variables or `application.yml` configuration:

```yaml
seeder:
  enabled: true # Enable/disable seeding
  auto-clear: false # Clear existing data before seeding
  environments: dev,test # Only run in these profiles
```

### Environment Variables

| Variable              | Default    | Description                              |
| --------------------- | ---------- | ---------------------------------------- |
| `SEEDER_ENABLED`      | `true`     | Enable/disable seeder                    |
| `SEEDER_AUTO_CLEAR`   | `false`    | Clear existing data before seeding       |
| `SEEDER_ENVIRONMENTS` | `dev,test` | Comma-separated list of allowed profiles |

### Usage

The seeder runs automatically on application startup when:

- `SEEDER_ENABLED=true` (default)
- Current profile is in `SEEDER_ENVIRONMENTS` (default: `dev,test`)

To force re-seeding with existing data:

```bash
export SEEDER_AUTO_CLEAR=true
mvn quarkus:dev
```

To disable seeder:

```bash
export SEEDER_ENABLED=false
mvn quarkus:dev
```

### Seed Data

#### Users (9 sample users)

- **Individual Free User** - `individual.free@example.com` / `password123`
- **Individual Premium User** - `individual.premium@example.com` / `password123`
- **Individual Enterprise User** - `individual.enterprise@example.com` / `password123`
- **Organization Owner (Trial)** - `owner.trial@example.com` / `password123`
- **Organization Owner (Free)** - `owner.free@example.com` / `password123`
- **Organization Owner (Pro)** - `owner.pro@example.com` / `password123`
- **Organization Owner (Enterprise)** - `owner.enterprise@example.com` / `password123`
- **Organization Admin** - `admin.member@example.com` / `password123`
- **Organization Member** - `regular.member@example.com` / `password123`
- **Platform Admin** - `admin@psycorp.com` / `admin123`

#### Organizations (4 sample organizations)

- PT Startup Trial (free_trial plan)
- CV Usaha Gratis (free plan)
- PT Perusahaan Pro (pro plan)
- PT Korporasi Enterprise (enterprise plan)

#### Posts (2 sample posts)

- Post 1 (published)
- Post 2 (draft)

### Best Practices Implemented

1. **Idempotent** - Checks by unique identifier (email/title), not count
2. **Non-destructive** - Doesn't delete existing data unless `auto-clear=true`
3. **Environment-aware** - Only runs in specified profiles (dev/test)
4. **Configurable** - Full control via environment variables
5. **Well-logged** - Clear logging for debugging and tracking
