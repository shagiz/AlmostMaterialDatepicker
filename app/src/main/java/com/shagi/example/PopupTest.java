package com.shagi.example;

import android.content.Context;
import android.support.v7.widget.ListPopupWindow;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.Toast;

import java.util.ArrayList;

public class PopupTest {
    ListPopupWindow popupWindow;

    public View.OnClickListener showPopupWindow() {
        return new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                ListPopupWindow popUp = popupWindowsort(v.getContext());
                popUp.setAnchorView(v);

//                popUp.showAsDropDown(v, 0, 0); // show popup like dropdown list
                popUp.show();
            }
        };
    }

    /**
     * show popup window method reuturn PopupWindow
     */
    private ListPopupWindow popupWindowsort(Context context) {

        // initialize a pop up window type
        popupWindow = new ListPopupWindow(context);

        ArrayList<String> sortList = new ArrayList<>();
        sortList.add("A to Z");
        sortList.add("Z to A");
        sortList.add("Low to high price");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_dropdown_item_1line,
                sortList);
        // the drop down list is a list view
        ListView listViewSort = new ListView(context);

        // set our adapter and pass our pop up window contents
        listViewSort.setAdapter(adapter);

        // set on item selected
        listViewSort.setOnItemClickListener(onItemClickListener());

        // some other visual settings for popup window
//        popupWindow.setFocusable(true);
        popupWindow.setWidth(250);
        // popupWindow.setBackgroundDrawable(getResources().getDrawable(R.drawable.white));
        popupWindow.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
        popupWindow.setAdapter(adapter);
        popupWindow.setOnItemClickListener(onItemClickListener());
        // set the listview as popup content
//        popupWindow.setContentView(listViewSort);

        return popupWindow;
    }

    private AdapterView.OnItemClickListener onItemClickListener() {
        return new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView parent, View view, int position, long id) {
                Toast.makeText(view.getContext(), "CLICK " + position, Toast.LENGTH_LONG).show();
                popupWindow.dismiss();
            }
        };
    }
}
