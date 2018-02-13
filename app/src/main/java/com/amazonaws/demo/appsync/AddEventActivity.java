package com.amazonaws.demo.appsync;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient;
import com.apollographql.apollo.GraphQLCall;
import com.apollographql.apollo.api.Error;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;

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

    private void save(){
        String nameString = name.getText().toString();
        String timeString = time.getText().toString();
        String upsString = where.getText().toString();
        String descriptionString = description.getText().toString();

        AWSAppSyncClient awsAppSyncClient = ClientFactory.getInstance(this.getApplicationContext());

        AddEventMutation addEventMutation = AddEventMutation.builder()
                .name(nameString)
                .when(timeString)
                .where(upsString)
                .description(descriptionString)
                .build();

        awsAppSyncClient.mutate(addEventMutation).enqueue(addEventsCallback);
    }

    private GraphQLCall.Callback<AddEventMutation.Data> addEventsCallback = new GraphQLCall.Callback<AddEventMutation.Data>() {
        @Override
        public void onResponse(@Nonnull final Response<AddEventMutation.Data> response) {
            if (response.hasErrors()) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Could not save", Toast.LENGTH_SHORT).show();
                        Log.d("Error: ", response.toString());

                        for (Error err : response.errors()) {
                            Log.d("Error: ", err.message());
                        }
                        finish();
                    }
                });
            } else {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Saved", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
            }
        }

        @Override
        public void onFailure(@Nonnull ApolloException e) {
            Log.e(TAG, "failed to make posts api call", e);
            Log.e(TAG, e.getMessage());
        }
    };
}
