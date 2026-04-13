/*
 * Copyright (c) 2012-2016 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package se.leap.bitmaskclient.base.fragments;

import static android.view.View.VISIBLE;
import static se.leap.bitmaskclient.R.string.exclude_apps_fragment_title;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Locale;
import java.util.Set;
import java.util.Vector;

import de.blinkt.openvpn.VpnProfile;
import se.leap.bitmaskclient.R;
import se.leap.bitmaskclient.base.utils.ApplicationInfoManager;
import se.leap.bitmaskclient.base.utils.PreferenceHelper;
import se.leap.bitmaskclient.base.utils.ViewHelper;
import se.leap.bitmaskclient.base.views.SimpleCheckBox;

/**
 * Created by arne on 16.11.14.
 */
public class ExcludeAppsFragment extends Fragment implements SimpleCheckBox.OnCheckedChangeListener {
    private static final String TAG = ExcludeAppsFragment.class.getSimpleName();
    private RecyclerView mRecyclerView;
    private LinearLayout emptyView;
    private VpnProfile mProfile;
    private PackageAdapter mListAdapter;

    private Set<String> apps;

    static class AppViewHolder extends RecyclerView.ViewHolder {
        public ApplicationInfo mInfo;
        public AppCompatTextView appName;
        public ImageView appIcon;
        public SimpleCheckBox checkBox;

        public AppViewHolder(@NonNull View itemView) {
            super(itemView);
            appName = itemView.findViewById(R.id.app_name);
            appIcon = itemView.findViewById(R.id.app_icon);
            checkBox = itemView.findViewById(R.id.app_selected);
            itemView.setOnClickListener(v -> checkBox.toggle());
        }

        public void bind(ApplicationInfo info, ApplicationInfoManager appInfoManager, Set<String> apps, SimpleCheckBox.OnCheckedChangeListener listener) {
            mInfo = info;
            CharSequence appLabelText = appInfoManager.loadLabel(info);
            if (TextUtils.isEmpty(appLabelText)) {
                appLabelText = info.packageName;
            }
            appName.setText(appLabelText);
            appIcon.setImageDrawable(appInfoManager.loadDrawable(info));
            checkBox.setTag(info.packageName);
            checkBox.setOnCheckedChangeListener(listener);
            checkBox.setChecked(apps.contains(info.packageName));
        }
    }

    @Override
    public void onCheckedChanged(SimpleCheckBox buttonView, boolean isChecked) {
        String packageName = (String) buttonView.getTag();

        if (isChecked) {
            Log.d("openvpn", "adding to allowed apps" + packageName);
            apps.add(packageName);

        } else {
            Log.d("openvpn", "removing from allowed apps" + packageName);
            apps.remove(packageName);
        }
    }

    class PackageAdapter extends RecyclerView.Adapter<AppViewHolder> implements Filterable {
        private Vector<ApplicationInfo> mPackages;
        private final LayoutInflater mInflater;
        private final ApplicationInfoManager mAppInfoManager;
        private final ItemFilter mFilter = new ItemFilter();
        private Vector<ApplicationInfo> mFilteredData;


        private class ItemFilter extends Filter {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {

                String filterString = constraint.toString().toLowerCase(Locale.getDefault());

                FilterResults results = new FilterResults();

                int count = mPackages.size();
                final Vector<ApplicationInfo> nlist = new Vector<>(count);

                for (int i = 0; i < count; i++) {
                    ApplicationInfo pInfo = mPackages.get(i);
                    CharSequence appName = mAppInfoManager.loadLabel(pInfo);

                    if (TextUtils.isEmpty(appName))
                        appName = pInfo.packageName;

                    if (appName instanceof String) {
                        if (((String) appName).toLowerCase(Locale.getDefault()).contains(filterString))
                            nlist.add(pInfo);
                    } else {
                        if (appName.toString().toLowerCase(Locale.getDefault()).contains(filterString))
                            nlist.add(pInfo);
                    }
                }
                results.values = nlist;
                results.count = nlist.size();

                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                mFilteredData = (Vector<ApplicationInfo>) results.values;
                notifyDataSetChanged();
            }
        }

        PackageAdapter(Context c, VpnProfile vp) {
            mAppInfoManager = new ApplicationInfoManager(c);
            mProfile = vp;
            mInflater = LayoutInflater.from(c);

            mPackages = new Vector<>();
            mFilteredData = mPackages;
        }
        @WorkerThread
        private void populateList(Activity c) {
            Vector<ApplicationInfo> appsList = mAppInfoManager.getApplicationInfos();
            mPackages = appsList;
            mFilteredData = appsList;
            c.runOnUiThread(() -> {
                if (!appsList.isEmpty()) {
                    mRecyclerView.setVisibility(VISIBLE);
                }
                this.notifyDataSetChanged();
            });
        }

        @Override
        public int getItemCount() {
            return mFilteredData.size();
        }

        @NonNull
        @Override
        public AppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = mInflater.inflate(R.layout.allowed_application_layout, parent, false);
            return new AppViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull AppViewHolder holder, int position) {
            ApplicationInfo mInfo = mFilteredData.get(position);
            holder.bind(mInfo, mAppInfoManager, apps, ExcludeAppsFragment.this);
        }

        @Override
        public Filter getFilter() {
            return mFilter;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroy() {
        PreferenceHelper.setExcludedApps(apps);
        super.onDestroy();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        apps = PreferenceHelper.getExcludedApps();

        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.allowed_apps, menu);
        SearchView searchView = (SearchView) menu.findItem(R.id.app_search_widget).getActionView();
        if (searchView != null) {
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    mListAdapter.getFilter().filter(query);
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    if (TextUtils.isEmpty(newText)) {
                        mListAdapter.getFilter().filter("");
                    } else {
                        mListAdapter.getFilter().filter(newText);
                    }
                    return true;
                }
            });
            searchView.setOnCloseListener(() -> {
                mListAdapter.getFilter().filter("");
                return false;
            });

            tintSearchViewChild(searchView);
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    private void tintSearchViewChild(ViewGroup view) {
        for (int i = 0; i < view.getChildCount(); i++) {
            View v = view.getChildAt(i);
            if (v instanceof ViewGroup) {
                tintSearchViewChild((ViewGroup) v);
            }
            if (v instanceof ImageView) {
                ((ImageView) v).setColorFilter(getResources().getColor(R.color.colorActionBarTitleFont),
                        android.graphics.PorterDuff.Mode.SRC_IN);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.allowed_vpn_apps, container, false);

        mRecyclerView = v.findViewById(R.id.list);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        emptyView = v.findViewById(R.id.loading_container);
        emptyView.setVisibility(VISIBLE);

        mListAdapter = new PackageAdapter(getActivity(), mProfile);
        mRecyclerView.setAdapter(mListAdapter);

        ViewHelper.setActionBarSubtitle(this, exclude_apps_fragment_title);

        new Thread(() -> mListAdapter.populateList(getActivity())).start();

        return v;
    }

    @Override
    public void onDestroyView() {
        Log.d(TAG, "onDestroyView");
        mRecyclerView.setAdapter(null);
        mListAdapter = null;
        super.onDestroyView();
    }
}