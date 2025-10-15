package com.example.maproject.ui.model;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.maproject.R;
import com.example.maproject.data.UserRepository;
import com.example.maproject.model.InventoryItem;
import com.example.maproject.model.User;
import com.example.maproject.ui.model.InventoryAdapter;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

public class InventoryActivity extends AppCompatActivity {

    private RecyclerView activeRecycler, availableRecycler;
    private InventoryAdapter activeAdapter, availableAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory);

        activeRecycler = findViewById(R.id.active_equipment_recycler);
        availableRecycler = findViewById(R.id.available_equipment_recycler);

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        UserRepository repo = new UserRepository();

        repo.getUser(userId, user -> {
            if (user == null) return;

            List<InventoryItem> active = user.getActiveEquipment();
            List<InventoryItem> available = user.getEquipment();

            activeAdapter = new InventoryAdapter(active, userId, true, availableAdapter, available);
            availableAdapter = new InventoryAdapter(available, userId, false, activeAdapter, active);

            activeRecycler.setLayoutManager(new LinearLayoutManager(this));
            availableRecycler.setLayoutManager(new LinearLayoutManager(this));

            activeRecycler.setAdapter(activeAdapter);
            availableRecycler.setAdapter(availableAdapter);
        });
    }
}
