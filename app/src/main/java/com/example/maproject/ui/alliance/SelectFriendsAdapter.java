package com.example.maproject.ui.alliance;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.maproject.R;
import com.example.maproject.model.User;

import java.util.List;

public class SelectFriendsAdapter extends RecyclerView.Adapter<SelectFriendsAdapter.ViewHolder> {

    private List<User> friends;
    private OnFriendSelectionListener listener;

    public interface OnFriendSelectionListener {
        void onFriendSelected(User friend, boolean isSelected);
    }

    public SelectFriendsAdapter(List<User> friends, OnFriendSelectionListener listener) {
        this.friends = friends;
        this.listener = listener;
    }

    public void updateFriends(List<User> newFriends) {
        this.friends = newFriends;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_select_friend, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User friend = friends.get(position);
        holder.bind(friend, listener);
    }

    @Override
    public int getItemCount() {
        return friends.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView avatarImageView;
        TextView usernameTextView;
        CheckBox selectCheckBox;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            avatarImageView = itemView.findViewById(R.id.selectFriendAvatarImageView);
            usernameTextView = itemView.findViewById(R.id.selectFriendUsernameTextView);
            selectCheckBox = itemView.findViewById(R.id.selectFriendCheckBox);
        }

        public void bind(User friend, OnFriendSelectionListener listener) {
            usernameTextView.setText(friend.getUsername());

            // Set avatar
            int avatarResId = itemView.getContext().getResources()
                    .getIdentifier(friend.getAvatar(), "drawable",
                            itemView.getContext().getPackageName());
            if (avatarResId != 0) {
                avatarImageView.setImageResource(avatarResId);
            }

            selectCheckBox.setOnCheckedChangeListener(null);
            selectCheckBox.setChecked(false);

            selectCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                listener.onFriendSelected(friend, isChecked);
            });

            itemView.setOnClickListener(v -> selectCheckBox.toggle());
        }
    }
}