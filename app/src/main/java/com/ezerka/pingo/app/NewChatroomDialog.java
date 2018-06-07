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
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.ezerka.pingo.R;
import com.ezerka.pingo.model.ChatMessage;
import com.ezerka.pingo.model.Chatroom;
import com.ezerka.pingo.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;


public class NewChatroomDialog extends DialogFragment {

    private static final String TAG = "NewChatroomDialog";

    private SeekBar mSeekBar;
    private EditText mChatroomName;
    private TextView mCreateChatroom, mSecurityLevel;
    private int mUserSecurityLevel;
    private int mSeekProgress;

    private FirebaseDatabase mData;
    private FirebaseAuth mAuth;

    private Context mContext;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_new_chatroom, container, false);
        assignViews(view);
        assignLinks();
        getUserSecurityLevel();
        return view;
    }

    private void assignViews(View view) {
        mChatroomName = view.findViewById(R.id.input_chatroom_name);
        mSeekBar = view.findViewById(R.id.input_security_level);
        mCreateChatroom = view.findViewById(R.id.create_chatroom);
        mSecurityLevel = view.findViewById(R.id.security_level);
        mSeekProgress = 0;
        mSecurityLevel.setText(String.valueOf(mSeekProgress));

        mData = FirebaseDatabase.getInstance();
        mAuth = FirebaseAuth.getInstance();

        mContext = getActivity();
    }

    private void assignLinks() {
        mCreateChatroom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createNewChatroom();
            }
        });

        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                mSeekProgress = i;
                mSecurityLevel.setText(String.valueOf(mSeekProgress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });


    }

    private void createNewChatroom() {
        String sChatroom = mChatroomName.getText().toString();

        if (!sChatroom.equals("")) {
            Log.d(TAG, "onClick: creating new chat room");


            if (mUserSecurityLevel >= mSeekBar.getProgress()) {

                DatabaseReference reference = mData.getReference();
                //get the new chatroom unique id
                String chatroomId = reference
                        .child(getString(R.string.dbnode_chatrooms))
                        .push().getKey();

                //create the chatroom
                Chatroom chatroom = new Chatroom();
                chatroom.setSecurity_level(String.valueOf(mSeekBar.getProgress()));
                chatroom.setChatroom_name(sChatroom);
                chatroom.setCreator_id(Objects.requireNonNull(mAuth.getCurrentUser()).getUid());
                chatroom.setChatroom_id(chatroomId);


                //insert the new chatroom into the database
                reference
                        .child(getString(R.string.dbnode_chatrooms))
                        .child(chatroomId)
                        .setValue(chatroom);

                //create a unique id for the message
                String messageId = reference
                        .child(getString(R.string.dbnode_chatrooms))
                        .push().getKey();

                //insert the first message into the chatroom
                ChatMessage message = new ChatMessage();

                message.setMessage("Welcome to the new chatroom!");
                message.setTimestamp(getTimestamp());
                reference
                        .child(getString(R.string.dbnode_chatrooms))
                        .child(chatroomId)
                        .child(getString(R.string.field_chatroom_messages))
                        .child(messageId)
                        .setValue(message);
                ((ChatActivity) getActivity()).getChatrooms();
                getDialog().dismiss();
            } else {
                makeToast("Insufficient Security Level");
            }

        }
    }

    private void getUserSecurityLevel() {
        DatabaseReference reference = mData.getReference();

        Query query = reference.child(getString(R.string.dbnode_users))
                .orderByKey()
                //OR could use ->.orderByChild(getString(R.string.field_user_id))
                .equalTo(mAuth.getCurrentUser().getUid());
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                //alternatively could have used:
                //DataSnapshot singleSnapshot = dataSnapshot.getChildren().iterator().next();
                for (DataSnapshot singleSnapshot : dataSnapshot.getChildren()) {
                    Log.d(TAG, "onDataChange: users security level: "
                            + singleSnapshot.getValue(User.class).getSecurity_level());

                    mUserSecurityLevel = Integer.parseInt(String.valueOf(
                            singleSnapshot.getValue(User.class).getSecurity_level()));
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                makeToast("Something Went Wrong");
            }
        });

    }

    private String getTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("Canada/Pacific"));
        return sdf.format(new Date());
    }

    private void makeToast(String input) {
        Toast.makeText(mContext, input, Toast.LENGTH_SHORT).show();
    }

}