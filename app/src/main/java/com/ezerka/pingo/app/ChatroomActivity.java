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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.ezerka.pingo.R;
import com.ezerka.pingo.model.ChatMessage;
import com.ezerka.pingo.model.Chatroom;
import com.ezerka.pingo.model.User;
import com.ezerka.pingo.utility.ChatMessageListAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

public class ChatroomActivity extends AppCompatActivity {

    private static final String TAG = "ChatroomActivity";
    public static boolean isActivityRunning;
    //firebase
    private FirebaseAuth.AuthStateListener mAuthListener;
    private DatabaseReference mMessagesReference;
    private FirebaseDatabase mData;
    private FirebaseAuth mAuth;
    //widgets
    private TextView mChatroomName;
    private ListView mListView;
    private EditText mMessage;
    private ImageView mCheckmark;
    //vars
    private Chatroom mChatroom;
    private List<ChatMessage> mMessagesList;
    private Set<String> mMessageIdSet;
    private ChatMessageListAdapter mAdapter;
    private ValueEventListener mValueEventListener;

    private Context mContext;




    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatroom);

        assignView();
        assignLinks();


        getSupportActionBar().hide();

        setupFirebaseAuth();
        getChatroom();
        hideSoftKeyboard();
    }

    private void assignView() {
        mChatroomName = findViewById(R.id.text_chatroom_name);
        mListView = findViewById(R.id.listView);
        mMessage = findViewById(R.id.input_message);
        mCheckmark = findViewById(R.id.checkmark);

        mData = FirebaseDatabase.getInstance();
        mAuth = FirebaseAuth.getInstance();

        mContext = getApplicationContext();

        mValueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                getChatroomMessages();

                //get the number of messages currently in the chat and update the database
                int numMessages = (int) dataSnapshot.getChildrenCount();
                updateNumMessages(numMessages);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
    }

    private void assignLinks() {

        mMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mListView.setSelection(mAdapter.getCount() - 1); //scroll to the bottom of the list
            }
        });

        mCheckmark.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendTheMessage();
            }
        });

    }

    private void sendTheMessage() {
        String sMessage = mMessage.getText().toString();

        if (!sMessage.equals("")) {
            Log.d(TAG, "onClick: sending new message: " + sMessage);

            //create the new message object for inserting
            ChatMessage newMessage = new ChatMessage();
            newMessage.setMessage(sMessage);
            newMessage.setTimestamp(getTimestamp());
            newMessage.setUser_id(mAuth.getCurrentUser().getUid());

            //get a database reference
            DatabaseReference reference = mData.getReference()
                    .child(getString(R.string.dbnode_chatrooms))
                    .child(mChatroom.getChatroom_id())
                    .child(getString(R.string.field_chatroom_messages));

            //create the new messages id
            String newMessageId = reference.push().getKey();

            //insert the new message into the chatroom
            assert newMessageId != null;
            reference
                    .child(newMessageId)
                    .setValue(newMessage);

            //clear the EditText
            mMessage.setText("");

            //refresh the messages list? Or is it done by the listener??
        }
    }

    /**
     * Retrieve the chatroom name using a query
     */
    private void getChatroom() {
        Log.d(TAG, "getChatroom: getting selected chatroom details");

        Intent intent = getIntent();
        if (intent.hasExtra(getString(R.string.intent_chatroom))) {
            Chatroom chatroom = intent.getParcelableExtra(getString(R.string.intent_chatroom));
            Log.d(TAG, "getChatroom: chatroom: " + chatroom.toString());
            mChatroom = chatroom;
            mChatroomName.setText(mChatroom.getChatroom_name());

            enableChatroomListener();
        }
    }

    private void getChatroomMessages() {

        if (mMessagesList == null) {
            mMessagesList = new ArrayList<>();
            mMessageIdSet = new HashSet<>();
            initMessagesList();
        }
        final DatabaseReference reference = mData.getReference();
        Query query = reference.child(getString(R.string.dbnode_chatrooms))
                .child(mChatroom.getChatroom_id())
                .child(getString(R.string.field_chatroom_messages));
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {

                    Log.d(TAG, "onDataChange: found chatroom message: "
                            + snapshot.getValue());
                    try {//need to catch null pointer here because the initial welcome message to the
                        //chatroom has no user id
                        ChatMessage message = new ChatMessage();
                        String userId = snapshot.getValue(ChatMessage.class).getUser_id();

                        //check to see if the message has already been added to the list
                        //if the message has already been added we don't need to add it again
                        if (!mMessageIdSet.contains(snapshot.getKey())) {
                            Log.d(TAG, "onDataChange: adding a new message to the list: " + snapshot.getKey());
                            //add the message id to the message set
                            mMessageIdSet.add(snapshot.getKey());
                            if (userId != null) { //check and make sure it's not the first message (has no user id)
                                message.setMessage(snapshot.getValue(ChatMessage.class).getMessage());
                                message.setUser_id(snapshot.getValue(ChatMessage.class).getUser_id());
                                message.setTimestamp(snapshot.getValue(ChatMessage.class).getTimestamp());
                                message.setProfile_image("");
                                message.setName("");
                                mMessagesList.add(message);
                            } else {
                                message.setMessage(snapshot.getValue(ChatMessage.class).getMessage());
                                message.setTimestamp(snapshot.getValue(ChatMessage.class).getTimestamp());
                                message.setProfile_image("");
                                message.setName("");
                                mMessagesList.add(message);
                            }
                        }

                    } catch (NullPointerException e) {
                        Log.e(TAG, "onDataChange: NullPointerException: " + e.getMessage());
                    }
                }
                //query the users node to get the profile images and names
                getUserDetails();
                mAdapter.notifyDataSetChanged(); //notify the adapter that the dataset has changed
                mListView.setSelection(mAdapter.getCount() - 1); //scroll to the bottom of the list
                //initMessagesList();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d(TAG, databaseError.getDetails());
            }
        });
    }

    private void getUserDetails() {
        DatabaseReference reference = mData.getReference();
        for (int i = 0; i < mMessagesList.size(); i++) {
            // Log.d(TAG, "onDataChange: searching for userId: " + mMessagesList.get(i).getUser_id());
            final int j = i;
            if (mMessagesList.get(i).getUser_id() != null && mMessagesList.get(i).getProfile_image().equals("")) {
                Query query = reference.child(getString(R.string.dbnode_users))
                        .orderByKey()
                        .equalTo(mMessagesList.get(i).getUser_id());
                query.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        DataSnapshot singleSnapshot = dataSnapshot.getChildren().iterator().next();
                        mMessagesList.get(j).setProfile_image(singleSnapshot.getValue(User.class).getProfile_image());
                        mMessagesList.get(j).setName(singleSnapshot.getValue(User.class).getName());
                        mAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.d(TAG, databaseError.getDetails());
                    }
                });
            }
        }

    }

    private void initMessagesList() {
        mAdapter = new ChatMessageListAdapter(mContext, R.layout.layout_chatmessage_listitem, mMessagesList);
        mListView.setAdapter(mAdapter);
        mListView.setSelection(mAdapter.getCount() - 1); //scroll to the bottom of the list
    }

    private String getTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("Canada/Pacific"));
        return sdf.format(new Date());
    }

    /*
            ----------------------------- Firebase setup ---------------------------------
    */


    private void checkAuthenticationState() {
        Log.d(TAG, "checkAuthenticationState: checking authentication state.");

        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) {
            Log.d(TAG, "checkAuthenticationState: user is null, navigating back to login screen.");

            Intent intent = new Intent(mContext, LoginActivity.class);
            makeToast("Please Login");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } else {
            Log.d(TAG, "checkAuthenticationState: user is authenticated.");
        }
    }

    private void setupFirebaseAuth() {
        Log.d(TAG, "setupFirebaseAuth: started.");

        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // User is signed in
                    Log.d(TAG, "onAuthStateChanged:signed_in:" + user.getUid());


                } else {
                    // User is signed out
                    Log.d(TAG, "onAuthStateChanged:signed_out");
                    makeToast("Signed Out");
                    Intent intent = new Intent(mContext, LoginActivity.class);
                    startActivity(intent);
                    finish();
                }
                // ...
            }
        };

    }

    /**
     * upadte the total number of message the user has seen
     */
    private void updateNumMessages(int numMessages) {
        DatabaseReference reference = mData.getReference();

        reference
                .child(getString(R.string.dbnode_chatrooms))
                .child(mChatroom.getChatroom_id())
                .child(getString(R.string.field_users))
                .child(mAuth.getCurrentUser().getUid())
                .child(getString(R.string.field_last_message_seen))
                .setValue(String.valueOf(numMessages));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMessagesReference.removeEventListener(mValueEventListener);
    }

    private void enableChatroomListener() {
         /*
            ---------- Listener that will watch the 'chatroom_messages' node ----------
         */
        mMessagesReference = mData.getReference().child(getString(R.string.dbnode_chatrooms))
                .child(mChatroom.getChatroom_id())
                .child(getString(R.string.field_chatroom_messages));

        mMessagesReference.addValueEventListener(mValueEventListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkAuthenticationState();
    }

    @Override
    public void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
        isActivityRunning = true;
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
        isActivityRunning = false;
    }

    private void hideSoftKeyboard() {
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    private void makeToast(String input) {
        Toast.makeText(mContext, input, Toast.LENGTH_SHORT).show();
    }
}






















