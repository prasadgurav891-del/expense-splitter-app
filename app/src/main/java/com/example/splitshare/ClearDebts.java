package com.example.splitshare;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ClearDebts extends AppCompatActivity {

    private TextView tvPayeeName, tvAmount, tvGroupName;
    private DatabaseReference databaseReference;
    private String loggedInUserPhone, selectedUserPhone, groupId, payeeUpiId;
    private long amountOwed;
    private static final int UPI_PAYMENT_REQUEST_CODE = 1;  // Unique request code

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clear_debts);

        // Initialize TextViews
        tvPayeeName = findViewById(R.id.tvPayeeName);
        tvAmount = findViewById(R.id.tvAmount);
        tvGroupName = findViewById(R.id.tvGroupName);

        // Retrieve data from Intent
        loggedInUserPhone = getIntent().getStringExtra("loggedInUserPhone");
        selectedUserPhone = getIntent().getStringExtra("selectedUserPhone");
        groupId = getIntent().getStringExtra("groupId");
        amountOwed = getIntent().getLongExtra("amountOwed", 0);

        // Display payee name and amount
        tvPayeeName.setText(selectedUserPhone != null ? selectedUserPhone : "N/A");
        tvAmount.setText("₹" + amountOwed);

        // Initialize Firebase database reference
        databaseReference = FirebaseDatabase.getInstance().getReference();

        // Fetch group name and UPI ID
        fetchGroupNameFromFirebase(groupId);
        fetchPayeeUpiId(selectedUserPhone);

        // Set up the payment button
        Button btnProceed1 = findViewById(R.id.btnProceed1);
        btnProceed1.setOnClickListener(v -> {
            if (payeeUpiId != null && !payeeUpiId.isEmpty()) {
                makeUPIPayment(payeeUpiId, amountOwed);
            } else {
                Toast.makeText(ClearDebts.this, "UPI ID not found for payee.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchGroupNameFromFirebase(String groupId) {
        databaseReference.child("groups").child(groupId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String groupName = dataSnapshot.child("groupName").getValue(String.class);
                    tvGroupName.setText(groupName != null ? groupName : "Group name not available");
                } else {
                    tvGroupName.setText("Group not found");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(ClearDebts.this, "Error fetching group name: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchPayeeUpiId(String payeePhone) {
        databaseReference.child("users").child(payeePhone).child("upiId").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    payeeUpiId = dataSnapshot.getValue(String.class);
                    Log.d("ClearDebts", "Fetched UPI ID: " + payeeUpiId);
                } else {
                    Toast.makeText(ClearDebts.this, "No UPI ID found for this user.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(ClearDebts.this, "Error fetching UPI ID: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void makeUPIPayment(String upiId, long amount) {
        Uri uri = Uri.parse("upi://pay")
                .buildUpon()
                .appendQueryParameter("pa", upiId) // Payee UPI ID
                .appendQueryParameter("pn", selectedUserPhone) // Payee Name
                .appendQueryParameter("tn", "Clearing debts via SplitShare") // Transaction note
                .appendQueryParameter("am", String.valueOf(amount)) // Amount
                .appendQueryParameter("cu", "INR") // Currency
                .build();

        Intent upiPayIntent = new Intent(Intent.ACTION_VIEW, uri);
        Intent chooser = Intent.createChooser(upiPayIntent, "Pay using UPI");

        try {
            startActivityForResult(chooser, UPI_PAYMENT_REQUEST_CODE);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No UPI app found. Please install one.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == UPI_PAYMENT_REQUEST_CODE) {
            if (resultCode == RESULT_OK && data != null) {
                String transactionResponse = data.getStringExtra("response");
                Log.d("ClearDebts", "UPI Transaction Response: " + transactionResponse);

                if (transactionResponse != null && transactionResponse.toLowerCase().contains("success")) {
                    Toast.makeText(this, "Payment Successful!", Toast.LENGTH_SHORT).show();
                    deleteDebtsAfterPayment();
                } else {
                    Toast.makeText(this, "Payment Failed or Cancelled.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Payment Cancelled.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void deleteDebtsAfterPayment() {
        DatabaseReference debtsRef = databaseReference.child("groups").child(groupId).child("debts");

        debtsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                boolean debtCleared = false;

                for (DataSnapshot debtSnapshot : dataSnapshot.getChildren()) {
                    String payerPhone = debtSnapshot.child("payerPhone").getValue(String.class);
                    String payeePhone = debtSnapshot.child("payeePhone").getValue(String.class);

                    if ((payerPhone != null && payeePhone != null) &&
                            ((payerPhone.equals(loggedInUserPhone) && payeePhone.equals(selectedUserPhone)) ||
                                    (payerPhone.equals(selectedUserPhone) && payeePhone.equals(loggedInUserPhone)))) {

                        // Remove the debt from Firebase
                        debtSnapshot.getRef().removeValue();
                        debtCleared = true;
                    }
                }

                if (debtCleared) {
                    Toast.makeText(ClearDebts.this, "Debt cleared successfully.", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(ClearDebts.this, memberbalances.class);
                    intent.putExtra("groupId", groupId);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(ClearDebts.this, "No debts found between the users.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(ClearDebts.this, "Error clearing debts: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
