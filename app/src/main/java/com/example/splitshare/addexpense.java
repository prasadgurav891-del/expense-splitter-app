package com.example.splitshare;

import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class addexpense extends AppCompatActivity {

    private EditText etExpenseName, etExpenseDescription, etExpenseAmount, etExpenseDate;
    private LinearLayout memberContainer;
    private Button btnSubmitExpense;
    private DatabaseReference groupsRef;

    private List<CheckBox> memberCheckBoxes = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_addexpense);

        // Initialize views
        etExpenseName = findViewById(R.id.etExpenseName);
        etExpenseDescription = findViewById(R.id.etExpenseDescription);
        etExpenseAmount = findViewById(R.id.etExpenseAmount);
        etExpenseDate = findViewById(R.id.etExpenseDate);
        memberContainer = findViewById(R.id.memberContainer);
        btnSubmitExpense = findViewById(R.id.btnSubmitExpense);

        // Firebase reference
        groupsRef = FirebaseDatabase.getInstance().getReference("groups");

        // Get group ID passed from the previous activity
        String groupId = getIntent().getStringExtra("groupId");

        // Fetch members from Firebase
        fetchMembers(groupId);

        // Handle submit button
        btnSubmitExpense.setOnClickListener(v -> {
            String expenseName = etExpenseName.getText().toString().trim();
            String expenseDescription = etExpenseDescription.getText().toString().trim();
            String expenseAmountStr = etExpenseAmount.getText().toString().trim();
            String expenseDate = etExpenseDate.getText().toString().trim();

            List<String> selectedMembers = getSelectedMembers();

            if (expenseName.isEmpty() || expenseDescription.isEmpty() || expenseAmountStr.isEmpty() || expenseDate.isEmpty() || selectedMembers.isEmpty()) {
                Toast.makeText(addexpense.this, "Please fill in all fields and select members.", Toast.LENGTH_SHORT).show();
            } else {
                try {
                    double expenseAmount = Double.parseDouble(expenseAmountStr); // Parse amount to double
                    addExpenseToFirebase(groupId, expenseName, expenseDescription, expenseAmount, expenseDate, selectedMembers);
                } catch (NumberFormatException e) {
                    Toast.makeText(addexpense.this, "Invalid amount format.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void fetchMembers(String groupId) {
        groupsRef.child(groupId).child("members").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String phoneNumber = snapshot.getKey();
                    fetchMemberName(phoneNumber); // Fetch and display names
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(addexpense.this, "Error fetching members: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchMemberName(String phoneNumber) {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
        usersRef.child(phoneNumber).child("name").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String memberName = dataSnapshot.getValue(String.class);
                if (memberName != null) {
                    addMemberCheckbox(memberName, phoneNumber); // Display name but keep phone number as tag
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(addexpense.this, "Error fetching member name: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addMemberCheckbox(String memberName, String phoneNumber) {
        CheckBox checkBox = new CheckBox(this);
        checkBox.setText(memberName); // Display the member's name
        checkBox.setTag(phoneNumber); // Use phone number as the hidden identifier
        memberCheckBoxes.add(checkBox);
        memberContainer.addView(checkBox);
    }

    private List<String> getSelectedMembers() {
        List<String> selectedMembers = new ArrayList<>();
        for (CheckBox checkBox : memberCheckBoxes) {
            if (checkBox.isChecked()) {
                selectedMembers.add((String) checkBox.getTag());
            }
        }
        return selectedMembers;
    }

    private void addExpenseToFirebase(String groupId, String expenseName, String expenseDescription, double expenseAmount, String expenseDate, List<String> selectedMembers) {
        String expenseId = groupsRef.child(groupId).child("expenses").push().getKey();

        Expense expense = new Expense(expenseName, expenseDescription, expenseAmount, expenseDate, selectedMembers);
        groupsRef.child(groupId).child("expenses").child(expenseId).setValue(expense)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Add debt record after expense is added within the group's debts table
                        saveDebtEntryInGroup(groupId, selectedMembers, expenseAmount, expenseDescription);
                        Toast.makeText(addexpense.this, "Expense added successfully!", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(addexpense.this, "Failed to add expense.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveDebtEntryInGroup(String groupId, List<String> selectedMembers, double amount, String expenseDescription) {
        // Get the authenticated user's phone number as the payer
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String payerPhone = currentUser != null ? currentUser.getPhoneNumber() : null;

        if (payerPhone == null) {
            Toast.makeText(addexpense.this, "Unable to determine payer. User is not authenticated.", Toast.LENGTH_SHORT).show();
            return;
        }

        // The rest of the members are payees who owe the payer
        for (int i = 0; i < selectedMembers.size(); i++) {
            String payeePhone = selectedMembers.get(i);
            if (!payeePhone.equals(payerPhone)) {
                double share = amount / selectedMembers.size();  // Divide the total expense equally

                // Create a debt entry for payee (payee owes payer)
                createDebtEntryInGroup(groupId, payerPhone, payeePhone, share, expenseDescription);
            }
        }
    }

    private void createDebtEntryInGroup(String groupId, String payerPhone, String payeePhone, double amount, String expenseDescription) {
        DatabaseReference debtsRef = FirebaseDatabase.getInstance().getReference("groups").child(groupId).child("debts");

        // Create a debt entry where payee owes the payer
        String debtId = debtsRef.push().getKey();
        Map<String, Object> debtEntry = new HashMap<>();
        debtEntry.put("payerPhone", payerPhone);
        debtEntry.put("payeePhone", payeePhone);
        debtEntry.put("amount", amount);
        debtEntry.put("description", expenseDescription); // Add description to the debt entry
        debtEntry.put("status", "pending"); // Status of the debt (pending, paid, etc.)

        // Add debt entry to the group's debts node
        debtsRef.child(debtId).setValue(debtEntry)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("Debt", "Debt entry created successfully for " + payeePhone + " to " + payerPhone);
                    } else {
                        Log.e("Debt", "Error creating debt entry: " + task.getException().getMessage());
                    }
                });
    }

    // Expense class
    public static class Expense {
        public String name;
        public String description;
        public double amount;
        public String date;
        public List<String> selectedMembers;

        public Expense(String name, String description, double amount, String date, List<String> selectedMembers) {
            this.name = name;
            this.description = description;
            this.amount = amount;
            this.date = date;
            this.selectedMembers = selectedMembers;
        }
    }
}
