package com.fjoggs.tidfortrening;

import android.os.Bundle;
import android.os.SystemClock;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Chronometer;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessActivities;
import com.google.android.gms.fitness.FitnessStatusCodes;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Session;
import com.google.android.gms.fitness.result.SessionStopResult;

import java.util.concurrent.TimeUnit;


public class MainActivity extends AppCompatActivity {
    private Chronometer stopWatch;
    private long timerPausedAt = 0;
    private GoogleApiClient googleApiClient = null;
    private Session session = null;
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        stopWatch = (Chronometer) findViewById(R.id.stopWatch);
        stopWatch.setBase(SystemClock.elapsedRealtime());
        buildFitnessClient();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void startTracking(View v) {
        stopWatch.setBase(SystemClock.elapsedRealtime() - timerPausedAt);
        stopWatch.start();
        if(session!=null) {
            startSession();
            startSubscribingToFitnessData();
        }
    }

    public void stopTracking(View v) {
        timerPausedAt = SystemClock.elapsedRealtime() - stopWatch.getBase();
        stopWatch.stop();
        if(session!=null) {
            stopSubscribingToFitnessData();
            stopSession();
        }
    }

    public void resetTracking(View v) {
        stopWatch.stop();
    }

    /**
     *  Build a {@link GoogleApiClient} that will authenticate the user and allow the application
     *  to connect to Fitness APIs. The scopes included should match the scopes your app needs
     *  (see documentation for details). Authentication will occasionally fail intentionally,
     *  and in those cases, there will be a known resolution, which the OnConnectionFailedListener()
     *  can address. Examples of this include the user never having signed in before, or having
     *  multiple accounts on the device and needing to specify which account to use, etc.
     */
    private void buildFitnessClient() {
        if (googleApiClient == null && checkPermissions()) {
            googleApiClient = new GoogleApiClient.Builder(MainActivity.this)
                    .addApi(Fitness.RECORDING_API)
                    .addApi(Fitness.SENSORS_API)
                    .addApi(Fitness.SESSIONS_API)
                    .addScope(new Scope(Scopes.FITNESS_ACTIVITY_READ_WRITE))
                    .addConnectionCallbacks(
                            new GoogleApiClient.ConnectionCallbacks() {
                                @Override
                                public void onConnected(Bundle bundle) {
                                    Log.i(TAG, "Connected!!!");
                                    // Now you can make calls to the Fitness APIs.
                                    session = createSessionObject();
                                }

                                @Override
                                public void onConnectionSuspended(int i) {
                                    // If your connection to the sensor gets lost at some point,
                                    // you'll be able to determine the reason and react to it here.
                                    if (i == GoogleApiClient.ConnectionCallbacks.CAUSE_NETWORK_LOST) {
                                        Log.i(TAG, "Connection lost.  Cause: Network Lost.");
                                    } else if (i
                                            == GoogleApiClient.ConnectionCallbacks.CAUSE_SERVICE_DISCONNECTED) {
                                        Log.i(TAG,
                                                "Connection lost.  Reason: Service Disconnected");
                                    }
                                }
                            }
                    )
                    .enableAutoManage(this, 0, new GoogleApiClient.OnConnectionFailedListener() {
                        @Override
                        public void onConnectionFailed(ConnectionResult result) {
                            Log.i(TAG, "Google Play services connection failed. Cause: " + result.toString());
                            Snackbar.make(
                                    findViewById(R.id.mainActivity),
                                    "Exception while connecting to Google Play services: " + result.getErrorMessage(),
                                    Snackbar.LENGTH_INDEFINITE).show();
                        }
                    })
                    .build();
        }
    }

    private boolean checkPermissions() {
        return true;
    }

    private Session createSessionObject() {
        return new Session.Builder()
                .setName("TfT Session")
                .setIdentifier("Tft Session Id")
                .setDescription("Tft Session Desc")
                .setStartTime(SystemClock.elapsedRealtime(), TimeUnit.MILLISECONDS)
                .setActivity(FitnessActivities.RUNNING)
                .build();
    }

    private void startSession() {
        PendingResult<Status> pendingResult = Fitness.SessionsApi.startSession(googleApiClient, session);
        Log.i(TAG, "startSession: starting session");
    }

    private void stopSession() {
        PendingResult<SessionStopResult> pendingResult =
                Fitness.SessionsApi.stopSession(googleApiClient, session.getIdentifier());
        Log.i(TAG, "stopSession: stopping session");
    }

    private void startSubscribingToFitnessData() {
        Fitness.RecordingApi.subscribe(googleApiClient, DataType.TYPE_ACTIVITY_SAMPLE)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        if (status.isSuccess()) {
                            if (status.getStatusCode()==FitnessStatusCodes.SUCCESS_ALREADY_SUBSCRIBED) {
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

    private void stopSubscribingToFitnessData() {
        Fitness.RecordingApi.unsubscribe(googleApiClient, DataType.TYPE_ACTIVITY_SAMPLE)
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
