package com.amazonaws.demo.appsync;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import com.amazonaws.demo.appsync.fragment.Event;
import com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient;
import com.amazonaws.mobileconnectors.appsync.fetcher.AppSyncResponseFetchers;
import com.apollographql.apollo.GraphQLCall;
import com.apollographql.apollo.api.Error;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nonnull;

public class AddEventActivity extends AppCompatActivity {

    private static final String TAG = AddEventActivity.class.getSimpleName();

    private EditText name;
    private EditText time;
    private EditText where;
    private EditText description;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_event);
        name = (EditText)findViewById(R.id.name);
        time = (EditText)findViewById(R.id.time);
        where = (EditText)findViewById(R.id.where);
        description = (EditText)findViewById(R.id.description);

        name.setText("Lunch");
        time.setText("12:30 pm");
        where.setText("Desk");
        description.setText("Grab a burger.");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_add_events, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_save) {
            save();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void save() {
        String nameString = name.getText().toString();
        String timeString = time.getText().toString();
        String upsString = where.getText().toString();
        String descriptionString = description.getText().toString();

        // Get the client instance
        AWSAppSyncClient awsAppSyncClient = ClientFactory.getInstance(this.getApplicationContext());

        // Create the mutation request
        AddEventMutation addEventMutation = AddEventMutation.builder()
                .name(nameString)
                .when(timeString)
                .where(upsString)
                .description(descriptionString)
                .build();

        // Enqueue the request (This will execute the request)
        awsAppSyncClient.mutate(addEventMutation).refetchQueries(ListEventsQuery.builder().build()).enqueue(addEventsCallback);

        // Add to event list while offline or before request returns
        List<Event.Item> items = new ArrayList<>();
        String tempID = UUID.randomUUID().toString();
        Event event = new Event("Event", tempID, descriptionString, nameString, timeString, upsString, new Event.Comments("Comment", items));
        addEventOffline(new ListEventsQuery.Item("Event", new ListEventsQuery.Item.Fragments(event)));

        // Close the add event when offline otherwise allow callback to close
        ConnectivityManager cm =
                (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();

        if (!isConnected) {
            finish();
        }
    }

    private GraphQLCall.Callback<AddEventMutation.Data> addEventsCallback = new GraphQLCall.Callback<AddEventMutation.Data>() {
        @Override
        public void onResponse(@Nonnull final Response<AddEventMutation.Data> response) {
            if (response.hasErrors()) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Could not save", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Error: " + response.toString());

                        for (Error err : response.errors()) {
                            Log.e(TAG, "Error: " + err.message());
                        }
                        finish();
                    }
                });
            } else {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Saved", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Add event succeeded");
                        finish();
                    }
                });
            }
        }

        @Override
        public void onFailure(@Nonnull ApolloException e) {
            Log.e(TAG, "Failed to make posts api call", e);
            Log.e(TAG, e.getMessage());
        }
    };

    private void addEventOffline(final ListEventsQuery.Item pendingItem) {
        final AWSAppSyncClient awsAppSyncClient = ClientFactory.getInstance(this);
        final ListEventsQuery listEventsQuery = ListEventsQuery.builder().build();

        awsAppSyncClient.query(listEventsQuery)
                .responseFetcher(AppSyncResponseFetchers.CACHE_ONLY)
                .enqueue(new GraphQLCall.Callback<ListEventsQuery.Data>() {
                    @Override
                    public void onResponse(@Nonnull Response<ListEventsQuery.Data> response) {
                        List<ListEventsQuery.Item> items = new ArrayList<>();
                        if (response.data() != null) {
                            items.addAll(response.data().listEvents().items());
                        }

                        items.add(pendingItem);
                        ListEventsQuery.Data data = new ListEventsQuery.Data(new ListEventsQuery.ListEvents("EventConnection", items, null));
                        awsAppSyncClient.getStore().write(listEventsQuery, data).enqueue(null);
                    }

                    @Override
                    public void onFailure(@Nonnull ApolloException e) {
                        Log.e(TAG, "Failed to update event query list.", e);
                    }
                });
    }
}
