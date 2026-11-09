# Cythernoir TermKey

TermKey is an Android on-screen keyboard for terminal work and everyday English and Spanish typing. It combines a compact QWERTY layout with terminal controls, a numeric keypad, an emoji panel, word suggestions, and configurable themes.

## Features

- Compact QWERTY layouts for English and Spanish, including the Spanish `ñ` key.
- Numeric keypad with digits, punctuation, arithmetic operators, backspace, space, and enter.
- Emoji panel with scrollable categories and a visible button to return to the keyboard.
- Terminal controls: `Ctrl`, `Alt`, `Shift`, `Esc`, `Tab`, arrows, `Home`, `End`, `PgUp`, `PgDn`, and `F1`–`F12` in the full layout.
- English and Spanish word suggestions, swipe typing, and an optional learned user dictionary.
- Customizable keyboard themes, key size and font, haptic/audio feedback, and one-handed layout.
- Editable quick-insert macros.
- Optional streaming voice input and OpenAI-compatible text tools, configured by the user.

## Keyboard use

- Tap `123` to open the numeric keypad; tap `ABC` to return to the current letter layout.
- Tap the emoji key to open the emoji categories. Tap the keyboard icon to return to typing.
- Tap the language key to switch English/Spanish. Long-press it to switch between compact and full terminal layouts.
- Use the full layout when terminal-specific keys such as function keys or control modifiers are needed.
- Use the candidate strip to insert a suggested word. Backspace supports repeat and the configured clear gesture.

The numeric keypad is also selected automatically for number and phone input fields. Password fields suppress suggestions and learned input.

## Requirements

- Android Studio with Android SDK 37 installed.
- JDK 17 or a JDK supported by the installed Android Gradle Plugin.
- `minSdk` 26 and `targetSdk` 36 (Android 16).

## Build and test

On Windows PowerShell:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

On macOS/Linux:

```bash
./gradlew testDebugUnitTest assembleDebug
```

To install on a connected device or emulator, run `installDebug` instead of `assembleDebug`. Then open TermKey, enable it in Android's keyboard settings, and select it as the active input method.

For manual input checks, launch `com.termkey.ime/.InputTestActivity`. Verify English and Spanish typing, `ñ`, suggestions, numeric keypad entry and return to `ABC`, emoji category scrolling and insertion, and switching back from the emoji panel.

The current automated validation passes `testDebugUnitTest` (23 tests) and `assembleDebug`. Device-specific behavior still needs to be checked on a connected emulator or phone using the manual checks above.

## Project layout

- `app/src/main/java/com/termkey/ime/`: IME service, settings, dictionaries, macros, and supporting clients.
- `app/src/main/res/layout/`: keyboard and activity layouts.
- `app/src/main/res/xml/`: input method and preference configuration.
- `app/src/main/assets/`: bundled offline data.
- `app/src/test/java/com/termkey/ime/`: JVM unit tests.

## Configuration and privacy

Voice and LLM features are optional and require the user's own service configuration. Do not commit API keys, voice credentials, or signing credentials. Use `keystore.properties.example` as the local signing template.

## License

MIT
