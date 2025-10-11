package com.example.maproject.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.maproject.data.UserRepository;

public class ViewModelFactory implements ViewModelProvider.Factory {

    private final UserRepository userRepository;

    public ViewModelFactory(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @SuppressWarnings("unchecked")
    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(AuthViewModel.class)) {
            return (T) new AuthViewModel(userRepository);
        }
        // add other view models
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}
