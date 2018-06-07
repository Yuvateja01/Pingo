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
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;


public class PasswordResetDialog extends DialogFragment {

    private static final String TAG = "PasswordResetDialog";

    //widgets
    private EditText mEmail;
    private TextView confirmDialog;

    //vars
    private Context mContext;

    private FirebaseAuth mAuth;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_resetpassword, container, false);
        assignView(view);
        assignLinks();


        return view;
    }

    private void assignView(View view) {
        mEmail = view.findViewById(R.id.email_password_reset);
        confirmDialog = view.findViewById(R.id.dialogConfirm);

        mContext = getActivity();

        mAuth = FirebaseAuth.getInstance();
    }

    private void assignLinks() {
        confirmDialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendPasswordResetEmailButton();
            }
        });
    }

    private void sendPasswordResetEmailButton() {
        String sEmail = mEmail.getText().toString();

        if (isEmpty(sEmail)) {
            Log.d(TAG, "onClick: attempting to send reset link to: " + sEmail);
            sendPasswordResetEmail(sEmail);
            getDialog().dismiss();
        }
    }

    public void sendPasswordResetEmail(String email) {
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "onComplete: Password Reset Email sent.");
                            makeToast("Password Reset Link sent to the Email");
                        } else {
                            Log.d(TAG, "onComplete: No user associated with that email.");
                            makeToast("No user is associated with this Email");

                        }
                    }
                });
    }


    private boolean isEmpty(String string) {
        return !string.equals("");
    }

    private void makeToast(String input) {
        Toast.makeText(mContext, input, Toast.LENGTH_SHORT).show();
    }
}

















