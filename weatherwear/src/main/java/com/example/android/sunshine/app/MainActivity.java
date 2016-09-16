package com.example.android.sunshine.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.CapabilityInfo;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MainActivity extends Activity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        DataApi.DataListener,
        MessageApi.MessageListener,
View.OnClickListener{

    private TextView mTextView;
    private GoogleApiClient googleClient;
    public DataMap dataMap;
    private boolean isMessageSent;
    private Node peerNode;
    private static final String
            VOICE_TRANSCRIPTION_CAPABILITY_NAME = "voice_transcription";
    private String transcriptionNodeId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mTextView = (TextView) stub.findViewById(R.id.text);
                mTextView.setText("12345678");
                mTextView.setOnClickListener(MainActivity.this);
            }
        });

        //Get google client
        googleClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

    }


    @Override
    protected void onResume() {
        super.onResume();
        if (!googleClient.isConnected()) {
            googleClient.connect();
        }

    }


    @Override
    protected void onPause() {
        super.onPause();
        isMessageSent = false;

        //System.out.println("ondatachange ----- destroy");
        if (googleClient.isConnected()) {
            //System.out.println("ondatachange ----- inside destroy");
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
        //System.out.println("onDatachange--------onConnected");
        Toast.makeText(this, "onConnected", Toast.LENGTH_SHORT).show();
        Wearable.DataApi.addListener(googleClient, this);
        Wearable.MessageApi.addListener(googleClient, this);
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

        Toast.makeText(this, "On data changed", Toast.LENGTH_SHORT).show();
        //System.out.println("ondatachange ------ launch --- ");
        for (DataEvent event : events) {
            String path = event.getDataItem().getUri().getPath();
            if (event.getType() == DataEvent.TYPE_CHANGED) {

                //System.out.println("ondatachange ----- " + path);


                if ("/showCurrentWeather".equals(path)) {

                    dataMap = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();

                    dataMap = dataMap.getDataMap("weatherData");

                    mTextView.setText(dataMap.getString("name"));

                } else {

                }

            } else if (event.getType() == DataEvent.TYPE_DELETED) {

            } else {

            }
        }
    }


    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if (messageEvent.getPath().equals("/showErrorMessage")) {

            final String str = new String(messageEvent.getData());
            Toast.makeText(this, str, Toast.LENGTH_SHORT).show();
            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    // pb.setVisibility(View.GONE);

                    /*Intent intError = new Intent(MainActivity.this, ErrorActivity.class);
                    intError.putExtra("errorMessage", str);
                    startActivity(intError);*/
                }
            });


        }
    }

    @Override
    public void onClick(View v) {
        if (googleClient.isConnected() && !isMessageSent) {
            //System.out.println("onDatachangeonDatachange--------onResume");
            //Toast.makeText(LandingScreen.this, "onclick", Toast.LENGTH_SHORT).show();
            //pb.setVisibility(View.VISIBLE);


            sendWeatherMessage();
        }

        Toast.makeText(this, googleClient.isConnected() +" "+ isMessageSent, Toast.LENGTH_SHORT).show();
    }

    //Method to send message to handheld device
    private void sendWeatherMessage() {
        //System.out.println("onDatachange--------send start message");
        Toast.makeText(this, "sendWeatherMessage", Toast.LENGTH_SHORT).show();
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
}
