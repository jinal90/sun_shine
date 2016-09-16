package com.example.android.sunshine.app.sync;

import android.content.Context;
import android.os.Bundle;
import android.text.Html;
import android.widget.Toast;

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

import java.util.ArrayList;
import java.util.Calendar;

/**
 * Created by jinal on 9/14/2016.
 */

public class SunshineWearableListener extends WearableListenerService {

    private Context mContext;
    private GoogleApiClient googleClient;
    private Node peerNode;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
        //System.out.println(" -- -- POC -- -- " + "onCreate");

        googleClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle connectionHint) {
                        // tellWatchConnectedState("connected");
                        //Toast.makeText(ctx, " -- -- POC -- -- " + "onConnected", Toast.LENGTH_SHORT).show();
                        Wearable.DataApi.addListener(googleClient, SunshineWearableListener.this);
                        Wearable.MessageApi.addListener(googleClient, SunshineWearableListener.this);
                        //  "onConnected: null" is normal.
                        //  There's nothing in our bundle.
                    }

                    @Override
                    public void onConnectionSuspended(int cause) {
                        //System.out.println(" -- -- POC -- -- " + "onConnectionSuspended");
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult result) {
                        //System.out.println(" -- -- POC -- -- " + "addOnConnectionFailedListener");
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
        sendMessage("hello");
        Toast.makeText(this, "onMessageReceived1", Toast.LENGTH_SHORT).show();
        //Check which button is clicked based on path sent from wearable
        if (messageEvent.getPath().equals("/fetchCurrentWeather")) {
            Toast.makeText(this, "message received", Toast.LENGTH_SHORT).show();
            sendCurrentWeatherData();
        }else if (messageEvent.getPath().equals("/voice_transcription")) {
            Toast.makeText(this, "voice_transcription", Toast.LENGTH_SHORT).show();
            /*Intent startIntent = new Intent(this, MainActivity.class);
            startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startIntent.putExtra("VOICE_DATA", messageEvent.getData());
            startActivity(startIntent);*/
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

        //System.out.println(" -- -- POC -- -- " + "send One");
        //Toast.makeText(ctx, "sendTrackedRunnersData", Toast.LENGTH_SHORT).show();
        //System.out.println("wear ---- "+"send");
        Thread t = new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        Calendar cal = Calendar.getInstance();

                        DataMap d = new DataMap();
                        d.putString("name", "Jinal");
                        d.putString("date", cal.get(Calendar.HOUR) + cal.get(Calendar.SECOND) + "");

                        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create("/showCurrentWeather");
                        putDataMapRequest.getDataMap().putDataMap("weatherData", d);
                        //Toast.makeText(ctx, "send data item 1", Toast.LENGTH_SHORT).show();
                        PutDataRequest request = putDataMapRequest.asPutDataRequest();

        /*if (!googleClient.isConnected()) {
            return;
        }*/
                        Wearable.DataApi.putDataItem(googleClient, request).await();
                        Wearable.DataApi.putDataItem(googleClient, request)
                                .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                                    @Override
                                    public void onResult(DataApi.DataItemResult dataItemResult) {
                                        if (!dataItemResult.getStatus().isSuccess()) {
                                            //
                                        } else {
                                            //Toast.makeText(ctx, "datasent -- Success", Toast.LENGTH_SHORT).show();
                                            //Toast.makeText(ctx, "ondatachange success", Toast.LENGTH_SHORT).show();
                                            //mGoogleApiClient.disconnect();
                                        }
                                    }
                                });
                    }
                }
        );
        t.start();

    }
}
