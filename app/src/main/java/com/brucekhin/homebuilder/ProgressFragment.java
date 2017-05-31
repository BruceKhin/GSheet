package com.brucekhin.homebuilder;

import android.app.ProgressDialog;
import android.support.v4.app.Fragment;

/**
 * Created by brucekhin_home on 5/9/17.
 */

public class ProgressFragment extends Fragment {
    ProgressDialog mProgressDialog;

    public void showProgressDialog(String str){
        if(mProgressDialog == null){
            mProgressDialog = new ProgressDialog(getContext());
        }else{
            mProgressDialog.setTitle(str);
            mProgressDialog.show();
        }
    }

    public void hideProgressDialog(){
        if(mProgressDialog != null)
            mProgressDialog.hide();
    }
}
