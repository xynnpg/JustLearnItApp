package com.example.justlearnitappp.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.example.justlearnitappp.R;
import com.example.justlearnitappp.databinding.FragmentHomeBinding;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Set up click listeners for subject cards
        binding.biologyCard.setOnClickListener(view -> {
            Navigation.findNavController(view).navigate(R.id.action_home_to_biology);
        });

        binding.historyCard.setOnClickListener(view -> {
            Navigation.findNavController(view).navigate(R.id.action_home_to_history);
        });

        binding.geographyCard.setOnClickListener(view -> {
            Navigation.findNavController(view).navigate(R.id.action_home_to_geography);
        });

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}