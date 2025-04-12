package com.example.justlearnitappp.ui.subjects;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.justlearnitappp.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

public class HistoryFragment extends Fragment {

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.activity_history, container, false);
        
        // Set up the quiz button
        FloatingActionButton fab = root.findViewById(R.id.fab);
        fab.setOnClickListener(view -> {
            Snackbar.make(view, "History Quiz Coming Soon!", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
        });
        
        // Set up lesson click listeners
        root.findViewById(R.id.lesson_ancient_civilizations).setOnClickListener(view -> {
            Snackbar.make(view, "Ancient Civilizations Lesson Coming Soon!", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
        });
        
        root.findViewById(R.id.lesson_world_wars).setOnClickListener(view -> {
            Snackbar.make(view, "World Wars Lesson Coming Soon!", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
        });
        
        return root;
    }
}