package com.example.maproject.ui.shop;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;

import com.example.maproject.R;
import com.example.maproject.data.UserRepository;
import com.example.maproject.model.InventoryItem;
import com.example.maproject.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ShopActivity extends AppCompatActivity {

    private UserRepository userRepository;
    private FirebaseUser firebaseUser;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shop);

        userRepository = new UserRepository();
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        if (firebaseUser == null) {
            Toast.makeText(this, "User is not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadCurrentUser();

        setupItem(R.id.item_potion1, R.drawable.ic_potion1, "Strength potion +20%", "+20% PP", "potion1", "potion", 50);
        setupItem(R.id.item_potion2, R.drawable.ic_potion2, "Strength potion +40%", "+40% PP", "potion2", "potion", 70);
        setupItem(R.id.item_potion3, R.drawable.ic_potion3, "Permanent potion +5%", "+5% PP permanently", "potion3", "potion", 200);
        setupItem(R.id.item_potion4, R.drawable.ic_potion4, "Permanent potion +10%", "+10% PP permanently", "potion4", "potion", 1000);

        setupItem(R.id.item_gloves, R.drawable.ic_gloves, "Hearty gloves", "+5 defense", "gloves", "clothing", 60);
        setupItem(R.id.item_boots, R.drawable.ic_boots, "Sparkly boots", "+7 speed", "boots", "clothing", 80);
        setupItem(R.id.item_shield, R.drawable.ic_shield, "Magic shield", "+15 defense", "shield", "clothing", 60);

        setupItem(R.id.item_bow, R.drawable.ic_bow, "Arrow and bow", "+12 attacks", "bow", "weapon", 0);
        setupItem(R.id.item_sword, R.drawable.ic_sword, "Sword", "+15 attacks", "sword", "weapon", 0);
    }

    private void setupItem(int itemId, int imageRes, String titleText, String descText, String itemKey, String type, int cost) {
        View itemView = findViewById(itemId);
        if (itemView == null) return;

        ImageView image = itemView.findViewById(R.id.item_image);
        TextView title = itemView.findViewById(R.id.item_title);
        TextView desc = itemView.findViewById(R.id.item_desc);
        TextView priceText = itemView.findViewById(R.id.item_price);
        ImageView coinIcon = itemView.findViewById(R.id.coin_icon);
        Button buyButton = itemView.findViewById(R.id.buy_button);

        image.setImageResource(imageRes);
        title.setText(titleText);
        desc.setText(descText);

        priceText.setText(String.valueOf(cost));
        coinIcon.setImageResource(R.drawable.coins);

        buyButton.setOnClickListener(v -> {
            if (cost <= 0) {
                Toast.makeText(this, "You can get this only from boss!", Toast.LENGTH_SHORT).show();
                return;
            }
            InventoryItem item = new InventoryItem(itemKey, titleText, type, 1, type.equals("clothing") ? 2 : 1);
            MutableLiveData<String> status = new MutableLiveData<>();
            status.observe(this, s -> Toast.makeText(this, s, Toast.LENGTH_SHORT).show());

            userRepository.buyItem(firebaseUser.getUid(), item, status);
        });
    }
    private void loadCurrentUser() {
        userRepository.getUser(firebaseUser.getUid(), user -> {
            currentUser = user;
        });
    }


}
