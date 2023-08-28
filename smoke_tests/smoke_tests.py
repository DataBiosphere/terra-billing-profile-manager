import argparse
import sys
import unittest
from unittest import TestSuite
import requests

from tests.smoke_test_case import SmokeTestCase
from tests.unauthenticated.status_tests import StatusTests
from tests.authenticated.azure.managed_apps_tests import ManagedAppsTests
from tests.authenticated.billing_profiles import BillingProfileTests

DESCRIPTION = """
BPM Smoke Test
Enter the host (domain and optional port) of the BPM instance you want to to test. This test will ensure that the BPM 
instance running on that host is minimally functional.
"""


def gather_tests(is_authenticated: bool = False, azure_subscription: bool = False) -> TestSuite:
    suite = unittest.TestSuite()
    status_tests = unittest.defaultTestLoader.loadTestsFromTestCase(StatusTests)
    suite.addTests(status_tests)
    if is_authenticated:
        profile_tests = unittest.defaultTestLoader.loadTestsFromTestCase(BillingProfileTests)
        suite.addTests(profile_tests)
        if azure_subscription:
            managed_apps_tests = unittest.defaultTestLoader.loadTestsFromTestCase(ManagedAppsTests)
            suite.addTests(managed_apps_tests)
    return suite


def main(main_args):
    if main_args.user_token:
        verify_user_token(main_args.user_token)

    SmokeTestCase.BPM_HOST = main_args.bpm_host
    SmokeTestCase.USER_TOKEN = main_args.user_token
    ManagedAppsTests.AZURE_SUBSCRIPTION_ID = main_args.azure_sub_id

    test_suite = gather_tests(main_args.user_token, main_args.azure_sub_id)

    runner = unittest.TextTestRunner(verbosity=main_args.verbosity)
    runner.run(test_suite)


def verify_user_token(user_token: str):
    response = requests.get(f"https://www.googleapis.com/oauth2/v1/tokeninfo?access_token={user_token}")
    assert response.status_code == 200, "User Token is no longer valid.  Please generate a new token and try again."


def parse_args():
    parser = argparse.ArgumentParser(
        prog="BPM Smoke Tests",
        description=DESCRIPTION,
    )
    parser.add_argument(
        "bpm_host",
        help="domain with optional port number of the BPM host you want to test"
    )
    parser.add_argument(
        "user_token",
        nargs='?',
        default=None,
        help="Optional. If present, will test additional authenticated endpoints using the specified token"
    )
    parser.add_argument(
        "--azure_sub_id",
        type=str,
        help="Optional Azure SubscriptionId. If present, will test retrieving azure managed apps using the specified id"
    )
    parser.add_argument(
        "-v",
        "--verbosity",
        type=int,
        choices=[0, 1, 2],
        default=1,
        help="""Python unittest verbosity setting: 
    0: Quiet - Prints only number of tests executed
    1: Minimal - (default) Prints number of tests executed plus a dot for each success and an F for each failure
    2: Verbose - Help string and its result will be printed for each test"""
    )
    parsed_args = parser.parse_args()
    # Need to pop off sys.argv values to avoid messing with args passed to unittest.main()
    for _ in range(len(sys.argv[1:])):
        sys.argv.pop()
    return parsed_args


if __name__ == "__main__":
    args = parse_args()
    main(args)
    sys.exit(0)
