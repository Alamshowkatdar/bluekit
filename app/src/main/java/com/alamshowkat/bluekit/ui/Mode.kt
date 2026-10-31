package com.alamshowkat.bluekit.ui

enum class Mode(val title: String, val subtitle: String) {
    KEYBOARD("Keyboard", "Type on the host"),
    TRACKPAD("Trackpad", "Pointer, clicks and scroll"),
    GAMEPAD("Gamepad", "Sticks, triggers and buttons"),
    REMOTE("Media", "Playback, volume and slides")
}
