package com.weather.weather;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

public class HomeActivity extends AppCompatActivity implements LocationListener {

    private static final String TAG = "HomeActivity";

    //saving API key from website in a variable in order to use
    private static final String API_KEY = "55295393bbe443daa2b161302241204";

    private TextView locationTextView;
    private TextView weatherTextView;
    private TextView temperatureTextView;
    private TextView feelsLikeTextView;
    private TextView windSpeedTextView;
    private TextView windDirectionTextView;
    private TextView humidityTextView;
    private TextView uvIndexTextView;
    private TextView visibilityTextView;

    private LocationManager locationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        locationTextView = findViewById(R.id.location_text_view);
        weatherTextView = findViewById(R.id.weather_text_view);
        temperatureTextView = findViewById(R.id.temperature_text_view);
        feelsLikeTextView = findViewById(R.id.feels_like_text_view);
        windSpeedTextView = findViewById(R.id.wind_speed_text_view);
        windDirectionTextView = findViewById(R.id.wind_direction_text_view);
        humidityTextView = findViewById(R.id.humidity_text_view);
        uvIndexTextView = findViewById(R.id.uv_index_text_view);
        visibilityTextView = findViewById(R.id.visibility_text_view);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        // Check for location permission both precise and non precise
        //Manifest.permission is a constant class in android hat manager permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    1);
            return;
        }

        // Request location updates
        try {
            // 0,0 means it will constantly keep on getting location updates
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        } catch (SecurityException e) {
            Log.e(TAG, "Failed to request location updates: " + e.getMessage());
        }
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        Log.d(TAG, "Location changed: " + location.getLatitude() + ", " + location.getLongitude());

        // Convert coordinates to location name
        String locationName = getLocationName(location.getLatitude(), location.getLongitude());
        // Update location TextView with location name
        locationTextView.setText(locationName);

        // Call the weather API
        fetchWeatherData(location.getLatitude(), location.getLongitude());
    }

    private String getLocationName(double latitude, double longitude) {
        //initializing geocoder (class provided by android SDK) this means current location if that is not found it gets the default location of the device
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            //LIST  of addresses, but in this case only 1 address is stored which is defined by last parameter
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && addresses.size() > 0) {
                //gets the first element from the list of address
                Address address = addresses.get(0);
                String city = address.getLocality();
                String area = address.getSubAdminArea();
                String country = address.getCountryName();

                // Construct the location name including area or city and country and append them in the string builder one by one
                StringBuilder locationNameBuilder = new StringBuilder();
                if (area != null) {
                    locationNameBuilder.append(area);
                } else if (city != null) {
                    locationNameBuilder.append(city);
                }
                if (country != null) {
                    if (locationNameBuilder.length() > 0) {
                        locationNameBuilder.append(", ");
                    }
                    locationNameBuilder.append(country);
                }
                return locationNameBuilder.toString();
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to get location name: " + e.getMessage());
        }
        return "Unknown";
    }


    private void fetchWeatherData(double latitude, double longitude) {
        //initialize a new thread that can run along with the main code
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection connection = null;
                try {
                    //set weather api url
                    URL url = new URL("https://api.weatherapi.com/v1/current.json?key=" + API_KEY +
                            "&q=" + latitude + "," + longitude);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");

                    //get input stream from weatherAPI
                    //input stream is java library used for reading data, we get it from connection(the url we created above)
                    InputStream inputStream = connection.getInputStream();
                    Scanner scanner = new Scanner(inputStream);
                    StringBuilder response = new StringBuilder();
                    //read API response
                    while (scanner.hasNext()) {
                        response.append(scanner.nextLine());
                    }

                    //parsing JSON response and saving the needed details in respective variables
                    JSONObject jsonResponse = new JSONObject(response.toString());
                    JSONObject current = jsonResponse.getJSONObject("current");
                    JSONObject condition = current.getJSONObject("condition");
                    String weatherDescription = condition.getString("text");
                    double temperature = current.getDouble("temp_c");
                    double feelsLikeTemperature = current.getDouble("feelslike_c");
                    double windSpeed = current.getDouble("wind_kph");
                    String windDirection = current.getString("wind_dir");
                    int humidity = current.getInt("humidity");
                    int uvIndex = current.getInt("uv");
                    double visibility = current.getDouble("vis_km");


                    //update UI with weather information
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // Display weather information
                            weatherTextView.setText(weatherDescription);
                            temperatureTextView.setText(String.valueOf(temperature) + "°C");
                            feelsLikeTextView.setText(String.valueOf(feelsLikeTemperature) +  "°C");
                            windSpeedTextView.setText(String.valueOf(windSpeed) + "Km/h");
                            windDirectionTextView.setText(String.valueOf(windDirection));
                            humidityTextView.setText(String.valueOf(humidity) + "%");
                            uvIndexTextView.setText(String.valueOf(uvIndex));
                            visibilityTextView.setText(String.valueOf(visibility) + "Km");
                        }
                    });

                } catch (IOException | JSONException e) {
                    Log.e(TAG, "Failed to fetch weather data: " + e.getMessage());
                } finally {
                    //disconnect API connection
                    if (connection != null) {
                        connection.disconnect();
                    }
                }
            }
        }).start();
    }


    //Methods required by LocationListener interface but can be removed
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }
}
