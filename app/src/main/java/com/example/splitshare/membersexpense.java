package com.example.splitshare;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

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

public class membersexpense extends AppCompatActivity {

    private RecyclerView recyclerView;
    private List<Member> memberList;
    private DatabaseReference groupsRef, usersRef;
    private String groupId;
    private Button btnAddExpense;
    private RecyclerView.Adapter<MemberViewHolder> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_membersexpense);

        // Initialize Firebase references
        groupsRef = FirebaseDatabase.getInstance().getReference("groups");
        usersRef = FirebaseDatabase.getInstance().getReference("users");

        // Get group ID from intent
        groupId = getIntent().getStringExtra("groupId");

        // Initialize RecyclerView
        recyclerView = findViewById(R.id.recyclerViewMembers);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        memberList = new ArrayList<>();

        // Initialize Add Expense Button
        btnAddExpense = findViewById(R.id.btnAddExpense);

        // Set OnClickListener for Add Expense Button
        btnAddExpense.setOnClickListener(v -> {
            // Create intent to navigate to AddExpense activity
            Intent intent = new Intent(membersexpense.this, addexpense.class);
            intent.putExtra("groupId", groupId);  // Pass the groupId or other necessary data
            startActivity(intent);
        });

        // Set the adapter for RecyclerView (Move it here to ensure it's set)
        adapter = new RecyclerView.Adapter<MemberViewHolder>() {
            @NonNull
            @Override
            public MemberViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                View itemView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_member, parent, false);
                return new MemberViewHolder(itemView);
            }

            @Override
            public void onBindViewHolder(MemberViewHolder holder, int position) {
                Member member = memberList.get(position);
                holder.memberPhone.setText(member.getPhoneNumber());
                holder.memberName.setText(member.getName());

                // Set OnClickListener to redirect to MemberBalanceActivity
                holder.itemView.setOnClickListener(v -> {
                    // Create intent to navigate to MemberBalanceActivity
                    Intent intent = new Intent(membersexpense.this, memberbalances.class);

                    // Pass both phoneNumber and groupId to the next activity
                    intent.putExtra("phoneNumber", member.getPhoneNumber());
                    intent.putExtra("groupId", groupId);  // Pass the groupId

                    startActivity(intent);
                });
            }

            @Override
            public int getItemCount() {
                return memberList.size();
            }
        };

        recyclerView.setAdapter(adapter);

        // Fetch members from Firebase
        fetchMembers();
    }

    private void fetchMembers() {
        if (groupId == null || groupId.isEmpty()) {
            // Handle missing group ID
            return;
        }

        groupsRef.child(groupId).child("members").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                memberList.clear();  // Clear any previous data

                // Iterate through the members node
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String phoneNumber = snapshot.getKey();

                    // Fetch member details (name) from the "users" node
                    fetchMemberDetails(phoneNumber);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("Membersexpense", "Error fetching members.", databaseError.toException());
            }
        });
    }

    private void fetchMemberDetails(String phoneNumber) {
        usersRef.child(phoneNumber).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String name = dataSnapshot.child("name").getValue(String.class);
                    memberList.add(new Member(phoneNumber, name));

                    // Notify the adapter that data has been updated
                    adapter.notifyDataSetChanged();
                } else {
                    Log.d("Membersexpense", "User not found: " + phoneNumber);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("Membersexpense", "Error fetching member details.", databaseError.toException());
            }
        });
    }

    // Member class to hold phone number and name
    public static class Member {
        private String phoneNumber;
        private String name;

        public Member(String phoneNumber, String name) {
            this.phoneNumber = phoneNumber;
            this.name = name;
        }

        public String getPhoneNumber() {
            return phoneNumber;
        }

        public String getName() {
            return name;
        }
    }

    // ViewHolder class for the RecyclerView
    public static class MemberViewHolder extends RecyclerView.ViewHolder {

        TextView memberPhone, memberName;

        public MemberViewHolder(View itemView) {
            super(itemView);
            memberPhone = itemView.findViewById(R.id.tvMemberPhone);
            memberName = itemView.findViewById(R.id.tvMemberName);  // Ensure this ID exists in item_member.xml
        }
    }
}
