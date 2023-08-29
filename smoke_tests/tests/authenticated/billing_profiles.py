from tests.smoke_test_case import SmokeTestCase


class BillingProfileTests(SmokeTestCase):
    AZURE_SUBSCRIPTION_ID = None

    @staticmethod
    def billing_profile_list_url() -> str:
        return SmokeTestCase.build_bpm_url("/api/profiles/v1")

    def test_retrieving_billing_profiles(self):
        response = SmokeTestCase.call_bpm(self.billing_profile_list_url(), user_token=SmokeTestCase.USER_TOKEN)
        self.assertEqual(response.status_code, 200)
