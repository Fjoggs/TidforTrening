package com.fjoggs.api;

import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessStatusCodes;
import com.google.android.gms.fitness.data.DataType;

public class RecordingAPI {
    private GoogleApiClient mClient = null;
    private static final String TAG = "RecordingAPI";

    public RecordingAPI(GoogleApiClient mClient) {
        this.mClient = mClient;
    }

    public void startSubscribingToFitnessData() {
        Fitness.RecordingApi.subscribe(mClient, DataType.TYPE_ACTIVITY_SAMPLE)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        if (status.isSuccess()) {
                            if (status.getStatusCode()== FitnessStatusCodes.SUCCESS_ALREADY_SUBSCRIBED) {
                                Log.i(TAG, "Existing subscription for activity detected.");
                            } else {
                                Log.i(TAG, "Successfully subscribed!");
                            }
                        } else {
                            Log.i(TAG, "There was a problem subscribing.");
                        }
                    }
                });
    }

    public void stopSubscribingToFitnessData() {
        Fitness.RecordingApi.unsubscribe(mClient, DataType.TYPE_ACTIVITY_SAMPLE)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        if (status.isSuccess()) {
                            Log.i(TAG, "Successfully unsubscribed for data type: " + DataType.TYPE_ACTIVITY_SAMPLE.getName());
                        } else {
                            // Subscription not removed
                            Log.i(TAG, "Failed to unsubscribe for data type: " + DataType.TYPE_ACTIVITY_SAMPLE.getName());
                        }
                    }
                });
    }
}
