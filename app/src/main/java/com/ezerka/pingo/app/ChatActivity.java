package com.ezerka.pingo.app;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.ezerka.pingo.R;
import com.ezerka.pingo.model.ChatMessage;
import com.ezerka.pingo.model.Chatroom;
import com.ezerka.pingo.model.User;
import com.ezerka.pingo.utility.ChatroomListAdapter;
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;


public class ChatActivity extends AppCompatActivity {

    private static final String TAG = "ChatActivity";
    public static boolean isActivityRunning;
    //widgets
    private ListView mListView;
    private FloatingActionButton mFob;
    //vars
    private ArrayList<Chatroom> mChatrooms;
    private ChatroomListAdapter mAdapter;
    private int mSecurityLevel;

    private HashMap<String, String> mNumChatroomMessages;

    private FirebaseDatabase mData;
    private FirebaseAuth mAuth;

    private Context mContext;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        assignView();
        assignLinks();

        getUserSecurityLevel();

    }


    private void assignView() {
        mListView = findViewById(R.id.listView);
        mFob = findViewById(R.id.fob);
        mChatrooms = new ArrayList<>();

        mData = FirebaseDatabase.getInstance();
        mAuth = FirebaseAuth.getInstance();

        mContext = getApplicationContext();
    }

    private void assignLinks() {
        mFob.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NewChatroomDialog dialog = new NewChatroomDialog();
                dialog.show(getSupportFragmentManager(), getString(R.string.dialog_new_chatroom));
            }
        });
    }

    private void setupChatroomList() {
        Log.d(TAG, "setupChatroomList: setting up chatroom listview");
        mAdapter = new ChatroomListAdapter(mContext, R.layout.layout_chatroom_listitem, mChatrooms);
        mListView.setAdapter(mAdapter);
    }


    public void joinChatroom(final Chatroom chatroom) {
        //make sure the chatroom exists before joining
        DatabaseReference reference = mData.getReference();

        Query query = reference.child(getString(R.string.dbnode_chatrooms)).orderByKey();

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                for (DataSnapshot singleSnapshot : dataSnapshot.getChildren()) {
                    Map<String, Object> objectMap = (HashMap<String, Object>) singleSnapshot.getValue();

                    if (objectMap.get(getString(R.string.field_chatroom_id)).toString()
                            .equals(chatroom.getChatroom_id())) {

                        if (mSecurityLevel >= Integer.parseInt(chatroom.getSecurity_level())) {
                            Log.d(TAG, "onItemClick: selected chatroom: " + chatroom.getChatroom_id());

                            //add user to the list of users who have joined the chatroom
                            addUserToChatroom(chatroom);

                            //navigate to the chatoom
                            Intent intent = new Intent(mContext, ChatroomActivity.class);
                            intent.putExtra(getString(R.string.intent_chatroom), chatroom);
                            startActivity(intent);
                        } else {
                            makeToast("Insufficient Security Levels");
                        }

                        break;
                    }
                }
                getChatrooms();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                databaseError.getDetails();
            }
        });

    }

    private void addUserToChatroom(Chatroom chatroom) {
        DatabaseReference reference = mData.getReference();

        reference.child(getString(R.string.dbnode_chatrooms))
                .child(chatroom.getChatroom_id())
                .child(getString(R.string.field_users))
                .child(mAuth.getCurrentUser().getUid())
                .child(getString(R.string.field_last_message_seen))
                .setValue(mNumChatroomMessages.get(chatroom.getChatroom_id()));

    }

    public void getChatrooms() {
        Log.d(TAG, "getChatrooms: retrieving chatrooms from firebase database.");
        DatabaseReference reference = mData.getReference();
        mNumChatroomMessages = new HashMap<>();

        if (mAdapter != null) {
            mAdapter.clear();
            mChatrooms.clear();
        }

        Query query = reference.child(getString(R.string.dbnode_chatrooms)).orderByKey();

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot singleSnapshot : dataSnapshot.getChildren()) {
//                    Log.d(TAG, "onDataChange: found chatroom: "
//                            + singleSnapshot.getValue());
                    try {
                        if (singleSnapshot.exists()) {
                            Chatroom chatroom = new Chatroom();
                            Map<String, Object> objectMap = (HashMap<String, Object>) singleSnapshot.getValue();

                            Log.d(TAG, "onDataChange: found a chatroom: "
                                    + objectMap.get(getString(R.string.field_chatroom_name)).toString());
                            chatroom.setChatroom_id(objectMap.get(getString(R.string.field_chatroom_id)).toString());
                            chatroom.setChatroom_name(objectMap.get(getString(R.string.field_chatroom_name)).toString());
                            chatroom.setCreator_id(objectMap.get(getString(R.string.field_creator_id)).toString());
                            chatroom.setSecurity_level(objectMap.get(getString(R.string.field_security_level)).toString());


                            //                    chatroom.setChatroom_id(singleSnapshot.getValue(Chatroom.class).getChatroom_id());
                            //                    chatroom.setSecurity_level(singleSnapshot.getValue(Chatroom.class).getSecurity_level());
                            //                    chatroom.setCreator_id(singleSnapshot.getValue(Chatroom.class).getCreator_id());
                            //                    chatroom.setChatroom_name(singleSnapshot.getValue(Chatroom.class).getChatroom_name());

                            //get the chatrooms messages
                            ArrayList<ChatMessage> messagesList = new ArrayList<ChatMessage>();
                            int numMessages = 0;
                            for (DataSnapshot snapshot : singleSnapshot
                                    .child(getString(R.string.field_chatroom_messages)).getChildren()) {
                                ChatMessage message = new ChatMessage();
                                message.setTimestamp(snapshot.getValue(ChatMessage.class).getTimestamp());
                                message.setUser_id(snapshot.getValue(ChatMessage.class).getUser_id());
                                message.setMessage(snapshot.getValue(ChatMessage.class).getMessage());
                                messagesList.add(message);
                                numMessages++;
                            }
                            if (messagesList.size() > 0) {
                                chatroom.setChatroom_messages(messagesList);

                                //add the number of chatrooms messages to a hashmap for reference
                                mNumChatroomMessages.put(chatroom.getChatroom_id(), String.valueOf(numMessages));
                            }

                            //get the list of users who have joined the chatroom
                            List<String> users = new ArrayList<String>();
                            for (DataSnapshot snapshot : singleSnapshot
                                    .child(getString(R.string.field_users)).getChildren()) {
                                String user_id = snapshot.getKey();
                                Log.d(TAG, "onDataChange: user currently in chatroom: " + user_id);
                                users.add(user_id);
                            }
                            if (users.size() > 0) {
                                chatroom.setUsers(users);
                            }

                            mChatrooms.add(chatroom);
                        }

                        setupChatroomList();
                    } catch (NullPointerException e) {
                        Log.e(TAG, "onDataChange: NullPointerException: " + e.getMessage());
                    }
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d(TAG, "getChatrooms: Error: " + databaseError.getDetails());
            }
        });
    }

    private void getUserSecurityLevel() {
        DatabaseReference reference = mData.getReference();
        Query query = reference.child(getString(R.string.dbnode_users))
                .orderByChild(getString(R.string.field_user_id))
                .equalTo(mAuth.getCurrentUser().getUid());

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d(TAG, "onDataChange: datasnapshot: " + dataSnapshot);
                DataSnapshot singleSnapshot = dataSnapshot.getChildren().iterator().next();
                int securityLevel = Integer.parseInt(singleSnapshot.getValue(User.class).getSecurity_level());
                Log.d(TAG, "onDataChange: user has a security level of: " + securityLevel);
                mSecurityLevel = securityLevel;
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d(TAG, "getUserSecurityLevel: Error: " + databaseError.getDetails());
            }

        });
    }

    private void checkAuthenticationState() {
        Log.d(TAG, "checkAuthenticationState: checking authentication state.");

        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) {
            Log.d(TAG, "checkAuthenticationState: user is null, navigating back to login screen.");

            Intent intent = new Intent(mContext, LoginActivity.class);
            makeToast("Session Timed Out Please Login");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } else {
            Log.d(TAG, "checkAuthenticationState: user is authenticated.");
        }
    }

    public void showDeleteChatroomDialog(String chatroomId) {
        DeleteChatroomDialog dialog = new DeleteChatroomDialog();
        Bundle args = new Bundle();
        args.putString(getString(R.string.field_chatroom_id), chatroomId);
        dialog.setArguments(args);
        dialog.show(getSupportFragmentManager(), getString(R.string.dialog_delete_chatroom));
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: called.");
        checkAuthenticationState();
        getChatrooms();
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

    private void makeToast(String input) {
        Toast.makeText(mContext, input, Toast.LENGTH_SHORT).show();
    }

    private String getTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("Canada/Pacific"));
        return sdf.format(new Date());
    }

}












