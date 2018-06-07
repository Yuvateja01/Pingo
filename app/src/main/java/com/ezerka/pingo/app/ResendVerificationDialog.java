package com.ezerka.pingo.app;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.ezerka.pingo.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;


public class ResendVerificationDialog extends DialogFragment {

    private static final String TAG = "ResendVerificationDialo";

    //widgets
    private EditText mConfirmPassword, mConfirmEmail;
    private TextView confirmDialog;
    private TextView cancelDialog;
    //vars
    private Context mContext;

    private FirebaseAuth mAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_resend_verification, container, false);
        assignViews(view);
        assignLinks();

        return view;
    }

    private void assignViews(View view) {
        mConfirmPassword = view.findViewById(R.id.confirm_password);
        mConfirmEmail = view.findViewById(R.id.confirm_email);

        confirmDialog = view.findViewById(R.id.dialogConfirm);
        cancelDialog = view.findViewById(R.id.dialogCancel);

        mContext = getActivity();

        mAuth = FirebaseAuth.getInstance();
    }

    private void assignLinks() {
        confirmDialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reSendTheVerificationEmail();
            }
        });

        cancelDialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getDialog().dismiss();
            }
        });
    }

    private void reSendTheVerificationEmail() {
        String sEmail = mConfirmEmail.getText().toString();
        String sPass = mConfirmPassword.getText().toString();

        Log.d(TAG, "onClick: attempting to resend verification email.");

        if (isEmpty(sEmail) && isEmpty(sPass)) {

            //temporarily authenticate and resend verification email
            authenticateAndResendEmail(sEmail, sPass);
        } else {
            makeToast("All fields must be filled");
        }
    }

    private void authenticateAndResendEmail(String email, String password) {
        AuthCredential credential = EmailAuthProvider
                .getCredential(email, password);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "onComplete: reauthenticate success.");
                            sendVerificationEmail();
                            mAuth.signOut();
                            getDialog().dismiss();
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                makeToast("Invalid Credentials");
                getDialog().dismiss();
            }
        });
    }

    /**
     * sends an email verification link to the user
     */
    public void sendVerificationEmail() {
        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null) {
            user.sendEmailVerification()
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                makeToast("Verification Email Sent");
                            } else {
                                makeToast("Couldn't send the email");
                            }
                        }
                    });
        }

    }

    private boolean isEmpty(String string) {
        return !string.equals("");
    }

    private void makeToast(String input) {
        Toast.makeText(mContext, input, Toast.LENGTH_SHORT).show();
    }
}

















