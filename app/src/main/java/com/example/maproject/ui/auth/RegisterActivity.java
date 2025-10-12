package com.example.maproject.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.maproject.R;
import com.example.maproject.data.UserRepository;
import com.example.maproject.viewmodel.AuthViewModel;
import com.example.maproject.viewmodel.ViewModelFactory;

public class RegisterActivity extends AppCompatActivity {

    private EditText emailEditText, passwordEditText, confirmPasswordEditText, usernameEditText;
    private Button registerButton, goToLoginButton;
    private ImageView avatar1, avatar2, avatar3, avatar4, avatar5;
    private String selectedAvatar = "";

    private AuthViewModel authViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);


        UserRepository userRepository = new UserRepository();
        ViewModelFactory factory = new ViewModelFactory(userRepository);
        authViewModel = new ViewModelProvider(this, factory).get(AuthViewModel.class);


        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);
        usernameEditText = findViewById(R.id.usernameEditText);
        registerButton = findViewById(R.id.registerButton);
        goToLoginButton = findViewById(R.id.goToLoginButton);

        avatar1 = findViewById(R.id.avatar1);
        avatar2 = findViewById(R.id.avatar2);
        avatar3 = findViewById(R.id.avatar3);
        avatar4 = findViewById(R.id.avatar4);
        avatar5 = findViewById(R.id.avatar5);

        setupAvatarSelection();


        registerButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();
            String confirmPassword = confirmPasswordEditText.getText().toString().trim();
            String username = usernameEditText.getText().toString().trim();

            if (selectedAvatar.isEmpty()) {
                Toast.makeText(this, "Choose an avatar.", Toast.LENGTH_SHORT).show();
                return;
            }

            authViewModel.registerUser(email, password, confirmPassword, username, selectedAvatar);
        });


        authViewModel.registrationStatus.observe(this, status -> {
            Toast.makeText(this, status, Toast.LENGTH_LONG).show();

            if (status.contains("Registration succesful")) {

                emailEditText.postDelayed(() -> {
                    Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }, 2000);
            }
        });


        goToLoginButton.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void setupAvatarSelection() {
        View.OnClickListener avatarClickListener = v -> {

            resetAvatarSelection();


            v.setAlpha(1.0f);
            v.setScaleX(1.2f);
            v.setScaleY(1.2f);


            int id = v.getId();
            if (id == R.id.avatar1) {
                selectedAvatar = "avatar_1";
            } else if (id == R.id.avatar2) {
                selectedAvatar = "avatar_2";
            } else if (id == R.id.avatar3) {
                selectedAvatar = "avatar_3";
            } else if (id == R.id.avatar4) {
                selectedAvatar = "avatar_4";
            } else if (id == R.id.avatar5) {
                selectedAvatar = "avatar_5";
            }
        };

        avatar1.setOnClickListener(avatarClickListener);
        avatar2.setOnClickListener(avatarClickListener);
        avatar3.setOnClickListener(avatarClickListener);
        avatar4.setOnClickListener(avatarClickListener);
        avatar5.setOnClickListener(avatarClickListener);
    }

    private void resetAvatarSelection() {
        avatar1.setAlpha(0.5f);
        avatar1.setScaleX(1.0f);
        avatar1.setScaleY(1.0f);

        avatar2.setAlpha(0.5f);
        avatar2.setScaleX(1.0f);
        avatar2.setScaleY(1.0f);

        avatar3.setAlpha(0.5f);
        avatar3.setScaleX(1.0f);
        avatar3.setScaleY(1.0f);

        avatar4.setAlpha(0.5f);
        avatar4.setScaleX(1.0f);
        avatar4.setScaleY(1.0f);

        avatar5.setAlpha(0.5f);
        avatar5.setScaleX(1.0f);
        avatar5.setScaleY(1.0f);
    }
}