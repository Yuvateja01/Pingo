package com.ezerka.pingo.app;


import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.ezerka.pingo.R;
import com.ezerka.pingo.utility.UniversalImageLoader;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.nostra13.universalimageloader.core.ImageLoader;


public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    //constants
    private static final int ERROR_DIALOG_REQUEST = 9001;
    public static boolean isActivityRunning;
    //Firebase
    // widgets
    private TextView mRegister;
    private TextView mResetPassword;
    private TextView mReSendVerificationEmail;
    private EditText mEmail, mPassword;
    private ProgressBar mProgressBar;
    private Button signIn;


    private Context mContext;

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    private GoogleApiAvailability mGoogleApi;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        assignViews();
        assignLinks();
        hideSoftKeyboard();
        setupFirebaseAuth();
        initImageLoader();

    }

    private void assignViews() {
        mRegister = findViewById(R.id.link_register);
        mResetPassword = findViewById(R.id.forgot_password);
        mReSendVerificationEmail = findViewById(R.id.resend_verification_email);

        mEmail = findViewById(R.id.email);
        mPassword = findViewById(R.id.password);
        mProgressBar = findViewById(R.id.progressBar);
        signIn = findViewById(R.id.email_sign_in_button);

        mContext = getApplicationContext();

        mAuth = FirebaseAuth.getInstance();

        mGoogleApi = GoogleApiAvailability.getInstance();
    }

    private void assignLinks() {
        signIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signInTheUserButton();
            }
        });

        mRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(mContext, RegisterActivity.class));
            }
        });

        mResetPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PasswordResetDialog dialog = new PasswordResetDialog();
                dialog.show(getSupportFragmentManager(), "dialog_password_reset");
            }
        });

        mReSendVerificationEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ResendVerificationDialog dialog = new ResendVerificationDialog();
                dialog.show(getSupportFragmentManager(), "dialog_resend_email_verification");
            }
        });
    }

    private void signInTheUserButton() {
        String sEmail = mEmail.getText().toString();
        String sPass = mPassword.getText().toString();


        if (isEmpty(sEmail) && isEmpty(sPass)) {
            Log.d(TAG, "onClick: Attempting To Authenticate.");

            showDialog();

            mAuth.signInWithEmailAndPassword(sEmail, sPass)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {

                            hideDialog();

                        }
                    }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    makeToast("Authentication Failed");
                    hideDialog();
                }
            });
        } else {
            makeToast("All the fields must be filled");
        }
    }


    public boolean servicesOK() {
        Log.d(TAG, "servicesOK: Checking Google Services.");

        int isAvailable = mGoogleApi.isGooglePlayServicesAvailable(mContext);

        if (isAvailable == ConnectionResult.SUCCESS) {
            //everything is ok and the user can make mapping requests
            Log.d(TAG, "servicesOK: Play Services is OK");
            return true;
        } else if (mGoogleApi.isUserResolvableError(isAvailable)) {
            //an error occured, but it's resolvable
            Log.d(TAG, "servicesOK: an error occured, but it's resolvable.");
            Dialog dialog = mGoogleApi.getErrorDialog(this, isAvailable, ERROR_DIALOG_REQUEST);
            dialog.show();
        } else {
            makeToast("Can't Connect to the Services");
        }

        return false;
    }

    private void setupFirebaseAuth() {
        Log.d(TAG, "setupFirebaseAuth: started.");

        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {

                FirebaseUser user = firebaseAuth.getCurrentUser();

                if (user != null) {

                    //check if email is verified
                    if (user.isEmailVerified()) {
                        Log.d(TAG, "onAuthStateChanged:signed_in:" + user.getUid());
                        makeToast("Authenticated with: " + user.getEmail());

                        Intent intent = new Intent(mContext, SignedInActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

                        //check for extras from FCM
                        if (getIntent().getExtras() != null) {
                            Log.d(TAG, "initFCM: found intent extras: " + getIntent().getExtras().toString());
                            for (String key : getIntent().getExtras().keySet()) {
                                Object value = getIntent().getExtras().get(key);
                                Log.d(TAG, "initFCM: Key: " + key + " Value: " + value);
                            }
                            String data = getIntent().getStringExtra("data");
                            Log.d(TAG, "initFCM: data: " + data);
                        }
                        startActivity(intent);
                        finish();

                    } else {
                        makeToast("Email is not verified");
                        mAuth.signOut();
                    }

                } else {
                    // User is signed out
                    Log.d(TAG, "onAuthStateChanged:signed_out");
                }

            }
        };
    }


    /**
     * init universal image loader
     */
    private void initImageLoader() {
        UniversalImageLoader imageLoader = new UniversalImageLoader(mContext);
        ImageLoader.getInstance().init(imageLoader.getConfig());
    }

    private boolean isEmpty(String input) {
        return !input.equals("");
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

    /*
        ----------------------------- Firebase setup ---------------------------------
     */


    private void makeToast(String input) {
        Toast.makeText(mContext, input, Toast.LENGTH_SHORT).show();
    }


    @Override
    public void onStart() {
        super.onStart();
        FirebaseAuth.getInstance().addAuthStateListener(mAuthListener);
        isActivityRunning = true;
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            FirebaseAuth.getInstance().removeAuthStateListener(mAuthListener);
        }
        isActivityRunning = false;
    }
}














