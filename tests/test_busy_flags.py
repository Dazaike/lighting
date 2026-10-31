import unittest
from concurrent.futures import Future

from happylighting.models import AppState
from happylighting.service import HappyLightingService


class _Stub:
    """Minimal stand-in for the service: only what _refresh_busy touches.

    Instantiating the real service starts a BLE event-loop thread and needs a
    QApplication, neither of which this invariant depends on.
    """

    _pending = staticmethod(HappyLightingService._pending)
    _refresh_busy = HappyLightingService._refresh_busy

    def __init__(self) -> None:
        self.state = AppState()
        self._scan_future: Future | None = None
        self._startup_reconnect_future: Future | None = None
        self._connect_future: Future | None = None


def _running() -> Future:
    return Future()


def _done() -> Future:
    future = Future()
    future.set_result(None)
    return future


class RefreshBusyTests(unittest.TestCase):
    def test_idle_clears_both_flags(self):
        stub = _Stub()
        stub.state.scanning = True
        stub.state.connecting = True
        stub._refresh_busy()
        self.assertFalse(stub.state.scanning)
        self.assertFalse(stub.state.connecting)

    def test_finished_startup_probe_keeps_user_scan_flagged(self):
        # The startup probe and a user-pressed Scan overlap; the probe finishing
        # must not report "not scanning" while the scan is still running.
        stub = _Stub()
        stub._startup_reconnect_future = _done()
        stub._scan_future = _running()
        stub._refresh_busy()
        self.assertTrue(stub.state.scanning)

    def test_finished_scan_keeps_startup_probe_flagged(self):
        stub = _Stub()
        stub._scan_future = _done()
        stub._startup_reconnect_future = _running()
        stub._refresh_busy()
        self.assertTrue(stub.state.scanning)

    def test_connecting_tracks_connect_future(self):
        stub = _Stub()
        stub._connect_future = _running()
        stub._refresh_busy()
        self.assertTrue(stub.state.connecting)
        stub._connect_future = _done()
        stub._refresh_busy()
        self.assertFalse(stub.state.connecting)

    def test_scanning_and_connecting_are_independent(self):
        stub = _Stub()
        stub._scan_future = _running()
        stub._connect_future = _done()
        stub._refresh_busy()
        self.assertTrue(stub.state.scanning)
        self.assertFalse(stub.state.connecting)


if __name__ == "__main__":
    unittest.main()
