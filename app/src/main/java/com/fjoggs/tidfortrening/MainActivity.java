package com.fjoggs.tidfortrening;

import android.*;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Chronometer;
import android.widget.TextView;

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
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.data.Session;
import com.google.android.gms.fitness.request.DataDeleteRequest;
import com.google.android.gms.fitness.request.SessionInsertRequest;
import com.google.android.gms.fitness.request.SessionReadRequest;
import com.google.android.gms.fitness.result.SessionReadResult;
import com.google.android.gms.fitness.result.SessionStopResult;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class MainActivity extends AppCompatActivity {
    private GoogleApiClient googleApiClient = null;
    private Chronometer stopWatch;
    private Session session = null;
    private Calendar calendar;
    private long timerPausedAt = 0;
    private long startTrackingTime = 0;
    private long stopTrackingTime = 0;
    private static final String TAG = "MainActivity";
    private static final String SAMPLE_SESSION_NAME = "TfT Session";
    private static final String SAMPLE_SESSION_ID = "Tft Session Id";
    private static final String SAMPLE_SESSION_DESC = "Tft Session Desc";
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (!checkPermissions()) {
            requestPermissions();
        }
        stopWatch = (Chronometer) findViewById(R.id.stopWatch);
        stopWatch.setBase(SystemClock.elapsedRealtime());
        calendar = Calendar.getInstance();
        calendar.setTime(new Date());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // This ensures that if the user denies the permissions then uses Settings to re-enable
        // them, the app will start working.
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
        startTrackingTime = calendar.getTimeInMillis();
    }

    public void stopTracking(View v) {
        timerPausedAt = SystemClock.elapsedRealtime() - stopWatch.getBase();
        stopWatch.stop();
        calendar.setTime(new Date());
        stopTrackingTime = calendar.getTimeInMillis();
    }

    public void storeActivity(View v) {
        new InsertAndVerifySessionTask().execute();
    }

    public void deleteActivity(View v) {
        deleteSession();
    }

    public void showAllActivites(View v) {
        new ReadSessionDataFromPastWeek().execute();
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
                    .addApi(Fitness.HISTORY_API)
                    .addApi(Fitness.SESSIONS_API)
                    .addScope(new Scope(Scopes.FITNESS_LOCATION_READ_WRITE))
                    .addScope(new Scope(Scopes.FITNESS_ACTIVITY_READ_WRITE))
                    .addConnectionCallbacks(
                            new GoogleApiClient.ConnectionCallbacks() {
                                @Override
                                public void onConnected(Bundle bundle) {
                                    Log.i(TAG, "Connected!!!");
                                    // Now you can make calls to the Fitness APIs.
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
        int permissionState = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    /**
     *  Create and execute a {@link SessionInsertRequest} to insert a session into the History API,
     *  and then create and execute a {@link SessionReadRequest} to verify the insertion succeeded.
     *  By using an AsyncTask to make our calls, we can schedule synchronous calls, so that we can
     *  query for sessions after confirming that our insert was successful. Using asynchronous calls
     *  and callbacks would not guarantee that the insertion had concluded before the read request
     *  was made. An example of an asynchronous call using a callback can be found in the example
     *  on deleting sessions below.
     */
    private class InsertAndVerifySessionTask extends AsyncTask<Void, Void, Void> {
        protected Void doInBackground(Void... params) {
            //First, create a new session and an insertion request.
            SessionInsertRequest insertRequest = insertFitnessSession();

            // [START insert_session]
            // Then, invoke the Sessions API to insert the session and await the result,
            // which is possible here because of the AsyncTask. Always include a timeout when
            // calling await() to avoid hanging that can occur from the service being shutdown
            // because of low memory or other conditions.
            Log.i(TAG, "Inserting the session in the History API");
            com.google.android.gms.common.api.Status insertStatus =
                    Fitness.SessionsApi.insertSession(googleApiClient, insertRequest)
                            .await(1, TimeUnit.MINUTES);

            // Before querying the session, check to see if the insertion succeeded.
            if (!insertStatus.isSuccess()) {
                Log.i(TAG, "There was a problem inserting the session: " +
                        insertStatus.getStatusMessage());
                return null;
            }

            // At this point, the session has been inserted and can be read.
            Log.i(TAG, "Session insert was successful!");
            // [END insert_session]

            // Begin by creating the query.
            SessionReadRequest readRequest = readFitnessSession();

            // [START read_session]
            // Invoke the Sessions API to fetch the session with the query and wait for the result
            // of the read request. Note: Fitness.SessionsApi.readSession() requires the
            // ACCESS_FINE_LOCATION permission.
            SessionReadResult sessionReadResult =
                    Fitness.SessionsApi.readSession(googleApiClient, readRequest).await(1, TimeUnit.MINUTES);

            // Get a list of the sessions that match the criteria to check the result.
            Log.i(TAG, "Session read was successful. Number of returned sessions is: "
                    + sessionReadResult.getSessions().size());
            for (Session session : sessionReadResult.getSessions()) {
                // Process the session
                dumpSession(session);

                // Process the data sets for this session
                List<DataSet> dataSets = sessionReadResult.getDataSet(session);
                for (DataSet dataSet : dataSets) {
                    dumpDataSet(dataSet);
                }
            }
            // [END read_session]

            return null;
        }
    }

    /**
     *  Create a {@link SessionInsertRequest} for a run that consists of 10 minutes running,
     *  10 minutes walking, and 10 minutes of running. The request contains two {@link DataSet}s:
     *  speed data and activity segments data.
     *
     *  {@link Session}s are time intervals that are associated with all Fit data that falls into
     *  that time interval. This data can be inserted when inserting a session or independently,
     *  without affecting the association between that data and the session. Future queries for
     *  that session will return all data relevant to the time interval created by the session.
     *
     *  Sessions may contain {@link DataSet}s, which are comprised of {@link DataPoint}s and a
     *  {@link DataSource}.
     *  A {@link DataPoint} is associated with a Fit {@link DataType}, which may be
     *  derived from the {@link DataSource}, as well as a time interval, and a value. A given
     *  {@link DataSet} may only contain data for a single data type, but a {@link Session} can
     *  contain multiple {@link DataSet}s.
     */
    private SessionInsertRequest insertFitnessSession() {
        Log.i(TAG, "Creating a new session for an afternoon run");

        // Create a data source
        DataSource speedDataSource = new DataSource.Builder()
                .setAppPackageName(this.getPackageName())
                .setDataType(DataType.TYPE_SPEED)
                .setName(SAMPLE_SESSION_NAME + "- speed")
                .setType(DataSource.TYPE_RAW)
                .build();

        float runSpeedMps = 10;

        // Create a data set of the run speeds to include in the session.
        DataSet speedDataSet = DataSet.create(speedDataSource);

        DataPoint firstRunSpeed = speedDataSet.createDataPoint().setTimeInterval(startTrackingTime, stopTrackingTime, TimeUnit.MILLISECONDS);
        firstRunSpeed.getValue(Field.FIELD_SPEED).setFloat(runSpeedMps);
        speedDataSet.add(firstRunSpeed);


        // [START build_insert_session_request_with_activity_segments]
        // Create a second DataSet of ActivitySegments to indicate the runner took a 10-minute walk
        // in the middle of the run.
        DataSource activitySegmentDataSource = new DataSource.Builder()
                .setAppPackageName(this.getPackageName())
                .setDataType(DataType.TYPE_ACTIVITY_SEGMENT)
                .setName(SAMPLE_SESSION_NAME + "-activity segments")
                .setType(DataSource.TYPE_RAW)
                .build();
        DataSet activitySegments = DataSet.create(activitySegmentDataSource);

        DataPoint runningSegment = activitySegments.createDataPoint().setTimeInterval(startTrackingTime, stopTrackingTime, TimeUnit.MILLISECONDS);
        runningSegment.getValue(Field.FIELD_ACTIVITY).setActivity(FitnessActivities.RUNNING);
        activitySegments.add(runningSegment);


        // [START build_insert_session_request]
        // Create a session with metadata about the activity.
        session = new Session.Builder()
                .setName(SAMPLE_SESSION_NAME)
                .setIdentifier(SAMPLE_SESSION_NAME)
                .setDescription(SAMPLE_SESSION_DESC)
                .setActivity(FitnessActivities.RUNNING)
                .setStartTime(startTrackingTime, TimeUnit.MILLISECONDS)
                .setEndTime(stopTrackingTime, TimeUnit.MILLISECONDS)
                .build();

        // Build a session insert request
        SessionInsertRequest insertRequest = new SessionInsertRequest.Builder()
                .setSession(session)
                .addDataSet(speedDataSet)
                .addDataSet(activitySegments)
                .build();
        // [END build_insert_session_request]
        // [END build_insert_session_request_with_activity_segments]

        return insertRequest;
    }

    private SessionReadRequest readFitnessSession() {
        Log.i(TAG, "Reading History API results for session: " + SAMPLE_SESSION_NAME);
        return new SessionReadRequest.Builder()
                .setTimeInterval(startTrackingTime, stopTrackingTime, TimeUnit.MILLISECONDS)
                .read(DataType.TYPE_SPEED)
                .setSessionName(SAMPLE_SESSION_NAME)
                .build();
    }

    private class ReadSessionDataFromPastWeek extends AsyncTask<Void, Void, Void> {
        protected Void doInBackground(Void... params) {
            // Begin by creating the query.
            SessionReadRequest readRequest = readFitnessSessionThisWeek();

            // [START read_session]
            // Invoke the Sessions API to fetch the session with the query and wait for the result
            // of the read request. Note: Fitness.SessionsApi.readSession() requires the
            // ACCESS_FINE_LOCATION permission.
            SessionReadResult sessionReadResult =
                    Fitness.SessionsApi.readSession(googleApiClient, readRequest).await(1, TimeUnit.MINUTES);

            // Get a list of the sessions that match the criteria to check the result.
            Log.i(TAG, "Session read was successful. Number of returned sessions is: "
                    + sessionReadResult.getSessions().size());
            for (Session session : sessionReadResult.getSessions()) {
                // Process the session
                dumpSession(session);

                // Process the data sets for this session
                List<DataSet> dataSets = sessionReadResult.getDataSet(session);
                for (DataSet dataSet : dataSets) {
                    dumpDataSet(dataSet);
                }
            }
            // [END read_session]
            return null;
        }
    }

    /**
     * Return a {@link SessionReadRequest} for all speed data in the past week.
     */
    private SessionReadRequest readFitnessSessionThisWeek() {
        Log.i(TAG, "Reading History API results for session: " + SAMPLE_SESSION_NAME);
        // [START build_read_session_request]
        // Set a start and end time for our query, using a start time of 1 week before this moment.
        Calendar cal = Calendar.getInstance();
        Date now = new Date();
        cal.setTime(now);
        long endTime = cal.getTimeInMillis();
        cal.add(Calendar.WEEK_OF_YEAR, -1);
        long startTime = cal.getTimeInMillis();

        // Build a session read request
        SessionReadRequest readRequest = new SessionReadRequest.Builder()
                .setTimeInterval(startTime, endTime, TimeUnit.MILLISECONDS)
                .read(DataType.TYPE_SPEED)
                .setSessionName(SAMPLE_SESSION_NAME)
                .build();
        // [END build_read_session_request]

        return readRequest;
    }

    private void dumpDataSet(DataSet dataSet) {
        Log.i(TAG, "Data returned for Data type: " + dataSet.getDataType().getName());
        for (DataPoint dp : dataSet.getDataPoints()) {
            DateFormat dateFormat = DateFormat.getTimeInstance();
            Log.i(TAG, "Data point:");
            Log.i(TAG, "\tType: " + dp.getDataType().getName());
            Log.i(TAG, "\tStart: " + dateFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS)));
            Log.i(TAG, "\tEnd: " + dateFormat.format(dp.getEndTime(TimeUnit.MILLISECONDS)));
            for(Field field : dp.getDataType().getFields()) {
                Log.i(TAG, "\tField: " + field.getName() +
                        " Value: " + dp.getValue(field));
            }
        }
    }

    private void dumpSession(Session session) {
        DateFormat dateFormat = DateFormat.getTimeInstance();
        final String startTime = dateFormat.format(session.getStartTime(TimeUnit.MILLISECONDS));
        final String endTime = dateFormat.format(session.getEndTime(TimeUnit.MILLISECONDS));
        Log.i(TAG, "Data returned for Session: " + session.getName()
                + "\n\tDescription: " + session.getDescription()
                + "\n\tStart: " + startTime
                + "\n\tEnd: " + endTime);

        TextView listSessions = (TextView) findViewById(R.id.activityList);
        final String text = session.getName() + "\n" + session.getDescription() + "\n" + startTime + "\n" + endTime;
        listSessions.setText(text);
    }

    /**
     * Delete the {@link DataSet} we inserted with our {@link Session} from the History API.
     * In this example, we delete all step count data for the past 24 hours. Note that this
     * deletion uses the History API, and not the Sessions API, since sessions are truly just time
     * intervals over a set of data, and the data is what we are interested in removing.
     */
    private void deleteSession() {
        Log.i(TAG, "Deleting today's session data for speed");

        // Create a delete request object, providing a data type and a time interval
        DataDeleteRequest request = new DataDeleteRequest.Builder()
                .setTimeInterval(startTrackingTime, stopTrackingTime, TimeUnit.MILLISECONDS)
                .addDataType(DataType.TYPE_SPEED)
                .addSession(session)
                .build();

        // Invoke the History API with the Google API client object and the delete request and
        // specify a callback that will check the result.
        Fitness.HistoryApi.deleteData(googleApiClient, request)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        if (status.isSuccess()) {
                            Log.i(TAG, "Successfully deleted today's sessions");
                        } else {
                            // The deletion will fail if the requesting app tries to delete data
                            // that it did not insert.
                            Log.i(TAG, "Failed to delete today's sessions");
                        }
                    }
                });
    }

    /**
     * Return the current state of the permissions needed.
     */
    private void requestPermissions() {
        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION);

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Log.i(TAG, "Displaying permission rationale to provide additional context.");
            Snackbar.make(
                    findViewById(R.id.mainActivity),
                    R.string.permission_rationale,
                    Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.permission_ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // Request permission
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                    REQUEST_PERMISSIONS_REQUEST_CODE);
                        }
                    })
                    .show();
        } else {
            Log.i(TAG, "Requesting permission");
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        Log.i(TAG, "onRequestPermissionResult");
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length <= 0) {
                // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
                Log.i(TAG, "User interaction was cancelled.");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted.
                buildFitnessClient();
            } else {
                // Permission denied.

                // In this Activity we've chosen to notify the user that they
                // have rejected a core permission for the app since it makes the Activity useless.
                // We're communicating this message in a Snackbar since this is a sample app, but
                // core permissions would typically be best requested during a welcome-screen flow.

                // Additionally, it is important to remember that a permission might have been
                // rejected without asking the user for permission (device policy or "Never ask
                // again" prompts). Therefore, a user interface affordance is typically implemented
                // when permissions are denied. Otherwise, your app could appear unresponsive to
                // touches or interactions which have required permissions.
                Snackbar.make(
                        findViewById(R.id.mainActivity),
                        R.string.permission_denied_explanation,
                        Snackbar.LENGTH_INDEFINITE)
                        .setAction(R.string.action_settings, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                // Build intent that displays the App settings screen.
                                Intent intent = new Intent();
                                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null);
                                intent.setData(uri);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            }
                        }).show();
            }
        }
    }
}
