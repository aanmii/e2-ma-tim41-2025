package com.example.maproject.ui.friends;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.maproject.R;
import com.example.maproject.model.User;

import java.util.ArrayList;
import java.util.List;

public class FriendsAdapter extends RecyclerView.Adapter<FriendsAdapter.FriendViewHolder> {

    private List<User> friends;
    private OnFriendClickListener listener;

    public interface OnFriendClickListener {
        void onFriendClick(User friend);
    }

    public FriendsAdapter(List<User> friends, OnFriendClickListener listener) {
        this.friends = friends != null ? friends : new ArrayList<>();
        this.listener = listener;
    }

    public void updateFriends(List<User> newFriends) {
        this.friends = newFriends != null ? newFriends : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FriendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_friend, parent, false);
        return new FriendViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FriendViewHolder holder, int position) {
        User friend = friends.get(position);
        if (friend != null) {
            holder.bind(friend, listener);
        }
    }

    @Override
    public int getItemCount() {
        return friends != null ? friends.size() : 0;
    }

    static class FriendViewHolder extends RecyclerView.ViewHolder {
        ImageView avatarImageView;
        TextView usernameTextView;

        public FriendViewHolder(@NonNull View itemView) {
            super(itemView);
            avatarImageView = itemView.findViewById(R.id.friendAvatarImageView);
            usernameTextView = itemView.findViewById(R.id.friendUsernameTextView);
        }

        public void bind(User friend, OnFriendClickListener listener) {
            usernameTextView.setText(friend.getUsername() != null ? friend.getUsername() : "Unknown");

            if (friend.getAvatar() != null) {
                int avatarResId = itemView.getContext().getResources()
                        .getIdentifier(friend.getAvatar(), "drawable",
                                itemView.getContext().getPackageName());
                if (avatarResId != 0) {
                    avatarImageView.setImageResource(avatarResId);
                }
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onFriendClick(friend);
            });
        }
    }
}
