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

import java.util.List;

public class AllianceInvitationsAdapter extends RecyclerView.Adapter<AllianceInvitationsAdapter.InvitationViewHolder> {

    private List<AllianceInvitation> invitations;
    private final OnInvitationActionListener listener;

    public interface OnInvitationActionListener {
        void onAction(AllianceInvitation invitation, String action); // "ACCEPT" or "REJECT"
    }

    public AllianceInvitationsAdapter(List<AllianceInvitation> invitations, OnInvitationActionListener listener) {
        this.invitations = invitations;
        this.listener = listener;
    }

    public void updateInvitations(List<AllianceInvitation> newInvitations) {
        this.invitations = newInvitations;
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
        holder.bind(invitation, listener);
    }

    @Override
    public int getItemCount() {
        return invitations.size();
    }

    static class InvitationViewHolder extends RecyclerView.ViewHolder {
        TextView allianceNameTextView;
        TextView senderTextView;
        Button acceptButton;
        Button rejectButton;

        public InvitationViewHolder(@NonNull View itemView) {
            super(itemView);
            allianceNameTextView = itemView.findViewById(R.id.allianceNameTextView);
            senderTextView = itemView.findViewById(R.id.senderTextView);
            acceptButton = itemView.findViewById(R.id.acceptButton);
            rejectButton = itemView.findViewById(R.id.rejectButton);
        }

        public void bind(AllianceInvitation invitation, OnInvitationActionListener listener) {
            allianceNameTextView.setText(invitation.getAllianceName());
            senderTextView.setText("Poslao/la: " + invitation.getSenderUsername());

            // Postavljanje listenera za akciju (Prihvati/Odbij)
            acceptButton.setOnClickListener(v -> listener.onAction(invitation, "ACCEPT"));
            rejectButton.setOnClickListener(v -> listener.onAction(invitation, "REJECT"));

            // Važno: Obezbediti da notifikacija ne može da se skloni dok se ne prihvati ili odbije
            // To se postiže logikom u Repository-ju i Activity-ju, a ne ovde u Adapteru.
            // Međutim, moramo osigurati da se pozivnica skloni nakon akcije.
        }
    }
}