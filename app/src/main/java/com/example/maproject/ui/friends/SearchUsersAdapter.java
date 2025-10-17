package com.example.maproject.ui.friends;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.maproject.R;
import com.example.maproject.model.User;

import java.util.ArrayList;
import java.util.List;

public class SearchUsersAdapter extends RecyclerView.Adapter<SearchUsersAdapter.UserViewHolder> {

    private List<User> users;
    private OnUserActionListener listener;

    public interface OnUserActionListener {
        void onAddFriend(User user);
        void onViewProfile(User user);
    }

    public SearchUsersAdapter(List<User> users, OnUserActionListener listener) {
        this.users = users != null ? users : new ArrayList<>();
        this.listener = listener;
    }

    public void updateUsers(List<User> newUsers) {
        this.users = newUsers != null ? newUsers : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_search_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = users.get(position);
        if (user != null) {
            holder.bind(user, listener);
        }
    }

    @Override
    public int getItemCount() {
        return users != null ? users.size() : 0;
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        ImageView avatarImageView;
        TextView usernameTextView;
        Button addButton;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            avatarImageView = itemView.findViewById(R.id.userAvatarImageView);
            usernameTextView = itemView.findViewById(R.id.userUsernameTextView);
            addButton = itemView.findViewById(R.id.addButton);
        }

        public void bind(User user, OnUserActionListener listener) {
            usernameTextView.setText(user.getUsername() != null ? user.getUsername() : "Unknown");

            if (user.getAvatar() != null) {
                int avatarResId = itemView.getContext().getResources()
                        .getIdentifier(user.getAvatar(), "drawable", itemView.getContext().getPackageName());
                if (avatarResId != 0) avatarImageView.setImageResource(avatarResId);
            }

            if (addButton != null && listener != null) {
                addButton.setOnClickListener(v -> listener.onAddFriend(user));
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onViewProfile(user);
                }
            });
        }
    }
}

