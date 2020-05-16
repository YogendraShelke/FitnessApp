package com.yogendra.fitnessapp

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.icu.util.Calendar
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.request.DataReadRequest
import java.util.*


class MainActivity : AppCompatActivity() {

    private val fitnessOptions = FitnessOptions.builder()
        .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
        .build()

    private val activityRequestCode = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            checkPermissions()
        } else {
            signIn()
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACTIVITY_RECOGNITION), activityRequestCode)
        } else {
            signIn()
        }
    }

    private fun signIn() {
        val account = GoogleSignIn.getAccountForExtension(this, fitnessOptions)
        if (!GoogleSignIn.hasPermissions(account, fitnessOptions)) {
            GoogleSignIn.requestPermissions(this, activityRequestCode, account,  fitnessOptions)
        } else {
            accessGoogleFit();
        }
    }

    private fun getDates(): Pair<Long, Long> {
        val cal: Calendar = Calendar.getInstance()
        cal.time = Date()
        val endTime: Long = cal.timeInMillis
        cal.add(Calendar.YEAR, -1)
        val startTime: Long = cal.timeInMillis
        return Pair(startTime, endTime)
    }

    private fun accessGoogleFit() {
        val dates = getDates()
        val readRequest = DataReadRequest.Builder()
            .aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
            .setTimeRange(dates.first, dates.second, java.util.concurrent.TimeUnit.MILLISECONDS)
            .bucketByTime(1, java.util.concurrent.TimeUnit.DAYS)
            .build()
        val account = GoogleSignIn.getAccountForExtension(this, fitnessOptions)

        Fitness.getHistoryClient(this, account).readData(readRequest)
            .addOnSuccessListener { response ->
                print(response)
                Toast.makeText(this, response.toString(), Toast.LENGTH_LONG).show()
            }
            .addOnFailureListener { e ->
                print(e.localizedMessage)
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && requestCode == activityRequestCode) {
            accessGoogleFit()
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == activityRequestCode && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            checkPermissions()
        }
    }
}
