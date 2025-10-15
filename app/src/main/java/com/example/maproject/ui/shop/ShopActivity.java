package com.example.maproject.ui.shop;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.maproject.R;

public class ShopActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shop);

        // 🔹 Napici
        setupItem(R.id.item_potion1, R.drawable.ic_potion1, "Napitak snage +20%", "Povećava PP za 20% (cena: 50% nagrade)");
        setupItem(R.id.item_potion2, R.drawable.ic_potion2, "Napitak snage +40%", "Povećava PP za 40% (cena: 70% nagrade)");
        setupItem(R.id.item_potion3, R.drawable.ic_potion3, "Trajni napitak +5%", "Trajno povećava PP za 5% (cena: 200% nagrade)");
        setupItem(R.id.item_potion4, R.drawable.ic_potion4, "Trajni napitak +10%", "Trajno povećava PP za 10% (cena: 1000% nagrade)");

        // 🔹 Odeća
        setupItem(R.id.item_gloves, R.drawable.ic_gloves, "Rukavice", "+5 odbrane");
        setupItem(R.id.item_boots, R.drawable.ic_boots, "Čizme", "+7 brzine");
        setupItem(R.id.item_shield, R.drawable.ic_shield, "Štit", "+15 odbrane");

        // 🔹 Oružje
        setupItem(R.id.item_bow, R.drawable.ic_bow, "Luk i strela", "+12 napada");
        setupItem(R.id.item_sword, R.drawable.ic_sword, "Mač hrabrosti", "+15 napada");
    }

    /** Popunjava jedan shop item. */
    private void setupItem(int itemId, int imageRes, String titleText, String descText) {
        View itemView = findViewById(itemId);
        if (itemView == null) return;

        ImageView image = itemView.findViewById(R.id.item_image);
        TextView title = itemView.findViewById(R.id.item_title);
        TextView desc = itemView.findViewById(R.id.item_desc);
        Button buyButton = itemView.findViewById(R.id.buy_button);

        image.setImageResource(imageRes);
        title.setText(titleText);
        desc.setText(descText);

        buyButton.setOnClickListener(v ->
                Toast.makeText(this, "Kupljeno: " + titleText, Toast.LENGTH_SHORT).show()
        );
    }
}
