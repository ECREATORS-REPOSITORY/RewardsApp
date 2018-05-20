
package com.mysampleapp;
import android.content.DialogInterface;
import android.text.Editable;
import android.text.TextWatcher;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoDevice;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.amazonaws.mobile.auth.core.DefaultSignInResultHandler;
import com.amazonaws.mobile.auth.core.IdentityManager;
import com.amazonaws.mobile.auth.core.IdentityProvider;
import com.amazonaws.mobile.auth.core.signin.ui.buttons.SignInButton;
import com.amazonaws.mobile.auth.ui.SignInActivity;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUser;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserSession;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationDetails;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.ChallengeContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.ChooseMfaContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.ForgotPasswordContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.MultiFactorAuthenticationContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.AuthenticationHandler;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.ForgotPasswordHandler;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.NewPasswordContinuation;
import com.mysampleapp.demo.DemoConfiguration;
import com.mysampleapp.demo.HomeDemoFragment;
import com.mysampleapp.navigation.NavigationDrawer;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements  View.OnClickListener {
    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    private final String TAG = "MainActivity";
    private NavigationDrawer navigationDrawer;
    private NavigationView nDrawer;
    private DrawerLayout mDrawer;
    private static final String BUNDLE_KEY_TOOLBAR_TITLE = "title";
    private IdentityManager identityManager;
    private Toolbar toolbar;
    private AlertDialog userDialog;
    private ProgressDialog waitDialog;
    private EditText inUsername;
    private EditText inPassword;
    private String username;
    private String password;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        identityManager = IdentityManager.getDefaultIdentityManager();
        setContentView(R.layout.activity_main);
        setupToolbar(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setupNavigationMenu(savedInstanceState);
        mDrawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawer, toolbar, R.string.nav_drawer_open, R.string.nav_drawer_close);
        mDrawer.addDrawerListener(mDrawerToggle);
        mDrawerToggle.syncState();
        nDrawer = (NavigationView) findViewById(R.id.nav_view);
        setNavDrawer();
        AppHelper.init(getApplicationContext());
        initApp();
        findCurrent();
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    @Override
    public void onPostCreate(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
        super.onPostCreate(savedInstanceState, persistentState);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case 1:
                if (resultCode == RESULT_OK) {
                    String name = data.getStringExtra("name");
                    if (!name.isEmpty()) {
                        inUsername.setText(name);
                        inPassword.setText("");
                        inPassword.requestFocus();
                    }
                    String userPasswd = data.getStringExtra("password");
                    if (!userPasswd.isEmpty()) {
                        inPassword.setText(userPasswd);
                    }
                    if (!name.isEmpty() && !userPasswd.isEmpty()) {
                        username = name;
                        password = userPasswd;
                        AppHelper.getPool().getUser(username).getSessionInBackground(authenticationHandler);
                    }
                }
                break;
            case 2:
                if (resultCode == RESULT_OK) {
                    String name = data.getStringExtra("name");
                    if (!name.isEmpty()) {
                        inUsername.setText(name);
                        inPassword.setText("");
                        inPassword.requestFocus();
                    }
                }
                break;
            case 3:
                if (resultCode == RESULT_OK) {
                    String newPass = data.getStringExtra("newPass");
                    String code = data.getStringExtra("code");
                    if (newPass != null && code != null) {
                        if (!newPass.isEmpty() && !code.isEmpty()) {
                            showWaitDialog("Setting new password...");
                            forgotPasswordContinuation.setPassword(newPass);
                            forgotPasswordContinuation.setVerificationCode(code);
                            forgotPasswordContinuation.continueTask();
                        }
                    }
                }
                break;
            case 4:
                if (resultCode == RESULT_OK) {
                    clearInput();
                    String name = data.getStringExtra("TODO");
                    if (name != null) {
                        if (!name.isEmpty()) {
                            name.equals("exit");
                            onBackPressed();
                        }
                    }
                }
                break;
            case 5:
                closeWaitDialog();
                if (resultCode == RESULT_OK) {
                    String code = data.getStringExtra("mfacode");
                    if (code != null) {
                        if (code.length() > 0) {
                            showWaitDialog("Sign+ing in...");
                            multiFactorAuthenticationContinuation.setMfaCode(code);
                            multiFactorAuthenticationContinuation.continueTask();
                        } else {
                            inPassword.setText("");
                            inPassword.requestFocus();
                        }
                    }
                }
                break;
            case 6:
                //New password
                closeWaitDialog();
                Boolean continueSignIn = false;
                if (resultCode == RESULT_OK) {
                    continueSignIn = data.getBooleanExtra("continueSignIn", false);
                }
                if (continueSignIn) {
                    continueWithFirstTimeSignIn();
                }
                break;
            case 7:
                closeWaitDialog();
                if (resultCode == RESULT_OK) {
                    String option = data.getStringExtra("mfaOption");
                    if (option != null) {
                        if (option.length() > 0) {
                            Log.d(TAG, " -- Selected Option: " + option);
                            conitnueWithSelectedMfa(option);
                        } else {
                            inPassword.setText("");
                            inPassword.requestFocus();
                            TextView label = (TextView) findViewById(R.id.textViewUserPasswordMessage);
                            label.setText("Login cancelled");
                        }
                    }
                }
        }
    }
    public void signUp(View view) {
        signUpNewUser();
    }
    public void logIn(View view) {
        signInUser();
    }
    public void forgotPassword(View view) {
        forgotpasswordUser();
    }
   private void setNavDrawer() {
        nDrawer.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {
                performAction(item);
                return true;
            }
        });
    }
    private void performAction(MenuItem item) {
        mDrawer.closeDrawers();
        switch (item.getItemId()) {
            case R.id.nav_sign_up:
                signUpNewUser();
                break;
            case R.id.nav_sign_up_confirm:
                confirmUser();
                break;
            case R.id.nav_sign_in_forgot_password:
                forgotpasswordUser();
                break;
            case R.id.nav_about:
                Intent aboutAppActivity = new Intent(this, AboutApp.class);
                startActivity(aboutAppActivity);
                break; } }
    private void signUpNewUser() {
        Intent registerActivity = new Intent(this, RegisterUser.class);
        startActivityForResult(registerActivity, 1);
    }
    private void signInUser() {
        username = inUsername.getText().toString();
        if (username == null || username.length() < 1) {
            TextView label = (TextView) findViewById(R.id.textViewUserIdMessage);
            label.setText(inUsername.getHint() + " cannot be empty");
            inUsername.setBackground(getDrawable(R.drawable.text_border_error));
            return;
        }

        AppHelper.setUser(username);
        password = inPassword.getText().toString();
        if (password == null || password.length() < 1) {
            TextView label = (TextView) findViewById(R.id.textViewUserPasswordMessage);
            label.setText(inPassword.getHint() + " cannot be empty");
            inPassword.setBackground(getDrawable(R.drawable.text_border_error));
            return;
        }
        showWaitDialog("Signing in...");
        AppHelper.getPool().getUser(username).getSessionInBackground(authenticationHandler);
    }
    private void forgotpasswordUser() {
        username = inUsername.getText().toString();
        if (username == null) {
            TextView label = (TextView) findViewById(R.id.textViewUserIdMessage);
            label.setText(inUsername.getHint() + " cannot be empty");
            inUsername.setBackground(getDrawable(R.drawable.text_border_error));
            return;
        }        if (username.length() < 1) {
            TextView label = (TextView) findViewById(R.id.textViewUserIdMessage);
            label.setText(inUsername.getHint() + " cannot be empty");
            inUsername.setBackground(getDrawable(R.drawable.text_border_error));
            return;
        } showWaitDialog("");
        AppHelper.getPool().getUser(username).forgotPasswordInBackground(forgotPasswordHandler);
    }
    private void getForgotPasswordCode(ForgotPasswordContinuation forgotPasswordContinuation) {
        this.forgotPasswordContinuation = forgotPasswordContinuation;
        Intent intent = new Intent(this, ForgotPasswordActivity.class);
        intent.putExtra("destination", forgotPasswordContinuation.getParameters().getDestination());
        intent.putExtra("deliveryMed", forgotPasswordContinuation.getParameters().getDeliveryMedium());
        startActivityForResult(intent, 3);
    }
    private void selectMfaToSignIn(List <String> options, Map <String, String> parameters) {
        Intent chooseMfaActivity = new Intent(this, ChooseMFA.class);
        AppHelper.setMfaOptionsForDisplay(options, parameters);
        startActivityForResult(chooseMfaActivity, 7);
    }
 private void conitnueWithSelectedMfa(String option) {
        mfaOptionsContinuation.setMfaOption(option);
        mfaOptionsContinuation.continueTask();
    }

    private void continueWithFirstTimeSignIn() {
        newPasswordContinuation.setPassword(AppHelper.getPasswordForFirstTimeLogin());
        Map <String, String> newAttributes = AppHelper.getUserAttributesForFirstTimeLogin();
        if (newAttributes != null) {
            for (Map.Entry <String, String> attr : newAttributes.entrySet()) {
                Log.d(TAG, String.format(" -- Adding attribute: %s, %s", attr.getKey(), attr.getValue()));
                newPasswordContinuation.setUserAttribute(attr.getKey(), attr.getValue());
            }
        }
        try {
            newPasswordContinuation.continueTask();
        } catch (Exception e) {
            closeWaitDialog();
            TextView label = (TextView) findViewById(R.id.textViewUserIdMessage);
            label.setText("Sign-in failed");
            inPassword.setBackground(getDrawable(R.drawable.text_border_error));
            label = (TextView) findViewById(R.id.textViewUserIdMessage);
            label.setText("Sign-in failed");
            inUsername.setBackground(getDrawable(R.drawable.text_border_error));
            showDialogMessage("Sign-in failed", AppHelper.formatException(e));
        }
    }
    private void confirmUser() {
        Intent confirmActivity = new Intent(this, SignUpConfirm.class);
        confirmActivity.putExtra("source", "main");
        startActivityForResult(confirmActivity, 2);
        }
    private void launchUser() {
        Intent userActivity = new Intent(this, UserActivity.class);
        userActivity.putExtra("name", username);
        startActivityForResult(userActivity, 4);
    }
    private void findCurrent() {
        CognitoUser user = AppHelper.getPool().getCurrentUser();
        username = user.getUserId();
        if (username != null) {
            AppHelper.setUser(username);
            inUsername.setText(user.getUserId());
            user.getSessionInBackground(authenticationHandler);
        }
    }
    private void getUserAuthentication(AuthenticationContinuation continuation, String username) {
        if (username != null) {
            this.username = username;
            AppHelper.setUser(username);
        }
        if (this.password == null) {
            inUsername.setText(username);
            password = inPassword.getText().toString();
            if (password == null) {
                TextView label = (TextView) findViewById(R.id.textViewUserPasswordMessage);
                label.setText(inPassword.getHint() + " enter password");
                inPassword.setBackground(getDrawable(R.drawable.text_border_error));
                return;
            }
            if (password.length() < 1) {
                TextView label = (TextView) findViewById(R.id.textViewUserPasswordMessage);
                label.setText(inPassword.getHint() + " enter password");
                inPassword.setBackground(getDrawable(R.drawable.text_border_error));
                return;
            }
        }
        AuthenticationDetails authenticationDetails = new AuthenticationDetails(this.username, password, null);
        continuation.setAuthenticationDetails(authenticationDetails);
        continuation.continueTask();
    }

    private void initApp() {
        inUsername = (EditText) findViewById(R.id.editTextUserId);
        inUsername.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (s.length() == 0) {
                    TextView label = (TextView) findViewById(R.id.textViewUserIdLabel);
                    label.setText(R.string.Username);
                    inUsername.setBackground(getDrawable(R.drawable.text_border_selector));
                }
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                TextView label = (TextView) findViewById(R.id.textViewUserIdMessage);
                label.setText("");
            }
            public void afterTextChanged(Editable s) {
                if (s.length() == 0) {
                    TextView label = (TextView) findViewById(R.id.textViewUserIdLabel);
                    label.setText(""); } }});
        inPassword = (EditText) findViewById(R.id.editTextUserPassword);
        inPassword.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (s.length() == 0) {
                    TextView label = (TextView) findViewById(R.id.textViewUserPasswordLabel);
                    label.setText(R.string.Password);
                    inPassword.setBackground(getDrawable(R.drawable.text_border_selector));
                }
            }
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                TextView label = (TextView) findViewById(R.id.textViewUserPasswordMessage);
                label.setText("");
            }
            public void afterTextChanged(Editable s) {
                if (s.length() == 0) {
                    TextView label = (TextView) findViewById(R.id.textViewUserPasswordLabel);
                    label.setText("");
                }
            }
        });
    }

    ForgotPasswordHandler forgotPasswordHandler = new ForgotPasswordHandler(){
        @Override
        public void onSuccess() {
            closeWaitDialog();
            showDialogMessage("Password successfully changed!","");
            inPassword.setText("");
            inPassword.requestFocus();
        }

        @Override
        public void getResetCode(ForgotPasswordContinuation forgotPasswordContinuation) {
            closeWaitDialog();
            getForgotPasswordCode(forgotPasswordContinuation);
        }

        @Override
        public void onFailure(Exception e) {
            closeWaitDialog();
            showDialogMessage("Forgot password failed",AppHelper.formatException(e));
        }
    };

    AuthenticationHandler authenticationHandler = new AuthenticationHandler() {

        public void onSuccess(CognitoUserSession cognitoUserSession, CognitoDevice device) {
            Log.d(TAG, " -- Auth Success");
            AppHelper.setCurrSession(cognitoUserSession);
            AppHelper.newDevice(device);
            closeWaitDialog();
            launchUser();
        }

        //@Override
        //public void getAuthenticationDetails(AuthenticationContinuation authenticationContinuation, String userId) {

        //}

        @Override
        public void getAuthenticationDetails(AuthenticationContinuation authenticationContinuation, String username) {
            closeWaitDialog();
            Locale.setDefault(Locale.US);
            getUserAuthentication(authenticationContinuation, username);
        }
        @Override
        public void getMFACode(MultiFactorAuthenticationContinuation continuation) {

        }
        @Override
        public void onFailure(Exception e) {
            closeWaitDialog();
            TextView label = (TextView) findViewById(R.id.textViewUserIdMessage);
            label.setText("Sign-in failed");
            inPassword.setBackground(getDrawable(R.drawable.text_border_error));

            label = (TextView) findViewById(R.id.textViewUserIdMessage);
            label.setText("Sign-in failed");
            inUsername.setBackground(getDrawable(R.drawable.text_border_error));

            showDialogMessage("Sign-in failed", AppHelper.formatException(e));
        }

        @Override
        public void authenticationChallenge(ChallengeContinuation continuation) {

            if ("NEW_PASSWORD_REQUIRED".equals(continuation.getChallengeName())) {
                newPasswordContinuation = (NewPasswordContinuation) continuation;
                AppHelper.setUserAttributeForDisplayFirstLogIn(newPasswordContinuation.getCurrentUserAttributes(),
                        newPasswordContinuation.getRequiredAttributes());
                closeWaitDialog();
                firstTimeSignIn();
            } else if ("SELECT_MFA_TYPE".equals(continuation.getChallengeName())) {
                closeWaitDialog();
                mfaOptionsContinuation = (ChooseMfaContinuation) continuation;
                List <String> mfaOptions = mfaOptionsContinuation.getMfaOptions();
                selectMfaToSignIn(mfaOptions, continuation.getParameters());
            }
        }
    };

    private void clearInput() {
        if (inUsername == null) {
            inUsername = (EditText) findViewById(R.id.editTextUserId);
        }

        if (inPassword == null) {
            inPassword = (EditText) findViewById(R.id.editTextUserPassword);
        }

        inUsername.setText("");
        inUsername.requestFocus();
        inUsername.setBackground(getDrawable(R.drawable.text_border_selector));
        inPassword.setText("");
        inPassword.setBackground(getDrawable(R.drawable.text_border_selector));
    }

    private void showWaitDialog(String message) {
        closeWaitDialog();
        waitDialog = new ProgressDialog(this);
        waitDialog.setTitle(message);
        waitDialog.show();
    }

    private void showDialogMessage(String title, String body) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title).setMessage(body).setNeutralButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    userDialog.dismiss();
                } catch (Exception e) {
                    //
                }
            }
        });
        userDialog = builder.create();
        userDialog.show();
    }

    private void closeWaitDialog() {
        try {
            waitDialog.dismiss();
        } catch (Exception e) {
            //
        }
    }


    private ActionBarDrawerToggle drawerToggle;

    private Bundle fragmentBundle;

    private Button signOutButton;
    private Button signInButton;

    private MultiFactorAuthenticationContinuation multiFactorAuthenticationContinuation;
    private ForgotPasswordContinuation forgotPasswordContinuation;
    private NewPasswordContinuation newPasswordContinuation;
    private ChooseMfaContinuation mfaOptionsContinuation;
    private ActionBarDrawerToggle mDrawerToggle;


    private void setupToolbar(final Bundle savedInstanceState) {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (savedInstanceState != null) {
            assert getSupportActionBar() != null;
            getSupportActionBar().setTitle(
                    savedInstanceState.getCharSequence(BUNDLE_KEY_TOOLBAR_TITLE));
        }
    }

    private void setupSignInButtons() {

        signOutButton = (Button) findViewById(R.id.button_signout);
        signOutButton.setOnClickListener(this);

        signInButton = (Button) findViewById(R.id.button_signin);
        signInButton.setOnClickListener(this);
        final boolean isUserSignedIn = identityManager.isUserSignedIn();
        signOutButton.setVisibility(isUserSignedIn ? View.VISIBLE : View.INVISIBLE);
        signInButton.setVisibility(!isUserSignedIn ? View.VISIBLE : View.INVISIBLE);

    }

  private void setupNavigationMenu(final Bundle savedInstanceState) {
        final DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        final ListView drawerItems = (ListView) findViewById(R.id.nav_drawer_items);
//
//        // Create the navigation drawer.
        navigationDrawer = new NavigationDrawer(this, toolbar, drawerLayout, drawerItems,
                R.id.main_fragment_container);
//
//        // Add navigation drawer menu items.
//        // Home isn't a demo, but is fake as a demo.
        DemoConfiguration.DemoFeature home = new DemoConfiguration.DemoFeature();
        home.iconResId = R.mipmap.icon_home;
        home.titleResId = R.string.main_nav_menu_item_home;
        navigationDrawer.addDemoFeatureToMenu(home);

        for (DemoConfiguration.DemoFeature demoFeature : DemoConfiguration.getDemoFeatureList()) {
            navigationDrawer.addDemoFeatureToMenu(demoFeature);
        }
        if (savedInstanceState == null) {
            navigationDrawer.showHome();
        }
    }
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        return super.onContextItemSelected(item);
    }
    @Override
    protected void onResume() {
        super.onResume();
        setupSignInButtons();
    }

    @Override
    protected void onSaveInstanceState(final Bundle bundle) {
        super.onSaveInstanceState(bundle);
        if (toolbar != null) {
            bundle.putCharSequence(BUNDLE_KEY_TOOLBAR_TITLE, toolbar.getTitle());
        }
    }

    @Override
    public void onClick(final View view) {
        if (view == signOutButton) {
            identityManager.signOut();
            identityManager.getUserID(null);
            signOutButton.setVisibility(View.INVISIBLE);
            signInButton.setVisibility(View.VISIBLE);
           navigationDrawer.closeDrawer();
            return;
        }
        if (view == signInButton) {
            final WeakReference <MainActivity> self = new WeakReference <MainActivity>(this);

            identityManager.setUpToAuthenticate(this, new DefaultSignInResultHandler() {

                @Override
                public void onSuccess(Activity activity, IdentityProvider identityProvider) {
                    // User has signed in
                    Log.e("Success", "User signed in");
                    activity.finish();
                }

                @Override
                public boolean onCancel(Activity activity) {
                    return true;
                }
            });
            SignInActivity.startSignInActivity(this, Application.sAuthUIConfiguration);
            navigationDrawer.closeDrawer();
            return;
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    public void setFragmentBundle(final Bundle fragmentBundle) {
        this.fragmentBundle = fragmentBundle;
    }

    public Bundle getFragmentBundle() {
        return this.fragmentBundle;
    }

    private void firstTimeSignIn() {
        Intent newPasswordActivity = new Intent(this, NewPassword.class);
        startActivityForResult(newPasswordActivity, 6);
  }

}