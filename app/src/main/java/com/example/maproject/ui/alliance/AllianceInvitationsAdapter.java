package com.example.maproject.ui.alliance;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.maproject.R;
import com.example.maproject.model.AllianceInvitation;

import java.util.ArrayList;
import java.util.List;

public class AllianceInvitationsAdapter extends RecyclerView.Adapter<AllianceInvitationsAdapter.InvitationViewHolder> {

    public interface InvitationActionListener {
        void onAccept(AllianceInvitation invitation);
        void onReject(AllianceInvitation invitation);
    }

    private List<AllianceInvitation> invitations;
    private final InvitationActionListener listener;

    public AllianceInvitationsAdapter(InvitationActionListener listener) {
        this.invitations = new ArrayList<>();
        this.listener = listener;
    }

    public void updateInvitations(List<AllianceInvitation> newInvitations) {
        invitations = newInvitations;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public InvitationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_alliance_invitation, parent, false);
        return new InvitationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull InvitationViewHolder holder, int position) {
        AllianceInvitation invitation = invitations.get(position);
        holder.senderTextView.setText(invitation.getSenderUsername());
        holder.allianceTextView.setText(invitation.getAllianceName());

        holder.acceptButton.setOnClickListener(v -> listener.onAccept(invitation));
        holder.rejectButton.setOnClickListener(v -> listener.onReject(invitation));
    }

    @Override
    public int getItemCount() {
        return invitations.size();
    }

    static class InvitationViewHolder extends RecyclerView.ViewHolder {
        TextView senderTextView, allianceTextView;
        Button acceptButton, rejectButton;

        public InvitationViewHolder(@NonNull View itemView) {
            super(itemView);
            senderTextView = itemView.findViewById(R.id.senderTextView);
            allianceTextView = itemView.findViewById(R.id.allianceNameTextView);
            acceptButton = itemView.findViewById(R.id.acceptButton);
            rejectButton = itemView.findViewById(R.id.rejectButton);
        }
    }
}