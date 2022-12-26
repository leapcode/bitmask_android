package se.leap.bitmaskclient.base.fragments;

import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Observable;
import java.util.Observer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import de.blinkt.openvpn.core.VpnStatus;
import se.leap.bitmaskclient.R;
import se.leap.bitmaskclient.base.utils.PreferenceHelper;
import se.leap.bitmaskclient.base.views.IconCheckboxEntry;
import se.leap.bitmaskclient.firewall.FirewallManager;
import se.leap.bitmaskclient.tethering.TetheringObservable;

/**
 * Copyright (c) 2020 LEAP Encryption Access Project and contributers
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

public class TetheringDialog extends AppCompatDialogFragment implements Observer {

    public final static String TAG = TetheringDialog.class.getName();

    @BindView(R.id.tvTitle)
    AppCompatTextView title;

    @BindView(R.id.user_message)
    AppCompatTextView userMessage;

    @BindView(R.id.selection_list_view)
    RecyclerView selectionListView;
    DialogListAdapter adapter;
    private DialogListAdapter.ViewModel[] dataset;
    private Unbinder unbinder;

    public static class DialogListAdapter extends RecyclerView.Adapter<DialogListAdapter.ViewHolder> {

        interface OnItemClickListener {
            void onItemClick(ViewModel item);
        }

        private ViewModel[] dataSet;
        private OnItemClickListener clickListener;

        DialogListAdapter(ViewModel[] dataSet, OnItemClickListener clickListener) {
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
        unbinder = ButterKnife.bind(this, view);

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
                    PreferenceHelper.allowWifiTethering(getContext(), dataset[0].checked);
                    PreferenceHelper.allowUsbTethering(getContext(), dataset[1].checked);
                    PreferenceHelper.allowBluetoothTethering(getContext(), dataset[2].checked);
                    TetheringObservable.allowVpnWifiTethering(dataset[0].checked);
                    TetheringObservable.allowVpnUsbTethering(dataset[1].checked);
                    TetheringObservable.allowVpnBluetoothTethering(dataset[2].checked);
                    FirewallManager firewallManager = new FirewallManager(getContext().getApplicationContext(), false);
                    if (VpnStatus.isVPNActive()) {
                        if (TetheringObservable.getInstance().getTetheringState().hasAnyDeviceTetheringEnabled() &&
                                TetheringObservable.getInstance().getTetheringState().hasAnyVpnTetheringAllowed()) {
                            firewallManager.startTethering();
                        } else {
                            firewallManager.stopTethering();
                        }
                    }
                }).setNegativeButton(R.string.cancel, (dialog, id) -> dialog.cancel());
        return builder.create();
    }

    @Override
    public void onResume() {
        super.onResume();
        dataset[0].enabled = TetheringObservable.getInstance().isWifiTetheringEnabled();
        dataset[1].enabled = TetheringObservable.getInstance().isUsbTetheringEnabled();
        dataset[2].enabled = TetheringObservable.getInstance().isBluetoothTetheringEnabled();
        adapter.notifyDataSetChanged();
        TetheringObservable.getInstance().addObserver(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        TetheringObservable.getInstance().deleteObserver(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    public void onItemClick(DialogListAdapter.ViewModel item) {

    }

    private CharSequence createUserMessage() {
        String tetheringMessage = getString(R.string.tethering_message);
        String systemSettingsMessage = getString(R.string.tethering_enabled_message);
        Pattern pattern = Pattern.compile("([\\w .]*)(<b>)+([\\w ]*)(</b>)([\\w .]*)");
        Matcher matcher = pattern.matcher(systemSettingsMessage);
        int startIndex = 0;
        int endIndex = 0;
        if (matcher.matches()) {
            startIndex = matcher.start(2);
            try {
                endIndex = startIndex + matcher.group(3).length();
            } catch (NullPointerException npe) {
                endIndex = -1;
            }
        }
        systemSettingsMessage = systemSettingsMessage.replace("<b>", "").replace("</b>", "");
        String wholeMessage = systemSettingsMessage + "\n\n" + tetheringMessage;
        if (startIndex == -1 || endIndex == -1) {
            Log.e(TAG, "Tethering string has wrong formatting!");
            return wholeMessage;
        }
        Spannable spannable = new SpannableString(wholeMessage);
        spannable.setSpan(new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                try {
                    final Intent intent = new Intent(Intent.ACTION_MAIN, null);
                    intent.addCategory(Intent.CATEGORY_LAUNCHER);
                    final ComponentName cn = new ComponentName("com.android.settings", "com.android.settings.TetherSettings");
                    intent.setComponent(cn);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    Intent intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
                    startActivity(intent);
                }

            }
        }, startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        return spannable;
    }

    private void initDataset() {
        dataset = new DialogListAdapter.ViewModel[] {
                new DialogListAdapter.ViewModel(getContext().getResources().getDrawable(R.drawable.ic_wifi),
                        getContext().getString(R.string.tethering_wifi),
                        PreferenceHelper.isWifiTetheringAllowed(getContext()),
                        TetheringObservable.getInstance().isWifiTetheringEnabled()),
                new DialogListAdapter.ViewModel(getContext().getResources().getDrawable(R.drawable.ic_usb),
                        getContext().getString(R.string.tethering_usb),
                        PreferenceHelper.isUsbTetheringAllowed(getContext()),
                        TetheringObservable.getInstance().isUsbTetheringEnabled()),
                new DialogListAdapter.ViewModel(getContext().getResources().getDrawable(R.drawable.ic_bluetooth),
                        getContext().getString(R.string.tethering_bluetooth),
                        PreferenceHelper.isBluetoothTetheringAllowed(getContext()),
                        TetheringObservable.getInstance().isUsbTetheringEnabled())
        };
    }

    @Override
    public void update(Observable o, Object arg) {
        if (o instanceof TetheringObservable) {
            TetheringObservable observable = (TetheringObservable) o;
            Log.d(TAG, "TetheringObservable is updated");
            dataset[0].enabled = observable.isWifiTetheringEnabled();
            dataset[1].enabled = observable.isUsbTetheringEnabled();
            dataset[2].enabled = observable.isBluetoothTetheringEnabled();
            adapter.notifyDataSetChanged();
        }
    }

}
