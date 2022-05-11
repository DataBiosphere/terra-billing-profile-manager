# Terra Billing Profile Manager

[![Build and Test](https://github.com/DataBiosphere/terra-billing-profile-manager/actions/workflows/build-and-test.yml/badge.svg?branch=main)](https://github.com/DataBiosphere/terra-billing-profile-manager/actions/workflows/build-and-test.yml)

Terra Billing Profile Manager provides an API to set up and control access to billing
within Terra across cloud platforms.

## Building the code

> If you are a new member of the Broad, follow the [getting started guide](docs/getting-started.md)
first.

Ensure you have Java 17 and that it is the default. To check this while in the
`terra-billing-profile-manager` directory, type `java --version`.

Then, to build the code, run:

```sh
./gradlew build
```

## Running the tests

For tests, ensure you have a local Postgres instance running. While in the
`terra-billing-profile-manager` directory, initialize the database:

```sh
./service/local-dev/run_postgres.sh start
```

After the database is initialized, run unit tests:
```sh
./gradlew test
```

To run integration tests:
```sh
./gradlew bootRun &    # start up a local instance of the billing profile manager service
sleep 5                # wait until service comes up
render-configs.sh      # render service account credentials needed for tests
./gradlew :integration:runTest --args="suites/FullIntegration.json /tmp/test"
```

## Linter
Automatically fix linting issues:
```
./gradlew spotlessApply
```
