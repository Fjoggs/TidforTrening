package com.fjoggs.api;

import android.os.SystemClock;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessActivities;
import com.google.android.gms.fitness.data.Session;
import com.google.android.gms.fitness.result.SessionStopResult;

import java.util.concurrent.TimeUnit;

public class SessionAPI {
    private String sessionName = "Default Name";
    private String sessionId = "Default ID";
    private String sessionDesc = "Default Description";
    private Session session;
    private GoogleApiClient mClient = null;
    private static final String TAG = "SessionAPI";

    public SessionAPI(GoogleApiClient mClient) {
        this.mClient = mClient;
    }

    private void createSessionObject() {
        session = new Session.Builder()
                .setName(sessionName)
                .setIdentifier(sessionId)
                .setDescription(sessionDesc)
                .setStartTime(SystemClock.elapsedRealtime(), TimeUnit.MILLISECONDS)
                .setActivity(FitnessActivities.RUNNING)
                .build();
    }

    private void startSession() {
        PendingResult<Status> pendingResult = Fitness.SessionsApi.startSession(mClient, session);
        Log.i(TAG, "startSession: starting session");
    }

    private void stopSession() {
        PendingResult<SessionStopResult> pendingResult =
                Fitness.SessionsApi.stopSession(mClient, session.getIdentifier());
        Log.i(TAG, "stopSession: stopping session");
    }
}
