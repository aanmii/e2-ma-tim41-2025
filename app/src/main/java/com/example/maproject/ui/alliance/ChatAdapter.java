package com.example.maproject.ui.alliance;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.maproject.R;
import com.example.maproject.model.ChatMessage;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.MessageViewHolder> {

    private List<ChatMessage> messages;
    private final String currentUserId;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

    public ChatAdapter(List<ChatMessage> messages, String currentUserId) {
        this.messages = messages;
        this.currentUserId = currentUserId;
    }

    public void updateMessages(List<ChatMessage> newMessages) {
        this.messages = newMessages;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        holder.bind(message, currentUserId, timeFormat);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        CardView messageCard;
        TextView senderNameTextView;
        TextView messageContentTextView;
        TextView messageTimeTextView;
        ConstraintLayout parentLayout;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageCard = itemView.findViewById(R.id.messageCard);
            senderNameTextView = itemView.findViewById(R.id.senderNameTextView);
            messageContentTextView = itemView.findViewById(R.id.messageContentTextView);
            messageTimeTextView = itemView.findViewById(R.id.messageTimeTextView);
            parentLayout = (ConstraintLayout) itemView;
        }

        public void bind(ChatMessage message, String currentUserId, SimpleDateFormat timeFormat) {
            boolean isCurrentUser = message.getSenderId().equals(currentUserId);

            messageContentTextView.setText(message.getContent());
            messageTimeTextView.setText(timeFormat.format(message.getTimestamp()));

            ConstraintSet constraintSet = new ConstraintSet();
            constraintSet.clone(parentLayout);

            constraintSet.clear(messageCard.getId(), ConstraintSet.START);
            constraintSet.clear(messageCard.getId(), ConstraintSet.END);

            if (isCurrentUser) {
                constraintSet.connect(messageCard.getId(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, 0);

                messageCard.setCardBackgroundColor(ContextCompat.getColor(itemView.getContext(), R.color.accent));
                messageContentTextView.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.textPrimary));
                senderNameTextView.setVisibility(View.GONE);

            } else {
                constraintSet.connect(messageCard.getId(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, 0);

                messageCard.setCardBackgroundColor(ContextCompat.getColor(itemView.getContext(), R.color.cardBackground));
                messageContentTextView.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.textPrimary));

                senderNameTextView.setText(message.getSenderUsername());
                senderNameTextView.setVisibility(View.VISIBLE);
            }

            constraintSet.applyTo(parentLayout);
        }
    }
}