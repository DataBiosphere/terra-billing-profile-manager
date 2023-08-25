from tests.bpm_smoke_test_case import BPMSmokeTestCase


class BillingProfileTests(BPMSmokeTestCase):
    AZURE_SUBSCRIPTION_ID = None

    @staticmethod
    def billing_profile_list_url() -> str:
        return BPMSmokeTestCase.build_bpm_url("/api/profiles/v1")

    def test_retrieving_billing_profiles(self):
        response = BPMSmokeTestCase.call_bpm(self.billing_profile_list_url(), user_token=BPMSmokeTestCase.USER_TOKEN)
        self.assertEqual(response.status_code, 200)

