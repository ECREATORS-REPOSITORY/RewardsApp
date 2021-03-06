
package com.mysampleapp;

//import org.apache.http.client.ResponseHandler;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoDevice;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUser;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserAttributes;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserCodeDeliveryDetails;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserDetails;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserSession;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.GenericHandler;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.GetDetailsHandler;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.UpdateAttributesHandler;
import com.amazonaws.mobileconnectors.dynamodbv2.document.Table;
import com.amazonaws.mobileconnectors.dynamodbv2.document.datatype.Document;
import com.amazonaws.mobileconnectors.dynamodbv2.document.datatype.Primitive;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBQueryExpression;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.DescribeTableResult;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import xdroid.toaster.Toaster;

import static com.amazonaws.mobile.util.ThreadUtils.runOnUiThread;
import static com.amazonaws.services.kms.model.KeyManagerType.AWS;


public class UserActivity extends AppCompatActivity {

    protected static String TAG1 = "JSONServerNetworkUtil";
    public static final String PARAM_SESSION_ID = "sessid";
    public static final String PARAM_USERNAME = "name";
    public static final String PARAM_PASSWORD = "pass";

    public static final int REGISTRATION_TIMEOUT = 30 * 1000; // ms

    public static final String BASE_URL = "https://gcifiqyy8c.execute-api.ap-south-1.amazonaws.com/Testing";

    public static final String CONNECT_URI = BASE_URL + "/system/connect.json";
    public static final String AUTH_URI = BASE_URL;


  //  protected static HttpClient mHttpClient;
    protected static String mSessId;

    private final String TAG="UserActivity";
    private NavigationView nDrawer;
    private DrawerLayout mDrawer;
    private ActionBarDrawerToggle mDrawerToggle;
    private Toolbar toolbar;
    private AlertDialog userDialog;
    private ProgressDialog waitDialog;
    private ListView attributesList;

    // Cognito user objects
    private CognitoUser user;
    private CognitoUserSession session;
    private CognitoUserDetails details;
    private DynamoDBMapper mapper;
    // User details
    private String username;

    // To track changes to user details
    private final List<String> attributesToDelete = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Set toolbar for this screen
        toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        toolbar.setTitle("");
       // TextView main_title = (TextView) findViewById(R.id.main_toolbar_title);
        //Need to change Acoount name 
       // main_title.setText("Account");
        setSupportActionBar(toolbar);

        // Set navigation drawer for this screen
        mDrawer = (DrawerLayout) findViewById(R.id.user_drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(this,mDrawer, toolbar, R.string.nav_drawer_open, R.string.nav_drawer_close);
        mDrawer.addDrawerListener(mDrawerToggle);
        mDrawerToggle.syncState();

        nDrawer = (NavigationView) findViewById(R.id.nav_view);
        setNavDrawer();
        init();
        View navigationHeader = nDrawer.getHeaderView(0);
        TextView navHeaderSubTitle = (TextView) navigationHeader.findViewById(R.id.textViewNavUserSub);
        navHeaderSubTitle.setText(username);
       // Button bot=findViewById(R.id.viewReward);
        //bot.setOnClickListener("");
        /*AmazonDynamoDBClient dynamoDBClient = new AmazonDynamoDBClient(AWSMobileClient.getInstance().getCredentialsProvider());
        this.dynamoDBMapper = DynamoDBMapper.builder()
                .dynamoDBClient(dynamoDBClient)
                .awsConfiguration(AWSMobileClient.getInstance().getConfiguration())
                .build();*/

        /*AWS.config.credentials.get(function(){
            // Access AWS resources here.
            console.log('Access Token is '+ AWS.config.credentials.identityId);
            var s3= new AWS.s3();

        });*/


    }
    public void viewRewardHandler(final View target) {
        // Do stuff
        //Toast.makeText("","inside view rewards");
        //prepareAndSendHttpPost(AUTH_URI,NULL);
       // JSONObject json = prepareAndSendHttpPost(AUTH_URI, null);
       /* AWSLambdaClient client = new AWSLambdaClient();
        //client.withRegion(Regions.fromName(""));
        //client.reg
        InvokeRequest invokeRequest = new InvokeRequest()
                .withFunctionName("LoadRewardData");
        InvokeResult invokeResult = null;

        BasicAWSCredentials awsCreds = new BasicAWSCredentials("AKIAI22W7JDQIUEHNTQA", "pkz1p18otWgidgspDdubKphEX53/09WMyZf793P+");

        AWSLambda awsLambda = AWSLambdaClientBuilder.standard()
                .withRegion(Regions.AP_SOUTH_1)
                .withCredentials(new AWSStaticCredentialsProvider(awsCreds)).build();
        //Reques
        //AWSREQ
        try {
            invokeResult = awsLambda.invoke(invokeRequest);
        }
        catch (Exception e) {
            System.out.println(e);
        }

        System.out.println(invokeResult.getStatusCode());*/
        //rewardThread(target);
         final Handler handlerIntentStart = new Handler() {

            /*
             * (non-Javadoc)
             *
             * @see android.os.Handler#handleMessage(android.os.Message)
             */
            @Override
            public void handleMessage(Message msg) {

                // ****** Acitity class must be added in manifest
                Intent aboutAppActivity2 = new Intent(".AboutApp");
                startActivity(aboutAppActivity2);
            }

        };
        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {
                try  {
                    //Your code goes here
                    //DynamoDBQueryExpression<TESTFROMCODE> queryExpression = new DynamoDBQueryExpression<TESTFROMCODE>();
                    CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                            getApplicationContext(), "ap-south-1:e98809fd-9c8e-430f-be85-b4e2c5908fe9", Regions.AP_SOUTH_1);
                    AmazonDynamoDBClient dbClient = new AmazonDynamoDBClient(credentialsProvider);
                   // Toaster.toast(dbClient.getSignerRegionOverride());
                    dbClient.setRegion(Region.getRegion(Regions.AP_SOUTH_1));
                    DescribeTableResult res = dbClient.describeTable("ecreators_user_app_rewards");
                    Toaster.toast(res.getTable().getTableName());
                  //  Toast.makeText(getApplicationContext(),  "dbclient: " + dbClient, Toast.LENGTH_LONG).show();
                    // Table dbTable = Table.loadTable(dbClient, "Movies");
                    //dbTable.getItem(new Primitive("2013"), new Primitive("2 Guns"));
                    mapper = new DynamoDBMapper(dbClient);
                    UserRewards partitionKey = new UserRewards();

                    partitionKey.setUserId(user.getUserId());
                    DynamoDBQueryExpression<UserRewards> queryExpression = new DynamoDBQueryExpression<UserRewards>()
                            .withHashKeyValues(partitionKey);

                    List<UserRewards> itemList = mapper.query(UserRewards.class, queryExpression);

                    //  Start the activity based on urs and use Item list to render the data.
                    Intent aboutAppActivity2 = new Intent(UserActivity.this, AboutApp.class);
                    //Intent aboutAppActivity2 = new Intent(".AboutApp");
                    startActivity(aboutAppActivity2);
                } catch (Exception e) {
                  //  e.printStackTrace();
                    //handlerIntentStart.sendEmptyMessage(0);
                    Intent aboutAppActivity2 = new Intent(UserActivity.this, AboutApp.class);
                    startActivity(aboutAppActivity2);
                    Toaster.toast(e.getMessage());
                }
            }
        });
        thread.start();

    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_user_menu, menu);
        return true;
    }
    public void rewardThread(final View v){

        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {
                    try  {
                    //Your code goes here

                    DynamoDBQueryExpression<TESTFROMCODE> queryExpression = new DynamoDBQueryExpression<TESTFROMCODE>();
                        TESTFROMCODE     book = mapper.load(TESTFROMCODE.class, "Kona");
                    v.post(new Runnable() {
                        public void run() {
                        //    Toast.makeText(getApplicationContext(),  "dbclient: " + book, Toast.LENGTH_LONG).show();
                            //Intent aboutAppActivity2 = new Intent(this, AboutApp.class);
                           // startActivity(aboutAppActivity2);
                         //   Activity.runOnUiThread()
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        thread.start();
       /* Toast.makeText(getApplicationContext(),  "dbclient: " + book, Toast.LENGTH_LONG).show();
        Intent aboutAppActivity2 = new Intent(this, AboutApp.class);
        startActivity(aboutAppActivity2);*/
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Find which menu item was selected
        int menuItem = item.getItemId();

        // Do the task
        if(menuItem == R.id.user_update_attribute) {
            //updateAllAttributes();
            showWaitDialog("Updating...");
            getDetails();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        exit();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case 20:
                // Settings
                if(resultCode == RESULT_OK) {
                    boolean refresh = data.getBooleanExtra("refresh", true);
                    if (refresh) {
                       // showAttributes();
                    }
                }
                break;
            case 21:
                // Verify attributes
                if(resultCode == RESULT_OK) {
                    boolean refresh = data.getBooleanExtra("refresh", true);
                    if (refresh) {
                       // showAttributes();
                    }
                }
                break;
            case 22:
                // Add attributes
                if(resultCode == RESULT_OK) {
                    boolean refresh = data.getBooleanExtra("refresh", true);
                    if (refresh) {
                        //showAttributes();
                    }
                }
                break;
        }
    }

    // Handle when the a navigation item is selected
    private void setNavDrawer() {
        nDrawer.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {
                performAction(item);
                return true;
            }
        });
    }

    // Perform the action for the selected navigation item
    private void performAction(MenuItem item) {
        // Close the navigation drawer
        mDrawer.closeDrawers();

        // Find which item was selected
        switch(item.getItemId()) {
/*
            case R.id.nav_user_add_attribute:
                // Add a new attribute
                addAttribute();
                break;
*/

            case R.id.nav_user_change_password:
                changePassword();
                break;

/*
            case R.id.nav_user_menu:
                ShowMenu();
                break;
*/

            case R.id.nav_user_verify_attribute:
                // Confirm new user
                // confirmUser();
                attributesVerification();
                break;
/*
            case R.id.nav_user_settings:
                // Show user settings
                showSettings();
                break;
*/
            case R.id.nav_user_sign_out:
                // Sign out from this account
                signOut();
/*
                break;
            case R.id.nav_user_trusted_devices:
                showTrustedDevices();
*/
                break;
            case R.id.nav_user_about:
                // For the inquisitive
                Intent aboutAppActivity = new Intent(this, AboutApp.class);
                startActivity(aboutAppActivity);
                break;
            case R.id.viewReward:
                Intent aboutAppActivity2 = new Intent(this, AboutApp.class);
                startActivity(aboutAppActivity2);
                break;
        }
    }

    // Get user details from CIP service
    private void getDetails() {
        AppHelper.getPool().getUser(username).getDetailsInBackground(detailsHandler);
    }

    // Show user attributes from CIP service
    private void showAttributes() {
        final UserAttributesAdapter attributesAdapter = new UserAttributesAdapter(getApplicationContext());
        final ListView attributesListView;
        attributesListView = (ListView) findViewById(R.id.listViewCurrentUserDetails);
        attributesListView.setAdapter(attributesAdapter);
        attributesList = attributesListView;

        attributesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                TextView data = (TextView) view.findViewById(R.id.editTextUserDetailInput);
                String attributeType = data.getHint().toString();
                String attributeValue = data.getText().toString();
                showUserDetail(attributeType, attributeValue);
            }
        });
    }

    // Update attributes
    private void updateAttribute(String attributeType, String attributeValue) {

        if(attributeType == null || attributeType.length() < 1) {
            return;
        }
        CognitoUserAttributes updatedUserAttributes = new CognitoUserAttributes();
        updatedUserAttributes.addAttribute(attributeType, attributeValue);
        Toast.makeText(getApplicationContext(), attributeType + ": " + attributeValue, Toast.LENGTH_LONG);
        showWaitDialog("Updating...");
        AppHelper.getPool().getUser(AppHelper.getCurrUser()).updateAttributesInBackground(updatedUserAttributes, updateHandler);
    }

    // Show user MFA Settings
    private void showSettings() {
        Intent userSettingsActivity = new Intent(this,SettingsActivity.class);
        startActivityForResult(userSettingsActivity, 20);
    }

    // Add a new attribute
    private void addAttribute() {
        Intent addAttrbutesActivity = new Intent(this,AddAttributeActivity.class);
        startActivityForResult(addAttrbutesActivity, 22);
    }

    // Delete attribute
    private void deleteAttribute(String attributeName) {
        showWaitDialog("Deleting...");
        List<String> attributesToDelete = new ArrayList<>();
        attributesToDelete.add(attributeName);
        AppHelper.getPool().getUser(AppHelper.getCurrUser()).deleteAttributesInBackground(attributesToDelete, deleteHandler);
    }

    // Change user password
    private void changePassword() {
        Intent changePssActivity = new Intent(this, ChangePasswordActivity.class);
        startActivity(changePssActivity);
    }


    private void ShowMenu() {
        Intent showmenu = new Intent(this, AndroidButtonWithIconAndText.class);
        startActivity(showmenu);
    }
    // Verify attributes
    private void attributesVerification() {
        Intent attrbutesActivity = new Intent(this,VerifyActivity.class);
        startActivityForResult(attrbutesActivity, 21);
    }

    private void showTrustedDevices() {
        Intent trustedDevicesActivity = new Intent(this, DeviceSettings.class);
        startActivity(trustedDevicesActivity);
    }

    // Sign out user
    private void signOut() {
        user.signOut();
        exit();
    }

    // Initialize this activity
    private void init() {
        // Get the user name
        Bundle extras = getIntent().getExtras();
        username = AppHelper.getCurrUser();
        user = AppHelper.getPool().getUser(username);
        getDetails();
    }

    GetDetailsHandler detailsHandler = new GetDetailsHandler() {
        @Override
        public void onSuccess(CognitoUserDetails cognitoUserDetails) {
            closeWaitDialog();
            // Store details in the AppHandler
            AppHelper.setUserDetails(cognitoUserDetails);
         //   showAttributes();
            // Trusted devices?
            handleTrustedDevice();
        }

        @Override
        public void onFailure(Exception exception) {
            closeWaitDialog();
            showDialogMessage("Could not fetch user details!", AppHelper.formatException(exception), true);
        }
    };

    private void handleTrustedDevice() {
        CognitoDevice newDevice = AppHelper.getNewDevice();
        if (newDevice != null) {
            AppHelper.newDevice(null);
            trustedDeviceDialog(newDevice);
        }
    }

    private void updateDeviceStatus(CognitoDevice device) {
        device.rememberThisDeviceInBackground(trustedDeviceHandler);
    }

    private void trustedDeviceDialog(final CognitoDevice newDevice) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Remember this device?");
        //final EditText input = new EditText(UserActivity.this);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);

        //input.setLayoutParams(lp);
        //input.requestFocus();
        //builder.setView(input);

        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    //String newValue = input.getText().toString();
                    showWaitDialog("Remembering this device...");
                    updateDeviceStatus(newDevice);
                    userDialog.dismiss();
                } catch (Exception e) {
                    // Log failure
                }
            }
        }).setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    userDialog.dismiss();
                } catch (Exception e) {
                    // Log failure
                }
            }
        });
        userDialog = builder.create();
        userDialog.show();
    }

    // Callback handlers

    UpdateAttributesHandler updateHandler = new UpdateAttributesHandler() {
        @Override
        public void onSuccess(List<CognitoUserCodeDeliveryDetails> attributesVerificationList) {
            // Update successful
            if(attributesVerificationList.size() > 0) {
                showDialogMessage("Updated", "The updated attributes has to be verified",  false);
            }
            getDetails();
        }

        @Override
        public void onFailure(Exception exception) {
            // Update failed
            closeWaitDialog();
            showDialogMessage("Update failed", AppHelper.formatException(exception), false);
        }
    };

    GenericHandler deleteHandler = new GenericHandler() {
        @Override
        public void onSuccess() {
            closeWaitDialog();
            // Attribute was deleted
            Toast.makeText(getApplicationContext(), "Deleted", Toast.LENGTH_SHORT);

            // Fetch user details from the the service
            getDetails();
        }

        @Override
        public void onFailure(Exception e) {
            closeWaitDialog();
            // Attribute delete failed
            showDialogMessage("Delete failed", AppHelper.formatException(e), false);

            // Fetch user details from the service
            getDetails();
        }
    };

    GenericHandler trustedDeviceHandler = new GenericHandler() {
        @Override
        public void onSuccess() {
            // Close wait dialog
            closeWaitDialog();
        }

        @Override
        public void onFailure(Exception exception) {
            closeWaitDialog();
            showDialogMessage("Failed to update device status", AppHelper.formatException(exception), true);
        }
    };

    private void showUserDetail(final String attributeType, final String attributeValue) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(attributeType);
        final EditText input = new EditText(UserActivity.this);
        input.setText(attributeValue);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);

        input.setLayoutParams(lp);
        input.requestFocus();
        builder.setView(input);

        builder.setNeutralButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    String newValue = input.getText().toString();
                    if(!newValue.equals(attributeValue)) {
                        showWaitDialog("Updating...");
                        updateAttribute(AppHelper.getSignUpFieldsC2O().get(attributeType), newValue);
                    }
                    userDialog.dismiss();
                } catch (Exception e) {
                    // Log failure
                }
            }
        }).setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    userDialog.dismiss();
                    deleteAttribute(AppHelper.getSignUpFieldsC2O().get(attributeType));
                } catch (Exception e) {
                    // Log failure
                }
            }
        });
        userDialog = builder.create();
        userDialog.show();
    }

    private void showWaitDialog(String message) {
        closeWaitDialog();
        waitDialog = new ProgressDialog(this);
        waitDialog.setTitle(message);
        waitDialog.show();
    }

    private void showDialogMessage(String title, String body, final boolean exit) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title).setMessage(body).setNeutralButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    userDialog.dismiss();
                    if(exit) {
                        exit();
                    }
                } catch (Exception e) {
                    // Log failure
                    Log.e(TAG," -- Dialog dismiss failed");
                    if(exit) {
                        exit();
                    }
                }
            }
        });
        userDialog = builder.create();
        userDialog.show();
    }

    private void closeWaitDialog() {
        try {
            waitDialog.dismiss();
        }
        catch (Exception e) {
            //
        }
    }

    private void exit () {
        Intent intent = new Intent();
        if(username == null)
            username = "";
        intent.putExtra("name",username);
        setResult(RESULT_OK, intent);
        finish();
    }





  /*  protected static JSONObject prepareAndSendHttpPost(String URI, ArrayList<NameValuePair> params){return null;} /*{
        Http http = HttpFactory.create(null);
        http.post("http://example.com/users")
                .data("John")
                .handler(new ResponseHandler<Void>() {
                    @Override
                    public void success(Void ignore, HttpResponse response) {
                    }

                    @Override
                    public void error(String message, HttpResponse response) {
                    }

                    @Override
                    public void failure(NetworkError error) {
                    }

                    @Override
                    public void complete() {
                    }
                }).execute();
        return null;

    }*/
/*
    protected static JSONObject decodeJSONResponse(HttpResponse resp) {

        InputStream is = null;
        try {
            is = resp.getEntity().getContent();
        }
        catch (IOException e) {
            Log.d(TAG1, "unable to get content from response entity");
            e.printStackTrace();
            return null;
        }

        String in = convertStreamToString(is);

        JSONObject json = null;
        try {
            json = new JSONObject(in);
        }
        catch (JSONException e) {
            Log.d(TAG1, "could not decode JSON response from: "+in);
        }

        return json;
    }*/

    protected static String convertStreamToString(InputStream is) {

        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();

        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }




}


