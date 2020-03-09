package com.amazonaws.demo.appsync;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

import com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient;
import com.amazonaws.mobileconnectors.appsync.fetcher.AppSyncResponseFetchers;
import com.apollographql.apollo.GraphQLCall;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

public class ListEventsActivity extends AppCompatActivity {

    private static final String TAG = ListEventsActivity.class.getSimpleName();

    private AWSAppSyncClient mAWSAppSyncClient;

    private List<ListEventsQuery.Item> events = new ArrayList<>();
    private EventsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_events);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ListEventsActivity.this, AddEventActivity.class);
                startActivity(intent);
            }
        });

        FloatingActionButton fabRefresh = (FloatingActionButton) findViewById(R.id.refresh);
        fabRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                query();
            }
        });

        adapter = new EventsAdapter(this, events);
        ListView mListView = (ListView) findViewById(R.id.postsList);
        mListView.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        query();
    }

    private void query() {
        if (mAWSAppSyncClient == null) {
            mAWSAppSyncClient = ClientFactory.getInstance(this);
        }
        mAWSAppSyncClient.query(ListEventsQuery.builder().build())
                .responseFetcher(AppSyncResponseFetchers.CACHE_AND_NETWORK)
                .enqueue(eventsCallback);
    }

    private GraphQLCall.Callback<ListEventsQuery.Data> eventsCallback = new GraphQLCall.Callback<ListEventsQuery.Data>() {
        @Override
        public void onResponse(@Nonnull Response<ListEventsQuery.Data> response) {
            if (response.data() != null) {
                events = response.data().listEvents().items();
            } else {
                events = new ArrayList<>();
            }
            adapter.setEvents(events);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "Notifying data set changed");
                    adapter.notifyDataSetChanged();
                }
            });
        }

        @Override
        public void onFailure(@Nonnull ApolloException e) {
            Log.e(TAG, "Failed to make events api call", e);
            Log.e(TAG, e.getMessage());
        }
    };

}
