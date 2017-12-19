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

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.ListPopupWindow;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.shagi.materialdatepicker.HapticFeedbackController;
import com.shagi.materialdatepicker.R;
import com.shagi.materialdatepicker.TypefaceHelper;
import com.shagi.materialdatepicker.Utils;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Dialog allowing users to select a date.
 */
public class DatePickerFragmentDialog extends DialogFragment implements
        OnClickListener, DatePickerController {

    private static final int UNINITIALIZED = -1;
    private static final int MONTH_AND_DAY_VIEW = 0;
    private static final int YEAR_VIEW = 1;

    private static final String KEY_SELECTED_YEAR = "year";
    private static final String KEY_SELECTED_MONTH = "month";
    private static final String KEY_SELECTED_DAY = "day";
    private static final String KEY_LIST_POSITION = "list_position";
    private static final String KEY_WEEK_START = "week_start";
    private static final String KEY_CURRENT_VIEW = "current_view";
    private static final String KEY_HIGHLIGHTED_DAYS = "highlighted_days";
    private static final String KEY_THEME_DARK = "theme_dark";
    private static final String KEY_THEME_DARK_CHANGED = "theme_dark_changed";
    private static final String KEY_ACCENT = "accent";
    private static final String KEY_DISMISS = "dismiss";
    private static final String KEY_AUTO_DISMISS = "auto_dismiss";
    private static final String KEY_DEFAULT_VIEW = "default_view";
    private static final String KEY_TITLE = "title";
    private static final String KEY_OK_RESID = "ok_resid";
    private static final String KEY_OK_STRING = "ok_string";
    private static final String KEY_OK_COLOR = "ok_color";
    private static final String KEY_CANCEL_RESID = "cancel_resid";
    private static final String KEY_CANCEL_STRING = "cancel_string";
    private static final String KEY_CANCEL_COLOR = "cancel_color";
    private static final String KEY_TIMEZONE = "timezone";
    private static final String KEY_DATERANGELIMITER = "daterangelimiter";

    private static final int ANIMATION_DURATION = 300;
    private static final int ANIMATION_DELAY = 500;

    private static SimpleDateFormat YEAR_FORMAT = new SimpleDateFormat("yyyy", Locale.getDefault());
    private static SimpleDateFormat MONTH_FORMAT = new SimpleDateFormat("MMMM", Locale.getDefault());
    private static SimpleDateFormat MONTH_FORMAT_STAND_ALONE = new SimpleDateFormat("LLLL", Locale.getDefault());
    private static SimpleDateFormat DAY_FORMAT = new SimpleDateFormat("dd", Locale.getDefault());

    private Calendar mCalendar = Utils.trimToMidnight(Calendar.getInstance(getTimeZone()));
    private OnDateSetListener mCallBack;
    private HashSet<OnDateChangedListener> mListeners = new HashSet<>();
    private DialogInterface.OnCancelListener mOnCancelListener;
    private DialogInterface.OnDismissListener mOnDismissListener;

    private AccessibleDateAnimator mAnimator;

    private ImageView mArrowLeft;
    private ImageView mArrowRight;
    private TextView mDatePickerHeaderView;
    private LinearLayout mMonthAndDayView;
    private TextView mSelectedMonthTextView;
    private TextView mSelectedDayTextView;
    private TextView mYearView;
    private TextView mMonthPickerView;
    private DayPickerView mDayPickerView;
    private ListPopupWindow mYearPickerPopup;

    private int mCurrentView = UNINITIALIZED;

    private int mWeekStart = mCalendar.getFirstDayOfWeek();
    private String mTitle;
    private HashSet<Calendar> highlightedDays = new HashSet<>();
    private boolean mThemeDark = false;
    private boolean mThemeDarkChanged = false;
    private int mAccentColor = -1;
    private boolean mDismissOnPause = false;
    private boolean mAutoDismiss = false;
    private int mDefaultView = MONTH_AND_DAY_VIEW;
    private int mOkResid = R.string.mdtp_ok;
    private String mOkString;
    private int mOkColor = -1;
    private int mCancelResid = R.string.mdtp_cancel;
    private String mCancelString;
    private int mCancelColor = -1;
    private TimeZone mTimezone;
    private DefaultDateRangeLimiter mDefaultLimiter = new DefaultDateRangeLimiter();
    private DateRangeLimiter mDateRangeLimiter = mDefaultLimiter;

    private HapticFeedbackController mHapticFeedbackController;

    private boolean mDelayAnimation = true;

    // Accessibility strings.
    private String mDayPickerDescription;
    private String mSelectDay;

    /**
     * The callback used to indicate the user is done filling in the date.
     */
    public interface OnDateSetListener {

        /**
         * @param view        The view associated with this listener.
         * @param year        The year that was set.
         * @param monthOfYear The month that was set (0-11) for compatibility
         *                    with {@link Calendar}.
         * @param dayOfMonth  The day of the month that was set.
         */
        void onDateSet(DatePickerFragmentDialog view, int year, int monthOfYear, int dayOfMonth);
    }

    /**
     * The callback used to notify other date picker components of a change in selected date.
     */
    @SuppressWarnings("WeakerAccess")
    protected interface OnDateChangedListener {

        void onDateChanged();
    }


    public DatePickerFragmentDialog() {
        // Empty constructor required for dialog fragment.
    }

    /**
     * @param callBack    How the parent is notified that the date is set.
     * @param year        The initial year of the dialog.
     * @param monthOfYear The initial month of the dialog.
     * @param dayOfMonth  The initial day of the dialog.
     */
    public static DatePickerFragmentDialog newInstance(OnDateSetListener callBack, int year, int monthOfYear, int dayOfMonth) {
        DatePickerFragmentDialog ret = new DatePickerFragmentDialog();
        ret.initialize(callBack, year, monthOfYear, dayOfMonth);
        return ret;
    }

    @SuppressWarnings("unused")
    public static DatePickerFragmentDialog newInstance(OnDateSetListener callback) {
        Calendar now = Calendar.getInstance();
        return DatePickerFragmentDialog.newInstance(callback, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH));
    }

    public void initialize(OnDateSetListener callBack, int year, int monthOfYear, int dayOfMonth) {
        mCallBack = callBack;
        mCalendar.set(Calendar.YEAR, year);
        mCalendar.set(Calendar.MONTH, monthOfYear);
        mCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Activity activity = getActivity();
        activity.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        mCurrentView = UNINITIALIZED;
        if (savedInstanceState != null) {
            mCalendar.set(Calendar.YEAR, savedInstanceState.getInt(KEY_SELECTED_YEAR));
            mCalendar.set(Calendar.MONTH, savedInstanceState.getInt(KEY_SELECTED_MONTH));
            mCalendar.set(Calendar.DAY_OF_MONTH, savedInstanceState.getInt(KEY_SELECTED_DAY));
            mDefaultView = savedInstanceState.getInt(KEY_DEFAULT_VIEW);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_SELECTED_YEAR, mCalendar.get(Calendar.YEAR));
        outState.putInt(KEY_SELECTED_MONTH, mCalendar.get(Calendar.MONTH));
        outState.putInt(KEY_SELECTED_DAY, mCalendar.get(Calendar.DAY_OF_MONTH));
        outState.putInt(KEY_WEEK_START, mWeekStart);
        outState.putInt(KEY_CURRENT_VIEW, mCurrentView);
        int listPosition = -1;
        if (mCurrentView == MONTH_AND_DAY_VIEW) {
            listPosition = mDayPickerView.getMostVisiblePosition();
        }
        outState.putInt(KEY_LIST_POSITION, listPosition);
        outState.putSerializable(KEY_HIGHLIGHTED_DAYS, highlightedDays);
        outState.putBoolean(KEY_THEME_DARK, mThemeDark);
        outState.putBoolean(KEY_THEME_DARK_CHANGED, mThemeDarkChanged);
        outState.putInt(KEY_ACCENT, mAccentColor);
        outState.putBoolean(KEY_DISMISS, mDismissOnPause);
        outState.putBoolean(KEY_AUTO_DISMISS, mAutoDismiss);
        outState.putInt(KEY_DEFAULT_VIEW, mDefaultView);
        outState.putString(KEY_TITLE, mTitle);
        outState.putInt(KEY_OK_RESID, mOkResid);
        outState.putString(KEY_OK_STRING, mOkString);
        outState.putInt(KEY_OK_COLOR, mOkColor);
        outState.putInt(KEY_CANCEL_RESID, mCancelResid);
        outState.putString(KEY_CANCEL_STRING, mCancelString);
        outState.putInt(KEY_CANCEL_COLOR, mCancelColor);
        outState.putSerializable(KEY_TIMEZONE, mTimezone);
        outState.putParcelable(KEY_DATERANGELIMITER, mDateRangeLimiter);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        int listPosition = -1;
        int currentView = mDefaultView;
        if (savedInstanceState != null) {
            mWeekStart = savedInstanceState.getInt(KEY_WEEK_START);
            currentView = savedInstanceState.getInt(KEY_CURRENT_VIEW);
            listPosition = savedInstanceState.getInt(KEY_LIST_POSITION);
            //noinspection unchecked
            highlightedDays = (HashSet<Calendar>) savedInstanceState.getSerializable(KEY_HIGHLIGHTED_DAYS);
            mThemeDark = savedInstanceState.getBoolean(KEY_THEME_DARK);
            mThemeDarkChanged = savedInstanceState.getBoolean(KEY_THEME_DARK_CHANGED);
            mAccentColor = savedInstanceState.getInt(KEY_ACCENT);
            mDismissOnPause = savedInstanceState.getBoolean(KEY_DISMISS);
            mAutoDismiss = savedInstanceState.getBoolean(KEY_AUTO_DISMISS);
            mTitle = savedInstanceState.getString(KEY_TITLE);
            mOkResid = savedInstanceState.getInt(KEY_OK_RESID);
            mOkString = savedInstanceState.getString(KEY_OK_STRING);
            mOkColor = savedInstanceState.getInt(KEY_OK_COLOR);
            mCancelResid = savedInstanceState.getInt(KEY_CANCEL_RESID);
            mCancelString = savedInstanceState.getString(KEY_CANCEL_STRING);
            mCancelColor = savedInstanceState.getInt(KEY_CANCEL_COLOR);
            mTimezone = (TimeZone) savedInstanceState.getSerializable(KEY_TIMEZONE);
            mDateRangeLimiter = savedInstanceState.getParcelable(KEY_DATERANGELIMITER);

            /*
            If the user supplied a custom limiter, we need to create a new default one to prevent
            null pointer exceptions on the configuration methods
            If the user did not supply a custom limiter we need to ensure both mDefaultLimiter
            and mDateRangeLimiter are the same reference, so that the config methods actually
            affect the behaviour of the picker (in the unlikely event the user reconfigures
            the picker when it is shown)
             */
            if (mDateRangeLimiter instanceof DefaultDateRangeLimiter) {
                mDefaultLimiter = (DefaultDateRangeLimiter) mDateRangeLimiter;
            } else {
                mDefaultLimiter = new DefaultDateRangeLimiter();
            }
        }

        mDefaultLimiter.setController(this);

        int viewRes = R.layout.mdtp_date_picker_dialog;
        View view = inflater.inflate(viewRes, container, false);
        // All options have been set at this point: round the initial selection if necessary
        mCalendar = mDateRangeLimiter.setToNearestDate(mCalendar);

        mArrowLeft = view.findViewById(R.id.mdtp_month_arrow_left);
        mArrowRight = view.findViewById(R.id.mdtp_month_arrow_right);
        mDatePickerHeaderView = view.findViewById(R.id.mdtp_date_picker_header);
        mMonthAndDayView = view.findViewById(R.id.mdtp_date_picker_month_and_day);
        mMonthAndDayView.setOnClickListener(this);
        mSelectedMonthTextView = view.findViewById(R.id.mdtp_date_picker_month);
        mSelectedDayTextView = view.findViewById(R.id.mdtp_date_picker_day);
        mYearView = view.findViewById(R.id.mdtp_date_picker_year);
        mMonthPickerView = view.findViewById(R.id.mdtp_month_picker);
        mYearView.setOnClickListener(this);
        mMonthPickerView.setOnClickListener(this);

        final Activity activity = getActivity();
        mDayPickerView = new SimpleDayPickerView(activity, this);

        YearPickerView yearPickerView = new YearPickerView(activity, this);

        mYearPickerPopup = new ListPopupWindow(mYearView.getContext());
        mYearPickerPopup.setAnchorView(mYearView);

        if (Build.VERSION.SDK_INT >= 19) {
            mYearPickerPopup.setPromptView(yearPickerView);
        } else {
            mYearPickerPopup.setOnItemClickListener(yearPickerView.getOnItemClickListener());
        }

        mYearPickerPopup.setAdapter(yearPickerView.getAdapter());

        // if theme mode has not been set by java code, check if it is specified in Style.xml
        if (!mThemeDarkChanged) {
            mThemeDark = Utils.isDarkTheme(activity, mThemeDark);
        }

        Resources res = getResources();
        mDayPickerDescription = res.getString(R.string.mdtp_day_picker_description);
        mSelectDay = res.getString(R.string.mdtp_select_day);

        int bgColorResource = mThemeDark ? R.color.mdtp_date_picker_view_animator_dark_theme : R.color.mdtp_date_picker_view_animator;
        int bgColor = ContextCompat.getColor(activity, bgColorResource);
        view.setBackgroundColor(bgColor);

        mAnimator = view.findViewById(R.id.mdtp_animator);
        mAnimator.setBackgroundColor(bgColor);
        mAnimator.addView(mDayPickerView);
        mAnimator.setDateMillis(mCalendar.getTimeInMillis());
        // TODO: Replace with animation decided upon by the design team.
        Animation animation = new AlphaAnimation(0.0f, 1.0f);
        animation.setDuration(ANIMATION_DURATION);
        mAnimator.setInAnimation(animation);
        // TODO: Replace with animation decided upon by the design team.
        Animation animation2 = new AlphaAnimation(1.0f, 0.0f);
        animation2.setDuration(ANIMATION_DURATION);
        mAnimator.setOutAnimation(animation2);

        Button okButton = view.findViewById(R.id.mdtp_ok);
        okButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                notifyOnDateListener();
                dismiss();
            }
        });
        okButton.setTypeface(TypefaceHelper.get(activity, "Roboto-Medium"));
        if (mOkString != null) {
            okButton.setText(mOkString);
        } else {
            okButton.setText(mOkResid);
        }

        Button cancelButton = view.findViewById(R.id.mdtp_cancel);
        cancelButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getDialog() != null) getDialog().cancel();
            }
        });
        cancelButton.setTypeface(TypefaceHelper.get(activity, "Roboto-Medium"));
        if (mCancelString != null) {
            cancelButton.setText(mCancelString);
        } else {
            cancelButton.setText(mCancelResid);
        }
        cancelButton.setVisibility(isCancelable() ? View.VISIBLE : View.GONE);

        // If an accent color has not been set manually, get it from the context
        if (mAccentColor == -1) {
            mAccentColor = Utils.getAccentColorFromThemeIfAvailable(getActivity());
        }
        if (mDatePickerHeaderView != null) {
            mDatePickerHeaderView.setBackgroundColor(Utils.darkenColor(mAccentColor));
        }
        view.findViewById(R.id.mdtp_day_picker_selected_date_layout).setBackgroundColor(mAccentColor);

        // Buttons can have a different color
        if (mOkColor != -1) okButton.setTextColor(mOkColor);
        else okButton.setTextColor(mAccentColor);
        if (mCancelColor != -1) cancelButton.setTextColor(mCancelColor);
        else cancelButton.setTextColor(mAccentColor);

        if (getDialog() == null) {
            view.findViewById(R.id.mdtp_done_background).setVisibility(View.GONE);
        }

        updateDisplay(false);
        setCurrentView(currentView);

        mArrowLeft.setOnClickListener(this);
        mArrowRight.setOnClickListener(this);
        if (listPosition != -1 && currentView == MONTH_AND_DAY_VIEW) {
            mDayPickerView.postSetSelection(listPosition);
        }

        mHapticFeedbackController = new HapticFeedbackController(activity);
        return view;
    }

    @Override
    public void onConfigurationChanged(final Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        ViewGroup viewGroup = (ViewGroup) getView();
        if (viewGroup != null) {
            viewGroup.removeAllViewsInLayout();
            View view = onCreateView(getActivity().getLayoutInflater(), viewGroup, null);
            viewGroup.addView(view);
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Override
    public void onResume() {
        super.onResume();
        mHapticFeedbackController.start();
    }

    @Override
    public void onPause() {
        super.onPause();
        mHapticFeedbackController.stop();
        if (mDismissOnPause) dismiss();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        if (mOnCancelListener != null) mOnCancelListener.onCancel(dialog);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        if (mOnDismissListener != null) mOnDismissListener.onDismiss(dialog);
    }

    private void setCurrentView(final int viewIndex) {
        long millis = mCalendar.getTimeInMillis();
        ObjectAnimator pulseAnimator = Utils.getPulseAnimator(mMonthAndDayView, 0.9f,
                1.05f);
        if (mDelayAnimation) {
            pulseAnimator.setStartDelay(ANIMATION_DELAY);
            mDelayAnimation = false;
        }
        mDayPickerView.onDateChanged();
        if (mCurrentView != viewIndex) {
            mMonthAndDayView.setSelected(true);
            mYearView.setSelected(false);
            mAnimator.setDisplayedChild(MONTH_AND_DAY_VIEW);
            mCurrentView = viewIndex;
        }
        pulseAnimator.start();

        int flags = DateUtils.FORMAT_SHOW_DATE;
        String dayString = DateUtils.formatDateTime(getActivity(), millis, flags);
        mAnimator.setContentDescription(mDayPickerDescription + ": " + dayString);
        Utils.tryAccessibilityAnnounce(mAnimator, mSelectDay);
    }

    private void updateDisplay(boolean announce) {
        mYearView.setText(YEAR_FORMAT.format(mCalendar.getTime()));

        mMonthPickerView.setText(MONTH_FORMAT_STAND_ALONE.format(mCalendar.getTime()));
        if (mDatePickerHeaderView != null) {
            if (mTitle != null)
                mDatePickerHeaderView.setText(mTitle.toUpperCase(Locale.getDefault()));
            else {
                mDatePickerHeaderView.setText(mCalendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG,
                        Locale.getDefault()).toUpperCase(Locale.getDefault()));
            }
        }
        mSelectedMonthTextView.setText(MONTH_FORMAT.format(mCalendar.getTime()));
        mSelectedDayTextView.setText(DAY_FORMAT.format(mCalendar.getTime()));


        // Accessibility.
        long millis = mCalendar.getTimeInMillis();
        mAnimator.setDateMillis(millis);
        int flags = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NO_YEAR;
        String monthAndDayText = DateUtils.formatDateTime(getActivity(), millis, flags);
        mMonthAndDayView.setContentDescription(monthAndDayText);

        if (announce) {
            flags = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR;
            String fullDateText = DateUtils.formatDateTime(getActivity(), millis, flags);
            Utils.tryAccessibilityAnnounce(mAnimator, fullDateText);
        }
    }

    /**
     * Set whether the picker should dismiss itself when being paused or whether it should try to survive an orientation change
     *
     * @param dismissOnPause true if the dialog should dismiss itself when it's pausing
     */
    public void dismissOnPause(boolean dismissOnPause) {
        mDismissOnPause = dismissOnPause;
    }

    /**
     * Set whether the picker should dismiss itself when a day is selected
     *
     * @param autoDismiss true if the dialog should dismiss itself when a day is selected
     */
    @SuppressWarnings("unused")
    public void autoDismiss(boolean autoDismiss) {
        mAutoDismiss = autoDismiss;
    }

    /**
     * Set whether the dark theme should be used
     *
     * @param themeDark true if the dark theme should be used, false if the default theme should be used
     */
    public void setThemeDark(boolean themeDark) {
        mThemeDark = themeDark;
        mThemeDarkChanged = true;
    }

    /**
     * Returns true when the dark theme should be used
     *
     * @return true if the dark theme should be used, false if the default theme should be used
     */
    @Override
    public boolean isThemeDark() {
        return mThemeDark;
    }

    /**
     * Set the accent color of this dialog
     *
     * @param color the accent color you want
     */
    @SuppressWarnings("unused")
    public void setAccentColor(String color) {
        mAccentColor = Color.parseColor(color);
    }

    /**
     * Set the accent color of this dialog
     *
     * @param color the accent color you want
     */
    public void setAccentColor(@ColorInt int color) {
        mAccentColor = Color.argb(255, Color.red(color), Color.green(color), Color.blue(color));
    }

    /**
     * Set the text color of the OK button
     *
     * @param color the color you want
     */
    @SuppressWarnings("unused")
    public void setOkColor(String color) {
        mOkColor = Color.parseColor(color);
    }

    /**
     * Set the text color of the OK button
     *
     * @param color the color you want
     */
    @SuppressWarnings("unused")
    public void setOkColor(@ColorInt int color) {
        mOkColor = Color.argb(255, Color.red(color), Color.green(color), Color.blue(color));
    }

    /**
     * Set the text color of the Cancel button
     *
     * @param color the color you want
     */
    @SuppressWarnings("unused")
    public void setCancelColor(String color) {
        mCancelColor = Color.parseColor(color);
    }

    /**
     * Set the text color of the Cancel button
     *
     * @param color the color you want
     */
    @SuppressWarnings("unused")
    public void setCancelColor(@ColorInt int color) {
        mCancelColor = Color.argb(255, Color.red(color), Color.green(color), Color.blue(color));
    }

    /**
     * Get the accent color of this dialog
     *
     * @return accent color
     */
    @Override
    public int getAccentColor() {
        return mAccentColor;
    }

    /**
     * Set whether the year picker of the month and day picker is shown first
     *
     * @param yearPicker boolean
     */
    public void showYearPickerFirst(boolean yearPicker) {
        mDefaultView = yearPicker ? YEAR_VIEW : MONTH_AND_DAY_VIEW;
    }

    @SuppressWarnings("unused")
    public void setFirstDayOfWeek(int startOfWeek) {
        if (startOfWeek < Calendar.SUNDAY || startOfWeek > Calendar.SATURDAY) {
            throw new IllegalArgumentException("Value must be between Calendar.SUNDAY and " +
                    "Calendar.SATURDAY");
        }
        mWeekStart = startOfWeek;
        if (mDayPickerView != null) {
            mDayPickerView.onChange();
        }
    }

    @SuppressWarnings("unused")
    public void setYearRange(int startYear, int endYear) {
        mDefaultLimiter.setYearRange(startYear, endYear);

        if (mDayPickerView != null) {
            mDayPickerView.onChange();
        }
    }

    /**
     * Sets the minimal date supported by this DatePicker. Dates before (but not including) the
     * specified date will be disallowed from being selected.
     *
     * @param calendar a Calendar object set to the year, month, day desired as the mindate.
     */
    @SuppressWarnings("unused")
    public void setMinDate(Calendar calendar) {
        mDefaultLimiter.setMinDate(calendar);

        if (mDayPickerView != null) {
            mDayPickerView.onChange();
        }
    }

    public void setMinDate(long millis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(millis);
        setMinDate(calendar);
    }

    /**
     * @return The minimal date supported by this DatePicker. Null if it has not been set.
     */
    @SuppressWarnings("unused")
    public Calendar getMinDate() {
        return mDefaultLimiter.getMinDate();
    }

    /**
     * Sets the minimal date supported by this DatePicker. Dates after (but not including) the
     * specified date will be disallowed from being selected.
     *
     * @param calendar a Calendar object set to the year, month, day desired as the maxdate.
     */
    @SuppressWarnings("unused")
    public void setMaxDate(Calendar calendar) {
        mDefaultLimiter.setMaxDate(calendar);

        if (mDayPickerView != null) {
            mDayPickerView.onChange();
        }
    }

    public void setMaxDate(long millis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(millis);
        setMaxDate(calendar);
    }

    /**
     * @return The maximal date supported by this DatePicker. Null if it has not been set.
     */
    @SuppressWarnings("unused")
    public Calendar getMaxDate() {
        return mDefaultLimiter.getMaxDate();
    }

    /**
     * Sets an array of dates which should be highlighted when the picker is drawn
     *
     * @param highlightedDays an Array of Calendar objects containing the dates to be highlighted
     */
    @SuppressWarnings("unused")
    public void setHighlightedDays(Calendar[] highlightedDays) {
        for (Calendar highlightedDay : highlightedDays) Utils.trimToMidnight(highlightedDay);
        this.highlightedDays.addAll(Arrays.asList(highlightedDays));
        if (mDayPickerView != null) mDayPickerView.onChange();
    }

    /**
     * @return The list of dates, as Calendar Objects, which should be highlighted. null is no dates should be highlighted
     */
    @SuppressWarnings("unused")
    public Calendar[] getHighlightedDays() {
        if (highlightedDays.isEmpty()) return null;
        Calendar[] output = highlightedDays.toArray(new Calendar[0]);
        Arrays.sort(output);
        return output;
    }

    @Override
    public boolean isHighlighted(int year, int month, int day) {
        Calendar date = Calendar.getInstance();
        date.set(Calendar.YEAR, year);
        date.set(Calendar.MONTH, month);
        date.set(Calendar.DAY_OF_MONTH, day);
        Utils.trimToMidnight(date);
        return highlightedDays.contains(date);
    }

    /**
     * Sets a list of days which are the only valid selections.
     * Setting this value will take precedence over using setMinDate() and setMaxDate()
     *
     * @param selectableDays an Array of Calendar Objects containing the selectable dates
     */
    @SuppressWarnings("unused")
    public void setSelectableDays(Calendar[] selectableDays) {
        mDefaultLimiter.setSelectableDays(selectableDays);
        if (mDayPickerView != null) mDayPickerView.onChange();
    }

    /**
     * @return an Array of Calendar objects containing the list with selectable items. null if no restriction is set
     */
    @SuppressWarnings("unused")
    public Calendar[] getSelectableDays() {
        return mDefaultLimiter.getSelectableDays();
    }

    /**
     * Sets a list of days that are not selectable in the picker
     * Setting this value will take precedence over using setMinDate() and setMaxDate(), but stacks with setSelectableDays()
     *
     * @param disabledDays an Array of Calendar Objects containing the disabled dates
     */
    @SuppressWarnings("unused")
    public void setDisabledDays(Calendar[] disabledDays) {
        mDefaultLimiter.setDisabledDays(disabledDays);
        if (mDayPickerView != null) mDayPickerView.onChange();
    }

    /**
     * @return an Array of Calendar objects containing the list of days that are not selectable. null if no restriction is set
     */
    @SuppressWarnings("unused")
    public Calendar[] getDisabledDays() {
        return mDefaultLimiter.getDisabledDays();
    }

    /**
     * Provide a DateRangeLimiter for full control over which dates are enabled and disabled in the picker
     *
     * @param dateRangeLimiter An implementation of the DateRangeLimiter interface
     */
    @SuppressWarnings("unused")
    public void setDateRangeLimiter(DateRangeLimiter dateRangeLimiter) {
        mDateRangeLimiter = dateRangeLimiter;
    }

    /**
     * Set a title to be displayed instead of the weekday
     *
     * @param title String - The title to be displayed
     */
    public void setTitle(String title) {
        mTitle = title;
    }

    /**
     * Set the label for the Ok button (max 12 characters)
     *
     * @param okString A literal String to be used as the Ok button label
     */
    @SuppressWarnings("unused")
    public void setOkText(String okString) {
        mOkString = okString;
    }

    /**
     * Set the label for the Ok button (max 12 characters)
     *
     * @param okResid A resource ID to be used as the Ok button label
     */
    @SuppressWarnings("unused")
    public void setOkText(@StringRes int okResid) {
        mOkString = null;
        mOkResid = okResid;
    }

    /**
     * Set the label for the Cancel button (max 12 characters)
     *
     * @param cancelString A literal String to be used as the Cancel button label
     */
    @SuppressWarnings("unused")
    public void setCancelText(String cancelString) {
        mCancelString = cancelString;
    }

    /**
     * Set the label for the Cancel button (max 12 characters)
     *
     * @param cancelResid A resource ID to be used as the Cancel button label
     */
    @SuppressWarnings("unused")
    public void setCancelText(@StringRes int cancelResid) {
        mCancelString = null;
        mCancelResid = cancelResid;
    }

    /**
     * Set which timezone the picker should use
     *
     * @param timeZone The timezone to use
     */
    @SuppressWarnings("unused")
    public void setTimeZone(TimeZone timeZone) {
        mTimezone = timeZone;
        mCalendar.setTimeZone(timeZone);
        YEAR_FORMAT.setTimeZone(timeZone);
        MONTH_FORMAT.setTimeZone(timeZone);
        DAY_FORMAT.setTimeZone(timeZone);
    }

    @SuppressWarnings("unused")
    public void setOnDateSetListener(OnDateSetListener listener) {
        mCallBack = listener;
    }

    @SuppressWarnings("unused")
    public void setOnCancelListener(DialogInterface.OnCancelListener onCancelListener) {
        mOnCancelListener = onCancelListener;
    }

    @SuppressWarnings("unused")
    public void setOnDismissListener(DialogInterface.OnDismissListener onDismissListener) {
        mOnDismissListener = onDismissListener;
    }

    /**
     * Get a reference to the callback
     *
     * @return OnDateSetListener the callback
     */
    @SuppressWarnings("unused")
    public OnDateSetListener getOnDateSetListener() {
        return mCallBack;
    }

    // If the newly selected month / year does not contain the currently selected day number,
    // change the selected day number to the last day of the selected month or year.
    //      e.g. Switching from Mar to Apr when Mar 31 is selected -> Apr 30
    //      e.g. Switching from 2012 to 2013 when Feb 29, 2012 is selected -> Feb 28, 2013
    private Calendar adjustDayInMonthIfNeeded(Calendar calendar) {
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        if (day > daysInMonth) {
            calendar.set(Calendar.DAY_OF_MONTH, daysInMonth);
        }
        return mDateRangeLimiter.setToNearestDate(calendar);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.mdtp_date_picker_year) {
            mYearPickerPopup.show();
        } else if (v.getId() == R.id.mdtp_date_picker_month_and_day) {
            setCurrentView(MONTH_AND_DAY_VIEW);
        } else if (v.getId() == R.id.mdtp_month_picker) {
            PopupMenu popupMenu = new PopupMenu(mMonthPickerView.getContext(), mMonthPickerView);

            Calendar calendar = Calendar.getInstance();
            for (int i = 0; i < 12; i++) {
                calendar.set(0, i + 1, 0);
                popupMenu.getMenu().add(Menu.NONE, i, 1, MONTH_FORMAT_STAND_ALONE.format(calendar.getTime()));
            }
            popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    mDayPickerView.scrollToMonth(item.getItemId());
                    mMonthPickerView.setText(item.getTitle());
                    return false;
                }
            });
            popupMenu.show();
        } else if (v.getId() == R.id.mdtp_month_arrow_left) {
            mDayPickerView.scrollToPrevMonth();
        } else if (v.getId() == R.id.mdtp_month_arrow_right) {
            mDayPickerView.scrollToNextMonth();
        }
    }

    @Override
    public void onYearSelected(int year) {
        mYearPickerPopup.dismiss();
        mCalendar.set(Calendar.YEAR, year);
        mCalendar = adjustDayInMonthIfNeeded(mCalendar);
        updatePickers();
        setCurrentView(MONTH_AND_DAY_VIEW);
        updateDisplay(true);
    }

    @Override
    public void onDayOfMonthSelected(int year, int month, int day) {
        mCalendar.set(Calendar.YEAR, year);
        mCalendar.set(Calendar.MONTH, month);
        mCalendar.set(Calendar.DAY_OF_MONTH, day);
        updatePickers();
        updateDisplay(true);
        if (mAutoDismiss) {
            notifyOnDateListener();
            dismiss();
        }
    }

    private void updatePickers() {
        for (OnDateChangedListener listener : mListeners) listener.onDateChanged();
    }


    @Override
    public MonthAdapter.CalendarDay getSelectedDay() {
        return new MonthAdapter.CalendarDay(mCalendar, getTimeZone());
    }

    @Override
    public Calendar getStartDate() {
        return mDateRangeLimiter.getStartDate();
    }

    @Override
    public Calendar getEndDate() {
        return mDateRangeLimiter.getEndDate();
    }

    @Override
    public int getMinYear() {
        return mDateRangeLimiter.getMinYear();
    }

    @Override
    public int getMaxYear() {
        return mDateRangeLimiter.getMaxYear();
    }


    @Override
    public boolean isOutOfRange(int year, int month, int day) {
        return mDateRangeLimiter.isOutOfRange(year, month, day);
    }

    @Override
    public int getFirstDayOfWeek() {
        return mWeekStart;
    }

    @Override
    public void registerOnDateChangedListener(OnDateChangedListener listener) {
        mListeners.add(listener);
    }

    @Override
    public void unregisterOnDateChangedListener(OnDateChangedListener listener) {
        mListeners.remove(listener);
    }

    @Override
    public TimeZone getTimeZone() {
        return mTimezone == null ? TimeZone.getDefault() : mTimezone;
    }

    public void notifyOnDateListener() {
        if (mCallBack != null) {
            mCallBack.onDateSet(DatePickerFragmentDialog.this, mCalendar.get(Calendar.YEAR),
                    mCalendar.get(Calendar.MONTH), mCalendar.get(Calendar.DAY_OF_MONTH));
        }
    }
}
