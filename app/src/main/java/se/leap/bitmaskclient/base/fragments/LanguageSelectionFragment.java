package se.leap.bitmaskclient.base.fragments;

import static android.content.res.Resources.getSystem;
import static androidx.core.app.LocaleManagerCompat.getSystemLocales;
import static androidx.core.os.ConfigurationCompat.getLocales;
import static se.leap.bitmaskclient.base.utils.ViewHelper.setActionBarSubtitle;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import se.leap.bitmaskclient.R;
import se.leap.bitmaskclient.base.utils.StringUtils;
import se.leap.bitmaskclient.base.views.SimpleCheckBox;
import se.leap.bitmaskclient.databinding.FLanguageSelectionBinding;
import se.leap.bitmaskclient.databinding.VSelectTextListItemBinding;

public class LanguageSelectionFragment extends BottomSheetDialogFragment {
    public static final String TAG = LanguageSelectionFragment.class.getSimpleName();
    static final String SYSTEM_LOCALE = "systemLocale";
    private FLanguageSelectionBinding binding;

    public static LanguageSelectionFragment newInstance(Locale defaultLocale) {
        LanguageSelectionFragment fragment = new LanguageSelectionFragment();
        Bundle args = new Bundle();
        args.putString(SYSTEM_LOCALE, defaultLocale.toLanguageTag());
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FLanguageSelectionBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setActionBarSubtitle(this, R.string.select_language);

        initRecyclerView(Arrays.asList(getResources().getStringArray(R.array.supported_languages)));
    }

    private static void customizeSelectionItem(VSelectTextListItemBinding binding) {
        binding.title.setVisibility(View.GONE);
        binding.bridgeImage.setVisibility(View.GONE);
        binding.quality.setVisibility(View.GONE);
    }

    private void initRecyclerView(List<String> supportedLanguages) {
        Locale currentLocale = getCurrentLocale();

        // new list so it doesn't crash on fixed size
        List<String> languagesWithSystem = new ArrayList<>(supportedLanguages);

        // Sort by the language's native name (endonym)
        languagesWithSystem.sort((lang1, lang2) -> {
            Locale loc1 = Locale.forLanguageTag(lang1);
            Locale loc2 = Locale.forLanguageTag(lang2);

            String name1 = loc1.getDisplayName(loc1);
            String name2 = loc2.getDisplayName(loc2);

            return name1.compareToIgnoreCase(name2);
        });

        // using add(0) because addFirst() causes a NoSuchMethodError on Android 7
        languagesWithSystem.add(0, "");

        binding.languages.setAdapter(
                new LanguageSelectionAdapter(languagesWithSystem, this::updateLocale, currentLocale)
        );
        binding.languages.setLayoutManager(new LinearLayoutManager(getContext()));
    }

    // use current locale or fallback to system default
    public static Locale getCurrentLocale() {
        LocaleListCompat appLocales = AppCompatDelegate.getApplicationLocales();
        return appLocales.isEmpty() ? LocaleListCompat.getDefault().get(0) : appLocales.get(0);
    }

    /**
     * Update the locale of the application
     *
     * @param languageTag the language tag to set the locale to
     */
    private void updateLocale(String languageTag) {
        if (languageTag.isEmpty()) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList());
        } else {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageTag));
        }
    }

    /**
     * Adapter for language selection recycler view.
     */
    static class LanguageSelectionAdapter extends RecyclerView.Adapter<LanguageSelectionAdapter.LanguageViewHolder> {

        private final List<String> languages;
        private final LanguageClickListener clickListener;
        private final Locale selectedLocale;

        public LanguageSelectionAdapter(List<String> languages, LanguageClickListener clickListener, Locale defaultLocale) {
            this.languages = languages;
            this.clickListener = clickListener;
            this.selectedLocale = defaultLocale;
        }

        @NonNull
        @Override
        public LanguageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            VSelectTextListItemBinding binding = VSelectTextListItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new LanguageViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull LanguageViewHolder holder, int position) {
            String languageTag = languages.get(position);

            // using system lang or not
            boolean isSystemSet = AppCompatDelegate.getApplicationLocales().isEmpty();

            if (languageTag.isEmpty()) {
                // handle empty string (system default)
                Locale systemLocale = null;

                try {
                    // Use LocaleManagerCompat to reliably bypass app-level locale overrides
                    LocaleListCompat trueSystemLocales = getSystemLocales(holder.itemView.getContext());
                    if (!trueSystemLocales.isEmpty()) {
                        systemLocale = trueSystemLocales.get(0);
                    }
                } catch (Exception e) {
                    // Fallback for older AndroidX library versions
                }

                if (systemLocale == null) {
                    systemLocale = getLocales(getSystem().getConfiguration()).get(0);
                }

                String systemLabel = systemLocale != null ?
                        getLocalizedSystemLabel(holder.itemView.getContext(), systemLocale) :
                        "";

                holder.languageName.setText(systemLabel);
                holder.selected.setChecked(isSystemSet);

            } else {
                // standard language
                Locale targetLocale = Locale.forLanguageTag(languageTag);

                // Pass targetLocale into getDisplayName to get the native name (endonym)
                String displayName = targetLocale.getDisplayName(targetLocale);

                // Capitalize the first letter
                if (!displayName.isEmpty()) {
                    displayName = StringUtils.capitalize(displayName, targetLocale);
                }

                holder.languageName.setText(displayName);

                // no double-check items if system default is active
                if (isSystemSet) {
                    holder.selected.setChecked(false);
                } else {
                    // match exact tag or just the language code
                    if (languages.contains(selectedLocale.toLanguageTag())) {
                        holder.selected.setChecked(selectedLocale.toLanguageTag().equals(languageTag));
                    } else {
                        holder.selected.setChecked(selectedLocale.getLanguage().equals(languageTag));
                    }
                }
            }

            holder.itemView.setOnClickListener(v -> clickListener.onLanguageClick(languageTag));
        }

        @Override
        public int getItemCount() {
            return languages.size();
        }

        // Language name in system language, suffix translated to current app language
        private String getLocalizedSystemLabel(Context context, Locale systemLocale) {
            // 1. Force the suffix to fetch strings in the APP'S current language (selectedLocale)
            Configuration appConfig = new Configuration(context.getResources().getConfiguration());
            appConfig.setLocale(selectedLocale);
            Context appLocalizedContext = context.createConfigurationContext(appConfig);

            String suffix = appLocalizedContext.getString(R.string.system_default);

            // 2. The language name uses its endonym (true system language's own name)
            String languageName = systemLocale.getDisplayName(systemLocale);
            languageName = StringUtils.capitalize(languageName, systemLocale);

            return String.format("%s\n(%s)", languageName, suffix);
        }

        /**
         * View holder for the language item
         */
        static class LanguageViewHolder extends RecyclerView.ViewHolder {
            TextView languageName;
            SimpleCheckBox selected;

            public LanguageViewHolder(@NonNull VSelectTextListItemBinding binding) {
                super(binding.getRoot());
                languageName = binding.location;
                selected = binding.selected;
                customizeSelectionItem(binding);
            }
        }
    }

    /**
     * Interface for the language click listener
     */
    interface LanguageClickListener {
        void onLanguageClick(String languageTag);
    }
}