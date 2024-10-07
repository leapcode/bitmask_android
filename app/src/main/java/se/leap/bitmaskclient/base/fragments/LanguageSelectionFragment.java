package se.leap.bitmaskclient.base.fragments;

import static se.leap.bitmaskclient.base.utils.ViewHelper.setActionBarSubtitle;

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

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import se.leap.bitmaskclient.R;
import se.leap.bitmaskclient.base.views.SimpleCheckBox;
import se.leap.bitmaskclient.databinding.FLanguageSelectionBinding;
import se.leap.bitmaskclient.databinding.VSelectTextListItemBinding;

public class LanguageSelectionFragment extends BottomSheetDialogFragment {
    static final String TAG = LanguageSelectionFragment.class.getSimpleName();
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
        Locale defaultLocale = AppCompatDelegate.getApplicationLocales().get(0);
        if (defaultLocale == null) {
            defaultLocale = LocaleListCompat.getDefault().get(0);
        }
        // NOTE: Sort the supported languages by their display names.
        // This would make updating supported languages easier as we don't have to tip toe around the order
        Collections.sort(supportedLanguages, (lang1, lang2) -> {
            String displayName1 = Locale.forLanguageTag(lang1).getDisplayName(Locale.ENGLISH);
            String displayName2 = Locale.forLanguageTag(lang2).getDisplayName(Locale.ENGLISH);
            return displayName1.compareTo(displayName2);
        });
        binding.languages.setAdapter(
                new LanguageSelectionAdapter(supportedLanguages, this::updateLocale, defaultLocale)
        );
        binding.languages.setLayoutManager(new LinearLayoutManager(getContext()));
    }

    public static Locale getCurrentLocale() {
        Locale defaultLocale = AppCompatDelegate.getApplicationLocales().get(0);
        if (defaultLocale == null) {
            defaultLocale = LocaleListCompat.getDefault().get(0);
        }
        return defaultLocale;
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
     * Adapter for the language selection recycler view.
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
            holder.languageName.setText(Locale.forLanguageTag(languageTag).getDisplayName(Locale.ENGLISH));
            if (languages.contains(selectedLocale.toLanguageTag())) {
                holder.selected.setChecked(selectedLocale.toLanguageTag().equals(languageTag));
            } else {
                holder.selected.setChecked(selectedLocale.getLanguage().equals(languageTag));
            }
            holder.itemView.setOnClickListener(v -> clickListener.onLanguageClick(languageTag));
        }

        @Override
        public int getItemCount() {
            return languages.size();
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
