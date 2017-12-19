package com.shagi.example

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.shagi.materialdatepicker.date.DatePickerFragmentDialog
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        button.setOnClickListener {
            val dialog = DatePickerFragmentDialog.newInstance({ view, year, monthOfYear, dayOfMonth -> },
                    2017, 11, 4)
            dialog.show(supportFragmentManager, "tag")
        }

        button2.setOnClickListener(PopupTest().showPopupWindow())
    }
}
