package com.example.splitshare;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class dashboard extends AppCompatActivity {

    CardView group_card;
    CardView group_list;
    CardView profile_track;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        group_card = findViewById(R.id.group_card);

        group_card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(dashboard.this,Groups.class);
                startActivity(intent);
            }
        });
        group_list = findViewById(R.id.group_list);

        group_list.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(dashboard.this,grouplist.class);
                startActivity(intent);
            }
        });

    }
}