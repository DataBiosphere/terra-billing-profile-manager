import json

from tests.smoke_test_case import SmokeTestCase


class ManagedAppsTests(SmokeTestCase):
    AZURE_SUBSCRIPTION_ID = None

    @staticmethod
    def managed_apps_url() -> str:
        return SmokeTestCase.build_bpm_url("/api/azure/v1/managedApps")

    def test_retrieving_managed_apps(self):
        params = {"azureSubscriptionId": self.AZURE_SUBSCRIPTION_ID}
        response = SmokeTestCase.call_bpm(self.managed_apps_url(), frozenset(params.items()),
                                          SmokeTestCase.USER_TOKEN)
        self.assertEqual(response.status_code, 200)
        body = json.loads(response.text)
        apps = body["managedApps"]
        # Note that this doesn't check that there are any number of items in the list of managed apps
        # only that any there have this form
        for app in apps:
            self.assertGreater(len(app["subscriptionId"]), 0)
            self.assertGreater(len(app["managedResourceGroupId"]), 0)
            self.assertGreater(len(app["tenantId"]), 0)
