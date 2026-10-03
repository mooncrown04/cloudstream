Preference.PreferenceItem.ListPreference(
    preference = settings.ui.appFont,
    icon = painterResource(R.drawable.ic_baseline_text_fields_24),
    title = stringResource(R.string.app_font_settings),
    entries = stringArrayResource(R.array.app_font_values).zip(
        stringArrayResource(R.array.app_font_names)
    ).toMap().toPersistentMap(),
    onValueChanged = { newValue ->
        settings.ui.appFont.set(newValue)
        safe {
            activity?.recreate()
        }
        return@ListPreference false
    }
)
