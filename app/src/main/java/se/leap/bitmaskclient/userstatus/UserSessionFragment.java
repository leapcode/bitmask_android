package se.leap.bitmaskclient.userstatus;

import android.app.*;
import android.os.*;
import android.view.*;
import android.widget.*;

import org.jetbrains.annotations.NotNull;

import java.util.*;

import butterknife.*;
import se.leap.bitmaskclient.*;
import se.leap.bitmaskclient.eip.EipStatus;

public class UserSessionFragment extends Fragment implements Observer, SessionDialog.SessionDialogInterface {

    private static View view;

    public static String TAG = UserSessionFragment.class.getSimpleName();
    private static Dashboard dashboard;
    private ProviderAPIResultReceiver providerAPI_result_receiver;

    @InjectView(R.id.user_session_status)
    TextView user_session_status_text_view;
    @InjectView(R.id.user_session_status_progress)
    ProgressBar user_session_status_progress_bar;
    @InjectView(R.id.user_session_button)
    Button main_button;

    private UserSessionStatus user_session_status;
    private boolean allows_registration = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        user_session_status = UserSessionStatus.getInstance(getResources());
        user_session_status.addObserver(this);

        handleNewUserSessionStatus(user_session_status);
    }

    @Override
    public void onSaveInstanceState(@NotNull Bundle outState) {
        if (user_session_status_text_view != null && user_session_status_text_view.getVisibility() == TextView.VISIBLE)
            outState.putSerializable(UserSessionStatus.TAG, user_session_status.sessionStatus());

        super.onSaveInstanceState(outState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_user_session, container, false);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Fragment fragment = (getFragmentManager().findFragmentById(R.id.user_session_fragment));
        FragmentTransaction ft = getActivity().getFragmentManager().beginTransaction();
        ft.remove(fragment);
        ft.commit();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        dashboard = (Dashboard) activity;

        providerAPI_result_receiver = new ProviderAPIResultReceiver(new Handler());
        providerAPI_result_receiver.setReceiver(dashboard);
    }

    public void restoreSessionStatus(Bundle savedInstanceState) {
        if (savedInstanceState != null)
            if (savedInstanceState.containsKey(UserSessionStatus.TAG)) {
                UserSessionStatus.SessionStatus status = (UserSessionStatus.SessionStatus) savedInstanceState.getSerializable(UserSessionStatus.TAG);
                user_session_status.updateStatus(status, getResources());
            }
    }

    @OnClick(R.id.user_session_button)
    public void handleMainButton() {
        if(user_session_status.isLoggedIn())
            logOut();
        else if(user_session_status.isLoggedOut())
            dashboard.sessionDialog(Bundle.EMPTY);
        else if(user_session_status.inProgress())
            cancelLoginOrSignup();
    }

    @Override
    public void update(Observable observable, Object data) {
        if (observable instanceof UserSessionStatus) {
            UserSessionStatus status = (UserSessionStatus) observable;
            handleNewUserSessionStatus(status);
        }
    }

    private void handleNewUserSessionStatus(UserSessionStatus status) {
        user_session_status = status;
        if (allows_registration) {
            if (user_session_status.inProgress())
                showUserSessionProgressBar();
            else
                hideUserSessionProgressBar();
            changeSessionStatusMessage();
            updateButton();
        }
    }

    private void showUserSessionProgressBar() {
        dashboard.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                user_session_status_progress_bar.setVisibility(ProgressBar.VISIBLE);
            }
        });
    }

    private void hideUserSessionProgressBar() {
        dashboard.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                user_session_status_progress_bar.setVisibility(ProgressBar.GONE);
            }
        });
    }

    private void changeSessionStatusMessage() {
        final String message = user_session_status.toString();
        dashboard.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                user_session_status_text_view.setText(message);
            }
        });
    }

    private void updateButton() {
        if(User.loggedIn())
            main_button.setText(getString(R.string.logout_button));
        else if(allows_registration)
            main_button.setText(getString(R.string.login_button));
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
        ProviderAPICommand.execute(Bundle.EMPTY, ProviderAPI.LOG_OUT, providerAPI_result_receiver);
    }

    public void cancelLoginOrSignup() {
        EipStatus.getInstance().setConnectedOrDisconnected();
    }

    private Bundle bundlePassword(String password) {
        Bundle parameters = new Bundle();
        if (!password.isEmpty())
            parameters.putString(SessionDialog.PASSWORD, password);
        return parameters;
    }
}
