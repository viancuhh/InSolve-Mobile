package com.angelfish.insolve;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RegisterActivity extends AppCompatActivity {

    DatabaseReference dbreference;
    TextView btnLoginRedirect;
    Button btnRegister;
    EditText etName, etUsername, etPassword, etConfirmPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);
        ImageView btnToggleTheme = findViewById(R.id.btnToggleTheme);
        SharedPreferences themePrefs = getSharedPreferences("ThemePrefs", MODE_PRIVATE);

        boolean isDark = themePrefs.getBoolean("isDark", false);
        btnToggleTheme.setImageResource(isDark ? R.drawable.ic_sun : R.drawable.ic_moon);

        btnToggleTheme.setOnClickListener(v -> {
            boolean currentDarkState = themePrefs.getBoolean("isDark", false);
            themePrefs.edit().putBoolean("isDark", !currentDarkState).apply();
            if (!currentDarkState) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        });
        ImageView btnLogout = findViewById(R.id.btnLogout);
        if (btnLogout != null) {
            btnLogout.setVisibility(View.GONE);
        }

        dbreference = FirebaseDatabase.getInstance("https://insolveapp-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference("users");

        btnLoginRedirect = findViewById(R.id.btnLoginRedirect);
        btnRegister = findViewById(R.id.btnRegister);
        etName = findViewById(R.id.etName);
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);

        btnLoginRedirect.setOnClickListener(v -> finish());

        btnRegister.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String confirmPass = etConfirmPassword.getText().toString().trim();

            if (name.isEmpty() || username.isEmpty() || password.isEmpty() || confirmPass.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            } else if (!password.equals(confirmPass)) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            } else {
                dbreference.child(username).get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult().exists()) {
                            Toast.makeText(this, "Username already taken", Toast.LENGTH_SHORT).show();
                        } else {
                            String role = "user";
                            User user = new User(name, username, password, role);
                            dbreference.child(username).setValue(user).addOnCompleteListener(regTask -> {
                                if (regTask.isSuccessful()) {
                                    Toast.makeText(this, "Registration Successful!", Toast.LENGTH_SHORT).show();
                                    finish();
                                } else {
                                    Toast.makeText(this, "Registration Failed", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    } else {
                        Toast.makeText(this, "Fetch Failed. Check connection.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
}