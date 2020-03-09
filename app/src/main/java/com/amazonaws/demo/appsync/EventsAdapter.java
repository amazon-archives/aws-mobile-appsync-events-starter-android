package com.amazonaws.demo.appsync;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

public class EventsAdapter extends BaseAdapter {

    private LayoutInflater mInflater;
    private List<ListEventsQuery.Item> events;

    EventsAdapter(Context context, List<ListEventsQuery.Item> events){
        this.events = events;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void setEvents(List<ListEventsQuery.Item> posts){
        this.events = posts;
    }

    @Override
    public int getCount() {
        return events.size();
    }

    @Override
    public Object getItem(int i) {
        return events.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View convertView, ViewGroup parent) {
        final ViewHolder holder;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.event_list_view, parent, false);
            holder = new ViewHolder();
            holder.nameTextView = (TextView) convertView.findViewById(R.id.eventTitle);
            holder.timeTextView = (TextView) convertView.findViewById(R.id.eventAuthor);
            holder.whereTextView = (TextView) convertView.findViewById(R.id.eventWhere);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        final ListEventsQuery.Item post = (ListEventsQuery.Item) getItem(i);

        holder.nameTextView.setText("Name: " + post.fragments().event().name());
        holder.timeTextView.setText("Time: " + post.fragments().event().when());
        holder.whereTextView.setText("Where: " + post.fragments().event().where());

        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ViewEventActivity.startActivity(view.getContext(), post.fragments().event());
            }
        });

        return convertView;
    }

    private static class ViewHolder {
        TextView nameTextView;
        TextView timeTextView;
        TextView whereTextView;
    }
}
