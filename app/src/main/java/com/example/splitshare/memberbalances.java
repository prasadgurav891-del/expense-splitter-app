package com.example.splitshare;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.ScrollView;
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

public class memberbalances extends AppCompatActivity {

    private TextView tvNewUserName, tvNewUserPhone, tvNewDebtHeading, tvNewDebtInfo;
    private LinearLayout linearLayoutDebts;
    private ScrollView scrollViewDebts;

    private DatabaseReference databaseReference;
    private String loggedInUserPhone;
    private String selectedUserPhone;

    private long netAmount = 0; // Global variable to hold aggregated net balance

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_memberbalances);

        // Initialize UI components
        tvNewUserName = findViewById(R.id.tvNewUserName);
        tvNewUserPhone = findViewById(R.id.tvNewUserPhone);
        tvNewDebtHeading = findViewById(R.id.tvNewDebtHeading);
        tvNewDebtInfo = findViewById(R.id.tvNewDebtInfo);
        linearLayoutDebts = findViewById(R.id.linearLayoutDebts);
        scrollViewDebts = findViewById(R.id.scrollViewNewDebts);

        FirebaseAuth auth = FirebaseAuth.getInstance();
        loggedInUserPhone = auth.getCurrentUser() != null ? auth.getCurrentUser().getPhoneNumber() : null;

        if (loggedInUserPhone == null) {
            Toast.makeText(this, "User is not logged in.", Toast.LENGTH_SHORT).show();
            return;
        }

        selectedUserPhone = getIntent().getStringExtra("phoneNumber");
        if (selectedUserPhone != null && !selectedUserPhone.isEmpty()) {
            tvNewUserPhone.setText(selectedUserPhone);
        } else {
            Toast.makeText(this, "No phone number provided.", Toast.LENGTH_SHORT).show();
            return;
        }

        databaseReference = FirebaseDatabase.getInstance().getReference();

        fetchUserName();
        fetchDebtsForSelectedGroup();

        // Set OnClickListener for the Clear Debts button
        findViewById(R.id.btnClearNewDebts).setOnClickListener(v -> {
            if (netAmount < 0) {
                // Redirect to ClearDebts activity
                Intent intent = new Intent(memberbalances.this, ClearDebts.class);
                intent.putExtra("loggedInUserPhone", loggedInUserPhone); // Pass logged-in user's phone number
                intent.putExtra("selectedUserPhone", selectedUserPhone); // Pass selected user's phone number
                intent.putExtra("groupId", getIntent().getStringExtra("groupId")); // Pass group ID
                intent.putExtra("amountOwed", Math.abs(netAmount)); // Pass absolute value of netAmount
                startActivity(intent);
            } else if (netAmount > 0) {
                // Redirect to ClearCash activity
                Intent intent = new Intent(memberbalances.this, ClearCash.class);
                intent.putExtra("loggedInUserPhone", loggedInUserPhone); // Pass logged-in user's phone number
                intent.putExtra("selectedUserPhone", selectedUserPhone); // Pass selected user's phone number
                intent.putExtra("groupId", getIntent().getStringExtra("groupId")); // Pass group ID
                intent.putExtra("amountReceived", netAmount); // Pass netAmount
                startActivity(intent);
            } else {
                // Show no outstanding debts
                Toast.makeText(memberbalances.this, "No outstanding debts.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchUserName() {
        if (selectedUserPhone == null || selectedUserPhone.isEmpty()) {
            Toast.makeText(this, "No phone number provided.", Toast.LENGTH_SHORT).show();
            return;
        }

        databaseReference.child("users").child(selectedUserPhone).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String userName = dataSnapshot.child("name").getValue(String.class);
                    tvNewUserName.setText(userName != null ? userName : "Name not available");
                } else {
                    tvNewUserName.setText("User not found");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(memberbalances.this, "Failed to fetch user details.", Toast.LENGTH_SHORT).show();
                Log.e("MemberBalances", "Database error: " + databaseError.getMessage());
            }
        });
    }

    private void fetchDebtsForSelectedGroup() {
        netAmount = 0; // Reset net amount

        // Get the selected group ID from Intent
        String groupId = getIntent().getStringExtra("groupId");
        if (groupId == null || groupId.isEmpty()) {
            Toast.makeText(this, "Group ID is missing.", Toast.LENGTH_SHORT).show();
            return;
        }

        fetchDebtsFromGroup(groupId);
    }

    private void fetchDebtsFromGroup(String groupId) {
        databaseReference.child("groups").child(groupId).child("debts").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Iterate through all debts for the selected group
                    for (DataSnapshot debtSnapshot : dataSnapshot.getChildren()) {
                        String payerPhone = debtSnapshot.child("payerPhone").getValue(String.class);
                        String payeePhone = debtSnapshot.child("payeePhone").getValue(String.class);
                        Long amount = debtSnapshot.child("amount").getValue(Long.class);
                        String description = debtSnapshot.child("description").getValue(String.class); // Get the description

                        if (amount != null) {
                            // Only consider debts between the logged-in user and the selected user
                            if ((loggedInUserPhone.equals(payeePhone) && selectedUserPhone.equals(payerPhone)) ||
                                    (loggedInUserPhone.equals(payerPhone) && selectedUserPhone.equals(payeePhone))) {

                                // Create TextView for displaying the debt
                                TextView debtTextView = new TextView(memberbalances.this);
                                debtTextView.setTextSize(16);
                                debtTextView.setPadding(16, 8, 16, 8);

                                // Add description and amount to the displayed debt
                                String debtInfo = description != null ? description : "No description provided";
                                if (loggedInUserPhone.equals(payeePhone) && selectedUserPhone.equals(payerPhone)) {
                                    netAmount -= amount; // Payee owes the payer
                                    debtTextView.setText(String.format("You owe the user ₹%d\nDescription: %s", amount, debtInfo));
                                } else if (loggedInUserPhone.equals(payerPhone) && selectedUserPhone.equals(payeePhone)) {
                                    netAmount += amount; // Payer owes the payee
                                    debtTextView.setText(String.format("The user owes you ₹%d\nDescription: %s", amount, debtInfo));
                                }

                                // Add the debt TextView to the LinearLayout inside the ScrollView
                                linearLayoutDebts.addView(debtTextView);
                            }
                        }
                    }

                    updateDebtInfoUI();
                } else {
                    Toast.makeText(memberbalances.this, "No debts found for the selected group.", Toast.LENGTH_SHORT).show();
                    updateDebtInfoUI();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(memberbalances.this, "Failed to fetch debts.", Toast.LENGTH_SHORT).show();
                Log.e("MemberBalances", "Database error: " + databaseError.getMessage());
            }
        });
    }

    private void updateDebtInfoUI() {
        if (netAmount > 0) {
            tvNewDebtInfo.setText(String.format("The user owes you ₹%d.", netAmount));
        } else if (netAmount < 0) {
            tvNewDebtInfo.setText(String.format("You owe the user ₹%d.", Math.abs(netAmount)));
        } else {
            tvNewDebtInfo.setText("No outstanding debts.");
        }
    }
}
