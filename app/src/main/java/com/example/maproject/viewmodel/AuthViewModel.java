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
        // Validacija
        if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || username.isEmpty() || avatar.isEmpty()) {
            registrationStatus.postValue("Sva polja moraju biti popunjena.");
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            registrationStatus.postValue("Unesite validnu email adresu.");
            return;
        }

        if (password.length() < 6) {
            registrationStatus.postValue("Lozinka mora imati najmanje 6 karaktera.");
            return;
        }

        if (!password.equals(confirmPassword)) {
            registrationStatus.postValue("Lozinke se ne poklapaju.");
            return;
        }

        if (username.length() < 3 || username.length() > 20) {
            registrationStatus.postValue("Korisničko ime mora imati između 3 i 20 karaktera.");
            return;
        }

        // Pozovi Repository
        userRepository.registerUser(email, password, username, avatar, registrationStatus);
    }

    public void loginUser(String email, String password) {
        // Validacija
        if (email.isEmpty() || password.isEmpty()) {
            loginStatus.postValue("Email i lozinka su obavezni.");
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            loginStatus.postValue("Unesite validnu email adresu.");
            return;
        }

        // Pozovi Repository
        userRepository.loginUser(email, password, loginStatus);
    }

    public void logout() {
        userRepository.logout();
    }
}