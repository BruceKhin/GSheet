package com.brucekhin.homebuilder;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;

class ProjectListAdapter extends BaseAdapter {

    private Context context;
    private ArrayList<SheetData> datas = new ArrayList<>();
    private static LayoutInflater inflater=null;

    public interface SetCompleteListener{
        void setCompleteProject(int id, String state);
    }

    public interface SetSyncCalendar{
        void setSyncCalendar(int id);
    }

    SetCompleteListener completeListener;
    SetSyncCalendar syncListener;

    public ProjectListAdapter(Activity mainActivity,
                              ArrayList<SheetData> datas, SetCompleteListener completeListener, SetSyncCalendar syncListener) {
        this.datas = datas;
        context = mainActivity;
        inflater = ( LayoutInflater )context.
                getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.completeListener = completeListener;
        this.syncListener = syncListener;
    }
    @Override
    public int getCount() {

        if(datas == null) return 0;

        return datas.size();
    }

    @Override
    public Object getItem(int position) {
        return position;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    private class Holder
    {
        private TextView tvWBS;
        private TextView tvActivity;
        private TextView tvSchedStartDate;
        private TextView tvSchedEndDate;
        private TextView tvDays;
        private TextView tvDep;
        private TextView tvActualStartDate;
        private TextView tvActualEndDate;
        private TextView tvNotes;
        private Button btnProjectComplete;
        private Button btnCalendarSync;
    }
    @SuppressLint({"ViewHolder", "SetTextI18n"})
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {

        final Holder holder=new Holder();
        inflater = ((Activity)context).getLayoutInflater();
        View rowView;
        rowView = inflater.inflate(R.layout.listitem, parent, false);
        holder.tvWBS = (TextView) rowView.findViewById(R.id.tvWBS);
        holder.tvActivity = (TextView) rowView.findViewById(R.id.tvActivity);
        holder.tvSchedStartDate = (TextView) rowView.findViewById(R.id.tvSchedStartDate);
        holder.tvSchedEndDate = (TextView) rowView.findViewById(R.id.tvSchedEndDate);
        holder.tvDays = (TextView) rowView.findViewById(R.id.tvDays);
        holder.tvDep = (TextView) rowView.findViewById(R.id.tvDep);
        holder.tvActualStartDate = (TextView) rowView.findViewById(R.id.tvActualStartDate);
        holder.tvActualEndDate = (TextView) rowView.findViewById(R.id.tvActualEndDate);
        holder.tvNotes = (TextView) rowView.findViewById(R.id.tvNotes);

        holder.tvWBS.setText(datas.get(position).WBS);
        holder.tvActivity.setText(datas.get(position).Activity);
        holder.tvSchedStartDate.setText(datas.get(position).SchedStartDate);
        holder.tvSchedEndDate.setText(datas.get(position).SchedEndDate);
        holder.tvDays.setText(datas.get(position).Days);
        holder.tvDep.setText(datas.get(position).Dep);
        holder.tvActualStartDate.setText(datas.get(position).ActualStartDate);
        holder.tvActualEndDate.setText(datas.get(position).ActualEndDate);
        holder.tvNotes.setText(datas.get(position).Notes);

        holder.btnProjectComplete = (Button) rowView.findViewById(R.id.btnProjectComplete);
        holder.btnCalendarSync = (Button) rowView.findViewById(R.id.btnCalendarSync);

        String[] sections = datas.get(position).WBS.split("\\.");
        if(sections.length == 1 && !sections[0].equals("")){
            holder.tvActivity.setTextColor(Color.RED);
        }

        if(datas.get(position).Activity.trim().equals("")){
            holder.btnProjectComplete.setVisibility(View.INVISIBLE);
            holder.btnCalendarSync.setVisibility(View.INVISIBLE);
        }
        if(datas.get(position).C.equals("x")) {
            holder.btnProjectComplete.setText("C");
            holder.btnProjectComplete.setBackground(context.getResources().getDrawable(R.drawable.item_bg));
            holder.btnProjectComplete.setTextColor(Color.GRAY);
        }else{
            holder.btnProjectComplete.setText("IC");
            holder.btnProjectComplete.setBackground(context.getResources().getDrawable(R.drawable.item_complete_bg));
        }

        holder.btnProjectComplete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String state = holder.btnProjectComplete.getText().toString();
                completeListener.setCompleteProject(position, state);

                if(state.equals("C")){
                    holder.btnProjectComplete.setBackground(context.getResources().getDrawable(R.drawable.item_complete_bg));
                    holder.btnProjectComplete.setText("IC");
                    holder.btnProjectComplete.setTextColor(Color.WHITE);
                } else {
                    holder.btnProjectComplete.setBackground(context.getResources().getDrawable(R.drawable.item_bg));
                    holder.btnProjectComplete.setText("C");
                    holder.btnProjectComplete.setTextColor(Color.GRAY);
                }
            }
        });

        holder.btnCalendarSync.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                syncListener.setSyncCalendar(position);
            }
        });
        return rowView;
    }



}


