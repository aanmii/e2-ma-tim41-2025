package com.example.maproject.ui.friends;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.maproject.R;
import com.example.maproject.model.Friend;

import java.util.ArrayList;
import java.util.List;

public class FriendRequestAdapter extends RecyclerView.Adapter<FriendRequestAdapter.RequestViewHolder> {

    private List<Friend> requests;
    private OnRequestActionListener listener;

    public interface OnRequestActionListener {
        void onAcceptRequest(Friend request);
        void onRejectRequest(Friend request);
    }

    public FriendRequestAdapter(List<Friend> requests, OnRequestActionListener listener) {
        this.requests = requests != null ? requests : new ArrayList<>();
        this.listener = listener;
    }

    public void updateRequests(List<Friend> newRequests) {
        this.requests = newRequests != null ? newRequests : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_friend_request, parent, false);
        return new RequestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RequestViewHolder holder, int position) {
        Friend request = requests.get(position);
        if (request != null) {
            holder.bind(request, listener);
        }
    }

    @Override
    public int getItemCount() {
        return requests != null ? requests.size() : 0;
    }

    static class RequestViewHolder extends RecyclerView.ViewHolder {
        TextView usernameTextView;
        Button acceptButton, rejectButton;

        public RequestViewHolder(@NonNull View itemView) {
            super(itemView);
            usernameTextView = itemView.findViewById(R.id.requestUsernameTextView);
            acceptButton = itemView.findViewById(R.id.acceptRequestButton);
            rejectButton = itemView.findViewById(R.id.rejectRequestButton);

        }

        public void bind(Friend request, OnRequestActionListener listener) {
            usernameTextView.setText(request.getFriendUsername() != null ? request.getFriendUsername() : "Unknown");

            acceptButton.setOnClickListener(v -> {
                if (listener != null) listener.onAcceptRequest(request);
            });
            rejectButton.setOnClickListener(v -> {
                if (listener != null) listener.onRejectRequest(request);
            });
        }
    }
}
