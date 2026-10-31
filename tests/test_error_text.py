import unittest

from bleak.exc import (
    BleakBluetoothNotAvailableError,
    BleakBluetoothNotAvailableReason,
    BleakDeviceNotFoundError,
    BleakError,
    BleakGATTProtocolError,
)

from happylighting.controller import describe_exception

CASES = [
    TimeoutError(),  # bleak winrt async_timeout raises this with args == ()
    BleakDeviceNotFoundError("AA:BB:CC:DD:EE:01"),
    BleakDeviceNotFoundError("AA:BB:CC:DD:EE:01", "Device with address ... was not found."),
    BleakBluetoothNotAvailableError(
        "Bluetooth radio is not powered on. Turn on Bluetooth and try again.",
        BleakBluetoothNotAvailableReason.POWERED_OFF,
    ),
    BleakGATTProtocolError(5),
    BleakError(""),
    BleakError("Unreachable"),
    RuntimeError("No light is connected."),
]


class DescribeExceptionTests(unittest.TestCase):
    def test_never_empty(self):
        for exc in CASES:
            with self.subTest(exc=repr(exc)):
                text = describe_exception(exc)
                self.assertTrue(text.strip(), f"empty text for {exc!r}")

    def test_timeout_is_actionable(self):
        self.assertIn("Timed out", describe_exception(TimeoutError()))

    def test_device_not_found_names_the_address(self):
        text = describe_exception(BleakDeviceNotFoundError("AA:BB:CC:DD:EE:01"))
        self.assertIn("AA:BB:CC:DD:EE:01", text)

    def test_radio_off_is_a_sentence_not_a_tuple(self):
        text = describe_exception(
            BleakBluetoothNotAvailableError(
                "Bluetooth radio is not powered on. Turn on Bluetooth and try again.",
                BleakBluetoothNotAvailableReason.POWERED_OFF,
            )
        )
        self.assertNotIn("(", text)
        self.assertTrue(text.startswith("Bluetooth radio is not powered on"))

    def test_gatt_protocol_error_is_the_message_not_the_tuple(self):
        text = describe_exception(BleakGATTProtocolError(5))
        self.assertTrue(text.startswith("GATT Protocol Error"))
        self.assertNotIn("(5,", text)


if __name__ == "__main__":
    unittest.main()
