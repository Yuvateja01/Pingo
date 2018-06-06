package com.ezerka.pingo.app;


import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.ezerka.pingo.R;
import com.ezerka.pingo.issues.IssuesActivity;
import com.ezerka.pingo.model.Chatroom;
import com.ezerka.pingo.model.User;
import com.ezerka.pingo.utility.UniversalImageLoader;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.nostra13.universalimageloader.core.ImageLoader;

public class SignedInActivity extends AppCompatActivity {

    private static final String TAG = "SignedInActivity";
    //vars
    public static boolean isActivityRunning;

    // widgets
    //Firebase
    private FirebaseAuth.AuthStateListener mAuthListener;
    private Boolean mIsAdmin = false;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signedin);
        Log.d(TAG, "onCreate: started.");

        assignViews();

        setupFirebaseAuth();
        isAdmin();
        initFCM();
        initImageLoader();
        getPendingIntent();
    }

    private void assignViews() {
        mAuth = FirebaseAuth.getInstance();
    }


    private void initFCM() {
        String token = FirebaseInstanceId.getInstance().getToken();
        Log.d(TAG, "initFCM: token: " + token);
        sendRegistrationToServer(token);

    }

    private void getPendingIntent() {
        Log.d(TAG, "getPendingIntent: checking for pending intents.");

        Intent intent = getIntent();
        if (intent.hasExtra(getString(R.string.intent_chatroom))) {
            Log.d(TAG, "getPendingIntent: pending intent detected.");

            //get the chatroom
            Chatroom chatroom = intent.getParcelableExtra(getString(R.string.intent_chatroom));
            //navigate to the chatoom
            Intent chatroomIntent = new Intent(SignedInActivity.this, ChatroomActivity.class);
            chatroomIntent.putExtra(getString(R.string.intent_chatroom), chatroom);
            startActivity(chatroomIntent);
        }

    }

    private void isAdmin() {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();

        Query query = reference.child(getString(R.string.dbnode_users))
                .orderByChild(getString(R.string.field_user_id))
                .equalTo(mAuth.getCurrentUser().getUid());

        query.addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d(TAG, "onDataChange: datasnapshot: " + dataSnapshot);

                DataSnapshot singleSnapshot = dataSnapshot.getChildren().iterator().next();

                int securityLevel = Integer.parseInt(singleSnapshot.getValue(User.class).getSecurity_level());

                if (securityLevel == 10) {
                    Log.d(TAG, "onDataChange: user is an admin.");
                    mIsAdmin = true;
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d(TAG,databaseError.getDetails());
            }

        });

    }

    /**
     * Persist token to third-party servers.
     * <p>
     * Modify this method to associate the user's FCM InstanceID token with any server-side account
     * maintained by your application.
     *
     * @param token The new token.
     */
    private void sendRegistrationToServer(String token) {
        Log.d(TAG, "sendRegistrationToServer: sending token to server: " + token);

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();

        reference.child(getString(R.string.dbnode_users))
                .child(mAuth.getCurrentUser().getUid())
                .child(getString(R.string.field_messaging_token))
                .setValue(token);

    }


    private void checkAuthenticationState() {
        Log.d(TAG, "checkAuthenticationState: checking authentication state.");

        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) {
            Log.d(TAG, "checkAuthenticationState: user is null, navigating back to login screen.");

            Intent intent = new Intent(SignedInActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }

        else {
            Log.d(TAG, "checkAuthenticationState: user is authenticated.");
        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.options_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {

            case R.id.optionSignOut:
                signOut();
                return true;

            case R.id.optionAccountSettings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;

            case R.id.optionChat:
                startActivity(new Intent(this, ChatActivity.class));
                return true;

            case R.id.optionAdmin:
                if (mIsAdmin) {
                    startActivity(new Intent(this, AdminActivity.class));
                }

                else {
                    makeToast("You're not an admin");
                }
                return true;

            case R.id.optionIssues:
                startActivity(new Intent(this, IssuesActivity.class));
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }


    /**
     * init universal image loader
     */
    private void initImageLoader() {
        UniversalImageLoader imageLoader = new UniversalImageLoader(SignedInActivity.this);
        ImageLoader.getInstance().init(imageLoader.getConfig());
    }

    /**
     * Sign out the current user
     */
    private void signOut() {
        Log.d(TAG, "signOut: signing out");
        mAuth.signOut();
        makeToast("Signing  Out");
    }

    /*
            ----------------------------- Firebase setup ---------------------------------
         */
    private void setupFirebaseAuth() {
        Log.d(TAG, "setupFirebaseAuth: started.");

        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {

                    Log.d(TAG, "onAuthStateChanged:signed_in:" + user.getUid());

                }

                else {
                    Log.d(TAG, "onAuthStateChanged:signed_out");
                    Intent intent = new Intent(SignedInActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }

            }

        };

    }

    public void makeToast(String input){
        Toast.makeText(SignedInActivity.this,input,Toast.LENGTH_SHORT).show();
    }


    @Override
    protected void onResume() {
        super.onResume();
        checkAuthenticationState();
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
