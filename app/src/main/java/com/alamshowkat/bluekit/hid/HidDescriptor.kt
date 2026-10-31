package com.alamshowkat.bluekit.hid

/**
 * One combined HID report descriptor for every BlueKit mode.
 *
 * Report IDs keep the four devices independent on a single HID interface:
 *
 *  1  keyboard       8-byte boot-style report (modifiers + 6 keys)
 *  2  mouse          4-byte report (buttons, dx, dy, wheel)
 *  3  consumer       2-byte usage code (media keys)
 *  4  gamepad        9-byte report (16 buttons, hat, 6 analog axes)
 *
 * The descriptor is written from the USB HID specification rather than copied
 * from another app. Keeping one descriptor means the host only has to pair once
 * and every mode is available immediately.
 */
object HidDescriptor {

    const val KEYBOARD_REPORT_ID = 1
    const val MOUSE_REPORT_ID = 2
    const val CONSUMER_REPORT_ID = 3
    const val GAMEPAD_REPORT_ID = 4

    private fun bytes(vararg values: Int) = ByteArray(values.size) { values[it].toByte() }

    val DESCRIPTOR: ByteArray = bytes(
        // ---------------------------------------------------------- keyboard
        0x05, 0x01,             // Usage Page (Generic Desktop)
        0x09, 0x06,             // Usage (Keyboard)
        0xA1, 0x01,             // Collection (Application)
        0x85, KEYBOARD_REPORT_ID,
        0x05, 0x07,             //   Usage Page (Keyboard/Keypad)
        0x19, 0xE0,             //   Usage Minimum (Left Control)
        0x29, 0xE7,             //   Usage Maximum (Right GUI)
        0x15, 0x00,             //   Logical Minimum (0)
        0x25, 0x01,             //   Logical Maximum (1)
        0x75, 0x01,             //   Report Size (1)
        0x95, 0x08,             //   Report Count (8)
        0x81, 0x02,             //   Input (Data, Variable, Absolute)
        0x95, 0x01,             //   Report Count (1)
        0x75, 0x08,             //   Report Size (8)
        0x81, 0x01,             //   Input (Constant) - reserved byte
        0x95, 0x06,             //   Report Count (6)
        0x75, 0x08,             //   Report Size (8)
        0x15, 0x00,             //   Logical Minimum (0)
        0x25, 0x65,             //   Logical Maximum (101)
        0x19, 0x00,             //   Usage Minimum (0)
        0x29, 0x65,             //   Usage Maximum (101)
        0x81, 0x00,             //   Input (Data, Array, Absolute)
        0xC0,                   // End Collection

        // ------------------------------------------------------------- mouse
        0x05, 0x01,             // Usage Page (Generic Desktop)
        0x09, 0x02,             // Usage (Mouse)
        0xA1, 0x01,             // Collection (Application)
        0x85, MOUSE_REPORT_ID,
        0x09, 0x01,             //   Usage (Pointer)
        0xA1, 0x00,             //   Collection (Physical)
        0x05, 0x09,             //     Usage Page (Button)
        0x19, 0x01,             //     Usage Minimum (1)
        0x29, 0x05,             //     Usage Maximum (5)
        0x15, 0x00,             //     Logical Minimum (0)
        0x25, 0x01,             //     Logical Maximum (1)
        0x75, 0x01,             //     Report Size (1)
        0x95, 0x05,             //     Report Count (5)
        0x81, 0x02,             //     Input (Data, Variable, Absolute)
        0x95, 0x01,             //     Report Count (1)
        0x75, 0x03,             //     Report Size (3)
        0x81, 0x01,             //     Input (Constant) - padding
        0x05, 0x01,             //     Usage Page (Generic Desktop)
        0x09, 0x30,             //     Usage (X)
        0x09, 0x31,             //     Usage (Y)
        0x09, 0x38,             //     Usage (Wheel)
        0x15, 0x81,             //     Logical Minimum (-127)
        0x25, 0x7F,             //     Logical Maximum (127)
        0x75, 0x08,             //     Report Size (8)
        0x95, 0x03,             //     Report Count (3)
        0x81, 0x06,             //     Input (Data, Variable, Relative)
        0xC0,                   //   End Collection
        0xC0,                   // End Collection

        // ---------------------------------------------------------- consumer
        0x05, 0x0C,             // Usage Page (Consumer)
        0x09, 0x01,             // Usage (Consumer Control)
        0xA1, 0x01,             // Collection (Application)
        0x85, CONSUMER_REPORT_ID,
        0x15, 0x00,             //   Logical Minimum (0)
        0x26, 0xFF, 0x03,       //   Logical Maximum (1023)
        0x19, 0x00,             //   Usage Minimum (0)
        0x2A, 0xFF, 0x03,       //   Usage Maximum (1023)
        0x75, 0x10,             //   Report Size (16)
        0x95, 0x01,             //   Report Count (1)
        0x81, 0x00,             //   Input (Data, Array, Absolute)
        0xC0,                   // End Collection

        // ----------------------------------------------------------- gamepad
        0x05, 0x01,             // Usage Page (Generic Desktop)
        0x09, 0x05,             // Usage (Game Pad)
        0xA1, 0x01,             // Collection (Application)
        0x85, GAMEPAD_REPORT_ID,
        0x05, 0x09,             //   Usage Page (Button)
        0x19, 0x01,             //   Usage Minimum (1)
        0x29, 0x10,             //   Usage Maximum (16)
        0x15, 0x00,             //   Logical Minimum (0)
        0x25, 0x01,             //   Logical Maximum (1)
        0x75, 0x01,             //   Report Size (1)
        0x95, 0x10,             //   Report Count (16)
        0x81, 0x02,             //   Input (Data, Variable, Absolute)
        0x05, 0x01,             //   Usage Page (Generic Desktop)
        0x09, 0x39,             //   Usage (Hat switch)
        0x15, 0x00,             //   Logical Minimum (0)
        0x25, 0x07,             //   Logical Maximum (7)
        0x35, 0x00,             //   Physical Minimum (0)
        0x46, 0x3B, 0x01,       //   Physical Maximum (315)
        0x65, 0x14,             //   Unit (English Rotation: degrees)
        0x75, 0x04,             //   Report Size (4)
        0x95, 0x01,             //   Report Count (1)
        0x81, 0x42,             //   Input (Data, Variable, Absolute, Null State)
        0x65, 0x00,             //   Unit (None)
        0x75, 0x04,             //   Report Size (4)
        0x95, 0x01,             //   Report Count (1)
        0x81, 0x01,             //   Input (Constant) - padding
        0x09, 0x30,             //   Usage (X)
        0x09, 0x31,             //   Usage (Y)
        0x09, 0x32,             //   Usage (Z)  - left trigger
        0x09, 0x35,             //   Usage (Rz) - right trigger
        0x15, 0x00,             //   Logical Minimum (0)
        0x26, 0xFF, 0x00,       //   Logical Maximum (255)
        0x75, 0x08,             //   Report Size (8)
        0x95, 0x04,             //   Report Count (4)
        0x81, 0x02,             //   Input (Data, Variable, Absolute)
        0x09, 0x33,             //   Usage (Rx)
        0x09, 0x34,             //   Usage (Ry)
        0x15, 0x00,             //   Logical Minimum (0)
        0x26, 0xFF, 0x00,       //   Logical Maximum (255)
        0x75, 0x08,             //   Report Size (8)
        0x95, 0x02,             //   Report Count (2)
        0x81, 0x02,             //   Input (Data, Variable, Absolute)
        0xC0                    // End Collection
    )
}
