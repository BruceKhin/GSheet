package com.brucekhin.homebuilder;

/**
 * Created by brucekhin_home on 5/31/17.
 */

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;

import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.ExponentialBackOff;

import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.EventReminder;
import com.google.api.services.sheets.v4.SheetsScopes;

import com.google.api.services.sheets.v4.model.*;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import android.Manifest;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class SheetLayout1 extends AppCompatActivity
        implements EasyPermissions.PermissionCallbacks, TabsListener, ProjectListAdapter.SetCompleteListener, ProjectListAdapter.SetSyncCalendar{

    static GoogleAccountCredential mCredential;
    ProgressDialog mProgress;

    private DatabaseReference mPostTabReference;
    ArrayList<ProjectData> homeProjectDatas = new ArrayList<>();
    private List<Fragment> fragmentList = new ArrayList<>();
    private List<String> tabTitles = new ArrayList<>();
    private ArrayList<String> sheetKeyValue = new ArrayList<>();
    ViewPager viewPager;
    private MyPagerAdapter pagerAdapter;
    private TabLayout tabLayout;
    String curSelSheetName = "";

    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;

    private static final String PREF_ACCOUNT_NAME = "accountName";
    private static final String[] SCOPES = { SheetsScopes.SPREADSHEETS, CalendarScopes.CALENDAR };

    static int rowIdx = -1;
    static int apiKind = -1;
    static String completeState = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sheet_layout);

        mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());

        mPostTabReference = FirebaseDatabase.getInstance().getReference();
        mPostTabReference = mPostTabReference.child(Consts.bookName).child(Consts.sheetName);

        mProgress = new ProgressDialog(this);
        mProgress.setMessage("Synchronize with Google Sheet ...");

        readFirebase();

    }

    private void readFirebase(){
        mProgress.show();
        homeProjectDatas = new ArrayList<>();
        tabTitles = new ArrayList<>();
        fragmentList = new ArrayList<>();

        // Setup the viewPager
        viewPager = (ViewPager) findViewById(R.id.view_pager);
        pagerAdapter = new MyPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(pagerAdapter);

        // Setup the Tabs
        tabLayout = (TabLayout) findViewById(R.id.tab_layout);
        // This method ensures that tab selection events update the ViewPager and page changes update the selected tab.
        tabLayout.setupWithViewPager(viewPager);
        tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener(){
            @Override
            public void onTabSelected(TabLayout.Tab tab){
                int position = tab.getPosition();
                curSelSheetName = tabTitles.get(position);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
        setChildListener();
    }

    public void setChildListener(){

        mProgress.show();

        mPostTabReference.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot sheetName, String s) {
                DataOfSheetFragment fragment = DataOfSheetFragment.newInstance();
                fragment.mySheetName = sheetName.getValue().toString();
                pagerAdapter.addTab(fragment, sheetName.getValue().toString());
                sheetKeyValue.add(sheetName.getKey());
                mProgress.hide();
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                int chTabIdx = sheetKeyValue.indexOf(dataSnapshot.getKey());
                if( chTabIdx != -1)
                {
                    pagerAdapter.changeTabName(chTabIdx, dataSnapshot.getValue().toString());
                }
                mProgress.hide();
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                int chTabIdx = sheetKeyValue.indexOf(dataSnapshot.getKey());
                if( chTabIdx != -1)
                    pagerAdapter.removeTab(chTabIdx);
                mProgress.hide();
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {
                mProgress.hide();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                mProgress.hide();
            }
        });
    }

    @AfterPermissionGranted(REQUEST_PERMISSION_GET_ACCOUNTS)
    private void chooseAccount() {
        if (EasyPermissions.hasPermissions(
                this, Manifest.permission.GET_ACCOUNTS)) {
            String accountName = getPreferences(Context.MODE_PRIVATE)
                    .getString(PREF_ACCOUNT_NAME, null);
            if (accountName != null) {
                mCredential.setSelectedAccountName(accountName);
                getResultsFromApi();
            } else {
                // Start a dialog from which the user can choose an account
                startActivityForResult(
                        mCredential.newChooseAccountIntent(),
                        REQUEST_ACCOUNT_PICKER);
            }
        } else {
            // Request the GET_ACCOUNTS permission via a user dialog
            EasyPermissions.requestPermissions(
                    this,
                    "This app needs to access your Google account (via Contacts).",
                    REQUEST_PERMISSION_GET_ACCOUNTS,
                    Manifest.permission.GET_ACCOUNTS);
        }
    }

    @Override
    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != RESULT_OK) {
                    AlertDialog alertDialog = new AlertDialog.Builder(SheetLayout1.this).create();
                    alertDialog.setTitle("Alert");
                    alertDialog.setMessage("This app requires Google Play Services. Please install " +
                            "Google Play Services on your device and relaunch this app.");
                    alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                    alertDialog.show();
                } else {
                    getResultsFromApi();
                }
                break;
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null &&
                        data.getExtras() != null) {
                    String accountName =
                            data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        SharedPreferences settings =
                                getPreferences(Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(PREF_ACCOUNT_NAME, accountName);
                        editor.apply();
                        mCredential.setSelectedAccountName(accountName);
                        getResultsFromApi();
                    }
                }
                break;
            case REQUEST_AUTHORIZATION:
                if (resultCode == RESULT_OK) {
                    getResultsFromApi();
                }
                break;
        }
    }

    /**
     * Respond to requests for permissions at runtime for API 23 and above.
     * @param requestCode The request code passed in
     *     requestPermissions(android.app.Activity, String, int, String[])
     * @param permissions The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *     which is either PERMISSION_GRANTED or PERMISSION_DENIED. Never null.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(
                requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> list) {
        // Do nothing.
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> list) {
        // Do nothing.
    }

    private boolean isDeviceOnline() {
        ConnectivityManager connMgr =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        return connectionStatusCode == ConnectionResult.SUCCESS;
    }

    private void acquireGooglePlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        final int connectionStatusCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
        }
    }


    void showGooglePlayServicesAvailabilityErrorDialog(final int connectionStatusCode) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        Dialog dialog = apiAvailability.getErrorDialog( SheetLayout1.this, connectionStatusCode, REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }

    @Override
    public void onTabAdded() {
        pagerAdapter.addTab(DataOfSheetFragment.newInstance(), "Tab " + (tabTitles.size() + 1));
    }

    @Override
    public void onTabRemoved() {
        pagerAdapter.removeTab(1);
    }


    @Override
    public void setCompleteProject(int _id, String _state) {
        mProgress.setMessage("Synchronize with Google Sheet ...");

        rowIdx = _id;
        apiKind = 0;
        completeState = _state;

        getResultsFromApi();
    }

    @Override
    public void setSyncCalendar(int _id) {
        mProgress.setMessage("Synchronize with Google Calendar ...");

        rowIdx = _id;
        apiKind = 1;
        completeState = "";

        getResultsFromApi();
    }

    private void getResultsFromApi() {
        if (! isGooglePlayServicesAvailable()) {
            acquireGooglePlayServices();
        } else if (mCredential.getSelectedAccountName() == null) {
            chooseAccount();
        } else if (! isDeviceOnline()) {
            AlertDialog alertDialog = new AlertDialog.Builder(SheetLayout1.this).create();
            alertDialog.setTitle("Alert");
            alertDialog.setMessage("No network connection available.");
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
            alertDialog.show();
        } else {
            if( rowIdx != -1 && apiKind != -1){
                new SheetLayout1.MakeRequestTask(mCredential).execute();
            }
        }
    }
    private class MyPagerAdapter extends FragmentStatePagerAdapter {

        public MyPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int pos) {
            return fragmentList.get(pos);
        }

        @Override
        public int getCount() {
            return fragmentList.size();
        }

        // This is called when notifyDataSetChanged() is called. Without this, getItem() is not triggered
        @Override
        public int getItemPosition(Object object) {
            // refresh all fragments when data set changed
            return PagerAdapter.POSITION_NONE;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return tabTitles.get(position);
        }

        public void addTab(Fragment fragment, String tabTitle, int index){
            fragmentList.add(index, fragment);
            tabTitles.add(index, tabTitle);
            notifyDataSetChanged();
        }

        public void addTab(Fragment fragment, String tabTitle) {
            fragmentList.add(fragment);
            tabTitles.add(tabTitle);
            notifyDataSetChanged();
        }

        public void removeTab(int tabPosition) {
            if (!fragmentList.isEmpty()) {
                fragmentList.remove(tabPosition);
                tabTitles.remove(tabPosition);
                notifyDataSetChanged();
            }
        }

        public void changeTabName(int tabPosition, String tabTitle){
            DataOfSheetFragment fragment = (DataOfSheetFragment)fragmentList.get(tabPosition);
            fragment.mySheetName = tabTitle;
            tabTitles.set(tabPosition, tabTitle);
            notifyDataSetChanged();
        }
    }

    private class MakeRequestTask extends AsyncTask<Void, Void, List<String>> {
        private com.google.api.services.sheets.v4.Sheets mSheetService = null;
        private com.google.api.services.calendar.Calendar mCalendarService = null;
        private Exception mLastError = null;

        private
        MakeRequestTask(GoogleAccountCredential credential) {
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mSheetService = new com.google.api.services.sheets.v4.Sheets.Builder(
                    transport, jsonFactory, null)
                    .setHttpRequestInitializer(credential)
                    .setApplicationName("HomeBuilder")
                    .build();

            mCalendarService = new com.google.api.services.calendar.Calendar.Builder(
                    transport, jsonFactory, null)
                    .setHttpRequestInitializer(credential)
                    .setApplicationName("HomeBuilder")
                    .build();

        }

        /**
         * Background task to call Google Sheets API.
         * @param params no parameters needed for this task.
         */
        @Override
        protected List<String> doInBackground(Void... params) {
            try {
                if(!curSelSheetName.equals("")){
                    String spreadsheetId = Consts.sheetID;
                    String range =  String.format("%s!B%d:B%d", curSelSheetName, rowIdx + 2, rowIdx + 2);

                    if(apiKind == 0){
                        writeDataToSheet(spreadsheetId, range, completeState);
                    } else {
                        createNewEvent();
                    }

                }
                return null;
            } catch (Exception e) {
                mLastError = e;
                cancel(true);
                return null;
            }
        }

        private void writeDataToSheet(String spreadsheetId, String range, String completeState) throws IOException {

            List<List<Object>> values = new ArrayList<>();

            List<Object> data = new ArrayList<>();

            if(completeState.equals("C")){
                data.add(" ");
            } else {
                data.add("x");
            }

            values.add(data);

            ValueRange valueRange = new ValueRange();
            valueRange.setMajorDimension("ROWS");
            valueRange.setRange(range);
            valueRange.setValues(values);

            //then gloriously execute this copy-pasted code ;)
            mSheetService.spreadsheets().values()
                    .update(spreadsheetId, range, valueRange)
                    .setValueInputOption("RAW")
                    .execute();
        }

        private void createNewEvent() throws  IOException{
            Event event = new Event()
                    .setSummary("Google I/O 2017")
                    .setLocation("CANADA")
                    .setDescription("Google Calenda Testing");
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String currentDateTimeString = df.format(new Date(System.currentTimeMillis()));
            Log.d("first time: ", currentDateTimeString);
            DateTime startDateTime = new DateTime(currentDateTimeString.replace(" ", "T"));

            EventDateTime start = new EventDateTime()
                    .setDateTime(startDateTime)
                    .setTimeZone("America/Los_Angeles");
            event.setStart(start);

            currentDateTimeString = df.format(new Date(System.currentTimeMillis()));
            Log.d("second time: ", currentDateTimeString);

            DateTime endDateTime = new DateTime(currentDateTimeString.replace(" ", "T"));
            EventDateTime end = new EventDateTime()
                    .setDateTime(endDateTime)
                    .setTimeZone("America/Los_Angeles");
            event.setEnd(end);

            String[] recurrence = new String[] {"RRULE:FREQ=DAILY;COUNT=1"};
            event.setRecurrence(Arrays.asList(recurrence));

            EventAttendee[] attendees = new EventAttendee[] {
                    new EventAttendee().setEmail("test1@brucekhin.com"),
                    new EventAttendee().setEmail("test2@brucekhin.com"),
            };
            event.setAttendees(Arrays.asList(attendees));

            EventReminder[] reminderOverrides = new EventReminder[] {
                    new EventReminder().setMethod("email").setMinutes(24 * 60),
                    new EventReminder().setMethod("popup").setMinutes(10),
            };
            Event.Reminders reminders = new Event.Reminders()
                    .setUseDefault(false)
                    .setOverrides(Arrays.asList(reminderOverrides));
            event.setReminders(reminders);

            String calendarId = "primary";
            event = mCalendarService.events().insert(calendarId, event).execute();
            System.out.printf("Event created: %s\n", event.getHtmlLink());
        }

        @Override
        protected void onPreExecute() {
            mProgress.show();
        }

        @Override
        protected void onPostExecute(List<String> output) {
            mProgress.hide();
        }

        @Override
        protected void onCancelled() {
            mProgress.hide();
            if (mLastError != null) {
                if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                    showGooglePlayServicesAvailabilityErrorDialog(
                            ((GooglePlayServicesAvailabilityIOException) mLastError)
                                    .getConnectionStatusCode());
                } else if (mLastError instanceof UserRecoverableAuthIOException) {
                    startActivityForResult(
                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
                            SheetLayout1.REQUEST_AUTHORIZATION);
                } else {

                    AlertDialog alertDialog = new AlertDialog.Builder(SheetLayout1.this).create();
                    alertDialog.setTitle("Alert");
                    alertDialog.setMessage("Connection error!\n"
                            + mLastError.getMessage() + "\n"
                            + mLastError.getCause().toString() + "\n"
                            + "Please Retry");
                    alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                    alertDialog.show();
                }
            } else {
                AlertDialog alertDialog = new AlertDialog.Builder(SheetLayout1.this).create();
                alertDialog.setTitle("Alert");
                alertDialog.setMessage("Request cancelled.");
                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                alertDialog.show();
            }
        }
    }
}
