package com.termkey.ime

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.settings_container, SettingsFragment())
                .commit()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)

            configureAppearancePreferences()
            configureLlmPreferences()

            findPreference<Preference>("edit_macros")?.setOnPreferenceClickListener {
                startActivity(Intent(requireContext(), MacroEditorActivity::class.java))
                true
            }

            findPreference<Preference>("reset_macros")?.setOnPreferenceClickListener {
                AlertDialog.Builder(requireContext())
                    .setTitle("Reset Macros")
                    .setMessage("Restore all macros to their defaults? This cannot be undone.")
                    .setPositiveButton("Reset") { _, _ ->
                        MacroManager.resetToDefaults(requireContext())
                        Toast.makeText(context, "Macros reset to defaults.", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
                true
            }

            findPreference<Preference>("clear_user_dictionary")?.setOnPreferenceClickListener {
                UserDictionaryStore(requireContext()).clearDictionary()
                Toast.makeText(context, R.string.dictionary_cleared, Toast.LENGTH_SHORT).show()
                true
            }
        }

        private fun configureAppearancePreferences() {
            listOf(
                "custom_surface_color" to R.string.pref_custom_surface_color,
                "custom_key_bg_color" to R.string.pref_custom_key_bg_color,
                "custom_text_color" to R.string.pref_custom_text_color,
                "custom_accent_color" to R.string.pref_custom_accent_color,
            ).forEach { (key, titleRes) ->
                findPreference<Preference>(key)?.apply {
                    title = getString(titleRes)
                    setOnPreferenceClickListener {
                        showColorPicker(key)
                        true
                    }
                    updateColorSummary(key)
                }
            }
            findPreference<ListPreference>("keyboard_theme")?.apply {
                summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
            }
            findPreference<ListPreference>("keyboard_font")?.apply {
                summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
            }
            findPreference<ListPreference>("key_text_size")?.apply {
                summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
            }
            findPreference<ListPreference>("key_height")?.apply {
                summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
            }
        }

        private fun updateColorSummary(key: String) {
            val color = PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getString(key, null)?.let { runCatching { Color.parseColor(it) }.getOrNull() }
            findPreference<Preference>(key)?.summary = color?.let {
                "●  ${String.format("#%06X", 0xFFFFFF and it)}"
            } ?: getString(R.string.pref_color_choose)
        }

        private fun showColorPicker(key: String) {
            val colors = listOf(
                "#0D1117", "#21262D", "#FFFFFF", "#00FF66", "#121212", "#2A2A2A",
                "#33B5E5", "#000000", "#00E5FF", "#282A36", "#44475A", "#FF79C6",
                "#2E3440", "#3B4252", "#88C0D0", "#282828", "#3C3836", "#FABD2F",
                "#002B36", "#073642", "#2AA198", "#120F0A", "#282015", "#FFB000",
                "#150E1F", "#2C1D42", "#00F0FF", "#F8F9FA", "#E9ECEF", "#007AFF",
                "#1A0F0F", "#331C1C", "#FF3333", "#6C5CE7", "#FF5722", "#9C27B0",
            )
            val container = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(24, 12, 24, 12)
            }
            val preview = TextView(requireContext()).apply {
                text = getString(R.string.pref_color_preview)
                gravity = Gravity.CENTER
                textSize = 18f
                setPadding(8, 18, 8, 18)
            }
            container.addView(preview)
            val grid = android.widget.GridLayout(requireContext()).apply { columnCount = 6 }
            colors.forEach { hex ->
                val swatch = TextView(requireContext()).apply {
                    val color = Color.parseColor(hex)
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(color)
                        setStroke((2 * resources.displayMetrics.density).toInt(), 0xFF888888.toInt())
                    }
                    contentDescription = hex
                    setOnClickListener {
                        preview.setBackgroundColor(color)
                        preview.setTextColor(if (Color.luminance(color) < 0.5f) Color.WHITE else Color.BLACK)
                        preview.text = hex
                        PreferenceManager.getDefaultSharedPreferences(requireContext()).edit()
                            .putString(key, hex).apply()
                        updateColorSummary(key)
                    }
                }
                grid.addView(swatch, android.widget.GridLayout.LayoutParams().apply {
                    width = (36 * resources.displayMetrics.density).toInt()
                    height = width
                    setMargins(6, 6, 6, 6)
                })
            }
            container.addView(grid)
            AlertDialog.Builder(requireContext())
                .setTitle(findPreference<Preference>(key)?.title)
                .setView(container)
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }

        private fun configureLlmPreferences() {
            findPreference<EditTextPreference>("llm_base_url")?.apply {
                summaryProvider = Preference.SummaryProvider<EditTextPreference> { pref ->
                    pref.text?.takeIf { it.isNotBlank() } ?: getString(R.string.pref_value_not_set)
                }
                setOnBindEditTextListener { editText ->
                    editText.isSingleLine = true
                }
            }

            findPreference<EditTextPreference>("llm_api_key")?.apply {
                summaryProvider = Preference.SummaryProvider<EditTextPreference> { pref ->
                    if (pref.text.isNullOrBlank()) {
                        getString(R.string.pref_value_not_set)
                    } else {
                        getString(R.string.pref_value_configured)
                    }
                }
                setOnBindEditTextListener { editText ->
                    editText.isSingleLine = true
                }
            }

            findPreference<EditTextPreference>("llm_model")?.apply {
                summaryProvider = Preference.SummaryProvider<EditTextPreference> { pref ->
                    pref.text?.takeIf { it.isNotBlank() } ?: getString(R.string.pref_value_not_set)
                }
                setOnBindEditTextListener { editText ->
                    editText.isSingleLine = true
                }
            }
        }
    }
}
