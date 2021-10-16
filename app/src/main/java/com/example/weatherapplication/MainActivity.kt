package com.example.weatherapplication

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.weatherapplication.models.WeatherResponse
import com.example.weatherapplication.network.WeatherService
import com.example.weatherapplication.utils.Constants
import com.google.android.gms.location.*
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import android.widget.TextView

class MainActivity : AppCompatActivity() {

    // This variable is required to gather user location; longitude and latitude
    private lateinit var myFusedLocationClient: FusedLocationProviderClient
    private var myProgressDialog: Dialog? = null

    private lateinit var textViewMain: TextView
    private lateinit var textViewMainDescription: TextView
    private lateinit var textViewHumidityMain: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initViews()
        myFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        requestPermission()

    }

    /** Function to initialize the view */
    private fun initViews() {
        textViewMain = findViewById<TextView>(R.id.textViewMain)
        textViewMainDescription = findViewById<TextView>(R.id.textViewMainDescription)
        textViewHumidityMain = findViewById<TextView>(R.id.textViewHumidityMain)
    }

    /** This variable is to access user's longitude and latitude values */
    private val myLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val myLastLocation: Location = locationResult.lastLocation
            val latitude = myLastLocation.latitude
            val longitude = myLastLocation.longitude
            Log.i("Current Latitude: ", latitude.toString())
            Log.i("Current Longitude: ", longitude.toString())

            getLocationWeatherDetails(latitude, longitude)
        }
    }

    /** This function returns the weather details on user's current location */
    private fun getLocationWeatherDetails(latitude: Double, longitude: Double) {
        if (Constants.isNetworkAvailable(this)) {
            /** Build a retrofit object with the given URL and convert
             * it via Gson factory into the needed format
             */
            val retrofit: Retrofit = Retrofit.Builder().baseUrl(Constants.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create()).build()

            /** Create a service based on the retrofit that is created */
            val service: WeatherService =
                retrofit.create<WeatherService>(WeatherService::class.java)

            /** Make a list call with the created service */
            val listCall: Call<WeatherResponse> =
                service.getWeather(latitude, longitude, Constants.METRIC_UNIT, Constants.APP_ID)

            showCustomProgressDialog()

            listCall.enqueue(object : Callback<WeatherResponse> {
                override fun onResponse(
                    call: Call<WeatherResponse>,
                    response: Response<WeatherResponse>
                ) {
                    if (response.isSuccessful) {
                        hideCustomProgressDialog()

                        val weatherList: WeatherResponse? = response.body()
                        if (weatherList != null) {
                            setUI(weatherList)
                        }
                        Log.i("Response Result", "$weatherList")
                    } else {
                        when (response.code()) {
                            400 -> {
                                Log.e("Error 400", "Bad Connection")
                            }
                            404 -> {
                                Log.e("Error 404", "Not Found")
                            }
                            else -> {
                                Log.e("Error", "Generic Error")
                            }
                        }
                    }
                }

                override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                    hideCustomProgressDialog()
                    Log.e("ERROR", t.message.toString())
                }

            })
        } else {
            Toast.makeText(
                this@MainActivity,
                "You are not connected to the internet.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /** This function is to request permissions from the user to allow
     * location services that needs to be provided to the application itself */
    private fun requestPermission() {
        /** Check location service access is granted or not */
        if (!isLocationEnabled()) {
            Toast.makeText(
                this,
                "Your location is turned off, please turn it on",
                Toast.LENGTH_SHORT
            ).show()

            /** Create an intent to access the settings for the GPS provider in the settings */
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        } else {
            /** If location services are not granted then... */
            Dexter.withContext(this).withPermissions(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ).withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    /** If all the permissions are granted, call the requestLocationData function
                     * to gather the longitude and latitude */
                    if (report!!.areAllPermissionsGranted()) {
                        requestLocationData()
                    }

                    if (report.isAnyPermissionPermanentlyDenied) {
                        Toast.makeText(
                            this@MainActivity,
                            "You have denied location permission, please allow to use the application.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                /** If permissions are not granted than call the
                 * showRationalDialogForPermission to direct application settings */
                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>?,
                    token: PermissionToken?
                ) {
                    showRationalDialogForPermission()
                }
            }).onSameThread().check()
        }
    }

    /** This function updates the user's location and stores it in the myFusedLocationClient*/
    @SuppressLint("MissingPermission")
    private fun requestLocationData() {
        val myLocationRequest = LocationRequest()
        myLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        myFusedLocationClient.requestLocationUpdates(
            myLocationRequest,
            myLocationCallback,
            Looper.myLooper()
        )
    }

    /** This function is to create an Alert Dialog for accessing the application settings for location services */
    private fun showRationalDialogForPermission() {
        AlertDialog.Builder(this)
            .setMessage("It looks like you have turned off permissions. It can be enabled under application settings.")
            .setPositiveButton(
                "Go to Settings"
            ) { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }.show()
    }

    /** This function checks if the user has allowed location services for the application or not */
    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    /** This function display the progress dialog bar */
    private fun showCustomProgressDialog() {
        myProgressDialog = Dialog(this)
        myProgressDialog!!.setContentView(R.layout.dialog_custom_progress)
        myProgressDialog!!.show()
    }

    /** This function hides the progress dialog bar */
    private fun hideCustomProgressDialog() {
        if(myProgressDialog != null) {
            myProgressDialog!!.dismiss()
        }
    }

    /** This function is to set the UI of the application */
    private fun setUI(weatherList: WeatherResponse) {
        for (i in weatherList.weather.indices) {
            Log.i("Weather Name", weatherList.weather.toString())

            textViewMain.text = weatherList.weather[i].main
            textViewMainDescription.text = weatherList.weather[i].description
            // Should not add concatenated strings to texts. Instead use a string resource value
            // and than add strings together
            textViewHumidityMain.text = getString(R.string.humidity_string, weatherList.main.humidity.toString(), getUnit(application.resources.configuration.toString()))
        }
    }

    private fun getUnit(value: String): String {
        var sign = ""
        sign = if (value == "US" || value == "LR" || value == "MM") {
            "°F"
        } else {
            "°C"
        }
        return sign
    }
}