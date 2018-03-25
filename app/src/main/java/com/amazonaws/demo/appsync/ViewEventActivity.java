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
import com.amazonaws.mobileconnectors.appsync.fetcher.AppSyncResponseFetchers;
import com.apollographql.apollo.GraphQLCall;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;

import java.util.Date;

import javax.annotation.Nonnull;

public class ViewEventActivity extends AppCompatActivity {
    public static final String TAG = ViewEventActivity.class.getSimpleName();

    private static Event event;
    private TextView name, time, where, description, comments;
    private EditText newComment;

    public static void startActivity(final Context context, Event e) {
        event = e;
        Intent intent = new Intent(context, ViewEventActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_event);

        name = (TextView) findViewById(R.id.viewName);
        time = (TextView) findViewById(R.id.viewTime);
        where = (TextView) findViewById(R.id.viewWhere);
        description = (TextView) findViewById(R.id.viewDescription);
        comments = (TextView) findViewById(R.id.comments);
        newComment = (EditText) findViewById(R.id.new_comment);

        name.setText(event.name());
        time.setText(event.when());
        where.setText(event.where());
        description.setText(event.description());

        refreshComments();
    }

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
                .refetchQueries(GetEventQuery.builder().id(event.id()).build())
                .enqueue(commentCallback);
    }

    private GraphQLCall.Callback<CommentOnEventMutation.Data> commentCallback = new GraphQLCall.Callback<CommentOnEventMutation.Data>() {
        @Override
        public void onResponse(@Nonnull Response<CommentOnEventMutation.Data> response) {
            Log.d(TAG, response.toString());
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    clearComment();
                    refreshEvent();
                }
            });
        }

        @Override
        public void onFailure(@Nonnull ApolloException e) {
            Log.e(TAG, "Failed to make comments mutation", e);
            Log.e(TAG, e.getMessage());
        }
    };

    private void refreshEvent() {
        GetEventQuery getEventQuery = GetEventQuery.builder().id(event.id()).build();

        ClientFactory.getInstance(getApplicationContext())
                .query(getEventQuery)
                .responseFetcher(AppSyncResponseFetchers.CACHE_FIRST)
                .enqueue(getEventCallback);
    }

    private GraphQLCall.Callback<GetEventQuery.Data> getEventCallback = new GraphQLCall.Callback<GetEventQuery.Data>() {
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

    private void refreshComments() {
        StringBuilder stringBuilder = new StringBuilder();
        for (Event.Item i : event.comments().items()) {
            stringBuilder.append("\n---------\n" + i.content());
        }
        comments.setText(stringBuilder.toString());
    }

    private void clearComment() {
        newComment.setText("");
    }

}
