package com.brucekhin.homebuilder;

import java.util.ArrayList;

/**
 * Created by brucekhin_home on 5/4/17.
 */

public class ProjectData {
    public String sheetName;
    public ArrayList<SheetData> sheetData;

    public ProjectData() {

    }

    public ProjectData(String sheetName, ArrayList<SheetData> sheetData) {
        this.sheetName = sheetName;
        this.sheetData = sheetData;
    }
}
