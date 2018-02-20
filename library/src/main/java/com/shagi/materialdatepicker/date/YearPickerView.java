/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.shagi.materialdatepicker.date;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.StateListDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.shagi.materialdatepicker.R;

/**
 * Displays a selectable list of years.
 */
public class YearPickerView extends ListView implements OnItemClickListener, DatePickerFragmentDialog.OnDateChangedListener {
    private static final String TAG = "YearPickerView";

    private final DatePickerController mController;
    private YearAdapter mAdapter;
    private int mViewSize;
    private int mChildSize;
    private TextView mSelectedView;

    public YearPickerView(Context context, DatePickerController controller) {
        super(context);
        mController = controller;
        mController.registerOnDateChangedListener(this);
        ViewGroup.LayoutParams frame = new ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT);
        setLayoutParams(frame);
        Resources res = context.getResources();
        mViewSize = res.getDimensionPixelOffset(R.dimen.amdp_date_picker_view_animator_height);
        mChildSize = res.getDimensionPixelOffset(R.dimen.amdp_year_label_height);
        setVerticalFadingEdgeEnabled(true);
        setFadingEdgeLength(mChildSize / 3);
        init();
        setOnItemClickListener(this);
        setSelector(new StateListDrawable());
        setDividerHeight(0);
        onDateChanged();
    }

    private void init() {
        mAdapter = new YearAdapter(mController.getMinYear(), mController.getMaxYear());
        setAdapter(mAdapter);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        TextView clickedView = (TextView) view;
        if (clickedView != null) {
            if (clickedView != mSelectedView) {
                if (mSelectedView != null) {
                    mSelectedView.requestLayout();
                }
                clickedView.requestLayout();
                mSelectedView = clickedView;
            }
            mController.onYearSelected(getYearFromTextView(clickedView));
            mAdapter.notifyDataSetChanged();
        }
    }

    private static int getYearFromTextView(TextView view) {
        return Integer.valueOf(view.getText().toString());
    }

    private final class YearAdapter extends BaseAdapter {
        private final int mMinYear;
        private final int mMaxYear;

        YearAdapter(int minYear, int maxYear) {
            if (minYear > maxYear) {
                throw new IllegalArgumentException("minYear > maxYear");
            }
            mMinYear = minYear;
            mMaxYear = maxYear;
        }

        @Override
        public int getCount() {
            return mMaxYear - mMinYear + 1;
        }

        @Override
        public Object getItem(int position) {
            return mMinYear + position;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView v;
            if (convertView != null) {
                v = (TextView) convertView;
            } else {
                v = (TextView) LayoutInflater.from(parent.getContext())
                  .inflate(R.layout.amdp_year_label_text_view, parent, false);
            }
            int year = mMinYear + position;
            boolean selected = mController.getSelectedDay().year == year;
            v.setText(String.valueOf(year));
            v.requestLayout();
            if (selected) {
                mSelectedView = v;
            }
            return v;
        }
    }

    public void postSetSelectionCentered(final int position) {
        postSetSelectionFromTop(position, mViewSize / 2 - mChildSize / 2);
    }

    public void postSetSelectionFromTop(final int position, final int offset) {
        post(new Runnable() {

            @Override
            public void run() {
                setSelectionFromTop(position, offset);
                requestLayout();
            }
        });
    }

    public int getFirstPositionOffset() {
        final View firstChild = getChildAt(0);
        if (firstChild == null) {
            return 0;
        }
        return firstChild.getTop();
    }

    @Override
    public void onDateChanged() {
        mAdapter.notifyDataSetChanged();
        postSetSelectionCentered(mController.getSelectedDay().year - mController.getMinYear());
    }

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            event.setFromIndex(0);
            event.setToIndex(0);
        }
    }
}
