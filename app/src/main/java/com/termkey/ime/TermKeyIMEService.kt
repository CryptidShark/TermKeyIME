package com.termkey.ime

import android.graphics.Rect
import android.inputmethodservice.InputMethodService
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.util.TypedValue
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.preference.PreferenceManager
import kotlin.math.hypot

/**
 * TermKeyIMEService — the main InputMethodService.
 *
 * Architecture:
 *  - onCreateInputView() inflates keyboard_view.xml and wires all key listeners
 *  - Modifier state (Ctrl, Alt, Shift) is handled as sticky toggles
 *  - Each key sends proper KeyEvents via InputConnection so terminal emulators
 *    (Termux, ConnectBot, JuiceSSH, etc.) receive real escape sequences
 *  - Ctrl combos generate the correct control characters (e.g. Ctrl+C → 0x03)
 */
class TermKeyIMEService : InputMethodService() {
    private data class RowTouchState(
        val bounds: Rect,
        val keys: List<Pair<View, Rect>>,
    )

    companion object {
        private const val TAG = "TermKeyVoice"
        private const val DELETE_REPEAT_INITIAL_DELAY_MS = 350L
        private const val DELETE_REPEAT_INTERVAL_MS = 60L
        private const val CLEAR_TEXT_CHUNK_SIZE = 1024

        private val ALL_KEY_IDS = intArrayOf(
            R.id.key_grave,
            R.id.key_1,
            R.id.key_2,
            R.id.key_3,
            R.id.key_4,
            R.id.key_5,
            R.id.key_6,
            R.id.key_7,
            R.id.key_8,
            R.id.key_9,
            R.id.key_0,
            R.id.key_minus,
            R.id.key_equals,
            R.id.key_backspace,
            R.id.key_tab,
            R.id.key_lbracket,
            R.id.key_q,
            R.id.key_w,
            R.id.key_e,
            R.id.key_r,
            R.id.key_t,
            R.id.key_y,
            R.id.key_u,
            R.id.key_i,
            R.id.key_o,
            R.id.key_p,
            R.id.key_rbracket,
            R.id.key_backslash,
            R.id.key_ctrl,
            R.id.key_semicolon,
            R.id.key_a,
            R.id.key_s,
            R.id.key_d,
            R.id.key_f_key,
            R.id.key_g,
            R.id.key_h,
            R.id.key_j,
            R.id.key_k,
            R.id.key_l,
            R.id.key_quote,
            R.id.key_enter,
            R.id.key_esc,
            R.id.key_alt,
            R.id.key_comma,
            R.id.key_z,
            R.id.key_x,
            R.id.key_c,
            R.id.key_v,
            R.id.key_b,
            R.id.key_n,
            R.id.key_m,
            R.id.key_period,
            R.id.key_slash,
            R.id.key_arrow_up,
            R.id.key_shift,
            R.id.key_lang,
            R.id.key_emoji,
            R.id.key_one_hand,
            R.id.key_page_up,
            R.id.key_page_down,
            R.id.key_home,
            R.id.key_end,
            R.id.key_space,
            R.id.key_delete,
            R.id.key_numbers,
            R.id.key_arrow_left,
            R.id.key_arrow_down,
            R.id.key_arrow_right,
        )

        private val COMPACT_EN_KEY_IDS = setOf(
            R.id.key_1,
            R.id.key_2,
            R.id.key_3,
            R.id.key_4,
            R.id.key_5,
            R.id.key_6,
            R.id.key_7,
            R.id.key_8,
            R.id.key_9,
            R.id.key_0,
            R.id.key_minus,
            R.id.key_equals,
            R.id.key_backspace,
            R.id.key_q,
            R.id.key_w,
            R.id.key_e,
            R.id.key_r,
            R.id.key_t,
            R.id.key_y,
            R.id.key_u,
            R.id.key_i,
            R.id.key_o,
            R.id.key_p,
            R.id.key_a,
            R.id.key_s,
            R.id.key_d,
            R.id.key_f_key,
            R.id.key_g,
            R.id.key_h,
            R.id.key_j,
            R.id.key_k,
            R.id.key_l,
            R.id.key_esc,
            R.id.key_z,
            R.id.key_x,
            R.id.key_c,
            R.id.key_v,
            R.id.key_b,
            R.id.key_n,
            R.id.key_m,
            R.id.key_arrow_up,
            R.id.key_shift,
            R.id.key_lang,
            R.id.key_emoji,
            R.id.key_one_hand,
            R.id.key_home,
            R.id.key_end,
            R.id.key_space,
            R.id.key_delete,
            R.id.key_numbers,
            R.id.key_arrow_right,
        )

        private val COMPACT_ES_KEY_IDS = COMPACT_EN_KEY_IDS + setOf(
            R.id.key_quote,
        )

        private val COMPACT_SYMBOL_KEY_IDS = setOf(
            R.id.key_grave,
            R.id.key_1,
            R.id.key_2,
            R.id.key_3,
            R.id.key_4,
            R.id.key_5,
            R.id.key_6,
            R.id.key_7,
            R.id.key_8,
            R.id.key_9,
            R.id.key_0,
            R.id.key_minus,
            R.id.key_equals,
            R.id.key_backspace,
            R.id.key_q,
            R.id.key_w,
            R.id.key_e,
            R.id.key_r,
            R.id.key_t,
            R.id.key_y,
            R.id.key_u,
            R.id.key_i,
            R.id.key_o,
            R.id.key_p,
            R.id.key_lbracket,
            R.id.key_rbracket,
            R.id.key_backslash,
            R.id.key_a,
            R.id.key_s,
            R.id.key_d,
            R.id.key_f_key,
            R.id.key_g,
            R.id.key_h,
            R.id.key_j,
            R.id.key_k,
            R.id.key_l,
            R.id.key_semicolon,
            R.id.key_quote,
            R.id.key_enter,
            R.id.key_z,
            R.id.key_x,
            R.id.key_c,
            R.id.key_v,
            R.id.key_b,
            R.id.key_n,
            R.id.key_m,
            R.id.key_comma,
            R.id.key_period,
            R.id.key_slash,
            R.id.key_lang,
            R.id.key_emoji,
            R.id.key_one_hand,
            R.id.key_home,
            R.id.key_space,
            R.id.key_numbers,
            R.id.key_arrow_right,
        )

        private val COMPACT_NUMERIC_KEY_IDS = setOf(
            R.id.key_1,
            R.id.key_2,
            R.id.key_3,
            R.id.key_4,
            R.id.key_5,
            R.id.key_6,
            R.id.key_7,
            R.id.key_8,
            R.id.key_9,
            R.id.key_0,
            R.id.key_minus,
            R.id.key_equals,
            R.id.key_backspace,
            R.id.key_q,
            R.id.key_w,
            R.id.key_e,
            R.id.key_r,
            R.id.key_t,
            R.id.key_y,
            R.id.key_comma,
            R.id.key_period,
            R.id.key_slash,
            R.id.key_space,
            R.id.key_enter,
            R.id.key_numbers,
            R.id.key_emoji,
            R.id.key_one_hand,
            R.id.key_lang,
            R.id.key_arrow_right,
        )

        private val SYMBOL_KEY_LABELS = mapOf(
            R.id.key_grave to "~",
            R.id.key_1 to "!",
            R.id.key_2 to "@",
            R.id.key_3 to "#",
            R.id.key_4 to "$",
            R.id.key_5 to "%",
            R.id.key_6 to "^",
            R.id.key_7 to "&",
            R.id.key_8 to "*",
            R.id.key_9 to "(",
            R.id.key_0 to ")",
            R.id.key_minus to "_",
            R.id.key_equals to "+",
            R.id.key_q to "[",
            R.id.key_w to "]",
            R.id.key_e to "{",
            R.id.key_r to "}",
            R.id.key_t to "^",
            R.id.key_y to "|",
            R.id.key_u to "<",
            R.id.key_i to ">",
            R.id.key_o to "<",
            R.id.key_p to ">",
            R.id.key_lbracket to "[",
            R.id.key_rbracket to "]",
            R.id.key_backslash to "`",
            R.id.key_a to ":",
            R.id.key_s to ";",
            R.id.key_d to "\"",
            R.id.key_f_key to "'",
            R.id.key_g to "/",
            R.id.key_h to "\\",
            R.id.key_j to "=",
            R.id.key_k to "+",
            R.id.key_l to "*",
            R.id.key_semicolon to "/",
            R.id.key_quote to "~",
            R.id.key_z to ",",
            R.id.key_x to ".",
            R.id.key_c to "?",
            R.id.key_v to "!",
            R.id.key_b to ":",
            R.id.key_n to ";",
            R.id.key_m to "-",
            R.id.key_comma to "<",
            R.id.key_period to ">",
            R.id.key_slash to "/",
        )

        private val NUMERIC_KEY_LABELS = mapOf(
            R.id.key_1 to "1",
            R.id.key_2 to "2",
            R.id.key_3 to "3",
            R.id.key_4 to "4",
            R.id.key_5 to "5",
            R.id.key_6 to "6",
            R.id.key_7 to "7",
            R.id.key_8 to "8",
            R.id.key_9 to "9",
            R.id.key_0 to "0",
            R.id.key_minus to "-",
            R.id.key_equals to "+",
            R.id.key_q to "+",
            R.id.key_w to "-",
            R.id.key_e to "*",
            R.id.key_r to "/",
            R.id.key_t to ".",
            R.id.key_y to ",",
            R.id.key_comma to ",",
            R.id.key_period to ".",
            R.id.key_slash to "/",
        )
    }

    private enum class InputLanguage {
        EN, ES
    }

    private enum class KeyboardLayoutMode {
        FULL,
        COMPACT_EN,
        COMPACT_ES,
        COMPACT_SYMBOL,
        COMPACT_NUMERIC,
        ONE_HANDED_LEFT,
        ONE_HANDED_RIGHT,
    }

    // ── Modifier state ───────────────────────────────────────────────────────
    private var ctrlActive = false
    private var altActive = false
    private var shiftActive = false
    private var lastShiftTapTime = 0L
    private var capsLockActive = false
    private var isPasswordField = false
    private var currentInputLang = InputLanguage.EN
    private val spanishLexiconStore by lazy { SpanishLexiconStore() }
    private val englishLexiconStore by lazy { EnglishLexiconStore() }
    private val userDictionaryStore by lazy { UserDictionaryStore(this) }
    private var currentSpanishWord = ""
    private var currentEnglishWord = ""
    private var isSwipeTypingActive = false
    private val swipeTraceChars = StringBuilder()

    private fun matchSwipeWord(traced: String): String? {
        return when (currentInputLang) {
            InputLanguage.ES -> {
                val candidates = spanishLexiconStore.lookupCandidates(traced)
                candidates.firstOrNull()?.text ?: spanishLexiconStore.correctSpelling(traced)
            }
            else -> {
                val commonWords = listOf("the", "and", "that", "with", "this", "from", "they", "know", "want", "been", "good", "much", "some", "time", "very", "when", "come", "here", "just", "like", "long", "make", "many", "more", "only", "over", "such", "take", "than", "them", "well", "were", "hola", "gracias", "por", "favor", "terminal", "keyboard", "code", "run", "test", "build")
                commonWords.firstOrNull { it.startsWith(traced) || it.contains(traced) }
                    ?: commonWords.minByOrNull { levenshteinDistance(it, traced) } ?: traced
            }
        }
    }

    private val cachedCharKeyBounds = mutableListOf<Pair<Rect, Char>>()

    private fun initCharKeyBounds(charKeysMap: Map<Int, Pair<Char, Char>>) {
        try {
            cachedCharKeyBounds.clear()
            if (!isViewInitialized()) return
            charKeysMap.forEach { (viewId, chars) ->
                val v = rootView.findViewById<View>(viewId) ?: return@forEach
                if (!v.isVisible || v.width <= 0 || v.height <= 0) return@forEach
                val loc = intArrayOf(0, 0)
                v.getLocationOnScreen(loc)
                val rect = Rect(loc[0], loc[1], loc[0] + v.width, loc[1] + v.height)
                cachedCharKeyBounds.add(rect to chars.first)
            }
        } catch (_: Exception) {}
    }

    private fun getCharUnderPointer(rawX: Float, rawY: Float): Char? {
        try {
            val rx = rawX.toInt()
            val ry = rawY.toInt()
            for ((rect, ch) in cachedCharKeyBounds) {
                if (rect.contains(rx, ry)) {
                    return ch
                }
            }
        } catch (_: Exception) {}
        return null
    }

    // ── Views ────────────────────────────────────────────────────────────────
    private lateinit var rootView: View
    private lateinit var macroScrollView: HorizontalScrollView
    private lateinit var macroContainer: LinearLayout
    private lateinit var macroSeparator: View
    private lateinit var candidateScrollView: HorizontalScrollView
    private lateinit var candidateContainer: LinearLayout
    private lateinit var fnRow: LinearLayout
    private lateinit var keyRows: List<LinearLayout>
    private lateinit var languageKey: TextView
    private lateinit var numbersKey: TextView
    private lateinit var numericPad: View
    private lateinit var llmPanel: View
    private lateinit var llmPanelTitle: TextView
    private lateinit var llmPanelStatus: TextView
    private lateinit var llmPanelSource: TextView
    private lateinit var llmOptionScroll: HorizontalScrollView
    private lateinit var llmOptionContainer: LinearLayout
    private lateinit var llmPanelResult: TextView
    private lateinit var llmActionReplace: TextView
    private lateinit var llmActionRetry: TextView
    private lateinit var llmActionCancel: TextView
    private lateinit var adaptiveTouchDelegate: AdaptiveTouchDelegateGroup
    private lateinit var swipeTrailView: KeyboardContentView

    // ── Prefs ────────────────────────────────────────────────────────────────
    private val prefs by lazy { PreferenceManager.getDefaultSharedPreferences(this) }
    private var layoutMode = KeyboardLayoutMode.COMPACT_EN
    private var previousCompactLayoutMode = KeyboardLayoutMode.COMPACT_EN
    private var llmClient: OpenAiCompatLlmClient? = null
    private var llmPreviewState: LlmPreviewState? = null
    private val repeatHandler = Handler(Looper.getMainLooper())
    private val adaptiveTouchRefreshRunnable = Runnable { rebuildAdaptiveTouchTargets() }

    // ── Vibrator ─────────────────────────────────────────────────────────────
    private val vibrator: Vibrator? by lazy {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (getSystemService(VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (_: Exception) {
            null
        }
    }

    // ── AudioManager ─────────────────────────────────────────────────────────
    private val audioManager: AudioManager? by lazy {
        try {
            getSystemService(AUDIO_SERVICE) as? AudioManager
        } catch (_: Exception) {
            null
        }
    }

    // ── IME lifecycle ────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
    }

    override fun onEvaluateFullscreenMode(): Boolean {
        return false
    }

    @Suppress("InflateParams")
    override fun onCreateInputView(): View {
        rootView = layoutInflater.inflate(R.layout.keyboard_view, null)
        macroScrollView = rootView.findViewById(R.id.macro_scroll)
        macroContainer = rootView.findViewById(R.id.macro_container)
        macroSeparator = rootView.findViewById(R.id.macro_separator)
        candidateScrollView = rootView.findViewById(R.id.candidate_scroll)
        candidateContainer = rootView.findViewById(R.id.candidate_container)
        fnRow = rootView.findViewById(R.id.fn_row)
        keyRows = listOf(
            rootView.findViewById(R.id.key_row_1),
            rootView.findViewById(R.id.key_row_2),
            rootView.findViewById(R.id.key_row_3),
            rootView.findViewById(R.id.key_row_4),
            rootView.findViewById(R.id.key_row_5),
        )
        languageKey = rootView.findViewById(R.id.key_lang)
        numbersKey = rootView.findViewById(R.id.key_numbers)
        llmPanel = rootView.findViewById(R.id.llm_panel)
        llmPanelTitle = rootView.findViewById(R.id.llm_panel_title)
        llmPanelStatus = rootView.findViewById(R.id.llm_panel_status)
        llmPanelSource = rootView.findViewById(R.id.llm_panel_source)
        llmOptionScroll = rootView.findViewById(R.id.llm_option_scroll)
        llmOptionContainer = rootView.findViewById(R.id.llm_option_container)
        llmPanelResult = rootView.findViewById(R.id.llm_panel_result)
        llmActionReplace = rootView.findViewById(R.id.llm_action_replace)
        llmActionRetry = rootView.findViewById(R.id.llm_action_retry)
        llmActionCancel = rootView.findViewById(R.id.llm_action_cancel)

        initializeKeyLabels()
        applyPreferences()
        buildMacroBar()
        wireKeys()
        wireLlmPanel()
        wireNumericPad()
        numericPad = rootView.findViewById(R.id.numeric_pad)
        adaptiveTouchDelegate = AdaptiveTouchDelegateGroup(rootView)
        swipeTrailView = rootView.findViewById(R.id.keyboard_content)
        rootView.touchDelegate = adaptiveTouchDelegate
        scheduleAdaptiveTouchTargetsUpdate()

        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
            val navInsets = insets.getInsets(
                WindowInsetsCompat.Type.navigationBars() or WindowInsetsCompat.Type.systemGestures(),
            )
            val bottomPadding = navInsets.bottom
            v.setPadding(
                v.paddingLeft,
                v.paddingTop,
                v.paddingRight,
                bottomPadding,
            )
            insets
        }

        return rootView
    }

    override fun onStartInput(info: EditorInfo?, restarting: Boolean) {
        super.onStartInput(info, restarting)
        handleInputStart(info)
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        try {
            super.onStartInputView(info, restarting)
            applyPreferences()
            resetModifiers()
            dismissLlmPanel(cancelRequest = true)
            handleInputStart(info)
        } catch (e: Exception) {
            Log.e(TAG, "Error in onStartInputView", e)
        }
    }

    private fun handleInputStart(info: EditorInfo?) {
        val incognitoPref = prefs.getBoolean("incognito_mode", false)
        val inputType = info?.inputType ?: 0
        val variation = inputType and EditorInfo.TYPE_MASK_VARIATION
        val classType = inputType and EditorInfo.TYPE_MASK_CLASS

        val isPassVar = classType == EditorInfo.TYPE_CLASS_TEXT && (
                variation == EditorInfo.TYPE_TEXT_VARIATION_PASSWORD ||
                variation == EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
                variation == EditorInfo.TYPE_TEXT_VARIATION_WEB_PASSWORD ||
                (inputType and EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS) != 0
                )

        isPasswordField = incognitoPref || isPassVar

        val isNumericField = classType == EditorInfo.TYPE_CLASS_NUMBER ||
                classType == EditorInfo.TYPE_CLASS_PHONE

        if (isPasswordField) {
            currentSpanishWord = ""
            currentEnglishWord = ""
            swipeTraceChars.setLength(0)
            layoutMode = defaultLayoutModeForLanguage()
            if (::candidateScrollView.isInitialized) {
                candidateScrollView.visibility = View.GONE
            }
            if (::rootView.isInitialized) updateKeyboardLayoutUi()
        } else {
            if (::candidateScrollView.isInitialized) {
                candidateScrollView.visibility = View.VISIBLE
            }
            if (isNumericField) {
                layoutMode = KeyboardLayoutMode.COMPACT_NUMERIC
            } else {
                layoutMode = defaultLayoutModeForLanguage()
            }
            if (::rootView.isInitialized) updateKeyboardLayoutUi()
        }
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        stopDeleteRepeats()
        dismissLlmPanel(cancelRequest = true)
    }

    override fun onDestroy() {
        stopDeleteRepeats()
        dismissLlmPanel(cancelRequest = true)
        super.onDestroy()
    }

    override fun onUpdateSelection(
        oldSelStart: Int,
        oldSelEnd: Int,
        newSelStart: Int,
        newSelEnd: Int,
        candidatesStart: Int,
        candidatesEnd: Int,
    ) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        if (newSelStart != newSelEnd) {
            currentSpanishWord = ""
            currentEnglishWord = ""
        }
        refreshLlmPreviewValidity()
        if (!isPasswordField) {
            refreshCandidateArea()
        }
    }

    private fun isViewInitialized(): Boolean {
        return ::rootView.isInitialized &&
                ::candidateContainer.isInitialized &&
                ::candidateScrollView.isInitialized &&
                ::macroContainer.isInitialized &&
                ::languageKey.isInitialized
    }

    // ── Preferences ──────────────────────────────────────────────────────────

    private fun applyPreferences() {
        val themeKey = prefs.getString("keyboard_theme", "terminal_green")
        val palette = KeyboardTheme.getPalette(themeKey, prefs)
        applyThemePalette(palette)
        applyKeyboardFont()

        val langPref = prefs.getString("input_language", "en")
        val newLang = when (langPref) {
            "es" -> InputLanguage.ES
            else -> InputLanguage.EN
        }
        if (newLang != currentInputLang) {
            currentInputLang = newLang
            layoutMode = defaultLayoutModeForLanguage()
        }

        if (prefs.getBoolean("incognito_mode", false)) {
            isPasswordField = true
        }

        refreshCandidateArea()
        updateKeyboardLayoutUi()
        updateLlmPanelUi()
        scheduleAdaptiveTouchTargetsUpdate()
    }

    private fun applyKeyboardFont() {
        val fontPref = prefs.getString("keyboard_font", "monospace")
        val typeface = when (fontPref) {
            "sans-serif" -> android.graphics.Typeface.SANS_SERIF
            "serif" -> android.graphics.Typeface.SERIF
            "sans-serif-medium" -> android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.BOLD)
            "default" -> android.graphics.Typeface.DEFAULT
            else -> android.graphics.Typeface.MONOSPACE
        }

        val applyTypeface: (View?) -> Unit = { view ->
            if (view is TextView) {
                view.typeface = typeface
            }
        }

        if (::keyRows.isInitialized) {
            keyRows.forEach { row ->
                for (i in 0 until row.childCount) {
                    applyTypeface(row.getChildAt(i))
                }
            }
        }
        if (::fnRow.isInitialized) {
            for (i in 0 until fnRow.childCount) {
                applyTypeface(fnRow.getChildAt(i))
            }
        }
    }

    private fun applyThemePalette(palette: KeyboardColorPalette) {
        if (!::rootView.isInitialized) return
        rootView.setBackgroundColor(palette.surfaceColor)
        if (::macroScrollView.isInitialized) macroScrollView.setBackgroundColor(palette.surface2Color)
        if (::candidateScrollView.isInitialized) candidateScrollView.setBackgroundColor(palette.surface2Color)
        if (::macroSeparator.isInitialized) macroSeparator.setBackgroundColor(palette.borderColor)
        if (::swipeTrailView.isInitialized) swipeTrailView.setTrailColor(palette.accentColor)

        val applyToView: (View?) -> Unit = { view ->
            when (view) {
                is TextView -> {
                    view.setTextColor(palette.textColor)
                    val states = arrayOf(
                        intArrayOf(android.R.attr.state_pressed),
                        intArrayOf()
                    )
                    val colors = intArrayOf(
                        palette.accentColor,
                        palette.keyBgColor
                    )
                    view.backgroundTintList = android.content.res.ColorStateList(states, colors)
                }
            }
        }

        if (::keyRows.isInitialized) {
            keyRows.forEach { row ->
                for (i in 0 until row.childCount) {
                    applyToView(row.getChildAt(i))
                }
            }
        }
        if (::fnRow.isInitialized) {
            for (i in 0 until fnRow.childCount) {
                applyToView(fnRow.getChildAt(i))
            }
        }
    }

    // ── Macro bar ─────────────────────────────────────────────────────────────

    private fun buildMacroBar() {
        macroContainer.removeAllViews()
        val macros = MacroManager.getMacros(this)
        macros.forEach { macro ->
            val btn = layoutInflater.inflate(R.layout.macro_button, macroContainer, false) as TextView
            btn.text = macro.label
            btn.setOnClickListener {
                feedbackVibrate()
                feedbackSound()
                currentInputConnection?.commitText(macro.text, 1)
            }
            btn.setOnLongClickListener {
                true
            }
            macroContainer.addView(btn)
        }
    }

    private fun baseCharacterKeyLabels() = mapOf(
            R.id.key_grave to "`",
            R.id.key_1 to "1",
            R.id.key_2 to "2",
            R.id.key_3 to "3",
            R.id.key_4 to "4",
            R.id.key_5 to "5",
            R.id.key_6 to "6",
            R.id.key_7 to "7",
            R.id.key_8 to "8",
            R.id.key_9 to "9",
            R.id.key_0 to "0",
            R.id.key_minus to "-",
            R.id.key_equals to "=",
            R.id.key_lbracket to "[",
            R.id.key_q to "Q",
            R.id.key_w to "W",
            R.id.key_e to "E",
            R.id.key_r to "R",
            R.id.key_t to "T",
            R.id.key_y to "Y",
            R.id.key_u to "U",
            R.id.key_i to "I",
            R.id.key_o to "O",
            R.id.key_p to "P",
            R.id.key_rbracket to "]",
            R.id.key_backslash to "\\",
            R.id.key_semicolon to ";",
            R.id.key_a to "A",
            R.id.key_s to "S",
            R.id.key_d to "D",
            R.id.key_f_key to "F",
            R.id.key_g to "G",
            R.id.key_h to "H",
            R.id.key_j to "J",
            R.id.key_k to "K",
            R.id.key_l to "L",
            R.id.key_quote to "'",
            R.id.key_comma to ",",
            R.id.key_z to "Z",
            R.id.key_x to "X",
            R.id.key_c to "C",
            R.id.key_v to "V",
            R.id.key_b to "B",
            R.id.key_n to "N",
            R.id.key_m to "M",
            R.id.key_period to ".",
            R.id.key_slash to "/",
        )

    private fun initializeKeyLabels() {
        val labels = baseCharacterKeyLabels() + mapOf(
            R.id.key_f1 to "F1",
            R.id.key_f2 to "F2",
            R.id.key_f3 to "F3",
            R.id.key_f4 to "F4",
            R.id.key_f5 to "F5",
            R.id.key_f6 to "F6",
            R.id.key_f7 to "F7",
            R.id.key_f8 to "F8",
            R.id.key_f9 to "F9",
            R.id.key_f10 to "F10",
            R.id.key_f11 to "F11",
            R.id.key_f12 to "F12",
            R.id.key_lang to getString(R.string.key_lang_en),
            R.id.key_emoji to "😊",
            R.id.key_numbers to "123",
        )

        labels.forEach { (viewId, label) ->
            rootView.findViewById<TextView>(viewId)?.text = label
        }
    }

    // ── Key wiring ────────────────────────────────────────────────────────────

    private fun wireKeys() {
        rootView.findViewById<View>(R.id.key_ctrl)?.setOnClickListener {
            if (handleCompactPunctuationKey(R.id.key_ctrl)) {
                feedbackVibrate(16)
                feedbackSound()
                return@setOnClickListener
            }
            feedbackVibrate(15)
            ctrlActive = !ctrlActive
            updateModifierUI()
        }
        rootView.findViewById<View>(R.id.key_alt)?.setOnClickListener {
            if (handleCompactPunctuationKey(R.id.key_alt)) {
                feedbackVibrate(16)
                feedbackSound()
                return@setOnClickListener
            }
            feedbackVibrate(15)
            altActive = !altActive
            updateModifierUI()
        }
        wireModifierKey(R.id.key_shift) {
            val now = System.currentTimeMillis()
            if (now - lastShiftTapTime < 400) {
                capsLockActive = !capsLockActive
                shiftActive = capsLockActive
            } else {
                if (capsLockActive) {
                    capsLockActive = false
                    shiftActive = false
                } else {
                    shiftActive = !shiftActive
                }
            }
            lastShiftTapTime = now
            updateModifierUI()
        }
        rootView.findViewById<View>(R.id.key_lang)?.apply {
            setOnClickListener {
                feedbackVibrate(15)
                toggleLanguageMode()
            }
            setOnLongClickListener {
                feedbackVibrate(20)
                toggleFullLayoutMode()
                true
            }
        }

        wireKey(R.id.key_esc)       {
            if (!handleCompactPunctuationKey(R.id.key_esc)) {
                sendEscape()
            }
        }
        wireKey(R.id.key_tab)       {
            if (!handleCompactPunctuationKey(R.id.key_tab)) {
                sendTab()
            }
        }
        wireKey(R.id.key_enter)     { sendEnter() }
        wireDeleteKey(R.id.key_backspace, ::performBackspace, ::clearAllBeforeCursor)
        wireKey(R.id.key_space)     { sendSpace() }
        rootView.findViewById<View>(R.id.key_space)?.setOnLongClickListener {
            feedbackVibrate(20)
            feedbackSound()
            showLanguagePickerDialog()
            true
        }
        wireDeleteKey(R.id.key_delete, ::performForwardDelete, ::clearAllAfterCursor)
        wireKey(R.id.key_emoji)     { showEmojiPicker() }
        wireKey(R.id.key_numbers)   { toggleNumericMode() }
        wireKey(R.id.key_one_hand)  {
            feedbackVibrate(15)
            feedbackSound()
            layoutMode = when (layoutMode) {
                KeyboardLayoutMode.FULL -> KeyboardLayoutMode.ONE_HANDED_LEFT
                KeyboardLayoutMode.ONE_HANDED_LEFT -> KeyboardLayoutMode.ONE_HANDED_RIGHT
                KeyboardLayoutMode.ONE_HANDED_RIGHT -> KeyboardLayoutMode.FULL
                else -> KeyboardLayoutMode.ONE_HANDED_LEFT
            }
            updateKeyboardLayoutUi()
        }

        COMPACT_SYMBOL_KEY_IDS.forEach { viewId ->
            rootView.findViewById<View>(viewId)?.setOnClickListener {
                if (layoutMode == KeyboardLayoutMode.COMPACT_SYMBOL) {
                    if (handleSymbolModeCharacterKey(viewId)) {
                        feedbackVibrate(16)
                        feedbackSound()
                    }
                }
            }
        }

        wireCompactBackspaceSwapKey(R.id.key_arrow_up)
        wireKey(R.id.key_arrow_down)  { sendKeyCode(KeyEvent.KEYCODE_DPAD_DOWN) }
        wireKey(R.id.key_arrow_left)  { sendKeyCode(KeyEvent.KEYCODE_DPAD_LEFT) }
        wireKey(R.id.key_arrow_right) {
            if (!handleCompactBottomEnterKey()) {
                sendKeyCode(KeyEvent.KEYCODE_DPAD_RIGHT)
            }
        }
        wireKey(R.id.key_page_up)     { sendKeyCode(KeyEvent.KEYCODE_PAGE_UP) }
        wireKey(R.id.key_page_down)   { sendKeyCode(KeyEvent.KEYCODE_PAGE_DOWN) }
        wireKey(R.id.key_home)        {
            if (!handleSymbolModeToggleKey()) {
                sendKeyCode(KeyEvent.KEYCODE_MOVE_HOME)
            }
        }
        wireKey(R.id.key_end)         {
            if (!handleCompactPunctuationKey(R.id.key_end)) {
                sendKeyCode(KeyEvent.KEYCODE_MOVE_END)
            }
        }

        val fnKeyIds = listOf(
            R.id.key_f1  to KeyEvent.KEYCODE_F1,
            R.id.key_f2  to KeyEvent.KEYCODE_F2,
            R.id.key_f3  to KeyEvent.KEYCODE_F3,
            R.id.key_f4  to KeyEvent.KEYCODE_F4,
            R.id.key_f5  to KeyEvent.KEYCODE_F5,
            R.id.key_f6  to KeyEvent.KEYCODE_F6,
            R.id.key_f7  to KeyEvent.KEYCODE_F7,
            R.id.key_f8  to KeyEvent.KEYCODE_F8,
            R.id.key_f9  to KeyEvent.KEYCODE_F9,
            R.id.key_f10 to KeyEvent.KEYCODE_F10,
            R.id.key_f11 to KeyEvent.KEYCODE_F11,
            R.id.key_f12 to KeyEvent.KEYCODE_F12,
        )
        fnKeyIds.forEach { (viewId, keyCode) ->
            wireKey(viewId) { sendKeyCode(keyCode) }
        }

        val charKeys = mapOf(
            R.id.key_grave  to Pair('`', '~'),
            R.id.key_1      to Pair('1', '!'),
            R.id.key_2      to Pair('2', '@'),
            R.id.key_3      to Pair('3', '#'),
            R.id.key_4      to Pair('4', '$'),
            R.id.key_5      to Pair('5', '%'),
            R.id.key_6      to Pair('6', '^'),
            R.id.key_7      to Pair('7', '&'),
            R.id.key_8      to Pair('8', '*'),
            R.id.key_9      to Pair('9', '('),
            R.id.key_0      to Pair('0', ')'),
            R.id.key_minus  to Pair('-', '_'),
            R.id.key_equals to Pair('=', '+'),
            R.id.key_lbracket to Pair('[', '{'),
            R.id.key_q      to Pair('q', 'Q'),
            R.id.key_w      to Pair('w', 'W'),
            R.id.key_e      to Pair('e', 'E'),
            R.id.key_r      to Pair('r', 'R'),
            R.id.key_t      to Pair('t', 'T'),
            R.id.key_y      to Pair('y', 'Y'),
            R.id.key_u      to Pair('u', 'U'),
            R.id.key_i      to Pair('i', 'I'),
            R.id.key_o      to Pair('o', 'O'),
            R.id.key_p      to Pair('p', 'P'),
            R.id.key_rbracket to Pair(']', '}'),
            R.id.key_backslash to Pair('\\', '|'),
            R.id.key_semicolon to Pair(';', ':'),
            R.id.key_a      to Pair('a', 'A'),
            R.id.key_s      to Pair('s', 'S'),
            R.id.key_d      to Pair('d', 'D'),
            R.id.key_f_key  to Pair('f', 'F'),
            R.id.key_g      to Pair('g', 'G'),
            R.id.key_h      to Pair('h', 'H'),
            R.id.key_j      to Pair('j', 'J'),
            R.id.key_k      to Pair('k', 'K'),
            R.id.key_l      to Pair('l', 'L'),
            R.id.key_quote  to Pair('\'', '"'),
            R.id.key_comma to Pair(',', '<'),
            R.id.key_z      to Pair('z', 'Z'),
            R.id.key_x      to Pair('x', 'X'),
            R.id.key_c      to Pair('c', 'C'),
            R.id.key_v      to Pair('v', 'V'),
            R.id.key_b      to Pair('b', 'B'),
            R.id.key_n      to Pair('n', 'N'),
            R.id.key_m      to Pair('m', 'M'),
            R.id.key_period to Pair('.', '>'),
            R.id.key_slash  to Pair('/', '?'),
        )

        rootView.post {
            initCharKeyBounds(charKeys)
        }

        charKeys.forEach { (viewId, chars) ->
            val view = rootView.findViewById<View>(viewId) ?: return@forEach
            if (prefs.getBoolean("long_press_extra", true)) {
                view.setOnLongClickListener {
                    if (handleSymbolModeCharacterKey(viewId)) {
                        feedbackVibrate(20)
                        feedbackSound()
                        return@setOnLongClickListener true
                    }
                    if (handleCompactPunctuationKey(viewId)) {
                        feedbackVibrate(20)
                        feedbackSound()
                        return@setOnLongClickListener true
                    }
                    feedbackVibrate(30)
                    handleCharacterInput(chars.first, chars.second, useAlternate = true)
                    true
                }
            }

            var startX = 0f
            var startY = 0f
            var hasMoved = false
            view.setOnTouchListener { _, event ->
                try {
                    if (layoutMode == KeyboardLayoutMode.COMPACT_SYMBOL) {
                        return@setOnTouchListener false
                    }
                    val rawX = event.rawX
                    val rawY = event.rawY

                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            view.isPressed = true
                            startX = event.x
                            startY = event.y
                            hasMoved = false
                            if (prefs.getBoolean("swipe_typing_enabled", true)) {
                                isSwipeTypingActive = true
                                swipeTraceChars.clear()
                                swipeTraceChars.append(chars.first)
                                if (cachedCharKeyBounds.isEmpty()) {
                                    initCharKeyBounds(charKeys)
                                }
                                val loc = intArrayOf(0, 0)
                                swipeTrailView.getLocationOnScreen(loc)
                                swipeTrailView.startPath(rawX - loc[0], rawY - loc[1])
                            }
                            true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            val dx = (event.x - startX).toDouble()
                            val dy = (event.y - startY).toDouble()
                            if (hypot(dx, dy) > 20f) {
                                hasMoved = true
                            }
                            if (isSwipeTypingActive && prefs.getBoolean("swipe_typing_enabled", true)) {
                                val touchedChar = getCharUnderPointer(rawX, rawY)
                                if (touchedChar != null) {
                                    if (swipeTraceChars.isEmpty() || swipeTraceChars.last() != touchedChar) {
                                        swipeTraceChars.append(touchedChar)
                                    }
                                }
                                val loc = intArrayOf(0, 0)
                                swipeTrailView.getLocationOnScreen(loc)
                                swipeTrailView.addPoint(rawX - loc[0], rawY - loc[1])
                            }
                            true
                        }
                        MotionEvent.ACTION_UP -> {
                            view.isPressed = false
                            view.performClick()
                            swipeTrailView.clearPath()
                            if (hasMoved && prefs.getBoolean("swipe_typing_enabled", true) && swipeTraceChars.length >= 2) {
                                isSwipeTypingActive = false
                                val traced = swipeTraceChars.toString()
                                val word = matchSwipeWord(traced)
                                swipeTraceChars.clear()
                                if (!word.isNullOrBlank()) {
                                    feedbackVibrate(24)
                                    feedbackSound()
                                    val ic = currentInputConnection
                                    if (ic != null && currentInputLang == InputLanguage.ES && currentSpanishWord.isNotEmpty()) {
                                        ic.deleteSurroundingText(currentSpanishWord.length, 0)
                                        currentSpanishWord = ""
                                    }
                                    ic?.commitText("$word ", 1)
                                    refreshCandidateArea()
                                    return@setOnTouchListener true
                                }
                            }
                            isSwipeTypingActive = false
                            swipeTraceChars.clear()

                            if (hasMoved) {
                                val dy = startY - event.y
                                if (dy > 25 && prefs.getBoolean("swipe_for_symbols", true)) {
                                    feedbackVibrate(20)
                                    handleCharacterInput(chars.first, chars.second, useAlternate = true)
                                    return@setOnTouchListener true
                                }
                                return@setOnTouchListener true
                            }

                            if (handleSymbolModeCharacterKey(viewId)) {
                                feedbackVibrate(16)
                                feedbackSound()
                                if (shiftActive) {
                                    shiftActive = false
                                    updateModifierUI()
                                }
                                return@setOnTouchListener true
                            }
                            if (handleCompactPunctuationKey(viewId)) {
                                feedbackVibrate(16)
                                feedbackSound()
                                if (shiftActive) {
                                    shiftActive = false
                                    updateModifierUI()
                                }
                                return@setOnTouchListener true
                            }
                            feedbackVibrate()
                            feedbackSound()
                            handleCharacterInput(chars.first, chars.second, useAlternate = shiftActive)
                            if (shiftActive) {
                                shiftActive = false
                                updateModifierUI()
                            }
                            true
                        }
                        MotionEvent.ACTION_CANCEL -> {
                            view.isPressed = false
                            isSwipeTypingActive = false
                            swipeTraceChars.clear()
                            swipeTrailView.clearPath()
                            true
                        }
                        else -> false
                    }
                } catch (_: Exception) {
                    view.isPressed = false
                    false
                }
            }
        }
    }

    // ── Key sending logic ─────────────────────────────────────────────────────

    private fun sendChar(ch: Char) {
        val ic = currentInputConnection ?: return

        when {
            ctrlActive -> {
                val controlChar = when {
                    ch.lowercaseChar() in 'a'..'z' -> (ch.lowercaseChar() - 'a' + 1).toChar()
                    ch == '[' -> 0x1B.toChar()
                    ch == '\\' -> 0x1C.toChar()
                    ch == ']' -> 0x1D.toChar()
                    ch == '^' -> 0x1E.toChar()
                    ch == '_' -> 0x1F.toChar()
                    else -> ch
                }
                ic.commitText(controlChar.toString(), 1)
                ctrlActive = false
                updateModifierUI()
            }
            altActive -> {
                ic.commitText("\u001b${ch}", 1)
                altActive = false
                updateModifierUI()
            }
            else -> {
                ic.commitText(ch.toString(), 1)
            }
        }
    }

    private fun sendEscape() {
        feedbackVibrate()
        feedbackSound()
        currentInputConnection?.commitText("\u001b", 1)
        resetModifiers()
    }

    private fun sendTab() {
        feedbackVibrate()
        feedbackSound()
        val ic = currentInputConnection ?: return
        if (ctrlActive) {
            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_TAB))
            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_TAB))
            ctrlActive = false
            updateModifierUI()
        } else {
            ic.commitText("\t", 1)
        }
    }

    private fun commitWordUsageAndReset() {
        if (isPasswordField) return
        when (currentInputLang) {
            InputLanguage.ES -> if (currentSpanishWord.length >= 2) {
                userDictionaryStore.recordWordUsage(currentSpanishWord)
            }
            InputLanguage.EN -> if (currentEnglishWord.length >= 2) {
                userDictionaryStore.recordWordUsage(currentEnglishWord)
            }
        }
        currentSpanishWord = ""
        currentEnglishWord = ""
        refreshCandidateArea()
    }

    private fun sendEnter() {
        feedbackVibrate()
        feedbackSound()
        val ic = currentInputConnection ?: return
        if (ctrlActive) {
            ic.commitText("\r", 1)
            ctrlActive = false
            updateModifierUI()
        } else {
            commitWordUsageAndReset()
            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
        }
    }

    private fun performBackspace() {
        if (currentInputLang == InputLanguage.ES && currentSpanishWord.isNotEmpty()) {
            currentSpanishWord = currentSpanishWord.dropLast(1)
            refreshCandidateArea()
        } else if (currentInputLang == InputLanguage.EN && currentEnglishWord.isNotEmpty()) {
            currentEnglishWord = currentEnglishWord.dropLast(1)
            refreshCandidateArea()
        }
        val ic = currentInputConnection ?: return
        val selectedText = ic.getSelectedText(0)
        if (!selectedText.isNullOrEmpty()) {
            ic.commitText("", 1)
        } else {
            ic.deleteSurroundingText(1, 0)
        }
    }

    private fun performForwardDelete() {
        val ic = currentInputConnection ?: return
        val selectedText = ic.getSelectedText(0)
        when {
            !selectedText.isNullOrEmpty() -> ic.commitText("", 1)
            !ic.getTextAfterCursor(1, 0).isNullOrEmpty() -> ic.deleteSurroundingText(0, 1)
            else -> sendKeyCodeInternal(KeyEvent.KEYCODE_FORWARD_DEL, playFeedback = false)
        }
    }

    private fun sendSpace() {
        feedbackVibrate()
        feedbackSound()
        val ic = currentInputConnection ?: return
        if (ctrlActive) {
            ic.commitText("\u0000", 1)
            ctrlActive = false
            updateModifierUI()
        } else {
            commitWordUsageAndReset()
            ic.commitText(" ", 1)
        }
    }

    private fun sendKeyCode(keyCode: Int) {
        sendKeyCodeInternal(keyCode, playFeedback = true)
    }

    private fun sendKeyCodeInternal(keyCode: Int, playFeedback: Boolean) {
        if (playFeedback) {
            feedbackVibrate()
            feedbackSound()
        }
        val ic = currentInputConnection ?: return
        val meta = (if (ctrlActive) KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON else 0) or
                (if (altActive) KeyEvent.META_ALT_ON or KeyEvent.META_ALT_LEFT_ON else 0) or
                (if (shiftActive) KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON else 0)
        if (ctrlActive) ctrlActive = false
        if (altActive) altActive = false
        if (shiftActive) shiftActive = false

        val eventTime = System.currentTimeMillis()
        ic.sendKeyEvent(KeyEvent(eventTime, eventTime, KeyEvent.ACTION_DOWN, keyCode, 0, meta))
        ic.sendKeyEvent(KeyEvent(eventTime, eventTime, KeyEvent.ACTION_UP, keyCode, 0, meta))
        updateModifierUI()
    }

    private fun levenshteinDistance(lhs: CharSequence, rhs: CharSequence): Int {
        val lhsLength = lhs.length
        val rhsLength = rhs.length
        var cost = IntArray(lhsLength + 1) { it }
        var newCost = IntArray(lhsLength + 1)

        for (i in 1..rhsLength) {
            newCost[0] = i
            for (j in 1..lhsLength) {
                val match = if (lhs[j - 1] == rhs[i - 1]) 0 else 1
                newCost[j] = minOf(
                    cost[j] + 1,
                    newCost[j - 1] + 1,
                    cost[j - 1] + match,
                )
            }
            val swap = cost
            cost = newCost
            newCost = swap
        }
        return cost[lhsLength]
    }

    // ── Modifier UI update ────────────────────────────────────────────────────

    private fun shouldAutoCapitalize(): Boolean {
        if (isPasswordField) return false
        if (shiftActive || capsLockActive) return true
        val ic = currentInputConnection ?: return false
        val textBefore = ic.getTextBeforeCursor(4, 0)?.toString() ?: return true
        if (textBefore.isEmpty()) return true
        val trimmed = textBefore.trimEnd()
        if (trimmed.isEmpty()) return true
        val lastChar = trimmed.last()
        return lastChar == '.' || lastChar == '?' || lastChar == '!' || lastChar == '\n'
    }

    private fun updateModifierUI() {
        rootView.findViewById<View>(R.id.key_ctrl)?.isActivated = ctrlActive
        rootView.findViewById<View>(R.id.key_alt)?.isActivated = altActive
        rootView.findViewById<View>(R.id.key_shift)?.isActivated = shiftActive || capsLockActive

        val shiftView = rootView.findViewById<TextView>(R.id.key_shift)
        shiftView?.alpha = if (shiftActive || capsLockActive) 1.0f else 0.7f
        shiftView?.text = if (capsLockActive) "⇪" else "⇧"
        updateKeyboardLayoutUi()
    }

    private fun resetModifiers() {
        ctrlActive = false
        altActive = false
        if (!capsLockActive) {
            shiftActive = false
        }
        updateModifierUI()
    }

    private fun wireKey(viewId: Int, action: () -> Unit) {
        rootView.findViewById<View>(viewId)?.setOnClickListener {
            feedbackVibrate()
            feedbackSound()
            action()
        }
    }

    private fun wireModifierKey(viewId: Int, action: () -> Unit) {
        rootView.findViewById<View>(viewId)?.setOnClickListener {
            feedbackVibrate(15)
            action()
        }
    }

    private fun wireDeleteKey(viewId: Int, repeatAction: () -> Unit, clearAction: () -> Unit) {
        val view = rootView.findViewById<View>(viewId) ?: return
        val swipeThresholdPx = 30f * resources.displayMetrics.density
        var startY = 0f
        var repeatStarted = false
        var swipeArmed = false
        var swipeCanceled = false
        var repeatRunnable: Runnable? = null
        var compactPunctuationMode = false

        view.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    compactPunctuationMode = handleCompactPunctuationKey(viewId)
                    if (compactPunctuationMode) {
                        feedbackVibrate(16)
                        feedbackSound()
                        return@setOnTouchListener true
                    }
                    feedbackVibrate()
                    feedbackSound()
                    startY = event.y
                    repeatStarted = false
                    swipeArmed = false
                    swipeCanceled = false
                    repeatRunnable = object : Runnable {
                        override fun run() {
                            repeatStarted = true
                            feedbackVibrate(8)
                            repeatAction()
                            repeatHandler.postDelayed(this, DELETE_REPEAT_INTERVAL_MS)
                        }
                    }
                    repeatHandler.postDelayed(repeatRunnable!!, DELETE_REPEAT_INITIAL_DELAY_MS)
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    if (compactPunctuationMode) {
                        return@setOnTouchListener true
                    }
                    val shouldArmClear = startY - event.y > swipeThresholdPx
                    if (shouldArmClear && !swipeArmed) {
                        swipeArmed = true
                        swipeCanceled = false
                        repeatRunnable?.let(repeatHandler::removeCallbacks)
                        showToast(R.string.delete_clear_release_hint)
                    } else if (!shouldArmClear && swipeArmed) {
                        swipeArmed = false
                        swipeCanceled = true
                    }
                    true
                }

                MotionEvent.ACTION_UP -> {
                    view.performClick()
                    if (compactPunctuationMode) {
                        compactPunctuationMode = false
                        return@setOnTouchListener true
                    }
                    repeatRunnable?.let(repeatHandler::removeCallbacks)
                    if (swipeArmed) {
                        clearAction()
                    } else if (!swipeCanceled && !repeatStarted) {
                        repeatAction()
                    }
                    repeatRunnable = null
                    true
                }

                MotionEvent.ACTION_CANCEL -> {
                    compactPunctuationMode = false
                    swipeArmed = false
                    swipeCanceled = false
                    repeatRunnable?.let(repeatHandler::removeCallbacks)
                    repeatRunnable = null
                    true
                }

                else -> false
            }
        }
    }

    private fun wireCompactBackspaceSwapKey(viewId: Int) {
        val view = rootView.findViewById<View>(viewId) ?: return
        val swipeThresholdPx = 30f * resources.displayMetrics.density
        var startY = 0f
        var repeatStarted = false
        var swipeArmed = false
        var swipeCanceled = false
        var repeatRunnable: Runnable? = null

        view.setOnTouchListener { _, event ->
            if (layoutMode == KeyboardLayoutMode.FULL) {
                return@setOnTouchListener false
            }
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    feedbackVibrate()
                    feedbackSound()
                    startY = event.y
                    repeatStarted = false
                    swipeArmed = false
                    swipeCanceled = false
                    repeatRunnable = object : Runnable {
                        override fun run() {
                            repeatStarted = true
                            feedbackVibrate(8)
                            performBackspace()
                            repeatHandler.postDelayed(this, DELETE_REPEAT_INTERVAL_MS)
                        }
                    }
                    repeatHandler.postDelayed(repeatRunnable!!, DELETE_REPEAT_INITIAL_DELAY_MS)
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val shouldArmClear = startY - event.y > swipeThresholdPx
                    if (shouldArmClear && !swipeArmed) {
                        swipeArmed = true
                        swipeCanceled = false
                        repeatRunnable?.let(repeatHandler::removeCallbacks)
                        showToast(R.string.delete_clear_release_hint)
                    } else if (!shouldArmClear && swipeArmed) {
                        swipeArmed = false
                        swipeCanceled = true
                    }
                    true
                }

                MotionEvent.ACTION_UP -> {
                    view.performClick()
                    repeatRunnable?.let(repeatHandler::removeCallbacks)
                    if (swipeArmed) {
                        clearAllBeforeCursor()
                    } else if (!swipeCanceled && !repeatStarted) {
                        performBackspace()
                    }
                    repeatRunnable = null
                    true
                }

                MotionEvent.ACTION_CANCEL -> {
                    swipeArmed = false
                    swipeCanceled = false
                    repeatRunnable?.let(repeatHandler::removeCallbacks)
                    repeatRunnable = null
                    true
                }

                else -> false
            }
        }

        view.setOnClickListener {
            if (layoutMode == KeyboardLayoutMode.FULL) {
                sendKeyCode(KeyEvent.KEYCODE_DPAD_UP)
            }
        }
    }

    private fun stopDeleteRepeats() {
        repeatHandler.removeCallbacksAndMessages(null)
    }

    private fun handleCompactPunctuationKey(viewId: Int): Boolean {
        val output = compactPunctuationOutput(viewId) ?: return false
        sendLiteralText(output)
        return true
    }

    private fun handleCompactBottomEnterKey(): Boolean {
        if (layoutMode == KeyboardLayoutMode.FULL) return false
        performEnterWithoutFeedback()
        return true
    }

    private fun performEnterWithoutFeedback() {
        val ic = currentInputConnection ?: return
        if (ctrlActive) {
            ic.commitText("\r", 1)
            ctrlActive = false
            updateModifierUI()
        } else {
            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
        }
    }

    private fun compactPunctuationOutput(viewId: Int): String? {
        if (layoutMode != KeyboardLayoutMode.COMPACT_EN && layoutMode != KeyboardLayoutMode.COMPACT_ES) return null
        if (currentInputLang == InputLanguage.ES && viewId == R.id.key_quote) return null
        return when (viewId) {
            R.id.key_esc -> "!"
            R.id.key_backspace -> "?"
            R.id.key_end -> ","
            R.id.key_delete -> "."
            else -> null
        }
    }

    private fun sendLiteralText(text: String) {
        currentInputConnection?.commitText(text, 1)
    }

    private fun clearAllBeforeCursor() {
        val ic = currentInputConnection ?: return
        ic.beginBatchEdit()
        try {
            val selectedText = ic.getSelectedText(0)
            if (!selectedText.isNullOrEmpty()) {
                ic.commitText("", 1)
            }
            while (true) {
                val before = ic.getTextBeforeCursor(CLEAR_TEXT_CHUNK_SIZE, 0)
                if (before.isNullOrEmpty()) break
                ic.deleteSurroundingText(before.length, 0)
                if (before.length < CLEAR_TEXT_CHUNK_SIZE) break
            }
        } finally {
            ic.endBatchEdit()
        }
    }

    private fun clearAllAfterCursor() {
        val ic = currentInputConnection ?: return
        ic.beginBatchEdit()
        try {
            val selectedText = ic.getSelectedText(0)
            if (!selectedText.isNullOrEmpty()) {
                ic.commitText("", 1)
            }
            while (true) {
                val after = ic.getTextAfterCursor(CLEAR_TEXT_CHUNK_SIZE, 0)
                if (after.isNullOrEmpty()) break
                ic.deleteSurroundingText(0, after.length)
                if (after.length < CLEAR_TEXT_CHUNK_SIZE) break
            }
        } finally {
            ic.endBatchEdit()
        }
    }

    private fun handleCharacterInput(primary: Char, alternate: Char, useAlternate: Boolean) {
        if (layoutMode == KeyboardLayoutMode.COMPACT_SYMBOL) {
            return
        }
        var effectivePrimary = primary
        var effectiveAlternate = alternate
        if (currentInputLang == InputLanguage.ES && primary == '\'' && alternate == '"') {
            effectivePrimary = 'ñ'
            effectiveAlternate = 'Ñ'
        }
        val isLetter = effectivePrimary in 'a'..'z' || effectivePrimary in 'A'..'Z' || effectivePrimary in 'á'..'ú' || effectivePrimary in 'Á'..'Ú' || effectivePrimary == 'ñ' || effectivePrimary == 'Ñ'
        
        val autoCap = shouldAutoCapitalize() && !shiftActive && !capsLockActive
        val effectiveChar = if (useAlternate || autoCap || shiftActive || capsLockActive) effectiveAlternate else effectivePrimary

        if (!isPasswordField && !ctrlActive && !altActive) {
            if (currentInputLang == InputLanguage.ES && isLetter) {
                currentSpanishWord += effectiveChar
                refreshCandidateArea()
                sendChar(effectiveChar)
                if (shiftActive && !capsLockActive) {
                    shiftActive = false
                    updateModifierUI()
                }
                return
            } else if (currentInputLang == InputLanguage.EN && isLetter) {
                currentEnglishWord += effectiveChar
                refreshCandidateArea()
                sendChar(effectiveChar)
                if (shiftActive && !capsLockActive) {
                    shiftActive = false
                    updateModifierUI()
                }
                return
            }
        }

        sendChar(effectiveChar)
        if (shiftActive && !capsLockActive) {
            shiftActive = false
            updateModifierUI()
        }
    }

    private fun toggleLanguageMode() {
        currentInputLang = when (currentInputLang) {
            InputLanguage.EN -> InputLanguage.ES
            InputLanguage.ES -> InputLanguage.EN
        }
        layoutMode = defaultLayoutModeForLanguage()
        currentSpanishWord = ""
        updateKeyboardLayoutUi()
    }

    private fun handleSymbolModeToggleKey(): Boolean {
        if (layoutMode == KeyboardLayoutMode.FULL) return false
        toggleSymbolMode()
        return true
    }

    private fun toggleSymbolMode() {
        if (layoutMode == KeyboardLayoutMode.COMPACT_SYMBOL) {
            layoutMode = previousCompactLayoutMode.takeIf {
                it == KeyboardLayoutMode.COMPACT_EN || it == KeyboardLayoutMode.COMPACT_ES
            } ?: defaultLayoutModeForLanguage()
            updateKeyboardLayoutUi()
            return
        }

        previousCompactLayoutMode = defaultLayoutModeForLanguage()
        layoutMode = KeyboardLayoutMode.COMPACT_SYMBOL
        updateKeyboardLayoutUi()
    }

    private fun handleSymbolModeCharacterKey(viewId: Int): Boolean {
        if (layoutMode != KeyboardLayoutMode.COMPACT_SYMBOL) return false
        val output = SYMBOL_KEY_LABELS[viewId] ?: return false
        commitSymbolAndReturn(output)
        return true
    }

    private fun commitSymbolAndReturn(symbol: String) {
        currentInputConnection?.commitText(symbol, 1)
        layoutMode = previousCompactLayoutMode.takeIf {
            it == KeyboardLayoutMode.COMPACT_EN || it == KeyboardLayoutMode.COMPACT_ES
        } ?: defaultLayoutModeForLanguage()
        updateKeyboardLayoutUi()
    }

    private fun toggleFullLayoutMode() {
        layoutMode = if (layoutMode == KeyboardLayoutMode.FULL) {
            defaultLayoutModeForLanguage()
        } else {
            KeyboardLayoutMode.FULL
        }
        updateKeyboardLayoutUi()
    }

    private fun defaultLayoutModeForLanguage(): KeyboardLayoutMode {
        return when (currentInputLang) {
            InputLanguage.EN -> KeyboardLayoutMode.COMPACT_EN
            InputLanguage.ES -> KeyboardLayoutMode.COMPACT_ES
        }
    }

    private fun rebuildLlmToolBar() {
        if (!::candidateContainer.isInitialized) return
        candidateContainer.removeAllViews()
        LlmTextTool.entries.forEach { tool ->
            val toolView = layoutInflater.inflate(R.layout.macro_button, candidateContainer, false) as TextView
            toolView.text = tool.title
            toolView.setTextColor(ContextCompat.getColor(this, R.color.terminal_cyan))
            toolView.setOnClickListener {
                feedbackVibrate(18)
                feedbackSound()
                startLlmTool(tool)
            }
            candidateContainer.addView(toolView)
        }
        candidateScrollView.visibility = View.VISIBLE
    }



    private fun rebuildEmptyCandidateBar() {
        if (!::candidateContainer.isInitialized) return
        candidateContainer.removeAllViews()
        val hintView = layoutInflater.inflate(R.layout.macro_button, candidateContainer, false) as TextView
        hintView.text = when (currentInputLang) {
            InputLanguage.ES -> "Escribe en español..."
            InputLanguage.EN -> "TermKey Keyboard"
        }
        hintView.setTextColor(ContextCompat.getColor(this, R.color.terminal_text_dim))
        hintView.isEnabled = false
        candidateContainer.addView(hintView)
        candidateScrollView.visibility = View.VISIBLE
    }

    private fun refreshCandidateArea() {
        if (!::candidateScrollView.isInitialized) return
        if (isPasswordField) {
            candidateScrollView.visibility = View.GONE
            return
        }
        candidateScrollView.visibility = View.VISIBLE
        when {
            currentInputLang == InputLanguage.ES && currentSpanishWord.isNotEmpty() && layoutMode != KeyboardLayoutMode.COMPACT_SYMBOL -> {
                val userWords = userDictionaryStore.lookupCandidates(currentSpanishWord)
                val lexiconWords = spanishLexiconStore.lookupCandidates(currentSpanishWord).map { it.text }
                val combined = (userWords + lexiconWords).distinct().take(10)
                if (combined.isNotEmpty()) {
                    rebuildSpanishCandidateBar(combined.map { SpanishCandidate(it) })
                } else {
                    rebuildEmptyCandidateBar()
                }
            }
            currentInputLang == InputLanguage.EN && currentEnglishWord.isNotEmpty() && layoutMode != KeyboardLayoutMode.COMPACT_SYMBOL -> {
                val userWords = userDictionaryStore.lookupCandidates(currentEnglishWord)
                val lexiconWords = englishLexiconStore.lookupCandidates(currentEnglishWord).map { it.text }
                val combined = (userWords + lexiconWords).distinct().take(10)
                if (combined.isNotEmpty()) {
                    rebuildEnglishCandidateBar(combined)
                } else {
                    rebuildEmptyCandidateBar()
                }
            }
            shouldShowLlmToolBar() -> {
                rebuildLlmToolBar()
            }
            else -> {
                rebuildEmptyCandidateBar()
            }
        }
    }

    @Suppress("SetTextI18n")
    private fun rebuildSpanishCandidateBar(candidates: List<SpanishCandidate>) {
        if (!isViewInitialized()) return
        try {
            candidateContainer.removeAllViews()
            candidates.forEachIndexed { index, candidate ->
                val itemView = layoutInflater.inflate(R.layout.candidate_item, candidateContainer, false)
                val indexView = itemView.findViewById<TextView>(R.id.candidate_index)
                val textView = itemView.findViewById<TextView>(R.id.candidate_text)
                val pinyinView = itemView.findViewById<TextView>(R.id.candidate_pinyin)

                indexView.text = "${index + 1}."
                textView.text = candidate.text
                pinyinView.visibility = View.GONE

                if (index == 0) {
                    textView.setTextColor(ContextCompat.getColor(this, R.color.terminal_green))
                } else {
                    textView.setTextColor(ContextCompat.getColor(this, R.color.terminal_text))
                }

                itemView.setOnClickListener {
                    feedbackVibrate(14)
                    feedbackSound()
                    val ic = currentInputConnection
                    if (ic != null && currentSpanishWord.isNotEmpty()) {
                        ic.deleteSurroundingText(currentSpanishWord.length, 0)
                        ic.commitText("${candidate.text} ", 1)
                    } else {
                        ic?.commitText("${candidate.text} ", 1)
                    }
                    currentSpanishWord = ""
                    refreshCandidateArea()
                }
                candidateContainer.addView(itemView)
            }
            candidateScrollView.visibility = if (candidates.isNotEmpty()) View.VISIBLE else View.GONE
        } catch (e: Exception) {
            Log.e(TAG, "Error rebuilding Spanish candidate bar", e)
        }
    }

    @Suppress("SetTextI18n")
    private fun rebuildEnglishCandidateBar(candidates: List<String>) {
        if (!isViewInitialized()) return
        try {
            candidateContainer.removeAllViews()
            candidates.forEachIndexed { index, candidate ->
                val itemView = layoutInflater.inflate(R.layout.candidate_item, candidateContainer, false)
                val indexView = itemView.findViewById<TextView>(R.id.candidate_index)
                val textView = itemView.findViewById<TextView>(R.id.candidate_text)
                val pinyinView = itemView.findViewById<TextView>(R.id.candidate_pinyin)

                indexView.text = "${index + 1}."
                textView.text = candidate
                pinyinView.visibility = View.GONE

                if (index == 0) {
                    textView.setTextColor(ContextCompat.getColor(this, R.color.terminal_green))
                } else {
                    textView.setTextColor(ContextCompat.getColor(this, R.color.terminal_text))
                }

                itemView.setOnClickListener {
                    feedbackVibrate(14)
                    feedbackSound()
                    val ic = currentInputConnection
                    if (ic != null && currentEnglishWord.isNotEmpty()) {
                        ic.deleteSurroundingText(currentEnglishWord.length, 0)
                        ic.commitText("$candidate ", 1)
                    } else {
                        ic?.commitText("$candidate ", 1)
                    }
                    currentEnglishWord = ""
                    refreshCandidateArea()
                }
                candidateContainer.addView(itemView)
            }
            candidateScrollView.visibility = if (candidates.isNotEmpty()) View.VISIBLE else View.GONE
        } catch (e: Exception) {
            Log.e(TAG, "Error rebuilding English candidate bar", e)
        }
    }

    private fun shouldShowLlmToolBar(): Boolean {
        if (!prefs.getBoolean("llm_enabled", false)) return false
        if (buildLlmConfig() == null) return false
        if (layoutMode == KeyboardLayoutMode.COMPACT_SYMBOL) return false
        return currentLlmSource() != null
    }

    private fun updateKeyboardLayoutUi() {
        if (!::languageKey.isInitialized || !::candidateScrollView.isInitialized) return
        val showMacro = prefs.getBoolean("show_macro_bar", true)
        val showFn = prefs.getBoolean("show_fn_row", true)
        val compactInvisibleKeys = if (currentInputLang == InputLanguage.ES) {
            setOf(R.id.key_ctrl)
        } else {
            setOf(
                R.id.key_ctrl,
                R.id.key_quote,
            )
        }
        val visibleCompactKeys = when (layoutMode) {
            KeyboardLayoutMode.COMPACT_EN,
            KeyboardLayoutMode.ONE_HANDED_LEFT,
            KeyboardLayoutMode.ONE_HANDED_RIGHT -> COMPACT_EN_KEY_IDS
            KeyboardLayoutMode.COMPACT_ES -> COMPACT_ES_KEY_IDS
            KeyboardLayoutMode.COMPACT_SYMBOL -> COMPACT_SYMBOL_KEY_IDS
            KeyboardLayoutMode.COMPACT_NUMERIC -> COMPACT_NUMERIC_KEY_IDS
            KeyboardLayoutMode.FULL -> emptySet()
        }

        languageKey.isActivated = (currentInputLang != InputLanguage.EN)
        languageKey.text = when (currentInputLang) {
            InputLanguage.EN -> getString(R.string.key_lang_en)
            InputLanguage.ES -> getString(R.string.key_lang_es)
        }
        applyKeyboardRowHeights()
        applyDynamicKeyLabels()
        applyDynamicKeyTextSizes()
        applyDynamicKeyWeights()
        when (layoutMode) {
            KeyboardLayoutMode.FULL -> {
                ALL_KEY_IDS.forEach { keyId ->
                    rootView.findViewById<View>(keyId)?.visibility = View.VISIBLE
                }
                rootView.findViewById<View>(R.id.key_lbracket)?.visibility = View.GONE
                rootView.findViewById<View>(R.id.key_semicolon)?.visibility = View.GONE
                rootView.findViewById<View>(R.id.key_comma)?.visibility = View.GONE
                macroScrollView.visibility = if (showMacro) View.VISIBLE else View.GONE
                macroSeparator.visibility = if (showMacro) View.VISIBLE else View.GONE
                fnRow.visibility = if (showFn) View.VISIBLE else View.GONE
            }

            KeyboardLayoutMode.COMPACT_EN,
            KeyboardLayoutMode.COMPACT_ES,
            KeyboardLayoutMode.COMPACT_SYMBOL,
            KeyboardLayoutMode.COMPACT_NUMERIC,
            KeyboardLayoutMode.ONE_HANDED_LEFT,
            KeyboardLayoutMode.ONE_HANDED_RIGHT -> {
                ALL_KEY_IDS.forEach { keyId ->
                    rootView.findViewById<View>(keyId)?.visibility =
                        when {
                            visibleCompactKeys.contains(keyId) -> View.VISIBLE
                            compactInvisibleKeys.contains(keyId) -> View.INVISIBLE
                            else -> View.GONE
                        }
                }
                macroScrollView.visibility = View.GONE
                macroSeparator.visibility = View.GONE
                fnRow.visibility = View.GONE
            }
        }

        rootView.findViewById<View>(R.id.key_row_1)?.visibility = View.VISIBLE

        if (::numericPad.isInitialized) {
            val isNumeric = (layoutMode == KeyboardLayoutMode.COMPACT_NUMERIC)
            numericPad.visibility = if (isNumeric) View.VISIBLE else View.GONE
            if (isNumeric) {
                keyRows.forEach { it.visibility = View.GONE }
                fnRow.visibility = View.GONE
                macroScrollView.visibility = View.GONE
                macroSeparator.visibility = View.GONE
            } else {
                keyRows.forEach { it.visibility = View.VISIBLE }
            }
        }

        if (::numbersKey.isInitialized) {
            numbersKey.text = if (layoutMode == KeyboardLayoutMode.COMPACT_NUMERIC) "ABC" else "123"
        }

        val spaceKey = rootView.findViewById<TextView>(R.id.key_space)
        spaceKey?.text = when (currentInputLang) {
            InputLanguage.EN -> "EN"
            InputLanguage.ES -> "ES"
        }

        when (layoutMode) {
            KeyboardLayoutMode.ONE_HANDED_LEFT -> {
                rootView.pivotX = 0f
                rootView.scaleX = 0.82f
                rootView.translationX = 0f
            }
            KeyboardLayoutMode.ONE_HANDED_RIGHT -> {
                rootView.pivotX = rootView.width.toFloat()
                rootView.scaleX = 0.82f
                rootView.translationX = (rootView.width * 0.18f)
            }
            else -> {
                rootView.scaleX = 1.0f
                rootView.translationX = 0f
            }
        }

        refreshCandidateArea()
        updateLlmPanelUi()
        scheduleAdaptiveTouchTargetsUpdate()
    }

    private fun scheduleAdaptiveTouchTargetsUpdate() {
        if (!::rootView.isInitialized) return
        rootView.removeCallbacks(adaptiveTouchRefreshRunnable)
        rootView.post(adaptiveTouchRefreshRunnable)
    }

    private fun rebuildAdaptiveTouchTargets() {
        val host = rootView as? ViewGroup ?: return
        if (!::adaptiveTouchDelegate.isInitialized) return

        val visibleRows = buildList {
            if (::fnRow.isInitialized && fnRow.isVisible) add(fnRow as ViewGroup)
            if (::keyRows.isInitialized) {
                keyRows.filterTo(this) { it.isVisible }
            }
            if (::numericPad.isInitialized && numericPad.isVisible) {
                for (index in 0 until (numericPad as ViewGroup).childCount) {
                    val row = (numericPad as ViewGroup).getChildAt(index)
                    if (row is ViewGroup && row.isVisible) add(row)
                }
            }
        }
        if (visibleRows.isEmpty()) {
            adaptiveTouchDelegate.updateTargets(emptyList())
            host.touchDelegate = adaptiveTouchDelegate
            return
        }

        val rowStates = visibleRows.mapNotNull { row ->
            val keyBounds = buildList {
                for (index in 0 until row.childCount) {
                    val child = row.getChildAt(index)
                    if (!child.isVisible || !child.isClickable || child.width <= 0 || child.height <= 0) continue
                    add(child to getBoundsRelativeToHost(host, child))
                }
            }.sortedBy { it.second.left }
            if (keyBounds.isEmpty()) {
                null
            } else {
                RowTouchState(
                    bounds = getBoundsRelativeToHost(host, row),
                    keys = keyBounds,
                )
            }
        }

        if (rowStates.isEmpty()) {
            adaptiveTouchDelegate.updateTargets(emptyList())
            host.touchDelegate = adaptiveTouchDelegate
            return
        }

        val extraSlopPx = maxOf(
            ViewConfiguration.get(this).scaledTouchSlop * 2,
            (48f * resources.displayMetrics.density).toInt(),
        )

        val targets = buildList {
            rowStates.forEachIndexed { rowIndex, rowState ->
                val bandTop = if (rowIndex == 0) {
                    rowState.bounds.top
                } else {
                    (rowStates[rowIndex - 1].bounds.bottom + rowState.bounds.top) / 2
                }
                val bandBottom = if (rowIndex == rowStates.lastIndex) {
                    rowState.bounds.bottom
                } else {
                    (rowState.bounds.bottom + rowStates[rowIndex + 1].bounds.top) / 2
                }

                rowState.keys.forEachIndexed { keyIndex, (view, actualBounds) ->
                    val left = if (keyIndex == 0) {
                        rowState.bounds.left
                    } else {
                        (rowState.keys[keyIndex - 1].second.centerX() + actualBounds.centerX()) / 2
                    }
                    val right = if (keyIndex == rowState.keys.lastIndex) {
                        rowState.bounds.right
                    } else {
                        (actualBounds.centerX() + rowState.keys[keyIndex + 1].second.centerX()) / 2
                    }
                    val delegateBounds = Rect(left, bandTop, right, bandBottom)
                    val slopBounds = Rect(delegateBounds).apply {
                        inset(-extraSlopPx, -extraSlopPx)
                    }
                    add(
                        AdaptiveTouchTarget(
                            view = view,
                            actualBounds = actualBounds,
                            delegateBounds = delegateBounds,
                            slopBounds = slopBounds,
                        ),
                    )
                }
            }
        }

        adaptiveTouchDelegate.updateTargets(targets)
        host.touchDelegate = if (targets.isEmpty()) null else adaptiveTouchDelegate
    }

    private fun getBoundsRelativeToHost(host: ViewGroup, view: View): Rect {
        return Rect(0, 0, view.width, view.height).also { rect ->
            host.offsetDescendantRectToMyCoords(view, rect)
        }
    }

    private fun applyDynamicKeyLabels() {
        val compactLabels = if (layoutMode == KeyboardLayoutMode.FULL) {
            emptyMap()
        } else if (layoutMode == KeyboardLayoutMode.COMPACT_SYMBOL) {
            SYMBOL_KEY_LABELS + mapOf(
                R.id.key_home to "ABC",
                R.id.key_arrow_right to "↩",
            )
        } else if (layoutMode == KeyboardLayoutMode.COMPACT_NUMERIC) {
            NUMERIC_KEY_LABELS + mapOf(
                R.id.key_numbers to "ABC",
                R.id.key_arrow_right to "↩",
            )
        } else if (currentInputLang == InputLanguage.ES) {
            mapOf(
                R.id.key_esc to "!",
                R.id.key_backspace to "?",
                R.id.key_home to "!#1",
                R.id.key_end to ",",
                R.id.key_delete to ".",
                R.id.key_space to "Español (ES)",
                R.id.key_arrow_up to "⌫",
                R.id.key_arrow_right to "↩",
                R.id.key_quote to if (shiftActive) "Ñ" else "ñ",
            )
        } else {
            mapOf(
                R.id.key_esc to "!",
                R.id.key_backspace to "?",
                R.id.key_home to "!#1",
                R.id.key_end to ",",
                R.id.key_delete to ".",
                R.id.key_space to "English (US)",
                R.id.key_arrow_up to "⌫",
                R.id.key_arrow_right to "↩",
            )
        }

        val defaultLabels = baseCharacterKeyLabels() + mapOf(
            R.id.key_tab to "TAB⇥",
            R.id.key_backslash to "\\",
            R.id.key_ctrl to "CTRL",
            R.id.key_alt to "ALT",
            R.id.key_quote to "'",
            R.id.key_esc to "ESC",
            R.id.key_backspace to "⌫",
            R.id.key_comma to ",",
            R.id.key_period to ".",
            R.id.key_home to "Home",
            R.id.key_end to "End",
            R.id.key_slash to "/",
            R.id.key_arrow_up to "↑",
            R.id.key_arrow_right to "→",
            R.id.key_delete to "Del",
        )

        defaultLabels.forEach { (viewId, defaultLabel) ->
            rootView.findViewById<TextView>(viewId)?.text = defaultLabel
        }
        compactLabels.forEach { (viewId, label) ->
            rootView.findViewById<TextView>(viewId)?.text = label
        }
    }

    private fun applyDynamicKeyWeights() {
        updateKeyWeight(R.id.key_enter, 2.0f)
        updateKeyWeight(R.id.key_arrow_right, if (layoutMode == KeyboardLayoutMode.FULL) 1.0f else 1.4f)
        updateKeyWeight(R.id.key_ctrl, if (layoutMode == KeyboardLayoutMode.FULL) 1.5f else 0.5f)
        updateKeyWeight(R.id.key_quote, 1.0f)
        updateKeyWeight(R.id.key_esc, if (layoutMode == KeyboardLayoutMode.FULL) 1.3f else 1.0f)
        updateKeyWeight(R.id.key_alt, if (layoutMode == KeyboardLayoutMode.FULL) 1.3f else 1.0f)
        updateKeyWeight(R.id.key_end, if (layoutMode == KeyboardLayoutMode.FULL) 1.1f else 1.0f)
        updateKeyWeight(R.id.key_delete, if (layoutMode == KeyboardLayoutMode.FULL) 1.1f else 1.0f)
        updateKeyWeight(R.id.key_space, if (layoutMode == KeyboardLayoutMode.FULL) 2.94f else 4.2f)
    }

    private fun applyDynamicKeyTextSizes() {
        val compactMode = layoutMode != KeyboardLayoutMode.FULL
        val textSizeMultiplier = when (prefs.getString("key_text_size", "normal")) {
            "small" -> 0.85f
            "large" -> 1.15f
            "xlarge" -> 1.30f
            else -> 1.0f
        }

        val baseSize = (if (compactMode) 15f else 13f) * textSizeMultiplier
        val wideSize = (if (compactMode) 18f else 16f) * textSizeMultiplier
        val utilitySize = (if (compactMode) 12.5f else 10f) * textSizeMultiplier
        val modifierSize = (if (compactMode) 13f else 11f) * textSizeMultiplier
        val enterSize = (if (compactMode) 13.5f else 11f) * textSizeMultiplier
        val arrowSize = (if (compactMode) 18f else 16f) * textSizeMultiplier

        val baseKeys = intArrayOf(
            R.id.key_grave,
            R.id.key_1,
            R.id.key_2,
            R.id.key_3,
            R.id.key_4,
            R.id.key_5,
            R.id.key_6,
            R.id.key_7,
            R.id.key_8,
            R.id.key_9,
            R.id.key_0,
            R.id.key_minus,
            R.id.key_equals,
            R.id.key_lbracket,
            R.id.key_q,
            R.id.key_w,
            R.id.key_e,
            R.id.key_r,
            R.id.key_t,
            R.id.key_y,
            R.id.key_u,
            R.id.key_i,
            R.id.key_o,
            R.id.key_p,
            R.id.key_rbracket,
            R.id.key_backslash,
            R.id.key_semicolon,
            R.id.key_a,
            R.id.key_s,
            R.id.key_d,
            R.id.key_f_key,
            R.id.key_g,
            R.id.key_h,
            R.id.key_j,
            R.id.key_k,
            R.id.key_l,
            R.id.key_quote,
            R.id.key_comma,
            R.id.key_z,
            R.id.key_x,
            R.id.key_c,
            R.id.key_v,
            R.id.key_b,
            R.id.key_n,
            R.id.key_m,
            R.id.key_period,
            R.id.key_slash,
        )
        baseKeys.forEach { setKeyTextSize(it, baseSize) }

        intArrayOf(R.id.key_backspace).forEach { setKeyTextSize(it, wideSize) }
        intArrayOf(R.id.key_tab, R.id.key_space, R.id.key_page_up, R.id.key_page_down, R.id.key_home, R.id.key_end, R.id.key_delete, R.id.key_emoji, R.id.key_numbers, R.id.key_shift, R.id.key_lang).forEach {
            setKeyTextSize(it, utilitySize)
        }
        intArrayOf(R.id.key_ctrl, R.id.key_alt, R.id.key_esc).forEach { setKeyTextSize(it, modifierSize) }
        intArrayOf(R.id.key_enter).forEach { setKeyTextSize(it, enterSize) }
        intArrayOf(R.id.key_arrow_up, R.id.key_arrow_down, R.id.key_arrow_left, R.id.key_arrow_right).forEach {
            setKeyTextSize(it, arrowSize)
        }
    }

    private fun applyKeyboardRowHeights() {
        if (!::keyRows.isInitialized) return
        val baseHeightDp = prefs.getString("key_height", "42")?.toIntOrNull() ?: 42
        val effectiveHeightDp = when (layoutMode) {
            KeyboardLayoutMode.FULL -> baseHeightDp
            KeyboardLayoutMode.COMPACT_EN,
            KeyboardLayoutMode.COMPACT_ES,
            KeyboardLayoutMode.COMPACT_SYMBOL,
            KeyboardLayoutMode.COMPACT_NUMERIC,
            KeyboardLayoutMode.ONE_HANDED_LEFT,
            KeyboardLayoutMode.ONE_HANDED_RIGHT -> nextCompactHeightDp(baseHeightDp)
        }
        val rowHeightPx = (effectiveHeightDp * resources.displayMetrics.density).toInt()
        keyRows.forEach { row ->
            row.updateLayoutHeight(rowHeightPx)
        }
    }

    private fun nextCompactHeightDp(baseHeightDp: Int): Int {
        return when {
            baseHeightDp < 36 -> 36
            baseHeightDp < 42 -> 42
            baseHeightDp < 48 -> 48
            baseHeightDp < 54 -> 54
            baseHeightDp < 60 -> 60
            else -> baseHeightDp
        }
    }

    private fun View.updateLayoutHeight(heightPx: Int) {
        val params = layoutParams ?: return
        if (params.height == heightPx) return
        params.height = heightPx
        layoutParams = params
    }

    private fun updateKeyWeight(viewId: Int, weight: Float) {
        val view = rootView.findViewById<View>(viewId) ?: return
        val params = view.layoutParams as? LinearLayout.LayoutParams ?: return
        if (params.weight == weight) return
        params.weight = weight
        view.layoutParams = params
    }

    private fun setKeyTextSize(viewId: Int, textSizeSp: Float) {
        rootView.findViewById<TextView>(viewId)?.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSp)
    }

    private fun wireLlmPanel() {
        llmActionCancel.setOnClickListener {
            feedbackVibrate(14)
            dismissLlmPanel(cancelRequest = true)
        }
        llmActionRetry.setOnClickListener {
            feedbackVibrate(14)
            retryLlmRequest()
        }
        llmActionReplace.setOnClickListener {
            feedbackVibrate(16)
            applyLlmResult()
        }
    }

    private fun startLlmTool(tool: LlmTextTool) {
        val source = currentLlmSource()
        if (source == null) {
            showToast(R.string.llm_no_source)
            refreshCandidateArea()
            return
        }
        val config = buildLlmConfig()
        if (config == null) {
            showToast(R.string.llm_missing_config)
            refreshCandidateArea()
            return
        }

        llmClient?.cancel()
        val request = LlmToolRequest(tool, source)
        llmPreviewState = LlmPreviewState(request = request, status = LlmPreviewStatus.LOADING)
        updateLlmPanelUi()

        llmClient = OpenAiCompatLlmClient(
            config = config,
            listener = object : OpenAiCompatLlmClient.Listener {
                override fun onSuccess(texts: List<String>) {
                    val current = llmPreviewState ?: return
                    if (current.request != request) return
                    val options = LlmToolSupport.extractResultOptions(request, texts)
                    val valid = isCurrentLlmSourceStillValid(request.source)
                    llmPreviewState = current.copy(
                        status = if (valid) LlmPreviewStatus.SUCCESS else LlmPreviewStatus.INVALIDATED,
                        resultOptions = options.ifEmpty { listOf(getString(R.string.llm_empty_result)) },
                        selectedResultIndex = 0,
                        errorMessage = "",
                    )
                    updateLlmPanelUi()
                }

                override fun onError(message: String) {
                    val current = llmPreviewState ?: return
                    if (current.request != request) return
                    llmPreviewState = current.copy(
                        status = LlmPreviewStatus.ERROR,
                        errorMessage = message,
                    )
                    updateLlmPanelUi()
                    showToast(message, Toast.LENGTH_LONG)
                }
            },
        )
        llmClient?.run(messages = LlmToolSupport.buildMessages(request))
    }

    private fun retryLlmRequest() {
        val request = llmPreviewState?.request ?: return
        startLlmTool(request.tool)
    }

    private fun applyLlmResult() {
        val preview = llmPreviewState ?: return
        if (preview.status != LlmPreviewStatus.SUCCESS) return
        val result = preview.selectedResultText.trim()
        if (result.isEmpty()) {
            showToast(R.string.llm_empty_result)
            return
        }

        val ic = currentInputConnection ?: return
        if (!isCurrentLlmSourceStillValid(preview.request.source)) {
            llmPreviewState = preview.copy(status = LlmPreviewStatus.INVALIDATED)
            updateLlmPanelUi()
            return
        }

        ic.beginBatchEdit()
        try {
            when (preview.request.source.kind) {
                LlmToolSource.Kind.SELECTION -> {
                    if (currentSelectedText()?.toString() != preview.request.source.rawText) {
                        llmPreviewState = preview.copy(status = LlmPreviewStatus.INVALIDATED)
                        updateLlmPanelUi()
                        return
                    }
                    ic.commitText(result, 1)
                }
                LlmToolSource.Kind.BEFORE_CURSOR -> {
                    val beforeCursor = currentBeforeCursorTextForLlm()?.toString().orEmpty()
                    if (!beforeCursor.endsWith(preview.request.source.rawText)) {
                        llmPreviewState = preview.copy(status = LlmPreviewStatus.INVALIDATED)
                        updateLlmPanelUi()
                        return
                    }
                    ic.deleteSurroundingText(preview.request.source.rawText.length, 0)
                    ic.commitText(result, 1)
                }
            }
        } finally {
            ic.endBatchEdit()
        }

        dismissLlmPanel(cancelRequest = false)
        refreshCandidateArea()
    }

    private fun dismissLlmPanel(cancelRequest: Boolean) {
        if (cancelRequest) {
            llmClient?.cancel()
        }
        llmClient = null
        llmPreviewState = null
        updateLlmPanelUi()
        refreshCandidateArea()
    }

    private fun showLanguagePickerDialog() {
        val languages = arrayOf("English (US)", "Español (ES)")
        val current = if (currentInputLang == InputLanguage.EN) 0 else 1
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Seleccionar idioma")
            .setSingleChoiceItems(languages, current) { d, which ->
                currentInputLang = if (which == 0) InputLanguage.EN else InputLanguage.ES
                layoutMode = defaultLayoutModeForLanguage()
                currentSpanishWord = ""
                currentEnglishWord = ""
                updateKeyboardLayoutUi()
                d.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .create()
        dialog.window?.setType(WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG)
        val token = rootView.windowToken
        if (token != null) {
            dialog.window?.attributes?.token = token
        }
        try {
            dialog.show()
        } catch (e: WindowManager.BadTokenException) {
            Log.e(TAG, "No se pudo mostrar el diálogo de idioma", e)
            dialog.dismiss()
        }
    }

    @Suppress("SetTextI18n")
    private fun updateLlmPanelUi() {
        if (!::llmPanel.isInitialized) return
        val preview = llmPreviewState
        if (preview == null) {
            llmPanel.visibility = View.GONE
            return
        }

        llmPanel.visibility = View.VISIBLE
        llmPanelTitle.text = "${getString(R.string.llm_panel_title)} - ${preview.request.tool.title}"
        llmPanelSource.text = preview.request.source.displayText
        rebuildLlmOptionChips(preview)
        llmPanelResult.text = when (preview.status) {
            LlmPreviewStatus.SUCCESS,
            LlmPreviewStatus.INVALIDATED -> preview.selectedResultText
            LlmPreviewStatus.ERROR -> preview.errorMessage
            else -> ""
        }
        llmPanelStatus.text = when (preview.status) {
            LlmPreviewStatus.LOADING -> getString(R.string.llm_loading)
            LlmPreviewStatus.ERROR -> preview.errorMessage
            LlmPreviewStatus.INVALIDATED -> getString(R.string.llm_invalidated)
            else -> ""
        }

        setActionEnabled(llmActionCancel, true)
        setActionEnabled(llmActionRetry, preview.status == LlmPreviewStatus.ERROR || preview.status == LlmPreviewStatus.INVALIDATED)
        setActionEnabled(llmActionReplace, preview.status == LlmPreviewStatus.SUCCESS)
    }

    @Suppress("SetTextI18n")
    private fun rebuildLlmOptionChips(preview: LlmPreviewState) {
        if (!::llmOptionContainer.isInitialized) return
        llmOptionContainer.removeAllViews()
        val options = preview.resultOptions
        if (preview.request.tool != LlmTextTool.POLISH || options.size <= 1) {
            llmOptionScroll.visibility = View.GONE
            return
        }

        options.forEachIndexed { index, _ ->
            val optionView = layoutInflater.inflate(R.layout.macro_button, llmOptionContainer, false) as TextView
            optionView.text = "Option ${index + 1}"
            optionView.setTextColor(
                ContextCompat.getColor(
                    this,
                    if (preview.selectedResultIndex == index) R.color.terminal_green else R.color.terminal_text,
                ),
            )
            optionView.setOnClickListener {
                feedbackVibrate(12)
                llmPreviewState = llmPreviewState?.copy(selectedResultIndex = index)
                updateLlmPanelUi()
            }
            llmOptionContainer.addView(optionView)
        }
        llmOptionScroll.visibility = View.VISIBLE
    }

    private fun setActionEnabled(view: TextView, enabled: Boolean) {
        view.isEnabled = enabled
        view.isClickable = enabled
        view.alpha = if (enabled) 1f else 0.42f
    }

    private fun refreshLlmPreviewValidity() {
        val preview = llmPreviewState ?: return
        if (preview.status == LlmPreviewStatus.LOADING || preview.status == LlmPreviewStatus.ERROR) return
        val nextStatus = if (isCurrentLlmSourceStillValid(preview.request.source)) {
            LlmPreviewStatus.SUCCESS
        } else {
            LlmPreviewStatus.INVALIDATED
        }
        if (nextStatus != preview.status) {
            llmPreviewState = preview.copy(status = nextStatus)
            updateLlmPanelUi()
        }
    }

    private fun currentLlmSource(): LlmToolSource? {
        return LlmToolSupport.resolveSource(
            selectedText = currentSelectedText(),
            beforeCursorText = currentBeforeCursorTextForLlm(),
        )
    }

    private fun currentSelectedText(): CharSequence? = currentInputConnection?.getSelectedText(0)

    private fun currentBeforeCursorTextForLlm(): CharSequence? {
        return currentInputConnection?.getTextBeforeCursor(LlmToolSupport.MAX_SOURCE_CHARS, 0)
    }

    private fun isCurrentLlmSourceStillValid(source: LlmToolSource): Boolean {
        return LlmToolSupport.isSourceStillValid(
            source = source,
            selectedText = currentSelectedText(),
            beforeCursorText = currentBeforeCursorTextForLlm(),
        )
    }

    private fun buildLlmConfig(): OpenAiCompatLlmClient.Config? {
        val baseUrl = prefs.getString("llm_base_url", "https://api.openai.com/v1")?.trim().orEmpty()
        val apiKey = prefs.getString("llm_api_key", null)?.trim().orEmpty()
        val model = prefs.getString("llm_model", null)?.trim().orEmpty()
        if (baseUrl.isBlank() || apiKey.isBlank() || model.isBlank()) {
            return null
        }
        return OpenAiCompatLlmClient.Config(
            baseUrl = baseUrl,
            apiKey = apiKey,
            model = model,
        )
    }

    private fun showEmojiPicker() {
        feedbackVibrate(14)
        feedbackSound()
        val categories = mapOf(
            "😀 Caritas" to listOf("😀", "😃", "😄", "😁", "😆", "😅", "🤣", "😂", "🙂", "🙃", "😉", "😊", "😇", "🥰", "😍", "🤩", "😘", "😗", "😚", "😙", "😋", "😛", "😜", "🤪", "😝", "🤑", "🤗", "🤭", "🤫", "🤔", "🤐", "🤨", "😐", "😑", "😶", "😏", "😒", "🙄", "😬", "😌", "😔", "😪", "🤤", "😴", "😷", "🤒", "🤕", "🤢", "🤮", "🤧", "🥵", "🥶", "🥳", "😎", "🤓", "🧐", "😕", "😟", "🙁", "😮", "😯", "😲", "😳", "🥺", "😦"),
            "❤️ Gestos" to listOf("👍", "👎", "👏", "🙌", "👐", "🤲", "🤝", "🙏", "💪", "👊", "✊", "🤛", "🤜", "🤞", "✌️", "🤟", "🤘", "👌", "🤌", "🤏", "👈", "👉", "👆", "👇", "☝️", "✋", "🤚", "🖐️", "🖖", "👋", "🤙", "✍️", "💅", "👄", "👅", "👂", "👃", "👣", "👁️", "👀", "🧠", "🦴", "👤", "👥", "🫂", "❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍", "🤎", "💔", "❤️‍🔥", "💖", "💗", "💘", "💕", "💞", "💓", "❣️"),
            "🐶 Animales" to listOf("🐶", "🐱", "🐭", "🐹", "🐰", "🦊", "🐻", "🐼", "🐨", "🐯", "🦁", "🐮", "🐷", "🐸", "🐵", "🐔", "🐧", "🐦", "🐤", "🐣", "🐥", "🦆", "🦅", "🦉", "🦇", "🐺", "🐗", "🐴", "🦄", "🐝", "🐛", "🦋", "🐌", "🐞", "🐜", "🐢", "🐍", "🦎", "🐙", "🦑", "🦀", "🦞", "🦐", "🐠", "🐟", "🐬", "🐳", "🐋", "🦈", "🐊", "🐅", "🐆", "🦓", "🦍", "🦧", "🐘", "🦛", "🦏", "🐪", "🐫", "🦒", "🦘", "🐃", "🐂", "🐄", "🐎", "🐖", "🐏", "🐑", "🦙", "🐐", "🦌", "🐕", "🐩", "🐈"),
            "🍎 Comida" to listOf("🍏", "🍎", "🍐", "🍊", "🍋", "🍌", "🍉", "🍇", "🍓", "🍈", "🍒", "🍑", "🍍", "🥥", "🥝", "🍅", "🍆", "🥑", "🥦", "🥬", "🥒", "🌶️", "🫑", "🌽", "🥕", "🫒", "🧄", "🧅", "🥔", "🍠", "🥐", "🥯", "🍞", "🥖", "🥨", "🧀", "🥚", "🍳", "🧈", "🥞", "🧇", "🥓", "🥩", "🍗", "🍖", "🌭", "🍔", "🍟", "🍕", "🥪", "🌮", "🌯", "🥗", "🥘", "🍝", "🍜", "🍲", "🍛", "🍣", "🍱", "🥟", "🍧", "🍨", "🍦", "🍩", "🍪", "☕", "🍵", "🍺", "🍻", "🥂", "🍷", "🍸", "🍹", "🍾"),
            "🚗 Viajes" to listOf("🚗", "🚕", "🚙", "🚌", "🚎", "🏎️", "🚓", "🚑", "🚒", "🚐", "🚚", "🚛", "🚜", "🏍️", "🛵", "🚲", "🛴", "🛹", "🛼", "⛽", "🚨", "🚥", "🚦", "🛑", "🚧", "⚓", "⛵", "🚤", "🚢", "✈️", "🛩️", "🚁", "🛰️", "🚀", "🛸", "⏰", "⏱️", "🧭", "⚽", "🏀", "🏈", "⚾", "🎾", "🏐", "🏉", "🎱", "🏓", "🏸", "🥅", "⛳", "🏆", "🥇", "🥈", "🥉", "🏅", "🎨", "🎤", "🎧", "🎼", "🎹", "🥁", "🎷", "🎺", "🎸", "🎻", "🎲", "🎯", "🎳", "🎮"),
            "💡 Objetos" to listOf("⌚", "📱", "💻", "⌨️", "🖥️", "🖨️", "🖱️", "💽", "💾", "💿", "📀", "📷", "📸", "📹", "🎥", "📺", "📻", "🎙️", "🔋", "🔌", "💡", "🔦", "🕯️", "🧯", "💸", "💵", "💴", "💶", "💷", "🪙", "💰", "💳", "💎", "⚖️", "🧰", "🔧", "🔨", "⚒️", "🛠️", "⛏️", "⚙️", "🧱", "🛡️", "🔑", "🗝️", "🔒", "🔓", "🔔", "🔕", "📢", "📣", "💬", "💭", "💤", "🚀", "💡", "📌", "📍", "📎", "📏", "📐", "✂️", "🖊️", "🖋️", "✒️", "✏️", "📝", "📁", "📂", "🗂️", "📅", "📆", "📇", "📈", "📉", "📊", "📋")
        )

        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(12, 12, 12, 12)
        }

        val tabScroll = HorizontalScrollView(this).apply {
            isHorizontalFadingEdgeEnabled = false
            isHorizontalScrollBarEnabled = false
        }
        val tabContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
        }
        tabScroll.addView(tabContainer)
        mainLayout.addView(tabScroll)

        val gridLayout = android.widget.GridLayout(this).apply {
            columnCount = 7
            setPadding(8, 8, 8, 8)
        }
        val emojiScroll = android.widget.ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (resources.displayMetrics.heightPixels * 0.45f).toInt().coerceAtLeast(
                    (resources.displayMetrics.density * 260).toInt(),
                ),
            )
            addView(gridLayout)
        }
        mainLayout.addView(emojiScroll)

        fun populateEmojis(categoryName: String) {
            gridLayout.removeAllViews()
            val list = categories[categoryName] ?: emptyList()
            list.forEach { emoji ->
                val btn = TextView(this).apply {
                    text = emoji
                    textSize = 26f
                    gravity = android.view.Gravity.CENTER
                    setPadding(10, 10, 10, 10)
                    setOnClickListener {
                        feedbackVibrate(12)
                        feedbackSound()
                        currentInputConnection?.commitText(emoji, 1)
                    }
                }
                gridLayout.addView(btn)
            }
        }

        categories.keys.forEachIndexed { index, catName ->
            val tabBtn = TextView(this).apply {
                text = catName
                textSize = 13f
                setPadding(14, 10, 14, 10)
                setTextColor(ContextCompat.getColor(context, R.color.terminal_cyan))
                setOnClickListener {
                    feedbackVibrate(10)
                    populateEmojis(catName)
                }
            }
            tabContainer.addView(tabBtn)
            if (index == 0) {
                populateEmojis(catName)
            }
        }

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Emojis")
            .setView(mainLayout)
            .setNegativeButton("Cerrar", null)
            .create()

        dialog.window?.setType(WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG)
        val token = rootView.windowToken
        if (token != null) {
            dialog.window?.attributes?.token = token
        }
        try {
            dialog.show()
        } catch (e: WindowManager.BadTokenException) {
            Log.e(TAG, "No se pudo mostrar el selector de emojis", e)
            dialog.dismiss()
        }
    }

    private fun wireNumericPad() {
        if (!::rootView.isInitialized) return
        val numMap = mapOf(
            R.id.num_1 to "1",
            R.id.num_2 to "2",
            R.id.num_3 to "3",
            R.id.num_4 to "4",
            R.id.num_5 to "5",
            R.id.num_6 to "6",
            R.id.num_7 to "7",
            R.id.num_8 to "8",
            R.id.num_9 to "9",
            R.id.num_0 to "0",
            R.id.num_dot to ".",
            R.id.num_comma to ",",
            R.id.num_div to "/",
            R.id.num_mul to "*",
            R.id.num_min to "-",
            R.id.num_plus to "+",
        )
        numMap.forEach { (viewId, charStr) ->
            rootView.findViewById<View>(viewId)?.apply {
                isClickable = true
                setOnClickListener {
                    feedbackVibrate(14)
                    feedbackSound()
                    currentInputConnection?.commitText(charStr, 1)
                }
            }
        }
        rootView.findViewById<View>(R.id.num_backspace)?.setOnClickListener {
            performBackspace()
        }
        rootView.findViewById<View>(R.id.num_enter)?.setOnClickListener {
            sendEnter()
        }
        rootView.findViewById<View>(R.id.num_space)?.setOnClickListener {
            sendSpace()
        }
        rootView.findViewById<View>(R.id.num_abc)?.setOnClickListener {
            feedbackVibrate(15)
            toggleNumericMode()
        }
    }

    private fun toggleNumericMode() {
        layoutMode = if (layoutMode == KeyboardLayoutMode.COMPACT_NUMERIC) {
            defaultLayoutModeForLanguage()
        } else {
            KeyboardLayoutMode.COMPACT_NUMERIC
        }
        updateKeyboardLayoutUi()
        scheduleAdaptiveTouchTargetsUpdate()
    }

    // ── Haptic / Audio feedback ────────────────────────────────────────────────

    private fun feedbackVibrate(ms: Long = 10) {
        if (!prefs.getBoolean("vibrate_on_keypress", true)) return
        val vib = vibrator ?: return
        try {
            vib.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
        } catch (_: SecurityException) {
        } catch (_: Exception) {
        }
    }

    private fun feedbackSound() {
        if (!prefs.getBoolean("sound_on_keypress", false)) return
        try {
            audioManager?.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD, -1f)
        } catch (_: Exception) {
        }
    }

    @Suppress("SameParameterValue")
    private fun showToast(text: String, duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(this, text, duration).show()
    }

    @Suppress("SameParameterValue")
    private fun showToast(resId: Int, duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(this, resId, duration).show()
    }
}
