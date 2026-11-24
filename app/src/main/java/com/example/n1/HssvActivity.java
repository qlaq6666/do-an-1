package com.example.n1;

import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

abstract class HssvActivity extends AppCompatActivity {
    final void useDarkSystemBars() {
        EdgeToEdge.enable(this);
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(false);
        controller.setAppearanceLightNavigationBars(false);
    }

    final void applyScreenInsets(View root, boolean includeKeyboard) {
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            int types = WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout();
            if (includeKeyboard) {
                types |= WindowInsetsCompat.Type.ime();
            }
            Insets safe = insets.getInsets(types);
            view.setPadding(safe.left, safe.top, safe.right, safe.bottom);
            return insets;
        });
    }
}
