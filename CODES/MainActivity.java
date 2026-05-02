package com.angelfish.insolve;

import android.content.Intent;
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
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class MainActivity extends AppCompatActivity {
    TextView btnRegister;
    Button btnLogin;
    EditText etUsername, etPassword;
    DatabaseReference dbreference;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences themePrefs = getSharedPreferences("ThemePrefs", MODE_PRIVATE);
        boolean isDark = themePrefs.getBoolean("isDark", false);

        if (isDark) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ImageView btnToggleTheme = findViewById(R.id.btnToggleTheme);

        btnToggleTheme.setImageResource(isDark ? R.drawable.ic_sun : R.drawable.ic_moon);

        btnToggleTheme.setOnClickListener(v -> {
            boolean newMode = !themePrefs.getBoolean("isDark", false);
            themePrefs.edit().putBoolean("isDark", newMode).apply();

            if (newMode) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        });

        ImageView btnLogout = findViewById(R.id.btnLogout);
        if (btnLogout != null) {
            btnLogout.setVisibility( View. GONE);
        }

        dbreference = FirebaseDatabase.getInstance("https://insolveapp-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference("users");

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(v -> {
            String userStr = etUsername.getText().toString().trim();
            String passStr = etPassword.getText().toString().trim();

            if (userStr.isEmpty() || passStr.isEmpty()) {
                Toast.makeText(MainActivity.this, "Please enter all fields", Toast.LENGTH_SHORT).show();
            } else {
                loginUser(userStr, passStr);
            }
        });

        btnRegister = findViewById(R.id.btnRegister);
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });
    }
    private void loginUser(String username, String password) {
        dbreference.child(username).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DataSnapshot snapshot = task.getResult();
                if (snapshot != null && snapshot.exists()) {
                    Object dbPasswordObj = snapshot.child("password").getValue();
                    String dbPassword = (dbPasswordObj != null) ? dbPasswordObj.toString() : "";

                    if (dbPassword.equals(password)) {
                        Object roleObj = snapshot.child("role").getValue();
                        String role = (roleObj != null) ? roleObj.toString() : "user";

                        Intent intent;
                        if (role.equalsIgnoreCase("admin")) {
                            intent = new Intent(MainActivity.this, AdminDashboardActivity.class);
                        } else {
                            intent = new Intent(MainActivity.this, UserDashboardActivity.class);
                        }
                        try {
                            startActivity(intent);
                            finish();
                        } catch (Exception e) {
                            Toast.makeText(this, "Crash: Activity not found in Manifest!", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(this, "Incorrect Password", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "Username not found", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Database error. Check your connection.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}