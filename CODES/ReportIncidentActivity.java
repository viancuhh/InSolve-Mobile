package com.angelfish.insolve;

import android.content.ContentResolver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Random;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import android.location.Geocoder;
import android.location.Address;
import java.util.List;
import java.util.Locale;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.content.ContextCompat;

public class ReportIncidentActivity extends BaseChildActivity {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
    private ActivityResultLauncher<String> imagePickerLauncher;
    private ActivityResultLauncher<Uri> cameraLauncher;
    private Uri tempCameraUri;
    private LinearLayout uploadPlaceholder;
    private ImageView ivPreview;
    private EditText etFullName, etContact, etExactAddress, etDescription;
    private Spinner spinnerIncidentType;
    private CheckBox checkAgree;
    private Button btnSubmit;
    private Uri selectedImageUri = null;
    private DatabaseReference dbreference;
    private FusedLocationProviderClient fusedLocationClient;
    private ActivityResultLauncher<String> locationPermissionLauncher;
    private androidx.cardview.widget.CardView btnGetLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_report_incident);
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

        //FIREBASE REF
        dbreference = FirebaseDatabase.getInstance("https://insolveapp-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference("reports");

        // INITIALIZE VIEWS
        etFullName = findViewById(R.id.etFullName);
        etContact = findViewById(R.id.etContactNumber);
        etExactAddress = findViewById(R.id.etExactAddress);
        etDescription = findViewById(R.id.etDescription);
        spinnerIncidentType = findViewById(R.id.spinnerIncidentType);
        checkAgree = findViewById(R.id.checkAgree);
        btnSubmit = findViewById(R.id.btnSubmit);
        uploadPlaceholder = findViewById(R.id.uploadPlaceholder);
        ivPreview = findViewById(R.id.ivPreview);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        btnGetLocation = findViewById(R.id.btnGetLocation);


        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        validateAndProcessImage(uri);
                    }
                }
        );

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (success && tempCameraUri != null) {
                        validateAndProcessImage(tempCameraUri);
                    } else {
                        Toast.makeText(this, "Camera capture canceled", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        uploadPlaceholder.setOnClickListener(v -> showImageSourceDialog());
        ivPreview.setOnClickListener(v -> showImageSourceDialog());

        btnSubmit.setOnClickListener(v -> validateAndSubmitReport());

        boolean isInitiallyChecked = checkAgree.isChecked();
        btnSubmit.setEnabled(isInitiallyChecked);
        btnSubmit.setAlpha(isInitiallyChecked ? 1.0f : 0.5f);

        checkAgree.setOnCheckedChangeListener((buttonView, isChecked) -> {
            btnSubmit.setEnabled(isChecked);
            btnSubmit.setAlpha(isChecked ? 1.0f : 0.5f);
        });

        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        fetchExactLocation();
                    } else {
                        Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        btnGetLocation.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                fetchExactLocation();
            } else {
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            }
        });
    }

    private void validateAndSubmitReport() {
        String name = etFullName.getText().toString().trim();
        String contact = etContact.getText().toString().trim();
        String address = etExactAddress.getText().toString().trim();
        String desc = etDescription.getText().toString().trim();
        String type = spinnerIncidentType.getSelectedItem().toString();

        if (name.isEmpty() || contact.isEmpty() || address.isEmpty() || desc.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }
        if (contact.length() != 11 || !contact.startsWith("0")) {
            Toast.makeText(this, "Contact number must be 11 digits and start with 0", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!checkAgree.isChecked()) {
            Toast.makeText(this, "You must agree to the terms", Toast.LENGTH_SHORT).show();
            return;
        }

        submitToFirebase(name, contact, type, desc, address);
    }

    private void submitToFirebase(String name, String contact, String type, String desc, String address) {
        btnSubmit.setEnabled(false);
        btnSubmit.setText("Submitting...");
        String base64Image = "";
        if (selectedImageUri != null) {
            base64Image = encodeImageToBase64(selectedImageUri);
            if (base64Image.isEmpty()) {
                Toast.makeText(this, "Failed to process image, submitting without it.", Toast.LENGTH_SHORT).show();
            }
        }
        findUniqueIdAndSave(name, contact, type, desc, address, base64Image);
    }

    private void findUniqueIdAndSave(String name, String contact, String type, String desc, String address, String base64Image) {
        String reportId = "INC-" + generateShortId();
        dbreference.child(reportId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (task.getResult().exists()) {
                    findUniqueIdAndSave(name, contact, type, desc, address, base64Image);
                } else {
                    saveReportToDatabase(reportId, name, contact, type, desc, address, base64Image);
                }
            } else {
                Toast.makeText(this, "Network error checking ID. Try again.", Toast.LENGTH_SHORT).show();
                btnSubmit.setEnabled(true);
                btnSubmit.setText("Submit");
            }
        });
    }

    private void saveReportToDatabase(String reportId, String name, String contact, String type, String desc, String address, String imageUrl) {
        IncidentReport report = new IncidentReport(
                reportId, name, contact, type, desc, address, imageUrl,
                System.currentTimeMillis(), "pending"
        );

        dbreference.child(reportId).setValue(report).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Report Submitted Successfully!" + "\nReport ID: " + reportId, Toast.LENGTH_LONG).show();
                finish();
            } else {
                Toast.makeText(this, "Failed to save report to database", Toast.LENGTH_SHORT).show();
                btnSubmit.setEnabled(true);
                btnSubmit.setText("Submit");
            }
        });
    }
    private String generateShortId() {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder result = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 5; i++) {
            result.append(characters.charAt(random.nextInt(characters.length())));
        }
        return result.toString();
    }

    private String encodeImageToBase64(Uri imageUri) {
        try {
            InputStream imageStream = getContentResolver().openInputStream(imageUri);
            Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);

            int maxSize = 800;
            int width = selectedImage.getWidth();
            int height = selectedImage.getHeight();
            float bitmapRatio = (float) width / (float) height;
            if (bitmapRatio > 1) {
                width = maxSize;
                height = (int) (width / bitmapRatio);
            } else {
                height = maxSize;
                width = (int) (height * bitmapRatio);
            }
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(selectedImage, width, height, true);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos);
            byte[] imageBytes = baos.toByteArray();

            return Base64.encodeToString(imageBytes, Base64.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    private void validateAndProcessImage(Uri uri) {
        ContentResolver resolver = getContentResolver();
        String mimeType = resolver.getType(uri);
        if (mimeType == null || !(mimeType.equals("image/jpeg") ||
                mimeType.equals("image/png") ||
                mimeType.equals("image/webp"))) {
            Toast.makeText(this, "Invalid format. Please use JPG, PNG, or WEBP.", Toast.LENGTH_SHORT).show();
            return;
        }
        long fileSize = getFileSize(uri);
        if (fileSize > MAX_FILE_SIZE) {
            Toast.makeText(this, "Image is too large. Maximum size is 10MB.", Toast.LENGTH_SHORT).show();
            return;
        }
        this.selectedImageUri = uri;
        ivPreview.setImageURI(uri);
        uploadPlaceholder.setVisibility(View.GONE);
        ivPreview.setVisibility(View.VISIBLE);
    }

    private void showImageSourceDialog() {
        String[] options = {"Take Photo", "Choose from Gallery"};
        new android.app.AlertDialog.Builder(this)
                .setTitle("Upload Visual Evidence")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        openCamera();
                    } else {
                        imagePickerLauncher.launch("image/*");
                    }
                })
                .show();
    }

    private void fetchExactLocation() {
        etExactAddress.setText("Locating...");

        try {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    convertCoordinatesToAddress(location.getLatitude(), location.getLongitude());
                } else {
                    etExactAddress.setText("");
                    Toast.makeText(this, "Make sure your GPS is turned on!", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private void convertCoordinatesToAddress(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);

                String fullAddress = address.getAddressLine(0);
                etExactAddress.setText(fullAddress);
            } else {
                etExactAddress.setText(latitude + ", " + longitude); // Fallback to raw coords
                Toast.makeText(this, "Could not find exact street name.", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            etExactAddress.setText(latitude + ", " + longitude); // Fallback to raw coords
        }
    }

    private void openCamera() {
        try {
            java.io.File tempFile = java.io.File.createTempFile("incident_", ".jpg", getCacheDir());

            tempCameraUri = androidx.core.content.FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    tempFile
            );

            cameraLauncher.launch(tempCameraUri);
        } catch (java.io.IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error preparing camera. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }

    private long getFileSize(Uri uri) {
        long size = 0;
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (sizeIndex != -1) {
                    size = cursor.getLong(sizeIndex);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return size;
    }
}