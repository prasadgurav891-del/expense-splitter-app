package com.example.splitshare;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ClearCash extends AppCompatActivity {

    private TextView tvPayeeName, tvAmount, tvGroupName;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clear_cash);

        // Initialize TextViews
        tvPayeeName = findViewById(R.id.tvPayeeName);
        tvAmount = findViewById(R.id.tvAmount);
        tvGroupName = findViewById(R.id.tvGroupName);

        // Retrieve data from the Intent
        String loggedInUserPhone = getIntent().getStringExtra("loggedInUserPhone");
        String selectedUserPhone = getIntent().getStringExtra("selectedUserPhone");
        String groupId = getIntent().getStringExtra("groupId");
        long amountReceived = getIntent().getLongExtra("amountReceived", 0);

        // Log the data to check correctness
        Log.d("ClearCash", "Logged-in user: " + loggedInUserPhone);
        Log.d("ClearCash", "Selected user: " + selectedUserPhone);
        Log.d("ClearCash", "Group ID: " + groupId);
        Log.d("ClearCash", "Amount received: " + amountReceived);

        // Check if any required data is missing
        if (loggedInUserPhone == null || selectedUserPhone == null || groupId == null) {
            Toast.makeText(this, "Missing required information.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Display the passed data
        tvPayeeName.setText(selectedUserPhone != null ? selectedUserPhone : "N/A");
        tvAmount.setText("₹" + amountReceived);

        // Initialize Firebase database reference
        databaseReference = FirebaseDatabase.getInstance().getReference();

        // Fetch and display group name from Firebase
        fetchGroupNameFromFirebase(groupId);

        // Set up the button click listener
        Button btnProceed = findViewById(R.id.clearcash);
        btnProceed.setOnClickListener(v -> clearDebtsFromGroup(loggedInUserPhone, selectedUserPhone, groupId));
    }

    private void fetchGroupNameFromFirebase(String groupId) {
        if (groupId == null || groupId.isEmpty()) {
            Toast.makeText(this, "Group ID is missing.", Toast.LENGTH_SHORT).show();
            return;
        }

        databaseReference.child("groups").child(groupId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String groupName = dataSnapshot.child("groupName").getValue(String.class);

                    if (groupName != null) {
                        tvGroupName.setText(groupName);
                    } else {
                        tvGroupName.setText("Group name not available");
                    }
                } else {
                    tvGroupName.setText("Group not found");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(ClearCash.this, "Error fetching group name: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void clearDebtsFromGroup(String loggedInUserPhone, String selectedUserPhone, String groupId) {
        if (groupId == null || groupId.isEmpty()) {
            Toast.makeText(this, "Group ID is missing.", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference debtsRef = databaseReference.child("groups").child(groupId).child("debts");

        debtsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                boolean debtCleared = false;

                for (DataSnapshot debtSnapshot : dataSnapshot.getChildren()) {
                    String payerPhone = debtSnapshot.child("payerPhone").getValue(String.class);
                    String payeePhone = debtSnapshot.child("payeePhone").getValue(String.class);

                    // Check if the debt is between the logged-in user and the selected user
                    if ((payerPhone != null && payeePhone != null) &&
                            ((payerPhone.equals(loggedInUserPhone) && payeePhone.equals(selectedUserPhone)) ||
                                    (payerPhone.equals(selectedUserPhone) && payeePhone.equals(loggedInUserPhone)))) {

                        // Remove the debt from Firebase
                        debtSnapshot.getRef().removeValue();
                        debtCleared = true;
                    }
                }

                if (debtCleared) {
                    Toast.makeText(ClearCash.this, "Debt cleared successfully.", Toast.LENGTH_SHORT).show();
                    // Redirect to MemberBalances activity
                    Intent intent = new Intent(ClearCash.this, memberbalances.class);
                    intent.putExtra("groupId", groupId);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(ClearCash.this, "No debts found between the users.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(ClearCash.this, "Error clearing debts: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
