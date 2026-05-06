package com.example.splitshare;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class Groups extends AppCompatActivity {

    private TextView tvName, tvPhone;
    private Button logoutButton, updateUpiButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_groups); // Replace with your layout file name

        // Initialize UI elements
        tvName = findViewById(R.id.tvName); // Replace with actual ID for name TextView
        tvPhone = findViewById(R.id.tvPhone); // Replace with actual ID for phone TextView
        logoutButton = findViewById(R.id.logoutButton); // Replace with actual ID for logout button
        updateUpiButton = findViewById(R.id.updateupi); // Update UPI button

        // FirebaseAuth instance
        FirebaseAuth auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() != null) {
            // Get the logged-in user's phone number
            String userPhone = auth.getCurrentUser().getPhoneNumber();

            if (userPhone != null) {
                // Reference the Firebase Realtime Database
                DatabaseReference userRef = FirebaseDatabase.getInstance()
                        .getReference("users")
                        .child(userPhone);

                // Fetch user data
                userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            // Retrieve user data from the snapshot
                            String name = snapshot.child("name").getValue(String.class);
                            String phone = snapshot.child("phone").getValue(String.class);

                            // Set the retrieved values to the TextViews
                            tvName.setText(name != null ? name : "Name not available");
                            tvPhone.setText(phone != null ? phone : "Phone not available");
                        } else {
                            Toast.makeText(Groups.this, "User data not found", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("FirebaseError", "Error fetching data", error.toException());
                        Toast.makeText(Groups.this, "Error fetching data", Toast.LENGTH_SHORT).show();
                    }
                });

                // Set up update UPI button functionality
                updateUpiButton.setOnClickListener(v -> {
                    Intent intent = new Intent(Groups.this, collectupi.class);
                    intent.putExtra("phone", userPhone); // Pass the user's phone number to the next activity
                    startActivity(intent);
                });

            } else {
                // Handle case where phone number is null
                Toast.makeText(this, "Unable to fetch phone number", Toast.LENGTH_SHORT).show();
                redirectToLogin();
            }
        } else {
            // Handle case where user is not logged in
            Toast.makeText(this, "User is not logged in", Toast.LENGTH_SHORT).show();
            redirectToLogin();
        }

        // Set up logout button functionality
        logoutButton.setOnClickListener(v -> {
            auth.signOut(); // Sign out the user
            redirectToLogin(); // Redirect to login activity
        });
    }

    private void redirectToLogin() {
        // Intent to navigate to the Login Activity
        Intent intent = new Intent(Groups.this, EnterMobileNumberOne.class); // Replace with your login activity class
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Clear activity stack
        startActivity(intent);
        finish(); // Finish the current activity
    }
}
