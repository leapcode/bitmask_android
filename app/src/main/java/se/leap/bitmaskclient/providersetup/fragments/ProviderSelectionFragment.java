package se.leap.bitmaskclient.providersetup.fragments;

import static se.leap.bitmaskclient.providersetup.fragments.viewmodel.ProviderSelectionViewModel.ADD_PROVIDER;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.util.ArrayList;

import se.leap.bitmaskclient.R;
import se.leap.bitmaskclient.base.models.Provider;
import se.leap.bitmaskclient.databinding.FProviderSelectionBinding;
import se.leap.bitmaskclient.providersetup.activities.SetupInterface;
import se.leap.bitmaskclient.providersetup.fragments.viewmodel.ProviderSelectionViewModel;
import se.leap.bitmaskclient.providersetup.fragments.viewmodel.ProviderSelectionViewModelFactory;

public class ProviderSelectionFragment extends Fragment {

    private ProviderSelectionViewModel viewModel;
    private ArrayList<RadioButton> radioButtons;
    private SetupInterface setupCallback;

    public static ProviderSelectionFragment newInstance() {
        return new ProviderSelectionFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this,
                new ProviderSelectionViewModelFactory(
                        getContext().getApplicationContext().getAssets(),
                        getContext().getExternalFilesDir(null))).
                get(ProviderSelectionViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        FProviderSelectionBinding binding = FProviderSelectionBinding.inflate(inflater, container, false);

        radioButtons = new ArrayList<>();
        for (int i = 0; i < viewModel.size(); i++) {
            Provider provider = viewModel.getProvider(i);
            RadioButton radioButton = new RadioButton(binding.getRoot().getContext());
            radioButton.setText(provider.getDomain());
            radioButton.setId(i);
            binding.providerRadioGroup.addView(radioButton);
            radioButtons.add(radioButton);
        }
        RadioButton radioButton = new RadioButton(binding.getRoot().getContext());
        radioButton.setText(getText(R.string.add_provider));
        radioButton.setId(ADD_PROVIDER);
        binding.providerRadioGroup.addView(radioButton);
        radioButtons.add(radioButton);

        binding.editCustomProvider.setVisibility(viewModel.getEditProviderVisibility());
        binding.providerRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            viewModel.setSelected(checkedId);
            for (RadioButton rb : radioButtons) {
                rb.setTypeface(Typeface.DEFAULT, rb.getId() == checkedId ? Typeface.BOLD : Typeface.NORMAL);
            }
            binding.providerDescription.setText(viewModel.getProviderDescription(getContext()));
            binding.editCustomProvider.setVisibility(viewModel.getEditProviderVisibility());
            if (setupCallback != null) {
                setupCallback.onSetupStepValidationChanged(viewModel.isValidConfig());
                if (checkedId != ADD_PROVIDER) {
                    setupCallback.onProviderSelected(viewModel.getProvider(checkedId));
                } else if (viewModel.isValidConfig()) {
                    setupCallback.onProviderSelected(new Provider(binding.editCustomProvider.getText().toString()));
                }
            }
        });
        binding.providerRadioGroup.check(viewModel.getSelected());

        binding.editCustomProvider.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.setCustomUrl(s.toString());
                if (setupCallback == null) return;
                setupCallback.onSetupStepValidationChanged(viewModel.isValidConfig());
                if (viewModel.isValidConfig()) {
                    setupCallback.onProviderSelected(new Provider(s.toString()));
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
        return binding.getRoot();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (getActivity() instanceof SetupInterface) {
            setupCallback = (SetupInterface) getActivity();
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        setupCallback = null;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        radioButtons = null;
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        setupCallback.onSetupStepValidationChanged(viewModel.isValidConfig());
    }
}