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
    // Format za prikaz vremena notifikacije
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("dd.MM. HH:mm", Locale.getDefault());


    public interface OnNotificationClickListener {
        void onNotificationClick(Notification notification);
    }

    public NotificationsAdapter(List<Notification> notifications, OnNotificationClickListener listener) {
        this.notifications = notifications;
        this.listener = listener;
    }

    /**
     * Ažurira listu notifikacija i osvežava prikaz.
     */
    public void updateNotifications(List<Notification> newNotifications) {
        this.notifications = newNotifications;
        notifyDataSetChanged();
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

    /**
     * ViewHolder za pojedinačnu stavku notifikacije.
     */
    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView notificationCard;
        ImageView iconImageView;
        TextView contentTextView;
        TextView timeTextView;
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

            // Prikaz vremena (pretpostavljamo da je timestamp long)
            timeTextView.setText(timeFormat.format(notification.getTimestamp()));

            // Postavljanje ikone na osnovu tipa notifikacije
            int iconRes = getIconResForType(notification.getType());
            iconImageView.setImageResource(iconRes);

            // Indikator nepročitane poruke (mala crvena tačka)
            readIndicator.setVisibility(notification.isRead() ? View.GONE : View.VISIBLE);

            // Opciono: Postavljanje drugačije boje pozadine za nepročitane
            int bgColor = itemView.getContext().getResources().getColor(
                    notification.isRead() ? R.color.cardBackground : R.color.unreadNotificationBackground
            );
            notificationCard.setCardBackgroundColor(bgColor);

            // Rukovanje klikom
            itemView.setOnClickListener(v -> listener.onNotificationClick(notification));
        }


        private int getIconResForType(String type) {
            switch (type) {
                case "ALLIANCE_INVITE":
                    return R.drawable.ic_alliance_invite;
                case "ALLIANCE_ACCEPTED":
                    return R.drawable.ic_alliance_accept;
                case "CHAT_MESSAGE":
                    return R.drawable.ic_chat_message;
                default:
                    return R.drawable.ic_notification; // Podrazumevana ikona
            }
        }
    }
}