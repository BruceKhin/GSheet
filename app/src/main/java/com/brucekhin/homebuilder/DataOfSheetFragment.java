package com.brucekhin.homebuilder;

/**
 * Created by brucekhin_home on 5/4/17.
 */

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;


public class DataOfSheetFragment extends ProgressFragment{

    public ArrayList<SheetData> sheetDatas = new ArrayList<>();

    public String mySheetName = "";

    private int mnCurPos;

    ListView lstProjects;

    private TextView headerTvWBS;
    private TextView headerTvActivity;
    private TextView headerTvSchedStartDate;
    private TextView headerTvSchedEndDate;
    private TextView headerTvDays;
    private TextView headerTvDep;
    private TextView headerTvActualStartDate;
    private TextView headerTvActualEndDate;
    private TextView headerTvNotes;

    Activity mContext;
    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment DataOfSheetFragment.
     */
    public static DataOfSheetFragment newInstance() {
        return new DataOfSheetFragment();
    }

    public DataOfSheetFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.data_of_sheet_fragment, container, false);
        lstProjects = (ListView)view.findViewById(R.id.lstProjects);

        headerTvWBS = (TextView)view.findViewById(R.id.headerTvWBS);
        headerTvActivity = (TextView)view.findViewById(R.id.headerTvActivity);
        headerTvSchedStartDate = (TextView)view.findViewById(R.id.headerTvSchedStartDate);
        headerTvSchedEndDate = (TextView)view.findViewById(R.id.headerTvSchedEndDate);
        headerTvDays = (TextView)view.findViewById(R.id.headerTvDays);
        headerTvDep = (TextView)view.findViewById(R.id.headerTvDep);
        headerTvActualStartDate = (TextView)view.findViewById(R.id.headerTvActualStartDate);
        headerTvActualEndDate = (TextView)view.findViewById(R.id.headerTvActualEndDate);
        headerTvNotes = (TextView)view.findViewById(R.id.headerTvNotes);

        mContext = getActivity();
        readFromFirebase();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        readFromFirebase();
    }

    private void readFromFirebase() {

        showProgressDialog("Loading sheet data...");

        DatabaseReference mPostTabReference = FirebaseDatabase.getInstance().getReference();
        mPostTabReference = mPostTabReference.child(Consts.bookName).child(Consts.sheetData).child(mySheetName);
        mPostTabReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                hideProgressDialog();
                sheetDatas = new ArrayList<>();
                int j = 0;
                for (DataSnapshot sheet : dataSnapshot.getChildren()) {
                    int k = 0;
                    SheetData rowData = new SheetData();
                    for (DataSnapshot row : sheet.getChildren()) {
                        switch (k) {
                            case 0:
                                rowData.WBS = row.getValue().toString();
                                break;
                            case 1:
                                rowData.C = row.getValue().toString();
                                break;
                            case 2:
                                rowData.Activity = row.getValue().toString();
                                break;
                            case 3:
                                rowData.SchedStartDate = row.getValue().toString();
                                break;
                            case 4:
                                rowData.SchedEndDate = row.getValue().toString();
                                break;
                            case 5:
                                rowData.Days = row.getValue().toString();
                                break;
                            case 6:
                                rowData.Dep = row.getValue().toString();
                                break;
                            case 7:
                                rowData.ActualStartDate = row.getValue().toString();
                                break;
                            case 8:
                                rowData.ActualEndDate = row.getValue().toString();
                                break;
                            case 9:
                                rowData.Notes = row.getValue().toString();
                                break;
                        }
                        k++;
                    }
                    if(j == 0){
                        headerTvWBS.setText(rowData.WBS);
                        headerTvActivity.setText(rowData.Activity);
                        headerTvSchedStartDate.setText(rowData.SchedStartDate);
                        headerTvSchedEndDate.setText(rowData.SchedEndDate);
                        headerTvDays.setText(rowData.Days);
                        headerTvDep.setText(rowData.Dep);
                        headerTvActualStartDate.setText(rowData.ActualStartDate);
                        headerTvActualEndDate.setText(rowData.ActualEndDate);
                        headerTvNotes.setText(rowData.Notes);
                    } else {
                        sheetDatas.add(rowData);
                        ProjectListAdapter adapter = new ProjectListAdapter(mContext, sheetDatas, (SheetLayout1) getActivity(), (SheetLayout1) getActivity());
                        lstProjects.setAdapter(adapter);
                        lstProjects.setSelection(mnCurPos);
                        lstProjects.setOnScrollListener(new AbsListView.OnScrollListener() {
                            @Override
                            public void onScrollStateChanged(AbsListView view, int scrollState) {
                                mnCurPos = view.getFirstVisiblePosition();
                            }

                            @Override
                            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                            }
                        });
                    }

                    j++;
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                hideProgressDialog();
            }
        });
    }

}