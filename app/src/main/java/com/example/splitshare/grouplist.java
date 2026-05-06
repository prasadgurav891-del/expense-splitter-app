package com.example.splitshare;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class grouplist extends AppCompatActivity {

    private RecyclerView recyclerView;
    private GroupAdapter groupAdapter;
    private List<Group> groupList;
    private FirebaseAuth auth;
    private DatabaseReference usersRef;
    private DatabaseReference groupsRef;
    private FloatingActionButton fabAddGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grouplist);

        // Initialize Firebase Auth and Database
        auth = FirebaseAuth.getInstance();
        usersRef = FirebaseDatabase.getInstance().getReference("userGroups");
        groupsRef = FirebaseDatabase.getInstance().getReference("groups");

        // Initialize RecyclerView
        recyclerView = findViewById(R.id.recyclerViewGroups);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        groupList = new ArrayList<>();
        groupAdapter = new GroupAdapter(groupList);
        recyclerView.setAdapter(groupAdapter);

        // Initialize FloatingActionButton
        fabAddGroup = findViewById(R.id.fabAddGroup);

        // Navigate to CreateGroup activity on FAB click
        fabAddGroup.setOnClickListener(v -> {
            Intent intent = new Intent(grouplist.this, CreateGroup.class);
            startActivity(intent);
        });

        // Fetch groups for the authenticated user
        fetchUserGroups();
    }

    private void fetchUserGroups() {
        String userPhoneNumber = auth.getCurrentUser() != null ? auth.getCurrentUser().getPhoneNumber() : null;

        if (userPhoneNumber != null) {
            usersRef.child(userPhoneNumber).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        for (DataSnapshot groupIdSnapshot : dataSnapshot.getChildren()) {
                            String groupId = groupIdSnapshot.getKey();
                            fetchGroupDetails(groupId);
                        }
                    } else {
                        Log.d("GroupList", "No groups found for user.");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("GroupList", "Error fetching user groups.", error.toException());
                }
            });
        } else {
            Log.e("GroupList", "User phone number is null.");
        }
    }

    private void fetchGroupDetails(String groupId) {
        groupsRef.child(groupId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String groupName = dataSnapshot.child("groupName").getValue(String.class);
                    String groupDescription = dataSnapshot.child("groupDescription").getValue(String.class);

                    Group group = new Group(groupId, groupName, groupDescription);
                    groupList.add(group);
                    groupAdapter.notifyDataSetChanged();
                } else {
                    Log.d("GroupList", "Group details not found for ID: " + groupId);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("GroupList", "Error fetching group details.", error.toException());
            }
        });
    }

    public static class Group {
        private String groupId;
        private String groupName;
        private String groupDescription;

        public Group(String groupId, String groupName, String groupDescription) {
            this.groupId = groupId;
            this.groupName = groupName;
            this.groupDescription = groupDescription;
        }

        public String getGroupId() {
            return groupId;
        }

        public String getGroupName() {
            return groupName;
        }

        public String getGroupDescription() {
            return groupDescription;
        }
    }

    public static class GroupAdapter extends RecyclerView.Adapter<GroupAdapter.GroupViewHolder> {
        private final List<Group> groupList;

        public GroupAdapter(List<Group> groupList) {
            this.groupList = groupList;
        }

        @NonNull
        @Override
        public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card, parent, false);
            return new GroupViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull GroupViewHolder holder, int position) {
            Group group = groupList.get(position);
            holder.groupName.setText(group.getGroupName());
            holder.groupDescription.setText(group.getGroupDescription());

            // Handle group item click
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(holder.itemView.getContext(), groupdetails.class);
                intent.putExtra("groupId", group.getGroupId());
                intent.putExtra("groupName", group.getGroupName());
                intent.putExtra("groupDescription", group.getGroupDescription());
                holder.itemView.getContext().startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return groupList.size();
        }

        public static class GroupViewHolder extends RecyclerView.ViewHolder {
            TextView groupName, groupDescription;

            public GroupViewHolder(@NonNull View itemView) {
                super(itemView);
                groupName = itemView.findViewById(R.id.tvGroupName);
                groupDescription = itemView.findViewById(R.id.tvGroupDescription);
            }
        }
    }
}
