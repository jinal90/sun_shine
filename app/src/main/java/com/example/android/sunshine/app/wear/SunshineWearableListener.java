package com.example.android.sunshine.app.wear;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.format.Time;
import android.util.Log;
import android.widget.Toast;

import com.example.android.sunshine.app.R;
import com.example.android.sunshine.app.Utility;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;

import static com.example.android.sunshine.app.ForecastFragment.LOG_TAG;
import static com.example.android.sunshine.app.sync.SunshineSyncAdapter.LOCATION_STATUS_INVALID;
import static com.example.android.sunshine.app.sync.SunshineSyncAdapter.LOCATION_STATUS_OK;
import static com.example.android.sunshine.app.sync.SunshineSyncAdapter.LOCATION_STATUS_SERVER_DOWN;
import static com.example.android.sunshine.app.sync.SunshineSyncAdapter.LOCATION_STATUS_SERVER_INVALID;
import static com.example.android.sunshine.app.sync.SunshineSyncAdapter.LOCATION_STATUS_UNKNOWN;

/**
 * Created by jinal on 9/14/2016.
 */

public class SunshineWearableListener extends WearableListenerService {

    private Context mContext;
    private GoogleApiClient googleClient;
    private Node peerNode;
    private WeatherWear weatherObj;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;

        googleClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle connectionHint) {
                        Wearable.DataApi.addListener(googleClient, SunshineWearableListener.this);
                        Wearable.MessageApi.addListener(googleClient, SunshineWearableListener.this);
                    }

                    @Override
                    public void onConnectionSuspended(int cause) {
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult result) {
                    }
                })
                .addApi(Wearable.API)
                .build();

        googleClient.connect();
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        super.onDataChanged(dataEvents);
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        super.onMessageReceived(messageEvent);
        Toast.makeText(this, "onMessageReceived1", Toast.LENGTH_SHORT).show();
        //Check which button is clicked based on path sent from wearable
        if (messageEvent.getPath().equals("/fetchCurrentWeather")) {
            Toast.makeText(this, "message received", Toast.LENGTH_SHORT).show();

            new WeatherAsyncTask().execute();
        }
    }

    public void sendMessage(final String msg) {
        // Toast.makeText(ctx, "sendMessage", Toast.LENGTH_SHORT).show();
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                String path = "/showErrorMessage";

                NodeApi.GetConnectedNodesResult rawNodes =
                        Wearable.NodeApi.getConnectedNodes(googleClient).await();

                for (final Node node : rawNodes.getNodes()) {
                    PendingResult<MessageApi.SendMessageResult> result = Wearable.MessageApi.sendMessage(
                            googleClient,
                            node.getId(),
                            path,
                            msg.getBytes()
                    );

                    result.setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                        @Override
                        public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                            //  The message is done sending.
                            //  This doesn't mean it worked, though.
                            peerNode = node;    //  Save the node that worked so we don't have to loop again.
                        }
                    });
                }
            }
        });
        t.start();
    }

    public void sendCurrentWeatherData() {
        Thread t = new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        Calendar cal = Calendar.getInstance();

                        DataMap d = new DataMap();
                        d.putString("name", "Jinal");
                        d.putString("date", cal.get(Calendar.HOUR) + cal.get(Calendar.SECOND) + "");

                        if (weatherObj != null) {

                            d.putString("dateTime", Utility.getFriendlyDayString(mContext, weatherObj.getDateTime(), true));
                            d.putString("pressure", mContext.getString(R.string.format_pressure, weatherObj.getPressure()));
                            d.putString("humidity", mContext.getString(R.string.format_humidity, (float)weatherObj.getHumidity()));
                            d.putString("wind", Utility.getFormattedWind(mContext,
                                    (float) weatherObj.getWindSpeed(),
                                    (float) weatherObj.getWindDirection()));
                            d.putString("high", Utility.formatTemperature(mContext, weatherObj.getHigh()));
                            d.putString("low", Utility.formatTemperature(mContext, weatherObj.getLow()));
                            d.putString("description", weatherObj.getDescription());
                            d.putInt("weatherId", 300);
                        }

                        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create("/showCurrentWeather");
                        putDataMapRequest.getDataMap().putDataMap("weatherData", d);
                        //Toast.makeText(ctx, "send data item 1", Toast.LENGTH_SHORT).show();
                        PutDataRequest request = putDataMapRequest.asPutDataRequest();

                        Wearable.DataApi.putDataItem(googleClient, request).await();
                        Wearable.DataApi.putDataItem(googleClient, request)
                                .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                                    @Override
                                    public void onResult(DataApi.DataItemResult dataItemResult) {
                                        if (!dataItemResult.getStatus().isSuccess()) {
                                            weatherObj = null;
                                        } else {
                                        }
                                    }
                                });
                    }
                }
        );
        t.start();

    }

    public class WeatherAsyncTask extends AsyncTask<String, Integer, Integer> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Integer doInBackground(String[] params) {

            Log.d(LOG_TAG, "Starting sync");

            // We no longer need just the location String, but also potentially the latitude and
            // longitude, in case we are syncing based on a new Place Picker API result.
            String locationQuery = Utility.getPreferredLocation(mContext);
            String locationLatitude = String.valueOf(Utility.getLocationLatitude(mContext));
            String locationLongitude = String.valueOf(Utility.getLocationLongitude(mContext));

            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String forecastJsonStr = null;

            String format = "json";
            String units = "metric";
            int numDays = 14;

            try {
                // Construct the URL for the OpenWeatherMap query
                // Possible parameters are avaiable at OWM's forecast API page, at
                // http://openweathermap.org/API#forecast
                final String FORECAST_BASE_URL =
                        "http://api.openweathermap.org/data/2.5/forecast/daily?";
                final String QUERY_PARAM = "q";
                final String LAT_PARAM = "lat";
                final String LON_PARAM = "lon";
                final String FORMAT_PARAM = "mode";
                final String UNITS_PARAM = "units";
                final String DAYS_PARAM = "cnt";
                final String APPID_PARAM = "APPID";

                Uri.Builder uriBuilder = Uri.parse(FORECAST_BASE_URL).buildUpon();

                // Instead of always building the query based off of the location string, we want to
                // potentially build a query using a lat/lon value. This will be the case when we are
                // syncing based off of a new location from the Place Picker API. So we need to check
                // if we have a lat/lon to work with, and use those when we do. Otherwise, the weather
                // service may not understand the location address provided by the Place Picker API
                // and the user could end up with no weather! The horror!
                if (Utility.isLocationLatLonAvailable(mContext)) {
                    uriBuilder.appendQueryParameter(LAT_PARAM, locationLatitude)
                            .appendQueryParameter(LON_PARAM, locationLongitude);
                } else {
                    uriBuilder.appendQueryParameter(QUERY_PARAM, locationQuery);
                }

                Uri builtUri = uriBuilder.appendQueryParameter(FORMAT_PARAM, format)
                        .appendQueryParameter(UNITS_PARAM, units)
                        .appendQueryParameter(DAYS_PARAM, Integer.toString(numDays))
                        //.appendQueryParameter(APPID_PARAM, BuildConfig.OPEN_WEATHER_MAP_API_KEY)
                        .appendQueryParameter(APPID_PARAM, "e6f407196ddee5a2b4b04236f0a04b4c")
                        .build();

                URL url = new URL(builtUri.toString());

                // Create the request to OpenWeatherMap, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return LOCATION_STATUS_SERVER_DOWN;
                }
                forecastJsonStr = buffer.toString();
                return getWeatherDataFromJson(forecastJsonStr, locationQuery);
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                // If the code didn't successfully get the weather data, there's no point in attempting
                // to parse it.
                return LOCATION_STATUS_SERVER_DOWN;
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
                return LOCATION_STATUS_SERVER_INVALID;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }
        }

        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);

            switch (result) {

                case LOCATION_STATUS_SERVER_INVALID:
                    sendMessage("LOCATION_STATUS_SERVER_INVALID");
                    break;
                case LOCATION_STATUS_SERVER_DOWN:
                    sendMessage("LOCATION_STATUS_SERVER_DOWN");
                    break;
                case LOCATION_STATUS_INVALID:
                    sendMessage("LOCATION_STATUS_INVALID");
                    break;
                case LOCATION_STATUS_OK:
                    if (weatherObj != null) {
                        sendCurrentWeatherData();
                    } else {
                        sendMessage("LOCATION_STATUS_INVALID");
                    }
                    break;
                case LOCATION_STATUS_UNKNOWN:
                    sendMessage("LOCATION_STATUS_UNKNOWN");
                    break;
                default:
                    sendMessage("Error");
                    break;

            }
        }
    }


    /**
     * Take the String representing the complete forecast in JSON Format and
     * pull out the data we need to construct the Strings needed for the wireframes.
     * <p>
     * Fortunately parsing is easy:  constructor takes the JSON string and converts it
     * into an Object hierarchy for us.
     */
    private Integer getWeatherDataFromJson(String forecastJsonStr,
                                           String locationSetting)
            throws JSONException {

        final String OWM_LIST = "list";
        final String OWM_PRESSURE = "pressure";
        final String OWM_HUMIDITY = "humidity";
        final String OWM_WINDSPEED = "speed";
        final String OWM_WIND_DIRECTION = "deg";
        final String OWM_TEMPERATURE = "temp";
        final String OWM_MAX = "max";
        final String OWM_MIN = "min";
        final String OWM_WEATHER = "weather";
        final String OWM_DESCRIPTION = "main";
        final String OWM_WEATHER_ID = "id";
        final String OWM_MESSAGE_CODE = "cod";

        try {
            JSONObject forecastJson = new JSONObject(forecastJsonStr);

            // do we have an error?
            if (forecastJson.has(OWM_MESSAGE_CODE)) {
                int errorCode = forecastJson.getInt(OWM_MESSAGE_CODE);

                switch (errorCode) {
                    case HttpURLConnection.HTTP_OK:
                        break;
                    case HttpURLConnection.HTTP_NOT_FOUND:
                        return LOCATION_STATUS_INVALID;
                    default:
                        return LOCATION_STATUS_SERVER_DOWN;
                }
            }

            JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

            Time dayTime = new Time();
            dayTime.setToNow();

            // we start at the day returned by local time. Otherwise this is a mess.
            int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);

            // now we work exclusively in UTC
            dayTime = new Time();

            // These are the values that will be collected.
            long dateTime;
            double pressure;
            int humidity;
            double windSpeed;
            double windDirection;

            double high;
            double low;

            String description;
            int weatherId;

            // Get the JSON object representing the day
            JSONObject dayForecast = weatherArray.getJSONObject(0);

            // Cheating to convert this to UTC time, which is what we want anyhow
            dateTime = dayTime.setJulianDay(julianStartDay + 0);

            pressure = dayForecast.getDouble(OWM_PRESSURE);
            humidity = dayForecast.getInt(OWM_HUMIDITY);
            windSpeed = dayForecast.getDouble(OWM_WINDSPEED);
            windDirection = dayForecast.getDouble(OWM_WIND_DIRECTION);

            // Description is in a child array called "weather", which is 1 element long.
            // That element also contains a weather code.
            JSONObject weatherObject =
                    dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
            description = weatherObject.getString(OWM_DESCRIPTION);

            // Temperatures are in a child object called "temp".  Try not to name variables
            // "temp" when working with temperature.  It confuses everybody.
            JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
            high = temperatureObject.getDouble(OWM_MAX);
            low = temperatureObject.getDouble(OWM_MIN);

            weatherObj = new WeatherWear();

            weatherObj.setDateTime(dateTime);
            weatherObj.setHumidity(humidity);
            weatherObj.setPressure(pressure);
            weatherObj.setWindSpeed(windSpeed);
            weatherObj.setWindDirection(windDirection);
            weatherObj.setHigh(high);
            weatherObj.setLow(low);
            weatherObj.setDescription(description);

            if (weatherObj != null) {
                return LOCATION_STATUS_OK;
            } else
                return LOCATION_STATUS_UNKNOWN;

        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
            return LOCATION_STATUS_SERVER_INVALID;
        }
    }
}
