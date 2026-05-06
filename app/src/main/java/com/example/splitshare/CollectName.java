package com.example.splitshare;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class CollectName extends AppCompatActivity {

    private EditText etUserName;
    private Button btnSubmit;
    private DatabaseReference usersRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collect_name);

        etUserName = findViewById(R.id.etUserName);
        btnSubmit = findViewById(R.id.btnSubmit);

        // Get the phone number from the intent (we'll use this as the key)
        String phone = getIntent().getStringExtra("phone");

        usersRef = FirebaseDatabase.getInstance().getReference("users");

        btnSubmit.setOnClickListener(v -> {
            String name = etUserName.getText().toString().trim();

            if (!name.isEmpty()) {
                // Create a map to hold the user's details
                Map<String, Object> userInfo = new HashMap<>();
                userInfo.put("name", name);
                userInfo.put("phone", phone);
                userInfo.put("isVerified", true); // Example additional info

                // Save the profile under the phone number as the key
                usersRef.child(phone).setValue(userInfo).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(CollectName.this, "Profile created successfully!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(getApplicationContext(), dashboard.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    } else {
                        Toast.makeText(CollectName.this, "Failed to create profile!", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                etUserName.setError("Name is required!");
            }
        });
    }
}
