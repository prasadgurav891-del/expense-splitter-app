package com.example.splitshare;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class groupdetails extends AppCompatActivity {

    private TextView tvGroupNameDetail, tvGroupDescriptionDetail, tvGroupIdDetail;
    private ImageView imgGroupIconDetail;
    private DatabaseReference groupsRef;
    private Button btnMembers, btnExpenses;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_groupdetails);

        // Initialize UI elements
        tvGroupNameDetail = findViewById(R.id.tvGroupNameDetail);
        tvGroupDescriptionDetail = findViewById(R.id.tvGroupDescriptionDetail);
        tvGroupIdDetail = findViewById(R.id.tvGroupIdDetail); // TextView for Group ID
        imgGroupIconDetail = findViewById(R.id.imgGroupIconDetail);
        btnMembers = findViewById(R.id.btnMembers);
        btnExpenses = findViewById(R.id.btnExpenses);

        // Get data passed via Intent
        String groupId = getIntent().getStringExtra("groupId");
        String groupName = getIntent().getStringExtra("groupName");
        String groupDescription = getIntent().getStringExtra("groupDescription");

        // Debug: Check Intent data
        if (groupId == null || groupId.isEmpty()) {
            tvGroupIdDetail.setText("Group ID is missing!");
            Log.e("GroupDetails", "Group ID is missing!");
            return; // Stop further execution if groupId is missing
        }

        // Display basic group data
        tvGroupNameDetail.setText(groupName != null ? groupName : "Unknown Group");
        tvGroupDescriptionDetail.setText(groupDescription != null ? groupDescription : "No Description");
        tvGroupIdDetail.setText("Group ID: " + groupId);

        // Initialize Firebase reference
        groupsRef = FirebaseDatabase.getInstance().getReference("groups");

        // Fetch additional details from Firebase
        fetchGroupDetails(groupId);

        // Set click listeners
        btnMembers.setOnClickListener(v -> {
            Intent intent = new Intent(groupdetails.this, membersexpense.class );
            intent.putExtra("groupId", groupId); // Pass groupId to the next activity/fragment
            startActivity(intent);
        });

        btnExpenses.setOnClickListener(v -> {
            Intent intent = new Intent(groupdetails.this, groupexpenses.class );
            intent.putExtra("groupId", groupId); // Pass groupId to the next activity/fragment
            startActivity(intent);
        });
    }

    private void fetchGroupDetails(String groupId) {
        groupsRef.child(groupId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String groupName = dataSnapshot.child("groupName").getValue(String.class);
                    String groupDescription = dataSnapshot.child("groupDescription").getValue(String.class);
                    String groupIdFromFirebase = dataSnapshot.child("groupId").getValue(String.class); // Fetch groupId

                    // Debug: Log Firebase data
                    Log.d("Firebase", "GroupName: " + groupName);
                    Log.d("Firebase", "GroupDescription: " + groupDescription);
                    Log.d("Firebase", "GroupId: " + groupIdFromFirebase);

                    // Update UI
                    tvGroupNameDetail.setText(groupName != null ? groupName : "Unknown Group");
                    tvGroupDescriptionDetail.setText(groupDescription != null ? groupDescription : "No Description Available");
                    tvGroupIdDetail.setText(groupIdFromFirebase != null ? "Group ID: " + groupIdFromFirebase : "Group ID: Not Available");
                } else {
                    tvGroupIdDetail.setText("Group details not found!");
                    Log.e("Firebase", "Group details not found!");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                tvGroupIdDetail.setText("Failed to fetch group details!");
                Log.e("Firebase", "Error: " + databaseError.getMessage());
            }
        });
    }
}
