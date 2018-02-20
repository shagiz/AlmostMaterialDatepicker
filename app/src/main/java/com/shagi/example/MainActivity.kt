package com.shagi.example

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.shagi.materialdatepicker.date.DatePickerFragmentDialog
import kotlinx.android.synthetic.main.activity_main.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        button.setOnClickListener {
            val dialog = DatePickerFragmentDialog.newInstance({ view, year, monthOfYear, dayOfMonth ->
                val calendar = Calendar.getInstance()
                calendar.set(year, monthOfYear, dayOfMonth)

                val simpleDateFormat = SimpleDateFormat("yyyy-MMM-dd", Locale.getDefault())

                Toast.makeText(applicationContext, simpleDateFormat.format(calendar.time), Toast.LENGTH_SHORT).show()
            }, 2017, 11, 4)
            dialog.show(supportFragmentManager, "tag")
        }
    }
}
