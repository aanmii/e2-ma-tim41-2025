package com.example.maproject.viewmodel;

import android.util.Patterns;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.maproject.data.UserRepository;

public class AuthViewModel extends ViewModel {

    private final UserRepository userRepository;
    public final MutableLiveData<String> registrationStatus = new MutableLiveData<>();
    public final MutableLiveData<String> loginStatus = new MutableLiveData<>();

    public AuthViewModel(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void registerUser(String email, String password, String confirmPassword, String username, String avatar) {

        if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || username.isEmpty() || avatar.isEmpty()) {
            registrationStatus.postValue("No fields can be empty.");
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            registrationStatus.postValue("Please type in a valid email address.");
            return;
        }

        if (password.length() < 6) {
            registrationStatus.postValue("Password must be at least 6 characters long.");
            return;
        }

        if (!password.equals(confirmPassword)) {
            registrationStatus.postValue("Passwords do not match.");
            return;
        }

        if (username.length() < 3 || username.length() > 20) {
            registrationStatus.postValue("Username must have between 3 and 20 characters.");
            return;
        }


        userRepository.registerUser(email, password, username, avatar, registrationStatus);
    }

    public void loginUser(String email, String password) {

        if (email.isEmpty() || password.isEmpty()) {
            loginStatus.postValue("Email and password are required.");
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            loginStatus.postValue("Please enter a valid email address.");
            return;
        }


        userRepository.loginUser(email, password, loginStatus);
    }

    public void logout() {
        userRepository.logout();
    }
}