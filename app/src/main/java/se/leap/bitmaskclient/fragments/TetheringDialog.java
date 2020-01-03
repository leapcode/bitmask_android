package se.leap.bitmaskclient.fragments;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialogFragment;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import butterknife.ButterKnife;
import butterknife.InjectView;
import se.leap.bitmaskclient.R;
import se.leap.bitmaskclient.eip.EipCommand;
import se.leap.bitmaskclient.tethering.WifiHotspotObserver;
import se.leap.bitmaskclient.utils.PreferenceHelper;
import se.leap.bitmaskclient.views.IconCheckboxEntry;

/**
 * Created by cyberta on 25.02.18.
 */

public class TetheringDialog extends AppCompatDialogFragment {

    public final static String TAG = TetheringDialog.class.getName();

    @InjectView(R.id.tvTitle)
    AppCompatTextView title;

    @InjectView(R.id.user_message)
    AppCompatTextView userMessage;

    @InjectView(R.id.selection_list_view)
    RecyclerView selectionListView;
    DialogListAdapter adapter;
    private DialogListAdapter.ViewModel[] dataset;

    public static class DialogListAdapter extends RecyclerView.Adapter<DialogListAdapter.ViewHolder> {

        interface OnItemClickListener {
            void onItemClick(ViewModel item);
        }

        private ViewModel[] dataSet;
        private OnItemClickListener clickListener;

        public DialogListAdapter(ViewModel[] dataSet, OnItemClickListener clickListener) {
            this.dataSet = dataSet;
            this.clickListener = clickListener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            IconCheckboxEntry v = new IconCheckboxEntry(viewGroup.getContext());
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
            viewHolder.bind(dataSet[i], clickListener);
        }

        @Override
        public int getItemCount() {
            return dataSet.length;
        }

        public static class ViewModel {

            public Drawable image;
            public String text;
            public boolean checked;
            public boolean enabled;

            ViewModel(Drawable image, String text, boolean checked, boolean enabled) {
                this.image = image;
                this.text = text;
                this.checked = checked;
                this.enabled = enabled;
            }
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {

            ViewHolder(IconCheckboxEntry v) {
                super(v);
            }

            public void bind(ViewModel model, OnItemClickListener onClickListener) {
                ((IconCheckboxEntry) this.itemView).bind(model);
                this.itemView.setOnClickListener(v -> {
                    model.checked = !model.checked;
                    ((IconCheckboxEntry) itemView).setChecked(model.checked);
                    onClickListener.onItemClick(model);
                });
            }
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.d_list_selection, null);
        ButterKnife.inject(this, view);

        title.setText(R.string.tethering);
        userMessage.setMovementMethod(LinkMovementMethod.getInstance());
        userMessage.setLinkTextColor(getContext().getResources().getColor(R.color.colorPrimary));
        userMessage.setText(createUserMessage());

        initDataset();
        adapter = new DialogListAdapter(dataset, this::onItemClick);
        selectionListView.setAdapter(adapter);
        selectionListView.setLayoutManager(new LinearLayoutManager(getActivity()));


        builder.setView(view)
                .setPositiveButton(android.R.string.ok, (dialog, id) -> {
                    PreferenceHelper.wifiTethering(getContext(), dataset[0].checked);
                    PreferenceHelper.usbTethering(getContext(), dataset[1].checked);
                    PreferenceHelper.bluetoothTethering(getContext(), dataset[2].checked);
                    EipCommand.configureTethering(getContext());
                })
                .setNegativeButton(R.string.cancel, (dialog, id) -> dialog.cancel());
        return builder.create();
    }

    @Override
    public void onResume() {
        super.onResume();
        dataset[0].enabled = WifiHotspotObserver.getInstance().isEnabled();
        adapter.notifyDataSetChanged();
    }

    public void onItemClick(DialogListAdapter.ViewModel item) {

    }

    private CharSequence createUserMessage() {
        String tetheringMessage = getString(R.string.tethering_message);
        String systemSettings = getString(R.string.tethering_system_settings);
        String systemSettingsMessage = getString(R.string.tethering_enabled_message, systemSettings);
        String wholeMessage = systemSettingsMessage + "\n\n" + tetheringMessage;
        int startIndex = wholeMessage.indexOf(systemSettings, 0);
        int endIndex = startIndex + systemSettings.length();

        Spannable spannable = new SpannableString(wholeMessage);
        spannable.setSpan(new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                Intent intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
                startActivity(intent);
            }
        }, startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        return spannable;
    }

    private void initDataset() {
        dataset = new DialogListAdapter.ViewModel[] {
                new DialogListAdapter.ViewModel(getContext().getResources().getDrawable(R.drawable.ic_wifi),
                        getContext().getString(R.string.tethering_wifi),
                        PreferenceHelper.getWifiTethering(getContext()),
                        WifiHotspotObserver.getInstance().isEnabled()),
                new DialogListAdapter.ViewModel(getContext().getResources().getDrawable(R.drawable.ic_usb),
                        getContext().getString(R.string.tethering_usb),
                        PreferenceHelper.getUsbTethering(getContext()),
                        true),
                new DialogListAdapter.ViewModel(getContext().getResources().getDrawable(R.drawable.ic_bluetooth),
                        getContext().getString(R.string.tethering_bluetooth),
                        PreferenceHelper.getBluetoothTethering(getContext()),
                        true)
        };
    }

}
