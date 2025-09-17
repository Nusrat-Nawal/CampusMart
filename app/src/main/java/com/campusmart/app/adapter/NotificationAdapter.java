package com.campusmart.app.adapter;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.campusmart.app.R;
import com.campusmart.app.model.Notification;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private List<Notification> notificationList;
    private Context context;
    private OnNotificationClickListener listener;

    public interface OnNotificationClickListener {
        void onNotificationClick(Notification notification);
    }

    public NotificationAdapter(Context context, List<Notification> notificationList, OnNotificationClickListener listener) {
        this.context = context;
        this.notificationList = notificationList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        Notification notification = notificationList.get(position);
        holder.bind(notification, listener);
    }

    @Override
    public int getItemCount() {
        return notificationList.size();
    }

    public void updateNotifications(List<Notification> newNotifications) {
        this.notificationList.clear();
        this.notificationList.addAll(newNotifications);
        notifyDataSetChanged();
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        TextView textViewMessage;
        TextView textViewTimestamp;

        NotificationViewHolder(View itemView) {
            super(itemView);
            textViewMessage = itemView.findViewById(R.id.textViewNotificationMessage);
            textViewTimestamp = itemView.findViewById(R.id.textViewNotificationTimestamp);
        }

        void bind(final Notification notification, final OnNotificationClickListener listener) {
            textViewMessage.setText(notification.getMessage());

            if (notification.getTimestamp() != null) {
                Timestamp ts = notification.getTimestamp();
                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
                textViewTimestamp.setText(sdf.format(ts.toDate()));
            } else {
                textViewTimestamp.setText("No timestamp");
            }

            if (!notification.isRead()) {
                textViewMessage.setTypeface(null, Typeface.BOLD);
            } else {
                textViewMessage.setTypeface(null, Typeface.NORMAL);
            }

            itemView.setOnClickListener(v -> {
                listener.onNotificationClick(notification);


                textViewMessage.setTypeface(null, Typeface.NORMAL);
            });
        }
    }
}
