# Cythernoir TermKey

Built for terminals. Tuned for multi-language input and speed.

Advanced Android terminal and productivity keyboard featuring English, Spanish, and Chinese (Natural Shuangpin) layouts, gesture swipe-to-type, customizable color themes, editable macros, and streaming voice input.

## Features

- Terminal-first key behavior: sticky `Ctrl` / `Alt` / `Shift`, real control characters, `Alt` as `ESC + key`, F1-F12, arrows, PgUp/PgDn, Home/End.
- Multiple keyboard layouts & languages:
  - `COMPACT_EN`: Standard QWERTY English layout.
  - `COMPACT_ES`: Standard QWERTY Spanish layout with base prediction dictionary.
  - `COMPACT_ZH`: Chinese compact layout for double-pinyin (Shuangpin) input.
  - `FULL`: Full terminal keyboard with function row and control keys.
- Gesture Swipe-to-Type:
  - Slide your finger across character keys to trace and type words automatically in English and Spanish.
- Customizable Keyboard Themes & Colors:
  - Terminal Green (Default)
  - Classic Dark
  - Terminal Amber
  - Cyberpunk Purple
  - Light Theme
  - Crimson Red
  - Dynamic Wallpaper / System Theme
  - State-aware touch visual feedback on keypress.
- Natural Shuangpin Chinese input:
  - offline lexicon-backed candidate lookup
  - phrase and sentence-first candidate ordering
  - local user frequency learning
  - candidate bar click-to-commit
  - `Space` confirms the first Chinese candidate by default
- Streaming voice input using Volcengine WebSocket ASR.
- Editable macro bar with custom macros and pre-configured Spanish terminal commands (`ayuda`, `listar`, `estado`, `limpiar`, `ejecutar`, etc.).
- Compact-mode touch target expansion, hold-to-delete, and swipe-up clear for delete keys.

## Current Input Behavior

### Spanish mode (`ES`)

- Tap the language key (`EN` / `ES` / `ZH`) or select default input language in settings to switch into Spanish mode.
- Features base Spanish word prediction dictionary for smart autocomplete and spell correction.
- Supports swipe-to-type gesture typing.

### Chinese mode (`ZH`)

- Tap `中` to switch into Chinese mode.
- Letter keys enter Natural Shuangpin code.
- The candidate bar stays visible in Chinese mode.
- The first candidate is used as composing text.
- `Space` confirms the first candidate and clears the current input buffer.

### English mode (`EN`)

- Compact English keeps letters, numbers, and common punctuation visible.
- `FULL` mode restores the full terminal layout.

### Voice input

- Tap `MIC` to start streaming recognition.
- Recognition text appears live in the current input field.
- Tap `MIC` again to stop and finalize.
- Requires microphone permission plus Volcengine `APP ID`, `Access Token`, and `Resource ID` in settings.

## Build

### Requirements

- Android Studio Hedgehog or newer
- JDK 17
- Android SDK 34
- minSdk 26

### Command

```bash
./gradlew installDebug
```

## Setup

1. Install the debug build.
2. Open the `Cythernoir TermKey` app.
3. Enable the input method in Android keyboard settings.
4. Switch the active keyboard to `Cythernoir TermKey`.
5. Open `Settings` inside the app to configure themes, language, macros, or voice input.

## Settings

Key settings currently exposed:

- Keyboard theme selection (7 themes including Dynamic / System Theme)
- Default input language (English, Spanish, Chinese)
- Swipe-to-type gesture typing toggle
- Show/hide macro bar
- Show/hide F-key row
- Key height
- Vibrate on keypress
- Sound on keypress
- Sticky modifiers
- Swipe-up & long-press alternate symbols
- Voice input configuration
- Macro editing and reset

## Project Structure

```text
app/src/main/java/com/termkey/ime/
├── TermKeyIMEService.kt
├── NaturalShuangpinEngine.kt
├── ChineseLexiconStore.kt
├── SpanishLexiconStore.kt
├── KeyboardTheme.kt
├── VolcengineVoiceInputClient.kt
├── MacroManager.kt
├── MacroEditorActivity.kt
├── SettingsActivity.kt
└── SetupActivity.kt
```

## License

MIT
