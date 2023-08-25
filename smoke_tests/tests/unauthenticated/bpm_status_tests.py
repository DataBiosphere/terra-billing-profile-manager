import json

from ..bpm_smoke_test_case import BPMSmokeTestCase


class BPMStatusTests(BPMSmokeTestCase):
    @staticmethod
    def status_url() -> str:
        return BPMSmokeTestCase.build_bpm_url("/status")

    def test_status_code_is_200(self):
        print("Using BPM status URL: " + self.status_url())
        response = BPMSmokeTestCase.call_bpm(self.status_url())
        self.assertEqual(response.status_code, 200)

    def test_subsystems(self):
        response = BPMSmokeTestCase.call_bpm(self.status_url())
        status = json.loads(response.text)
        for system in status["systems"]:
            self.assertEqual(status["systems"][system]["ok"], True, f"{system} is not OK")