package com.brucekhin.gsheet;

import com.google.firebase.database.IgnoreExtraProperties;
import java.lang.String;
/**
 * Created by brucekhin_home on 5/3/17.
 */

@IgnoreExtraProperties
class SheetData {

    public String WBS;
    public String C;
    public String Activity;
    public String SchedStartDate;
    public String SchedEndDate;
    public String Days;
    public String Dep;
    public String ActualStartDate;
    public String ActualEndDate;
    public String Notes;

    public String Sync;

    public SheetData() {
        WBS = "";
        C = "";
        Activity = "";
        SchedStartDate = "";
        SchedEndDate = "";
        Days = "";
        Dep = "";
        ActualStartDate = "";
        ActualEndDate = "";
        Notes = "";

        Sync = "";
    }

    public SheetData(
            String wbs,
            String c,
            String activity,
            String schedStartDate,
            String schedEndDate,
            String days,
            String dep,
            String actualStartDate,
            String actualEndDate,
            String notes,
            String sync) {
        this.WBS = wbs;
        this.C = c;
        this.Activity = activity;
        this.SchedStartDate = schedStartDate;
        this.SchedEndDate = schedEndDate;
        this.Days = days;
        this.Dep = dep;
        this.ActualStartDate = actualStartDate;
        this.ActualEndDate = actualEndDate;
        this.Notes = notes;
        this.Sync = sync;
    }

}

