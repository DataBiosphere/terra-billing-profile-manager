

## Purpose
To provide a low overhead way to check that BPM is basically operational in a given environment.

These tests are intended to be run either as the final step in the deployment check list for releasing the service independently, or on an ad-hoc basis if needed.
They are currently set up to be run manually, but could be automated in the future.

## Scope
Verifying _basic_ functionality, based on arguments passed:
* The status endpoint returns 200, and all subsystems specified by the service are 'OK' 
* If a user access token is passed, the `GET /api/profiles/v1` endpoint will be called, and checked that it returns 200.
  This provides basic verification that the database connection is intact, even if the user has no billing profiles available.
* If an azure subscription id is passed, in addition to the user access token, the `GET /api/azure/v1/managedApps` endpoint, and checked that it returns 200.
  This provides basic verification that the service can connect to Azure.


## Setup

* Install [poetry](https://python-poetry.org)
* Install dependencies: `poetry install`
* Get a shell in the created venv to run the scripts: `poetry shell`
* Run the test: `python smoke_tests.py <args>`
* For full usage information: `python smoke_tests.py -h`

## Running as part of post-deployment steps:
* Do setup as described above above
* Get a user access token (any user that has access to BPM should be sufficient). Eg: `gcloud auth print-access-token`
* Get an Azure subscription id
* Run script in poetry shell: `python smoke_tests.py "${BPM_HOST}" "${USER_ACCESS_TOKEN}"  --azure-sub-id="${SUBSCRIPTION_ID}`