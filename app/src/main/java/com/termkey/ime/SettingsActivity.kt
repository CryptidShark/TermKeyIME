package com.termkey.ime

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat

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
