package com.amazonaws.demo.appsync;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.demo.appsync.fragment.Event;
import com.amazonaws.mobileconnectors.appsync.AppSyncSubscriptionCall;
import com.amazonaws.mobileconnectors.appsync.fetcher.AppSyncResponseFetchers;
import com.apollographql.apollo.GraphQLCall;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.Nonnull;

public class ViewEventActivity extends AppCompatActivity {
    public static final String TAG = ViewEventActivity.class.getSimpleName();

    private static Event event;
    private TextView comments;
    private EditText newComment;
    private AppSyncSubscriptionCall<NewCommentOnEventSubscription.Data> subscriptionWatcher;

    public static void startActivity(final Context context, Event e) {
        event = e;
        Intent intent = new Intent(context, ViewEventActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_event);

        TextView name = (TextView) findViewById(R.id.viewName);
        TextView time = (TextView) findViewById(R.id.viewTime);
        TextView where = (TextView) findViewById(R.id.viewWhere);
        TextView description = (TextView) findViewById(R.id.viewDescription);
        comments = (TextView) findViewById(R.id.comments);
        newComment = (EditText) findViewById(R.id.new_comment);

        name.setText(event.name());
        time.setText(event.when());
        where.setText(event.where());
        description.setText(event.description());

        refreshEvent(true);
        startSubscription();
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (subscriptionWatcher != null) {
            subscriptionWatcher.cancel();
        }
    }

    private void startSubscription() {
        NewCommentOnEventSubscription subscription = NewCommentOnEventSubscription.builder().eventId(event.id()).build();

        subscriptionWatcher = ClientFactory.getInstance(this.getApplicationContext()).subscribe(subscription);
        subscriptionWatcher.execute(subscriptionCallback);
    }

    private AppSyncSubscriptionCall.Callback<NewCommentOnEventSubscription.Data> subscriptionCallback = new AppSyncSubscriptionCall.Callback<NewCommentOnEventSubscription.Data>() {
        @Override
        public void onResponse(final @Nonnull Response<NewCommentOnEventSubscription.Data> response) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(ViewEventActivity.this, response.data().subscribeToEventComments().eventId().substring(0, 5) + response.data().subscribeToEventComments().content(), Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Subscription response: " + response.data().toString());
                    NewCommentOnEventSubscription.SubscribeToEventComments comment = response.data().subscribeToEventComments();

                    // UI only write
                    addComment(comment.content());

                    // Cache write
                    addCommentToCache(comment);

                    // Show changes from in cache
                    refreshEvent(true);
                }
            });
        }

        @Override
        public void onFailure(final @Nonnull ApolloException e) {
            Log.e(TAG, "Subscription failure", e);
        }

        @Override
        public void onCompleted() {
            Log.d(TAG, "Subscription completed");
        }
    };

    /**
     * Adds the new comment to the event in the cache.
     * @param comment
     */
    private void addCommentToCache(NewCommentOnEventSubscription.SubscribeToEventComments comment) {
        try {
            // Read the old event data
            GetEventQuery getEventQuery = GetEventQuery.builder().id(event.id()).build();
            GetEventQuery.Data readData = ClientFactory.getInstance(ViewEventActivity.this).getStore().read(getEventQuery).execute();
            Event event = readData.getEvent().fragments().event();

            // Create the new comment object
            Event.Item newComment = new Event.Item(
                    comment.__typename(),
                    comment.eventId(),
                    comment.commentId(),
                    comment.content(),
                    comment.createdAt());

            // Create the new comment list attached to the event
            List<Event.Item> items = new LinkedList<>(event.comments().items());
            items.add(0, newComment);

            // Create the new event data
            GetEventQuery.Data madeData = new GetEventQuery.Data(new GetEventQuery.GetEvent(readData.getEvent().__typename(), new GetEventQuery.GetEvent.Fragments(new Event(readData.getEvent().fragments().event().__typename(),
                    event.id(),
                    event.description(),
                    event.name(),
                    event.when(),
                    event.where(),
                    new Event.Comments(readData.getEvent().fragments().event().comments().__typename(), items)))));

            // Write the new event data
            ClientFactory.getInstance(ViewEventActivity.this).getStore().write(getEventQuery, madeData).execute();
            Log.d(TAG, "Wrote comment to database");
        } catch (ApolloException e) {
            Log.e(TAG, "Failed to update local database", e);
        }
    }

    /**
     * UI triggered method to add a comment. This will read the text box and submit a new comment.
     * @param view
     */
    public void addComment(View view) {
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(newComment.getWindowToken(), 0);

        Toast.makeText(this, "Submitting comment", Toast.LENGTH_SHORT).show();

        CommentOnEventMutation comment = CommentOnEventMutation.builder().content(newComment.getText().toString())
                .createdAt(new Date().toString())
                .eventId(event.id())
                .build();

        ClientFactory.getInstance(view.getContext())
                .mutate(comment)
                .enqueue(addCommentCallback);
    }

    /**
     * Service response subscriptionCallback confirming receipt of new comment triggered by UI.
     */
    private GraphQLCall.Callback<CommentOnEventMutation.Data> addCommentCallback = new GraphQLCall.Callback<CommentOnEventMutation.Data>() {
        @Override
        public void onResponse(@Nonnull Response<CommentOnEventMutation.Data> response) {
            Log.d(TAG, response.toString());
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    clearComment();
                }
            });
        }

        @Override
        public void onFailure(@Nonnull ApolloException e) {
            Log.e(TAG, "Failed to make comments mutation", e);
            Log.e(TAG, e.getMessage());
        }
    };

    /**
     * Refresh the event object to latest from service.
     */
    private void refreshEvent(final boolean cacheOnly) {
        GetEventQuery getEventQuery = GetEventQuery.builder().id(event.id()).build();

        ClientFactory.getInstance(getApplicationContext())
                .query(getEventQuery)
                .responseFetcher(cacheOnly ? AppSyncResponseFetchers.CACHE_ONLY : AppSyncResponseFetchers.CACHE_AND_NETWORK)
                .enqueue(refreshEventCallback);
    }

    private GraphQLCall.Callback<GetEventQuery.Data> refreshEventCallback = new GraphQLCall.Callback<GetEventQuery.Data>() {
        @Override
        public void onResponse(@Nonnull final Response<GetEventQuery.Data> response) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (response.errors().size() < 1) {
                        event = response.data().getEvent().fragments().event();
                        refreshComments();
                    } else {
                        Log.e(TAG, "Failed to get event.");
                    }
                }
            });
        }

        @Override
        public void onFailure(@Nonnull ApolloException e) {
            Log.e(TAG, "Failed to get event.");
        }
    };

    /**
     * Triggered by subscriptions/programmatically
     * @param comment
     */
    private void addComment(final String comment) {
        comments.setText(comment + "\n-----------\n");
    }

    /**
     * Reads the comments from the event object and preps it for display.
     */
    private void refreshComments() {
        StringBuilder stringBuilder = new StringBuilder();
        for (Event.Item i : event.comments().items()) {
            stringBuilder.append(i.content() + "\n---------\n");
        }
        comments.setText(stringBuilder.toString());
    }

    private void clearComment() {
        newComment.setText("");
    }

}
