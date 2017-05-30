package com.brucekhin.gsheet;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.EventReminder;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SheetLayout extends AppCompatActivity implements TabsListener, ProjectListAdapter.SetCompleteListener, ProjectListAdapter.SetSyncCalendar {

    private List<Fragment> fragmentList = new ArrayList<>();
    private List<String> tabTitles = new ArrayList<>();
    private ArrayList<String> sheetKeyValue = new ArrayList<>();

    private MyPagerAdapter pagerAdapter;
    private TabLayout tabLayout;
    ProgressDialog mProgressDialog;
    ViewPager viewPager;

    private com.google.api.services.sheets.v4.Sheets mSheetService = null;
    private com.google.api.services.calendar.Calendar mCalendarService = null;
    private DatabaseReference mPostTabReference;
    ArrayList<ProjectData> homeProjectDatas = new ArrayList<>();
    String curSelSheetName = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sheet_layout);

        mPostTabReference = FirebaseDatabase.getInstance().getReference();
        mPostTabReference = mPostTabReference.child(Consts.bookName).child(Consts.sheetName);

        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage("Synchronize with Google Sheet ...");

        readFirebase();

    }

    private void readFirebase(){
        mProgressDialog.show();
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
        mProgressDialog.show();
        mPostTabReference.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot sheetName, String s) {
                DataOfSheetFragment fragment = DataOfSheetFragment.newInstance();
                fragment.mySheetName = sheetName.getValue().toString();
                pagerAdapter.addTab(fragment, sheetName.getValue().toString());
                sheetKeyValue.add(sheetName.getKey());
                mProgressDialog.hide();
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                int chTabIdx = sheetKeyValue.indexOf(dataSnapshot.getKey());
                if( chTabIdx != -1)
                {
                    pagerAdapter.changeTabName(chTabIdx, dataSnapshot.getValue().toString());
                }
                mProgressDialog.hide();
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                int chTabIdx = sheetKeyValue.indexOf(dataSnapshot.getKey());
                if( chTabIdx != -1)
                    pagerAdapter.removeTab(chTabIdx);
                mProgressDialog.hide();
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {
                mProgressDialog.hide();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                mProgressDialog.hide();
            }
        });
    }


    @Override
    public void onTabAdded() {
        pagerAdapter.addTab(DataOfSheetFragment.newInstance(), "Tab " + (tabTitles.size() + 1));
    }

    @Override
    public void onTabRemoved() {
        pagerAdapter.removeTab(1);
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

    @Override
    public void setCompleteProject(int id, String state) {
        mProgressDialog.setMessage("Synchronize with Google Sheet ...");
        getResultsFromApi(id, 0, state);

    }
    @Override
    public void  setSyncCalendar(int id){
        mProgressDialog.setMessage("Synchronize with Google Calendar ...");
        getResultsFromApi(id, 1, "");
    }
    private void getResultsFromApi(int id, int apiKind, String completeState) {
        new MakeRequestTask(MainActivity.mCredential, id, apiKind, completeState).execute();
    }

    private class MakeRequestTask extends AsyncTask<Void, Void, List<String>> {

        private Exception mLastError = null;
        private int rowIdx;
        private int apiKind;
        private String completeState;
        private
        MakeRequestTask(GoogleAccountCredential credential, int id, int apiKind, String completeState) {
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mSheetService = new com.google.api.services.sheets.v4.Sheets.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("Google API Android Quickstart")
                    .build();

            mCalendarService = new com.google.api.services.calendar.Calendar.Builder(
                    transport, jsonFactory, credential)
                    .build();
            rowIdx = id;
            this.apiKind = apiKind;
            this.completeState = completeState;
        }

        @Override
        protected List<String> doInBackground(Void... params) {
            try {
                if(!curSelSheetName.equals("")){
                    String spreadsheetId = Consts.sheetID;
                    String range =  String.format("%s!B%d:B%d", curSelSheetName, rowIdx + 2, rowIdx + 2);
                    Log.d("rowIdx", rowIdx + "");
                    if(this.apiKind == 0){
                        writeDataToSheet(spreadsheetId, range, this.completeState);
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

            Log.d("complete State:", completeState + ", " + range);

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

            DateTime startDateTime = new DateTime("2017-05-03T09:00:00-07:00");
            EventDateTime start = new EventDateTime()
                    .setDateTime(startDateTime)
                    .setTimeZone("America/Los_Angeles");
            event.setStart(start);

            DateTime endDateTime = new DateTime("2017-05-03T10:00:00-07:00");
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
            mProgressDialog.show();
        }

        @Override
        protected void onPostExecute(List<String> output) {
            mProgressDialog.hide();
            if (output == null || output.size() == 0) {
                Log.d("Result","No results returned.");
            } else {
                output.add(0, "Data retrieved using the Google Sheets API:");
            }
        }

        @Override
        protected void onCancelled() {
            mProgressDialog.hide();
            if (mLastError != null) {
                if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
//                    showGooglePlayServicesAvailabilityErrorDialog(
//                            ((GooglePlayServicesAvailabilityIOException) mLastError)
//                                    .getConnectionStatusCode());
                } else if (mLastError instanceof UserRecoverableAuthIOException) {
                    startActivityForResult(
                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
                            MainActivity.REQUEST_AUTHORIZATION);
                } else {
                    Log.d("Cancel", "The following error occurred:\n"
                            + mLastError.getMessage());
                }
            } else {
                Log.d("Cancel", "Request cancelled.");
            }
        }

    }
}
