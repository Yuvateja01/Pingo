package com.ezerka.pingo.app;


import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ezerka.pingo.R;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;


public class DeleteChatroomDialog extends DialogFragment {

    private static final String TAG = "DeleteChatroomDialog";
    private String mChatroomId;
    private TextView mDelete;
    private TextView mCancel;

    private FirebaseDatabase mData;

    public DeleteChatroomDialog() {
        super();
        setArguments(new Bundle());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_delete_chatroom, container, false);

        assignView(view);
        assignLink();

        return view;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: started");

        mChatroomId = getArguments().getString(getString(R.string.field_chatroom_id));
        if (mChatroomId != null) {
            Log.d(TAG, "onCreate: got the chatroom id: " + mChatroomId);
        }
    }

    private void assignView(View view) {
        mDelete = view.findViewById(R.id.confirm_delete);
        mCancel = view.findViewById(R.id.cancel);
    }

    private void assignLink() {
        mDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteTheChatroom();

            }
        });

        mCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: canceling deletion of chatroom");
                getDialog().dismiss();
            }
        });


    }

    private void deleteTheChatroom() {
        if (mChatroomId != null) {
            Log.d(TAG, "onClick: deleting chatroom: " + mChatroomId);

            DatabaseReference reference = mData.getReference();
            reference.child(getString(R.string.dbnode_chatrooms))
                    .child(mChatroomId)
                    .removeValue();
            ((ChatActivity) getActivity()).getChatrooms();
            getDialog().dismiss();
        }
    }


}

















