package com.example.maproject.ui.notifications;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.maproject.R;
import com.example.maproject.model.Notification;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationsAdapter extends RecyclerView.Adapter<NotificationsAdapter.NotificationViewHolder> {

    private List<Notification> notifications;
    private final OnNotificationClickListener listener;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("dd.MM. HH:mm", Locale.getDefault());

    public interface OnNotificationClickListener {
        void onNotificationClick(Notification notification);
    }

    public NotificationsAdapter(List<Notification> notifications, OnNotificationClickListener listener) {
        this.notifications = notifications;
        this.listener = listener;
        setHasStableIds(true);
    }

    public void updateNotifications(List<Notification> newNotifications) {
        this.notifications = newNotifications;
        notifyDataSetChanged();
    }

    @Override
    public long getItemId(int position) {
        // Ensure notificationId is not null before calling hashCode()
        String id = notifications.get(position).getNotificationId();
        return id != null ? id.hashCode() : RecyclerView.NO_ID;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notifications, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        Notification notification = notifications.get(position);
        holder.bind(notification, listener, timeFormat);
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView notificationCard;
        ImageView iconImageView;
        TextView contentTextView, timeTextView;
        View readIndicator;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            notificationCard = itemView.findViewById(R.id.notificationCard);
            iconImageView = itemView.findViewById(R.id.notificationIcon);
            contentTextView = itemView.findViewById(R.id.notificationContent);
            timeTextView = itemView.findViewById(R.id.notificationTime);
            readIndicator = itemView.findViewById(R.id.readIndicator);
        }

        public void bind(Notification notification, OnNotificationClickListener listener, SimpleDateFormat timeFormat) {
            contentTextView.setText(notification.getContent());

            // FIX: Convert the Firebase Timestamp object to a Java Date object for SimpleDateFormat.
            // notification.getTimestamp() now returns com.google.firebase.Timestamp.
            Date displayDate = notification.getTimestamp() != null
                    ? notification.getTimestamp().toDate()
                    : null;

            if (displayDate != null) {
                timeTextView.setText(timeFormat.format(displayDate));
            } else {
                timeTextView.setText("");
            }

            int iconRes = getIconResForType(notification.getType());
            iconImageView.setImageResource(iconRes);

            readIndicator.setVisibility(notification.isRead() ? View.GONE : View.VISIBLE);

            int colorResId = notification.isRead() ? R.color.cardBackground : R.color.unreadNotificationBackground;
            int bgColor;

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                bgColor = itemView.getContext().getResources().getColor(
                        colorResId,
                        itemView.getContext().getTheme()
                );
            } else {
                bgColor = itemView.getContext().getResources().getColor(colorResId);
            }

            notificationCard.setCardBackgroundColor(bgColor);

            itemView.setOnClickListener(v -> listener.onNotificationClick(notification));
        }

        private int getIconResForType(String type) {
            switch (type) {
                case "ALLIANCE_INVITE": return R.drawable.ic_alliance_invite;
                case "ALLIANCE_ACCEPTED": return R.drawable.ic_alliance_accept;
                case "CHAT_MESSAGE": return R.drawable.ic_chat_message;
                default: return R.drawable.ic_notification;
            }
        }
    }
}
