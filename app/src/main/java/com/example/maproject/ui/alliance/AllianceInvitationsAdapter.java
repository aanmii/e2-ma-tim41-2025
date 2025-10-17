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
    private boolean userHasAlliance;

    public AllianceInvitationsAdapter(InvitationActionListener listener) {
        this.invitations = new ArrayList<>();
        this.listener = listener;
        this.userHasAlliance = false;
    }

    public void updateInvitations(List<AllianceInvitation> newInvitations) {
        invitations = newInvitations;
        notifyDataSetChanged();
    }

    public void setUserHasAlliance(boolean hasAlliance) {
        this.userHasAlliance = hasAlliance;
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
        holder.bind(invitation, listener, userHasAlliance);
    }

    @Override
    public int getItemCount() {
        return invitations.size();
    }

    static class InvitationViewHolder extends RecyclerView.ViewHolder {
        TextView senderTextView, allianceTextView, warningTextView;
        Button acceptButton, rejectButton;

        public InvitationViewHolder(@NonNull View itemView) {
            super(itemView);
            senderTextView = itemView.findViewById(R.id.senderTextView);
            allianceTextView = itemView.findViewById(R.id.allianceNameTextView);
            warningTextView = itemView.findViewById(R.id.warningTextView);
            acceptButton = itemView.findViewById(R.id.acceptButton);
            rejectButton = itemView.findViewById(R.id.rejectButton);
        }

        public void bind(AllianceInvitation invitation, InvitationActionListener listener, boolean userHasAlliance) {
            senderTextView.setText("Poslao/la: " + invitation.getSenderUsername());
            allianceTextView.setText(invitation.getAllianceName());

            // Prikaži upozorenje ako korisnik već ima savez
            if (userHasAlliance) {
                warningTextView.setVisibility(View.VISIBLE);
            } else {
                warningTextView.setVisibility(View.GONE);
            }

            acceptButton.setOnClickListener(v -> listener.onAccept(invitation));
            rejectButton.setOnClickListener(v -> listener.onReject(invitation));
        }
    }
}