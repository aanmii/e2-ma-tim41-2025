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
            Toast.makeText(this, "Korisnik nije ulogovan!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Učitaj korisnika iz Firestore
        loadCurrentUser();

        // 🔹 Napici
        setupItem(R.id.item_potion1, R.drawable.ic_potion1, "Napitak snage +20%", "Povećava PP za 20%", "potion1", "potion", 50);
        setupItem(R.id.item_potion2, R.drawable.ic_potion2, "Napitak snage +40%", "Povećava PP za 40%", "potion2", "potion", 70);
        setupItem(R.id.item_potion3, R.drawable.ic_potion3, "Trajni napitak +5%", "Trajno povećava PP za 5%", "potion3", "potion", 200);
        setupItem(R.id.item_potion4, R.drawable.ic_potion4, "Trajni napitak +10%", "Trajno povećava PP za 10%", "potion4", "potion", 1000);

        // 🔹 Odeća
        setupItem(R.id.item_gloves, R.drawable.ic_gloves, "Rukavice", "+5 odbrane", "gloves", "clothing", 60);
        setupItem(R.id.item_boots, R.drawable.ic_boots, "Čizme", "+7 brzine", "boots", "clothing", 80);
        setupItem(R.id.item_shield, R.drawable.ic_shield, "Štit", "+15 odbrane", "shield", "clothing", 60);

        // 🔹 Oružje (nekupljivo)
        setupItem(R.id.item_bow, R.drawable.ic_bow, "Luk i strela", "+12 napada", "bow", "weapon", 0);
        setupItem(R.id.item_sword, R.drawable.ic_sword, "Mač hrabrosti", "+15 napada", "sword", "weapon", 0);
    }

    /** Popunjava jedan shop item sa cenom i kupovinom */
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

        // Pokaži cenu
        priceText.setText(String.valueOf(cost));
        coinIcon.setImageResource(R.drawable.coins);

        buyButton.setOnClickListener(v -> {
            if (cost <= 0) {
                Toast.makeText(this, "Ovo oružje se dobija samo od boss-a!", Toast.LENGTH_SHORT).show();
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
