package com.example.splitshare;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class groupexpenses extends AppCompatActivity {

    private RecyclerView rvExpenses;
    private ExpensesAdapter adapter;
    private List<Expense> expenseList;
    private DatabaseReference expensesRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_groupexpenses);

        rvExpenses = findViewById(R.id.rvExpenses);
        rvExpenses.setLayoutManager(new LinearLayoutManager(this));

        // Initialize the expense list
        expenseList = new ArrayList<>();

        // Get groupId from Intent
        String groupId = getIntent().getStringExtra("groupId");
        if (groupId == null || groupId.isEmpty()) {
            Toast.makeText(this, "Group ID is missing!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Initialize Firebase reference
        expensesRef = FirebaseDatabase.getInstance().getReference("groups").child(groupId).child("expenses");

        // Fetch expenses from Firebase
        fetchExpenses();
    }

    private void fetchExpenses() {
        expensesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                expenseList.clear();
                for (DataSnapshot expenseSnapshot : snapshot.getChildren()) {
                    Expense expense = expenseSnapshot.getValue(Expense.class);
                    if (expense != null) {
                        expenseList.add(expense);
                    }
                }
                // Update adapter
                adapter = new ExpensesAdapter(expenseList);
                rvExpenses.setAdapter(adapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(groupexpenses.this, "Failed to fetch expenses!", Toast.LENGTH_SHORT).show();
                Log.e("Firebase", "Error: " + error.getMessage());
            }
        });
    }

    // ExpensesAdapter class
    private class ExpensesAdapter extends RecyclerView.Adapter<ExpensesAdapter.ExpenseViewHolder> {

        private List<Expense> expenses;

        public ExpensesAdapter(List<Expense> expenses) {
            this.expenses = expenses;
        }

        @NonNull
        @Override
        public ExpenseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // Inflate the layout for each item
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transactions, parent, false);
            return new ExpenseViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ExpenseViewHolder holder, int position) {
            // Bind the data to the UI elements
            Expense expense = expenses.get(position);
            holder.tvExpenseName.setText("Expense name: " + expense.getName());
            holder.tvExpenseDescription.setText("Expense description: " +expense.getDescription());
            holder.tvExpenseAmount.setText("₹" + expense.getAmount());
            holder.tvExpenseDate.setText("Date: " +expense.getDate());
        }

        @Override
        public int getItemCount() {
            return expenses.size();
        }

        // ViewHolder class
        class ExpenseViewHolder extends RecyclerView.ViewHolder {
            TextView tvExpenseName, tvExpenseDescription, tvExpenseAmount, tvExpenseDate;

            public ExpenseViewHolder(@NonNull View itemView) {
                super(itemView);
                tvExpenseName = itemView.findViewById(R.id.tvExpenseName);
                tvExpenseDescription = itemView.findViewById(R.id.tvExpenseDescription);
                tvExpenseAmount = itemView.findViewById(R.id.tvExpenseAmount);
                tvExpenseDate = itemView.findViewById(R.id.tvExpenseDate);
            }
        }
    }

    // Expense model class
    public static class Expense {
        private String name;
        private String description;
        private String date;
        private int amount;

        public Expense() {
            // Default constructor for Firebase
        }

        public Expense(String name, String description, String date, int amount) {
            this.name = name;
            this.description = description;
            this.date = date;
            this.amount = amount;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public String getDate() {
            return date;
        }

        public int getAmount() {
            return amount;
        }
    }
}
