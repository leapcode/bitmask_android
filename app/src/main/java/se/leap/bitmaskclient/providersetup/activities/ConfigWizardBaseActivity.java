package se.leap.bitmaskclient.providersetup.activities;

import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.Guideline;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Observable;
import java.util.Observer;

import butterknife.BindView;
import se.leap.bitmaskclient.R;
import se.leap.bitmaskclient.base.models.Provider;
import se.leap.bitmaskclient.base.views.ProviderHeaderView;
import se.leap.bitmaskclient.tor.TorStatusObservable;

import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE;
import static se.leap.bitmaskclient.base.models.Constants.PROVIDER_KEY;
import static se.leap.bitmaskclient.base.models.Constants.SHARED_PREFERENCES;
import static se.leap.bitmaskclient.tor.TorStatusObservable.getBootstrapProgress;
import static se.leap.bitmaskclient.tor.TorStatusObservable.getLastLogs;
import static se.leap.bitmaskclient.tor.TorStatusObservable.getLastSnowflakeLog;
import static se.leap.bitmaskclient.tor.TorStatusObservable.getLastTorLog;
import static se.leap.bitmaskclient.tor.TorStatusObservable.getStringForCurrentStatus;

/**
 * Base Activity for configuration wizard activities
 *
 * Created by fupduck on 09.01.18.
 */

public abstract class ConfigWizardBaseActivity extends ButterKnifeActivity implements Observer {

    private static final String TAG = ConfigWizardBaseActivity.class.getName();
    public static final float GUIDE_LINE_COMPACT_DELTA = 0.1f;
    protected SharedPreferences preferences;

    @BindView(R.id.header)
    ProviderHeaderView providerHeaderView;

    //Add provider screen has no loading screen
    @Nullable
    @BindView(R.id.loading_screen)
    protected LinearLayout loadingScreen;

    @Nullable
    @BindView(R.id.btn_connection_detail)
    protected AppCompatTextView connectionDetailBtn;

    @Nullable
    @BindView(R.id.connection_detail_header_container)
    protected RelativeLayout connectionDetailHeaderContainer;

    @Nullable
    @BindView(R.id.connection_details_title)
    protected AppCompatTextView connectionDetailsTitle;

    @Nullable
    @BindView(R.id.connection_detail_container)
    protected RelativeLayout connectionDetailContainer;

    @Nullable
    @BindView(R.id.log_container)
    protected RelativeLayout logsContainer;

    @Nullable
    @BindView(R.id.tor_state)
    protected AppCompatTextView torState;

    @Nullable
    @BindView(R.id.snowflake_state)
    protected AppCompatTextView snowflakeState;

    @Nullable
    @BindView(R.id.connection_detail_logs)
    protected RecyclerView connectionDetailLogs;

    private TorLogAdapter torLogAdapter;

    @Nullable
    @BindView(R.id.progressbar)
    protected ProgressBar progressBar;

    @Nullable
    @BindView(R.id.progressbar_title)
    protected AppCompatTextView progressbarTitle;

    @Nullable
    @BindView(R.id.progressbar_description)
    protected AppCompatTextView progressbarDescription;

    //Only tablet layouts have guidelines as they are based on a ConstraintLayout
    @Nullable
    @BindView(R.id.guideline_top)
    protected Guideline guideline_top;

    @Nullable
    @BindView(R.id.guideline_bottom)
    protected Guideline guideline_bottom;

    @BindView(R.id.content)
    protected LinearLayout content;

    protected Provider provider;

    protected boolean isCompactLayout = false;
    protected boolean isActivityShowing;

    private float defaultGuidelineTopPercentage;
    private float defaultGuidelineBottomPercentage;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE);
        provider = getIntent().getParcelableExtra(PROVIDER_KEY);
    }

    @Override
    public void setContentView(View view) {
        super.setContentView(view);
        initContentView();
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        initContentView();
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        super.setContentView(view, params);
        initContentView();
    }

    private void initContentView() {
        if (provider != null) {
            setProviderHeaderText(provider.getName());
        }
        setProgressbarColorForPreLollipop();
        setDefaultGuidelineValues();
        setGlobalLayoutChangeListener();
    }

    private void setDefaultGuidelineValues() {
        if (isTabletLayout()) {
            defaultGuidelineTopPercentage = ((ConstraintLayout.LayoutParams) guideline_top.getLayoutParams()).guidePercent;
            defaultGuidelineBottomPercentage = ((ConstraintLayout.LayoutParams) guideline_bottom.getLayoutParams()).guidePercent;
        }
    }

    private void setProgressbarColorForPreLollipop() {
        if (progressBar == null || Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return;
        }
        progressBar.getIndeterminateDrawable().setColorFilter(
                ContextCompat.getColor(this, R.color.colorPrimary),
                PorterDuff.Mode.SRC_IN);
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (provider != null) {
            outState.putParcelable(PROVIDER_KEY, provider);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        isActivityShowing = false;
        TorStatusObservable.getInstance().deleteObserver(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        isActivityShowing = true;
        TorStatusObservable.getInstance().addObserver(this);
        setProgressbarDescription(getStringForCurrentStatus(this));
    }

    protected void restoreState(Bundle savedInstanceState) {
        if (savedInstanceState != null && savedInstanceState.containsKey(PROVIDER_KEY)) {
            provider = savedInstanceState.getParcelable(PROVIDER_KEY);
        }
    }

    protected void setProviderHeaderLogo(@DrawableRes int providerHeaderLogo) {
        providerHeaderView.setLogo(providerHeaderLogo);
    }

    protected void setProviderHeaderText(String providerHeaderText) {
        providerHeaderView.setTitle(providerHeaderText);
    }

    protected void setProviderHeaderText(@StringRes int providerHeaderText) {
        providerHeaderView.setTitle(providerHeaderText);
    }

    protected void hideConnectionDetails() {
        if (loadingScreen == null) {
            return;
        }
        if (connectionDetailContainer.getVisibility() == VISIBLE) {
            connectionDetailBtn.setText(R.string.show_connection_details);
        }
        connectionDetailHeaderContainer.setVisibility(GONE);
        connectionDetailContainer.setVisibility(GONE);
        logsContainer.setVisibility(GONE);
    }

    protected void showConnectionDetails() {
        if (loadingScreen == null) {
            return;
        }
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        connectionDetailLogs.setLayoutManager(layoutManager);
        torLogAdapter = new TorLogAdapter(getLastLogs());
        connectionDetailLogs.setAdapter(torLogAdapter);

        connectionDetailLogs.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState != SCROLL_STATE_IDLE) {
                    torLogAdapter.postponeUpdate = true;
                } else if (newState == SCROLL_STATE_IDLE && getFirstVisibleItemPosion() == 0) {
                    torLogAdapter.postponeUpdate = false;
                }
            }
        });

        snowflakeState.setText(getLastSnowflakeLog());
        torState.setText(getLastTorLog());
        connectionDetailBtn.setOnClickListener(v -> {
            if (logsContainer.getVisibility() == VISIBLE) {
                logsContainer.setVisibility(GONE);
                connectionDetailContainer.setVisibility(GONE);
                connectionDetailsTitle.setVisibility(GONE);
                connectionDetailBtn.setText(R.string.show_connection_details);
            } else {
                logsContainer.setVisibility(VISIBLE);
                connectionDetailContainer.setVisibility(VISIBLE);
                connectionDetailsTitle.setVisibility(VISIBLE);
                connectionDetailBtn.setText(R.string.hide);
            }
        });
        connectionDetailHeaderContainer.setVisibility(VISIBLE);
    }

    private int getFirstVisibleItemPosion() {
        return ((LinearLayoutManager)connectionDetailLogs.getLayoutManager()).findFirstVisibleItemPosition();
    }

    protected void hideProgressBar() {
        if (loadingScreen == null) {
            return;
        }
        hideConnectionDetails();
        loadingScreen.setVisibility(GONE);
        content.setVisibility(VISIBLE);
    }

    protected void showProgressBar() {
        if (loadingScreen == null) {
            return;
        }
        content.setVisibility(GONE);
        loadingScreen.setVisibility(VISIBLE);
    }

    protected void setProgressbarTitle(@StringRes int progressbarTitle) {
        if (loadingScreen == null) {
            return;
        }
        this.progressbarTitle.setText(progressbarTitle);
    }

    protected void setProgressbarDescription(String progressbarDescription) {
        if (loadingScreen == null) {
            return;
        }
        this.progressbarDescription.setText(progressbarDescription);
    }

    protected void setConfigProgress(int value) {
        if (loadingScreen == null) {
            return;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            progressBar.setProgress(value);
        } else {
            progressBar.setProgress(value, true);
        }
        progressBar.setIndeterminate(value >= 100 || value < 0);
    }


    protected void showCompactLayout() {
        if (isCompactLayout) {
            return;
        }

        if (isTabletLayoutInLandscape() || isPhoneLayout()) {
            providerHeaderView.showCompactLayout();
        }

        showIncreasedTabletContentArea();
        isCompactLayout = true;
    }

    protected void showStandardLayout() {
        if (!isCompactLayout) {
            return;
        }
        providerHeaderView.showStandardLayout();
        showStandardTabletContentArea();
        isCompactLayout = false;
    }

    private boolean isTabletLayoutInLandscape() {
        // TabletLayout is based on a ConstraintLayout and uses Guidelines whereas the phone layout
        // has no such elements in it's layout xml file
        return  guideline_top != null &&
                guideline_bottom != null &&
                getResources().getConfiguration().orientation == ORIENTATION_LANDSCAPE;
    }

    protected boolean isPhoneLayout() {
        return guideline_top == null && guideline_bottom == null;
    }

    protected boolean isTabletLayout() {
        return guideline_top != null && guideline_bottom != null;
    }

    /**
     * Increases the white content area in tablet layouts
     */
    private void showIncreasedTabletContentArea() {
        if (isPhoneLayout()) {
            return;
        }
        ConstraintLayout.LayoutParams guideLineTopParams = (ConstraintLayout.LayoutParams) guideline_top.getLayoutParams();
        float increasedTopPercentage = defaultGuidelineTopPercentage - GUIDE_LINE_COMPACT_DELTA;
        guideLineTopParams.guidePercent = increasedTopPercentage > 0f ? increasedTopPercentage : 0f;
        guideline_top.setLayoutParams(guideLineTopParams);

        ConstraintLayout.LayoutParams guideLineBottomParams = (ConstraintLayout.LayoutParams) guideline_bottom.getLayoutParams();
        float increasedBottomPercentage = defaultGuidelineBottomPercentage + GUIDE_LINE_COMPACT_DELTA;
        guideLineBottomParams.guidePercent = increasedBottomPercentage < 1f ? increasedBottomPercentage : 1f;
        guideline_bottom.setLayoutParams(guideLineBottomParams);
    }

    /**
     * Restores the default size of the white content area in tablet layouts
     */
    private void showStandardTabletContentArea() {
        if (isPhoneLayout()) {
            return;
        }
        ConstraintLayout.LayoutParams guideLineTopParams = (ConstraintLayout.LayoutParams) guideline_top.getLayoutParams();
        guideLineTopParams.guidePercent = defaultGuidelineTopPercentage;
        guideline_top.setLayoutParams(guideLineTopParams);

        ConstraintLayout.LayoutParams guideLineBottomParams = (ConstraintLayout.LayoutParams) guideline_bottom.getLayoutParams();
        guideLineBottomParams.guidePercent = defaultGuidelineBottomPercentage;
        guideline_bottom.setLayoutParams(guideLineBottomParams);
    }

    /**
     * Checks if the keyboard is shown and switches between the standard layout and the compact layout
     */
    private void setGlobalLayoutChangeListener() {
        final View rootView = content.getRootView();
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Rect r = new Rect();
                //r will be populated with the coordinates of your view that area still visible.
                rootView.getWindowVisibleDisplayFrame(r);

                float deltaHiddenScreen =  1f - ((float) (r.bottom - r.top) / (float) rootView.getHeight());
                if (deltaHiddenScreen > 0.25f) {
                    // if more than 1/4 of the screen is hidden
                    showCompactLayout();
                } else {
                    showStandardLayout();
                }
            }
        });
    }

    @Override
    public void update(Observable o, Object arg) {
        if (o instanceof TorStatusObservable) {
            runOnUiThread(() -> {
                if (TorStatusObservable.getStatus() != TorStatusObservable.TorStatus.OFF && loadingScreen != null) {
                    if (connectionDetailContainer.getVisibility() == GONE) {
                        showConnectionDetails();
                    } else {
                        setLogs(getLastTorLog(), getLastSnowflakeLog(), getLastLogs());
                    }
                }
                setProgressbarDescription(getStringForCurrentStatus(ConfigWizardBaseActivity.this));
                setConfigProgress(getBootstrapProgress());
            });
        }
    }

    protected void setLogs(String torLog, String snowflakeLog, List<String> lastLogs) {
        if (loadingScreen == null) {
            return;
        }
        torLogAdapter.updateData(lastLogs);
        torState.setText(torLog);
        snowflakeState.setText(snowflakeLog);
    }

    static class TorLogAdapter extends RecyclerView.Adapter<TorLogAdapter.ViewHolder> {
        private List<String> values;
        private boolean postponeUpdate;

        static class ViewHolder extends RecyclerView.ViewHolder {
            public AppCompatTextView logTextLabel;
            public View layout;

            public ViewHolder(View v) {
                super(v);
                layout = v;
                logTextLabel = v.findViewById(android.R.id.text1);
            }
        }

        public void updateData(List<String> data) {
            values = data;
            if (!postponeUpdate) {
                notifyDataSetChanged();
            }
        }

        public TorLogAdapter(List<String> data) {
            values = data;
        }

        @NonNull
        @Override
        public TorLogAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(
                    parent.getContext());
            View v = inflater.inflate(R.layout.v_log_item, parent, false);
            return new TorLogAdapter.ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(TorLogAdapter.ViewHolder holder, final int position) {
            final String log = values.get(position);
            holder.logTextLabel.setText(log);
        }

        @Override
        public int getItemCount() {
            return values.size();
        }
    }
}
