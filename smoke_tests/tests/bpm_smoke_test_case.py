import re
from functools import cache
from unittest import TestCase
from urllib.parse import urljoin

import requests
from requests import Response


class BPMSmokeTestCase(TestCase):
    BPM_HOST = None
    USER_TOKEN = None

    @staticmethod
    def build_bpm_url(path: str) -> str:
        assert BPMSmokeTestCase.BPM_HOST, "ERROR - BPMSmokeTests.BPM_HOST not properly set"
        if re.match(r"^\s*https?://", BPMSmokeTestCase.BPM_HOST):
            return urljoin(BPMSmokeTestCase.BPM_HOST, path)
        else:
            return urljoin(f"https://{BPMSmokeTestCase.BPM_HOST}", path)

    @staticmethod
    @cache
    def call_bpm(url: str, params: dict = None, user_token: str = None) -> Response:
        """Function is memoized so that we only make the call once"""
        headers = {"Authorization": f"Bearer {user_token}"} if user_token else {}
        return requests.get(url, params=params, headers=headers)
