package com.example.splitshare;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.firebase.FirebaseException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.auth.FirebaseAuth;

import java.util.concurrent.TimeUnit;

public class EnterMobileNumberOne extends AppCompatActivity {

    EditText enternumber;
    Button getotpbutton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enter_mobile_number_one);
        enternumber = findViewById(R.id.input_mobile_number);
        getotpbutton = findViewById(R.id.buttongetotp);
        ProgressBar progressBar = findViewById(R.id.progressbar_sending_otp);

        getotpbutton.setOnClickListener(v -> {
            // Check if mobile number is entered and has a length of 10
            if (!enternumber.getText().toString().trim().isEmpty()) {
                if (enternumber.getText().toString().trim().length() == 10) {

                    // Show progress bar and hide the button
                    progressBar.setVisibility(View.VISIBLE);
                    getotpbutton.setVisibility(View.INVISIBLE);

                    // Start Firebase phone verification with the new API
                    PhoneAuthOptions options = PhoneAuthOptions.newBuilder(FirebaseAuth.getInstance())
                            .setPhoneNumber("+91" + enternumber.getText().toString()) // Adding country code
                            .setTimeout(60L, TimeUnit.SECONDS) // Timeout duration
                            .setActivity(EnterMobileNumberOne.this) // Activity to handle the result
                            .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                                @Override
                                public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                                    // This is called when verification is completed
                                    progressBar.setVisibility(View.GONE);
                                    getotpbutton.setVisibility(View.VISIBLE);
                                }

                                @Override
                                public void onVerificationFailed(@NonNull FirebaseException e) {
                                    // This is called when verification fails
                                    progressBar.setVisibility(View.GONE);
                                    getotpbutton.setVisibility(View.VISIBLE);
                                    Toast.makeText(EnterMobileNumberOne.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                                }

                                @Override
                                public void onCodeSent(@NonNull String backendotp, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                                    // This is called when the OTP is sent
                                    progressBar.setVisibility(View.GONE);
                                    getotpbutton.setVisibility(View.VISIBLE);

                                    // Send OTP and mobile number to the next activity
                                    Intent intent = new Intent(getApplicationContext(), verification_otp.class);
                                    intent.putExtra("mobile", enternumber.getText().toString());
                                    intent.putExtra("backendotp", backendotp);
                                    startActivity(intent);
                                }
                            })
                            .build();

                    // Start the verification process
                    PhoneAuthProvider.verifyPhoneNumber(options);

                } else {
                    Toast.makeText(EnterMobileNumberOne.this, "Please enter a correct number", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(EnterMobileNumberOne.this, "Enter Mobile Number", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
