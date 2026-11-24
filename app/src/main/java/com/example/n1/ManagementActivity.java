package com.example.n1;

import android.view.View;
import android.widget.FrameLayout;

import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

abstract class ManagementActivity extends HssvActivity {
    final void showManagementScreen(int layout, CharSequence title) {
        useDarkSystemBars();
        setContentView(R.layout.activity_management);
        FrameLayout content = findViewById(R.id.management_content);
        getLayoutInflater().inflate(layout, content, true);

        Toolbar toolbar = findViewById(R.id.management_toolbar);
        setSupportActionBar(toolbar);
        setTitle(title);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        View header = findViewById(R.id.management_header);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.management_root), (view, insets) -> {
            Insets safeArea = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            header.setPadding(safeArea.left, safeArea.top, safeArea.right, 0);
            content.setPadding(safeArea.left, 0, safeArea.right, safeArea.bottom);
            return insets;
        });
    }
}
