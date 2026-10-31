from __future__ import annotations

import asyncio
import logging
from collections.abc import Callable
from concurrent.futures import CancelledError, Future

from PySide6.QtCore import QObject, QTimer, Signal

from .bridge import AsyncBridge
from .controller import HappyLightingClient, describe_exception, is_likely_happylighting
from .models import AppState, LightDevice
from .settings import SettingsStore

_LOG = logging.getLogger(__name__)


class HappyLightingService(QObject):
    state_changed = Signal(object)
    error_message = Signal(str)
    _ble_dropped = Signal()  # fired from asyncio thread, handled on Qt thread
    _completion = Signal(object)  # zero-arg callable, delivered on the Qt thread

    def __init__(self) -> None:
        super().__init__()
        self.bridge = AsyncBridge()
        self.client = HappyLightingClient()
        self.settings = SettingsStore()
        self.state = AppState(
            saved_device_address=self.settings.get("last_device_address"),
            saved_device_name=self.settings.get("last_device_name"),
            brightness=int(self.settings.get("brightness", 100)),
            base_color=tuple(self.settings.get("base_color", [255, 140, 66])),
            applied_color=tuple(self.settings.get("applied_color", [255, 140, 66])),
        )
        self.client.set_brightness(self.state.brightness)
        self.client.set_base_color(self.state.base_color)
        self._scan_future: Future | None = None
        self._connect_future: Future | None = None
        self._startup_reconnect_future: Future | None = None
        self._shutting_down = False
        self._shutdown_complete = False
        self._expect_disconnect = False
        self._scan_user_initiated = True
        self._connect_user_initiated = True

        self._brightness_timer = QTimer(self)
        self._brightness_timer.setSingleShot(True)
        self._brightness_timer.setInterval(200)
        self._brightness_timer.timeout.connect(self._apply_pending_brightness)

        self._color_timer = QTimer(self)
        self._color_timer.setSingleShot(True)
        self._color_timer.setInterval(150)
        self._color_timer.timeout.connect(self._apply_pending_color)

        self._ble_dropped.connect(self._on_device_dropped)
        self._completion.connect(self._run_completion)

    # ---------------------------------------------------------------- public

    def emit_state(self) -> None:
        self.state_changed.emit(self.state.as_dict())

    def set_status(self, text: str) -> None:
        self.state.status_text = text
        self.emit_state()

    def startup(self) -> None:
        self.emit_state()
        if self.state.saved_device_address:
            self._start_saved_device_reconnect()
        else:
            self.scan_devices()

    def shutdown(self) -> None:
        if self._shutdown_complete:
            return
        self._shutting_down = True
        self._shutdown_complete = True
        self._expect_disconnect = True
        self._brightness_timer.stop()
        self._color_timer.stop()
        self._persist_color_state()
        try:
            self.bridge.submit(self.client.disconnect()).result(timeout=3.0)
        except Exception:
            _LOG.debug("disconnect during shutdown failed", exc_info=True)
        self.bridge.stop()

    def scan_devices(self, *, user_initiated: bool = True) -> None:
        if self._shutting_down:
            return
        if self._pending(self._scan_future):
            return
        self._startup_reconnect_future = None
        self._scan_user_initiated = user_initiated
        self.state.scanning = True
        self.set_status("Scanning for nearby BLE devices...")
        future = self.bridge.submit(self.client.scan_devices())
        self._scan_future = future
        future.add_done_callback(self._completed(self._handle_scan_complete))

    def connect_device(
        self, address: str | None = None, *, user_initiated: bool = True
    ) -> None:
        if self._shutting_down:
            return
        if self._pending(self._connect_future):
            return
        self._expect_disconnect = False
        self._startup_reconnect_future = None
        self._connect_user_initiated = user_initiated
        target_address = address or self.state.saved_device_address
        if not target_address:
            self.error_message.emit("No device selected — press Scan first.")
            return
        device = next((d for d in self.state.devices if d.address == target_address), None)
        if not device:
            device = LightDevice(self.state.saved_device_name or target_address, target_address)
        self.state.connecting = True
        self.set_status(f"Connecting to {device.name}...")
        future = self.bridge.submit(
            self.client.connect(device, self._ble_disconnected_callback)
        )
        self._connect_future = future
        future.add_done_callback(
            self._completed(lambda done: self._handle_connect_complete(done, device))
        )

    def disconnect_device(self) -> None:
        if self._shutting_down:
            return
        self._expect_disconnect = True
        self._brightness_timer.stop()
        self._color_timer.stop()
        future = self.bridge.submit(self.client.disconnect())
        future.add_done_callback(self._completed(self._handle_disconnect_complete))

    def reset_and_reconnect(self) -> None:
        if self._shutting_down:
            return
        address = self.state.saved_device_address or self.state.device_address
        if not address:
            self.error_message.emit("No saved device — connect to a device first.")
            return
        self._expect_disconnect = True
        self._brightness_timer.stop()
        self._color_timer.stop()
        self.set_status("Clearing stuck BLE connection…")
        future = self.bridge.submit(self._do_reset_and_reconnect(address))
        future.add_done_callback(self._completed(self._handle_reset_complete))

    def turn_on(self) -> None:
        self._submit_action(self.client.turn_on(), "Light powered on.")

    def turn_off(self) -> None:
        self._submit_action(self.client.turn_off(), "Light powered off.")

    def set_color(self, color: tuple[int, int, int]) -> None:
        if self._shutting_down:
            return
        self.state.base_color = color
        self.client.set_base_color(color)
        self.state.applied_color = self.client.output_color
        self.emit_state()
        self._color_timer.start()

    def set_brightness(self, value: int) -> None:
        if self._shutting_down:
            return
        self.state.brightness = value
        self.client.set_brightness(value)
        self.state.applied_color = self.client.output_color
        self.emit_state()
        self._brightness_timer.start()

    def apply_current_color(self, success_text: str | None = None) -> None:
        self.state.applied_color = self.client.output_color
        self._persist_color_state()
        self._submit_action(self.client.apply_current_color(), success_text or "Applied color.")

    # --------------------------------------------------------- BLE callbacks

    def _ble_disconnected_callback(self, _client) -> None:
        """Runs in the asyncio thread — relay to Qt via signal."""
        self._ble_dropped.emit()

    def _on_device_dropped(self) -> None:
        """Handles unexpected BLE disconnection on the Qt main thread."""
        if self._shutting_down or self._expect_disconnect:
            return
        self._brightness_timer.stop()
        self._color_timer.stop()
        self.state.connected = False
        self.state.device_name = "Not connected"
        self.state.device_address = None
        self.set_status("Device disconnected unexpectedly.")

    # ----------------------------------------------------------- async tasks

    async def _do_reset_and_reconnect(self, address: str) -> str:
        from .bluetooth_reset import reset_ble_device, reset_bluetooth_radio
        await self.client.disconnect()
        # Try targeted device reset first — fast and non-disruptive
        ok = await reset_ble_device(address)
        if not ok:
            # Fall back to full radio toggle
            ok = await reset_bluetooth_radio()
            if not ok:
                raise RuntimeError(
                    "Bluetooth reset failed. Try toggling Bluetooth manually in "
                    "Windows Settings → Bluetooth & devices."
                )
        await asyncio.sleep(1.5)
        return address

    def _persist_color_state(self) -> None:
        """Write the colour/brightness trio once, off the per-event path."""
        self.settings.update(
            {
                "base_color": list(self.state.base_color),
                "applied_color": list(self.state.applied_color),
                "brightness": self.state.brightness,
            }
        )

    def _apply_pending_brightness(self) -> None:
        self._persist_color_state()
        if not self.client.is_connected:
            return
        self._submit_action(self.client.apply_current_color(), "Brightness updated.")

    def _apply_pending_color(self) -> None:
        self._persist_color_state()
        if not self.client.is_connected:
            return
        self._submit_action(self.client.apply_current_color(), "Applied color.")

    # --------------------------------------------------------- submit helpers

    def _submit_action(self, coro, success_text: str) -> None:
        if self._shutting_down:
            coro.close()
            return
        future = self.bridge.submit(coro)
        future.add_done_callback(
            self._completed(lambda done: self._handle_generic_complete(done, success_text))
        )

    def _completed(self, handler: Callable[[Future], None]) -> Callable[[Future], None]:
        """Wrap a Future done-callback so it runs on the Qt thread, not the BLE thread."""

        def _dispatch(future: Future) -> None:
            self._completion.emit(lambda: handler(future))

        return _dispatch

    def _run_completion(self, callback: Callable[[], None]) -> None:
        callback()

    @staticmethod
    def _pending(future: Future | None) -> bool:
        return future is not None and not future.done()

    def _refresh_busy(self) -> None:
        """Recompute scanning/connecting from the in-flight futures.

        Two discovery paths can overlap (startup probe + a user pressing Scan),
        so whichever finishes first must not clear the other one's flag.
        """
        self.state.scanning = self._pending(self._scan_future) or self._pending(
            self._startup_reconnect_future
        )
        self.state.connecting = self._pending(self._connect_future)

    def _fail(self, exc: BaseException, label: str, *, banner: bool) -> None:
        if self._shutting_down:
            return
        text = describe_exception(exc)
        _LOG.warning("%s failed", label, exc_info=exc)
        self.set_status(text)
        if banner:
            self.error_message.emit(text)

    # ------------------------------------------------------------ callbacks

    def _handle_scan_complete(self, future: Future) -> None:
        if self._scan_future is future:
            self._scan_future = None
        self._refresh_busy()
        try:
            devices = future.result()
        except CancelledError:
            return
        except Exception as exc:
            self._fail(exc, "scan", banner=self._scan_user_initiated)
            return
        self.state.devices = devices
        likely = sum(1 for d in devices if is_likely_happylighting(d.name))
        self.set_status(
            f"Found {len(devices)} named BLE devices, {likely} likely HappyLighting-compatible."
        )
        if self.state.saved_device_address and not self.state.connected:
            if any(d.address == self.state.saved_device_address for d in devices):
                self.connect_device(
                    self.state.saved_device_address,
                    user_initiated=self._scan_user_initiated,
                )
            else:
                self.emit_state()

    def _handle_connect_complete(self, future: Future, device: LightDevice) -> None:
        if self._connect_future is future:
            self._connect_future = None
        self._refresh_busy()
        try:
            future.result()
        except CancelledError:
            return
        except Exception as exc:
            self._fail(exc, "connect", banner=self._connect_user_initiated)
            return
        self._expect_disconnect = False
        self.state.connected = True
        self.state.device_name = device.name
        self.state.device_address = device.address
        self.state.saved_device_address = device.address
        self.state.saved_device_name = device.name
        self.settings.update(
            {"last_device_address": device.address, "last_device_name": device.name}
        )
        self.set_status(f"Connected to {device.name}.")
        self.apply_current_color("Restored saved color.")

    def _start_saved_device_reconnect(self) -> None:
        if self._pending(self._startup_reconnect_future) or self._pending(self._connect_future):
            return
        address = self.state.saved_device_address
        if not address:
            self.scan_devices(user_initiated=False)
            return
        self.state.scanning = True
        self.set_status("Looking for your saved controller...")
        future = self.bridge.submit(self.client.find_saved_device(address, timeout=5.0))
        self._startup_reconnect_future = future
        future.add_done_callback(self._completed(self._handle_startup_reconnect_complete))

    def _handle_startup_reconnect_complete(self, future: Future) -> None:
        if self._startup_reconnect_future is future:
            self._startup_reconnect_future = None
        try:
            device = future.result()
        except CancelledError:
            return
        except Exception as exc:
            self._refresh_busy()
            self._fail(exc, "saved-device probe", banner=False)
            return
        if device is None:
            self.set_status("Saved controller not found yet. Scanning all nearby devices...")
            self.scan_devices(user_initiated=False)
            return
        self._refresh_busy()
        self.state.devices = [device, *[d for d in self.state.devices if d.address != device.address]]
        self.emit_state()
        self.connect_device(device.address, user_initiated=False)

    def _handle_disconnect_complete(self, future: Future) -> None:
        try:
            future.result()
        except Exception:
            # Errors during an intentional disconnect are expected and harmless.
            _LOG.debug("disconnect reported an error", exc_info=True)
        self.state.connected = False
        self.state.device_name = "Not connected"
        self.state.device_address = None
        self.set_status("Disconnected.")

    def _handle_reset_complete(self, future: Future) -> None:
        try:
            address = future.result()
        except CancelledError:
            return
        except Exception as exc:
            self._expect_disconnect = False
            self._fail(exc, "bluetooth reset", banner=True)
            return
        self.state.connected = False
        self.state.device_name = "Not connected"
        self.emit_state()
        self.set_status("Connection cleared — reconnecting…")
        self.connect_device(address, user_initiated=True)

    def _handle_generic_complete(self, future: Future, success_text: str) -> None:
        try:
            future.result()
        except CancelledError:
            return
        except Exception as exc:
            self._fail(exc, "device command", banner=False)
            return
        self.emit_state()
        if success_text:
            self.set_status(success_text)
