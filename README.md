# Terra Billing Profile Manager

[![Build and Test](https://github.com/DataBiosphere/terra-billing-profile-manager/actions/workflows/build-and-test.yml/badge.svg?branch=main)](https://github.com/DataBiosphere/terra-billing-profile-manager/actions/workflows/build-and-test.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=DataBiosphere_terra-billing-profile-manager&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=DataBiosphere_terra-billing-profile-manager)

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

Troubleshooting:
* If you run into an error about `minney-kinney`, execute:

```sh
./minnie-kenny.sh --force   
```
* The build also runs the tests, so the tests will fail unless `postgres` is running (see next step).

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

To set up service account credentials and other configuration for running locally:
* Install yq `brew install yq`
* Run `render-configs.sh`

To run integration tests:
```sh
./gradlew bootRun &    # start up a local instance of the billing profile manager service
sleep 5                # wait until service comes up
./gradlew :integration:runTest --args="suites/FullIntegration.json /tmp/test"
```

### To run smoke tests
See `smoke_tests/README.md`

## Running Billing Profile Manager Locally

```sh
./gradlew :service:bootRun
```

Then navigate to the Swagger: http://localhost:8080/swagger-ui.html

## Linter
Automatically fix linting issues:
```sh
./gradlew spotlessApply
```

## Generate Java classes from Swagger documentation (.yml file definition)

```sh
./gradlew generateSwaggerCode
```


## SourceClear

[SourceClear](https://srcclr.github.io) is a static analysis tool that scans a project's Java
dependencies for known vulnerabilities. If you are working on addressing dependency vulnerabilities
in response to a SourceClear finding, you may want to run a scan off of a feature branch and/or local code.

### Github Action

You can trigger BPM's SCA scan on demand via its
[Github Action](https://github.com/broadinstitute/dsp-appsec-sourceclear-github-actions/actions/workflows/z-manual-terra-billing-profile-manager.yml),
and optionally specify a Github ref (branch, tag, or SHA) to check out from the repo to scan.  By default,
the scan is run off of BPM's `main` branch.

High-level results are outputted in the Github Actions run.

### Running Locally

You will need to get the API token from Vault before running the Gradle `srcclr` task.

```sh
export SRCCLR_API_TOKEN=$(vault read -field=api_token secret/secops/ci/srcclr/gradle-agent)
./gradlew srcclr
```

High-level results are outputted to the terminal.

### Veracode

Full results including dependency graphs are uploaded to
[Veracode](https://sca.analysiscenter.veracode.com/workspaces/jppForw/projects/551485/issues)
(if running off of a feature branch, navigate to Project Details > Selected Branch > Change to select your feature branch).
You can request a Veracode account to view full results from #dsp-infosec-champions.


## Tech Stack
BPM adheres to the [Terra Tech Stack](https://docs.google.com/document/d/1JkTrtaci7EI0TnuR-68zYgTx_mRCNu-u2eV9XhexWTI/edit#heading=h.5z6knaqygr4a). See linked document for relevant technology choices and the rationale behind their inclusion in this service. 
