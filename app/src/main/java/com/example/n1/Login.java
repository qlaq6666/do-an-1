package com.example.n1;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;

public class Login extends HssvActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        useDarkSystemBars();
        setContentView(R.layout.activity_main);
        applyScreenInsets(findViewById(R.id.main), true);

        EditText username = findViewById(R.id.txtuser);
        EditText password = findViewById(R.id.txtpass);
        findViewById(R.id.btnlogin).setOnClickListener(view -> {
            if ("admin".contentEquals(username.getText()) && "1".contentEquals(password.getText())) {
                startActivity(new Intent(this, Trang_Chu.class));
                finish();
            } else {
                password.setError(getString(R.string.login_error));
            }
        });
    }
}
