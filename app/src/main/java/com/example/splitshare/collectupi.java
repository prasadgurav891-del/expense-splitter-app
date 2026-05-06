package com.example.splitshare;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class collectupi extends AppCompatActivity {

    private EditText etUpiId;
    private Button btnValidate;

    private DatabaseReference databaseReference;
    private String userPhone; // To store the passed phone number

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collectupi);

        // Initialize UI elements
        etUpiId = findViewById(R.id.etUpiId);
        btnValidate = findViewById(R.id.btnValidate);

        // Initialize Firebase database reference
        databaseReference = FirebaseDatabase.getInstance().getReference("users");

        // Get the phone number passed from the previous activity
        userPhone = getIntent().getStringExtra("phone");

        if (userPhone == null || userPhone.isEmpty()) {
            Toast.makeText(this, "User phone number not provided!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Set button click listener
        btnValidate.setOnClickListener(v -> {
            String upiId = etUpiId.getText().toString().trim();

            // Validate the UPI ID
            if (upiId.isEmpty()) {
                etUpiId.setError("UPI ID is required!");
                return;
            }

            if (!validateUpiId(upiId)) {
                etUpiId.setError("Invalid UPI ID format!");
                return;
            }

            // Update the UPI ID in the database
            updateUpiIdInDatabase(userPhone, upiId);
        });
    }

    /**
     * Validates the UPI ID format.
     */
    private boolean validateUpiId(String upiId) {
        String upiPattern = "^[\\w.]+@[\\w]+$"; // Regex pattern for UPI ID
        return upiId.matches(upiPattern);
    }

    /**
     * Updates the UPI ID in the Firebase database for an existing user.
     */
    private void updateUpiIdInDatabase(String phone, String upiId) {
        // Update only the `upiId` field for the user
        databaseReference.child(phone).child("upiId").setValue(upiId)
                .addOnSuccessListener(aVoid -> {
                    // Show success message
                    Toast.makeText(collectupi.this, "UPI ID updated successfully!", Toast.LENGTH_SHORT).show();

                    // Redirect to Groups activity
                    Intent intent = new Intent(collectupi.this, Groups.class);
                    startActivity(intent);
                    finish(); // Finish the current activity to prevent returning to it
                })
                .addOnFailureListener(e ->
                        Toast.makeText(collectupi.this, "Failed to update UPI ID: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }
}
