import json

from tests.bpm_smoke_test_case import BPMSmokeTestCase


class BPMManagedAppsTests(BPMSmokeTestCase):
    AZURE_SUBSCRIPTION_ID = None

    @staticmethod
    def managed_apps_url() -> str:
        return BPMSmokeTestCase.build_bpm_url("/api/azure/v1/managedApps")

    def test_retrieving_managed_apps(self):
        params = {"azureSubscriptionId": self.AZURE_SUBSCRIPTION_ID}
        response = BPMSmokeTestCase.call_bpm(self.managed_apps_url(), frozenset(params.items()),
                                             BPMSmokeTestCase.USER_TOKEN)
        self.assertEqual(response.status_code, 200)
        body = json.loads(response.text)
        apps = body["managedApps"]
        for app in apps:
            self.assertGreater(len(app["subscriptionId"]), 0)
            self.assertGreater(len(app["managedResourceGroupId"]), 0)
            self.assertGreater(len(app["tenantId"]), 0)
