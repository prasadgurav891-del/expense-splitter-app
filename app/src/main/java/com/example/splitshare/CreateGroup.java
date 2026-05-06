package com.example.splitshare;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class CreateGroup extends AppCompatActivity {

    private EditText editGroupName, editGroupDescription, editNumberOfMembers;
    private Button btnGenerateMembers, btnCreateGroup;
    private LinearLayout linearLayoutMembers;
    private FirebaseAuth auth;
    private DatabaseReference usersRef;
    private DatabaseReference groupsRef;
    private DatabaseReference userGroupsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_group);

        // Initialize Firebase Auth and Realtime Database
        auth = FirebaseAuth.getInstance();
        usersRef = FirebaseDatabase.getInstance().getReference("users");
        groupsRef = FirebaseDatabase.getInstance().getReference("groups");
        userGroupsRef = FirebaseDatabase.getInstance().getReference("userGroups");

        // Initialize views
        editGroupName = findViewById(R.id.editGroupName);
        editGroupDescription = findViewById(R.id.editGroupDescription);
        editNumberOfMembers = findViewById(R.id.editNumberOfMembers);
        btnGenerateMembers = findViewById(R.id.btnGenerateMembers);
        btnCreateGroup = findViewById(R.id.btnCreateGroup);
        linearLayoutMembers = findViewById(R.id.linearLayoutMembers);

        // Set click listener for "Generate Member Fields" button
        btnGenerateMembers.setOnClickListener(v -> generateMemberFields());

        // Set click listener for "Create Group" button
        btnCreateGroup.setOnClickListener(v -> createGroup());
    }

    private void generateMemberFields() {
        String numberOfMembersText = editNumberOfMembers.getText().toString();

        // Validate input
        if (TextUtils.isEmpty(numberOfMembersText)) {
            Toast.makeText(this, "Please enter the number of members", Toast.LENGTH_SHORT).show();
            return;
        }

        int numberOfMembers;
        try {
            numberOfMembers = Integer.parseInt(numberOfMembersText);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid number of members", Toast.LENGTH_SHORT).show();
            return;
        }

        // Clear previous member fields
        linearLayoutMembers.removeAllViews();

        // Dynamically add EditText fields for each member
        for (int i = 0; i < numberOfMembers; i++) {
            EditText memberEditText = new EditText(this);
            memberEditText.setHint("Enter member " + (i + 1) + " phone number");
            memberEditText.setPadding(16, 16, 16, 16);
            linearLayoutMembers.addView(memberEditText);
        }
    }

    private void createGroup() {
        String groupName = editGroupName.getText().toString();
        String groupDescription = editGroupDescription.getText().toString();

        // Validate group details
        if (TextUtils.isEmpty(groupName)) {
            Toast.makeText(this, "Please enter a group name", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(groupDescription)) {
            Toast.makeText(this, "Please enter a group description", Toast.LENGTH_SHORT).show();
            return;
        }

        // Retrieve entered member phone numbers
        int childCount = linearLayoutMembers.getChildCount();
        Map<String, Boolean> membersMap = new HashMap<>();

        // Collect member phone numbers from the dynamic EditText fields
        for (int i = 0; i < childCount; i++) {
            View child = linearLayoutMembers.getChildAt(i);
            if (child instanceof EditText) {
                String memberPhone = ((EditText) child).getText().toString().trim();

                // Prepend country code +91 if not present
                if (!memberPhone.startsWith("+91")) {
                    memberPhone = "+91" + memberPhone;
                }

                if (TextUtils.isEmpty(memberPhone)) {
                    Toast.makeText(this, "Please fill all member fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                membersMap.put(memberPhone, true);
            }
        }

        // Add the current user to the group members
        String currentUserPhone = auth.getCurrentUser().getPhoneNumber();
        if (currentUserPhone != null) {
            // Prepend country code +91 if not present
            if (!currentUserPhone.startsWith("+91")) {
                currentUserPhone = "+91" + currentUserPhone;
            }
            membersMap.put(currentUserPhone, true);
        }

        // Create group in the database
        String groupId = groupsRef.push().getKey();
        if (groupId != null) {
            Group newGroup = new Group(groupId, groupName, groupDescription, membersMap);

            // Add group to "groups" node
            groupsRef.child(groupId).setValue(newGroup).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // Update userGroups for each member
                    for (String memberPhone : membersMap.keySet()) {
                        userGroupsRef.child(memberPhone).child(groupId).setValue(true)
                                .addOnCompleteListener(memberTask -> {
                                    if (memberTask.isSuccessful()) {
                                        Log.d("CreateGroup", "Group added to userGroups for: " + memberPhone);
                                    } else {
                                        Log.e("CreateGroup", "Error adding group to userGroups for: " + memberPhone, memberTask.getException());
                                    }
                                });
                    }

                    Toast.makeText(this, "Group created successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(this, "Failed to create group. Please try again.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    // Group class representing the group data structure
    public static class Group {
        private String groupId;
        private String groupName;
        private String groupDescription;
        private Map<String, Boolean> members;

        public Group(String groupId, String groupName, String groupDescription, Map<String, Boolean> members) {
            this.groupId = groupId;
            this.groupName = groupName;
            this.groupDescription = groupDescription;
            this.members = members;
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

        public Map<String, Boolean> getMembers() {
            return members;
        }
    }
}
