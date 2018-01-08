package se.leap.bitmaskclient.userstatus;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.jetbrains.annotations.NotNull;

import java.util.Observable;
import java.util.Observer;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import se.leap.bitmaskclient.Dashboard;
import se.leap.bitmaskclient.MainActivity;
import se.leap.bitmaskclient.Provider;
import se.leap.bitmaskclient.ProviderAPI;
import se.leap.bitmaskclient.ProviderAPICommand;
import se.leap.bitmaskclient.ProviderAPIResultReceiver;
import se.leap.bitmaskclient.R;

public class UserStatusFragment extends Fragment implements Observer, SessionDialog.SessionDialogInterface {

    public static String TAG = UserStatusFragment.class.getSimpleName();
    private ProviderAPIResultReceiver providerAPI_result_receiver;

    @InjectView(R.id.user_status_username)
    TextView username;
    @InjectView(R.id.user_status_icon)
    FabButton icon;
    @InjectView(R.id.user_status_button)
    Button button;

    private UserStatus status;
    private boolean allows_registration = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        status = UserStatus.getInstance(getResources());
        status.addObserver(this);
    }

    @Override
    public void onSaveInstanceState(@NotNull Bundle outState) {
        if (username != null && username.getVisibility() == TextView.VISIBLE)
            outState.putSerializable(UserStatus.TAG, status.sessionStatus());

        super.onSaveInstanceState(outState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.user_session_fragment, container, false);
        ButterKnife.inject(this, view);

        Bundle arguments = getArguments();
        allows_registration = arguments.getBoolean(Provider.ALLOW_REGISTRATION);
        handleNewStatus(status);

        return view;
    }

    @Override
    public void onAttach(Context activity) {
        super.onAttach(activity);
        providerAPI_result_receiver = new ProviderAPIResultReceiver(new Handler(), Dashboard.dashboardReceiver);
    }

    public void restoreSessionStatus(Bundle savedInstanceState) {
        if (savedInstanceState != null)
            if (savedInstanceState.containsKey(UserStatus.TAG)) {
                UserStatus.SessionStatus status = (UserStatus.SessionStatus) savedInstanceState.getSerializable(UserStatus.TAG);
                UserStatus.updateStatus(status, getResources());
            }
    }

    @OnClick(R.id.user_status_button)
    public void handleButton() {
        android.util.Log.d(TAG, status.toString());
        if(status.isLoggedIn())
            logOut();
        else if(status.isLoggedOut())
            MainActivity.sessionDialog(Bundle.EMPTY);
        else if(status.inProgress())
            cancelLoginOrSignup();
    }

    @Override
    public void update(Observable observable, Object data) {
        if (observable instanceof UserStatus) {
            final UserStatus status = (UserStatus) observable;
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    handleNewStatus(status);
                }
            });
        }
    }

    private void handleNewStatus(UserStatus status) {
        this.status = status;
        if (allows_registration) {
            if (this.status.inProgress())
                showUserSessionProgressBar();
            else
                hideUserSessionProgressBar();
            changeMessage();
            updateButton();
        }
    }

    private void showUserSessionProgressBar() {
        icon.showProgress(true);
    }

    private void hideUserSessionProgressBar() {
        icon.showProgress(false);
    }

    private void changeMessage() {
        final String message = User.userName();
        username.setText(message);
    }

    private void updateButton() {
        if(status.isLoggedIn() || status.didntLogOut())
            button.setText(getActivity().getString(R.string.logout_button));
        else if(allows_registration) {
            if (status.isLoggedOut() || status.notLoggedIn())
                button.setText(getActivity().getString(R.string.login_button));
            else if (status.inProgress())
                button.setText(getActivity().getString(android.R.string.cancel));
        }
    }


    @Override
    public void signUp(String username, String password) {
        User.setUserName(username);
        Bundle parameters = bundlePassword(password);
        ProviderAPICommand.execute(parameters, ProviderAPI.SIGN_UP, providerAPI_result_receiver);
    }

    @Override
    public void logIn(String username, String password) {
        User.setUserName(username);
        Bundle parameters = bundlePassword(password);
        ProviderAPICommand.execute(parameters, ProviderAPI.LOG_IN, providerAPI_result_receiver);
    }

    public void logOut() {
        android.util.Log.d(TAG, "Log out");
        ProviderAPICommand.execute(Bundle.EMPTY, ProviderAPI.LOG_OUT, providerAPI_result_receiver);
    }

    public void cancelLoginOrSignup() {
        //EipStatus.getInstance().setConnectedOrDisconnected();
    }

    private Bundle bundlePassword(String password) {
        Bundle parameters = new Bundle();
        if (!password.isEmpty())
            parameters.putString(SessionDialog.PASSWORD, password);
        return parameters;
    }
}
