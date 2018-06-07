package com.ezerka.pingo.app;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.ezerka.pingo.R;
import com.ezerka.pingo.model.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;


public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";

    private static final String DOMAIN_NAME = "gmail.com";
    //vars
    public static boolean isActivityRunning;
    //widgets
    private EditText mEmail, mPassword, mConfirmPassword;
    private Button mRegister;
    private ProgressBar mProgressBar;
    private Context mContext;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        assignViews();
        assignLinks();

        hideSoftKeyboard();

    }

    private void assignViews() {
        mEmail = findViewById(R.id.input_email);
        mPassword = findViewById(R.id.input_password);
        mConfirmPassword = findViewById(R.id.input_confirm_password);
        mRegister = findViewById(R.id.btn_register);
        mProgressBar = findViewById(R.id.progressBar);

        mAuth = FirebaseAuth.getInstance();

        mContext = getApplicationContext();
    }

    private void assignLinks() {
        mRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                registerTheUser();
            }
        });

    }

    private void registerTheUser() {
        String sEmail = mEmail.getText().toString();
        String sPass = mPassword.getText().toString();
        String sConPass = mConfirmPassword.getText().toString();

        Log.d(TAG, "onClick: attempting to register.");

        //check for null valued EditText fields
        if (isEmpty(sEmail) && isEmpty(sPass) && isEmpty(sConPass)) {

            //check if user has a company email address
            if (isValidDomain(sEmail)) {

                //check if passwords match
                if (doStringsMatch(sPass, sConPass)) {

                    //Initiate registration task
                    registerNewEmail(sEmail, sPass);
                } else {
                    makeToast("Passwords Dont Match");
                }

            } else {
                makeToast("Please Register With Company Email ID");
            }

        } else {
            makeToast("Fill out all the details");
        }
    }

    public void registerNewEmail(final String email, String password) {

        showDialog();

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG, "createUserWithEmail:onComplete:" + task.isSuccessful());

                        if (task.isSuccessful()) {
                            Log.d(TAG, "onComplete: AuthState: " + mAuth.getCurrentUser().getUid());

                            //send email verificaiton
                            sendVerificationEmail();

                            //insert some default data
                            User user = new User();
                            user.setName(email.substring(0, email.indexOf("@")));
                            user.setPhone("1");
                            user.setProfile_image("");
                            user.setSecurity_level("1");
                            user.setUser_id(mAuth.getCurrentUser().getUid());
                            FirebaseDatabase.getInstance().getReference()
                                    .child(getString(R.string.dbnode_users))
                                    .child(mAuth.getCurrentUser().getUid())
                                    .setValue(user)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            mAuth.signOut();

                                            //redirect the user to the login screen
                                            redirectLoginScreen();
                                        }
                                    })

                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            makeToast("Something Went Wrong");
                                            mAuth.signOut();

                                            //redirect the user to the login screen
                                            redirectLoginScreen();
                                        }
                                    });

                        }
                        if (!task.isSuccessful()) {
                            makeToast("Unable To Register");
                            Log.d(TAG, "OnComplete: Failed" + task.getException());
                        }
                        hideDialog();

                        // ...
                    }
                });
    }

    /**
     * sends an email verification link to the user
     */
    private void sendVerificationEmail() {
        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null) {
            user.sendEmailVerification()
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                makeToast("Sent Verification Email");
                            } else {
                                makeToast("Couldn't Verify the Email");
                            }
                        }
                    });
        }

    }


    private boolean isValidDomain(String email) {
        Log.d(TAG, "isValidDomain: verifying email has correct domain: " + email);
        String domain = email.substring(email.indexOf("@") + 1).toLowerCase();
        Log.d(TAG, "isValidDomain: users domain: " + domain);
        return domain.equals(DOMAIN_NAME);
    }

    /**
     * Redirects the user to the login screen
     */
    private void redirectLoginScreen() {
        Log.d(TAG, "redirectLoginScreen: redirecting to login screen.");

        Intent intent = new Intent(mContext, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    private boolean doStringsMatch(String s1, String s2) {
        return s1.equals(s2);
    }


    private boolean isEmpty(String string) {
        return !string.equals("");
    }


    private void showDialog() {
        mProgressBar.setVisibility(View.VISIBLE);

    }

    private void hideDialog() {
        if (mProgressBar.getVisibility() == View.VISIBLE) {
            mProgressBar.setVisibility(View.INVISIBLE);
        }
    }

    private void hideSoftKeyboard() {
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    public void makeToast(String input) {
        Toast.makeText(mContext, input, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onStart() {
        super.onStart();
        isActivityRunning = true;
    }

    @Override
    public void onStop() {
        super.onStop();
        isActivityRunning = false;
    }

}
















