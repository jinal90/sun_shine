package com.example.android.sunshine.app;

import android.app.Activity;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.List;

public class MainActivity extends Activity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        DataApi.DataListener,
        MessageApi.MessageListener {


    private TextView tvDesc, tvDate, tvHigh, tvLow, tvHumidity, tvPressure, tvWind, tvError;
    private ImageView ivWeatherIcon;
    private ProgressBar progressIndicator;
    private RelativeLayout rlContent;

    private GoogleApiClient googleClient;
    public DataMap dataMap;
    private boolean isMessageSent;
    private Node peerNode;
    private boolean isLayoutInflated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                setupUI();
                showProgress();
                isLayoutInflated = true;
            }
        });

        //Get google client
        googleClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

    }

    public void showProgress() {
        progressIndicator.setVisibility(View.VISIBLE);
        rlContent.setVisibility(View.GONE);
        tvError.setVisibility(View.GONE);
    }

    public void showMainContent() {
        progressIndicator.setVisibility(View.GONE);
        rlContent.setVisibility(View.VISIBLE);
        tvError.setVisibility(View.GONE);
    }

    public void showError(String errorMsg) {
        progressIndicator.setVisibility(View.GONE);
        rlContent.setVisibility(View.GONE);
        tvError.setVisibility(View.VISIBLE);
        tvError.setText(errorMsg);
    }

    public void setupUI() {
        tvDesc = (TextView) findViewById(R.id.tvDesc);
        tvDate = (TextView) findViewById(R.id.tvDate);
        tvHigh = (TextView) findViewById(R.id.tvHighTemp);
        tvLow = (TextView) findViewById(R.id.tvLowTemp);
        tvHumidity = (TextView) findViewById(R.id.tvHumidityValue);
        tvPressure = (TextView) findViewById(R.id.tvPressureValue);
        tvWind = (TextView) findViewById(R.id.tvWindValue);
        ivWeatherIcon = (ImageView) findViewById(R.id.ivWeatherIcon);
        progressIndicator = (ProgressBar) findViewById(R.id.progressIndicator);
        rlContent = (RelativeLayout) findViewById(R.id.rlContent);
        tvError = (TextView) findViewById(R.id.tvError);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!googleClient.isConnected()) {
            googleClient.connect();
        }
        if (isLayoutInflated)
            showProgress();

    }


    @Override
    protected void onPause() {
        super.onPause();
        isMessageSent = false;

        if (googleClient.isConnected()) {
            //Register callback listeners for data change and message received
            Wearable.DataApi.removeListener(googleClient, this);
            Wearable.MessageApi.removeListener(googleClient, this);

            googleClient.disconnect();
        }

        Wearable.DataApi.removeListener(googleClient, this);
        Wearable.MessageApi.removeListener(googleClient, this);
    }

    @Override
    public void onConnected(Bundle bundle) {
//Register callback listeners for data change and message received
        Wearable.DataApi.addListener(googleClient, this);
        Wearable.MessageApi.addListener(googleClient, this);

        if (googleClient.isConnected() && !isMessageSent) {
            sendWeatherMessage();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        final List<DataEvent> events = FreezableUtils.freezeIterable(dataEvents);
        dataEvents.close();
        /*if(prog != null && prog.isShowing())
            prog.dismiss();*/

        for (DataEvent event : events) {
            String path = event.getDataItem().getUri().getPath();
            if (event.getType() == DataEvent.TYPE_CHANGED) {

                if ("/showCurrentWeather".equals(path)) {


                    dataMap = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();

                    dataMap = dataMap.getDataMap("weatherData");

                    tvPressure.setText(dataMap.getString("pressure"));
                    tvHumidity.setText(dataMap.getString("humidity"));
                    tvWind.setText(dataMap.getString("wind"));
                    tvDesc.setText(dataMap.getString("description"));
                    tvDate.setText(dataMap.getString("dateTime"));
                    tvHigh.setText(dataMap.getString("high"));
                    tvLow.setText(dataMap.getString("low"));
                    if (getArtResourceForWeatherCondition(dataMap.getInt("weatherId")) != -1)
                        ivWeatherIcon.setImageResource(getArtResourceForWeatherCondition(dataMap.getInt("weatherId")));

                    showMainContent();
                } else {
                    showError("Some Error Occurred");
                }

            } else if (event.getType() == DataEvent.TYPE_DELETED) {
                showError("Some Error Occurred");
            } else {
                showError("Some Error Occurred");
            }
        }
    }


    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if (messageEvent.getPath().equals("/showErrorMessage")) {

            final String str = new String(messageEvent.getData());
            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    // pb.setVisibility(View.GONE);
                    showError(str);
                    /*Intent intError = new Intent(MainActivity.this, ErrorActivity.class);
                    intError.putExtra("errorMessage", str);
                    startActivity(intError);*/
                }
            });


        }
    }

    //Method to send message to handheld device
    private void sendWeatherMessage() {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                String path = "/fetchCurrentWeather";

                NodeApi.GetConnectedNodesResult rawNodes =
                        Wearable.NodeApi.getConnectedNodes(googleClient).await();

                for (final Node node : rawNodes.getNodes()) {
                    PendingResult<MessageApi.SendMessageResult> result = Wearable.MessageApi.sendMessage(
                            googleClient,
                            node.getId(),
                            path,
                            null
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

    /**
     * Helper method to provide the art resource id according to the weather condition id returned
     * by the OpenWeatherMap call.
     *
     * @param weatherId from OpenWeatherMap API response
     * @return resource id for the corresponding icon. -1 if no relation is found.
     */
    public static int getArtResourceForWeatherCondition(int weatherId) {
        // Based on weather code data found at:
        // http://bugs.openweathermap.org/projects/api/wiki/Weather_Condition_Codes
        if (weatherId >= 200 && weatherId <= 232) {
            return R.drawable.ic_storm;
        } else if (weatherId >= 300 && weatherId <= 321) {
            return R.drawable.ic_light_rain;
        } else if (weatherId >= 500 && weatherId <= 504) {
            return R.drawable.ic_rain;
        } else if (weatherId == 511) {
            return R.drawable.ic_snow;
        } else if (weatherId >= 520 && weatherId <= 531) {
            return R.drawable.ic_rain;
        } else if (weatherId >= 600 && weatherId <= 622) {
            return R.drawable.ic_snow;
        } else if (weatherId >= 701 && weatherId <= 761) {
            return R.drawable.ic_fog;
        } else if (weatherId == 761 || weatherId == 781) {
            return R.drawable.ic_storm;
        } else if (weatherId == 800) {
            return R.drawable.ic_clear;
        } else if (weatherId == 801) {
            return R.drawable.ic_light_clouds;
        } else if (weatherId >= 802 && weatherId <= 804) {
            return R.drawable.ic_cloudy;
        }
        return -1;
    }
}
