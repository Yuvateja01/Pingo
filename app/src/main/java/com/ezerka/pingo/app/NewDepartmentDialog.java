package com.ezerka.pingo.app;

import android.content.Context;
import android.os.Bundle;
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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;


public class NewDepartmentDialog extends DialogFragment {

    private static final String TAG = "NewDepartmentDialog";

    //widgets
    private EditText mNewDepartment;
    private TextView confirmDialog;

    private Context mContext;

    private FirebaseDatabase mData;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_add_department, container, false);
        assignViews(view);
        assignLinks();

        return view;
    }

    private void assignViews(View view) {
        mNewDepartment = view.findViewById(R.id.input_new_department);
        confirmDialog = view.findViewById(R.id.dialogConfirm);

        mContext = getActivity();

        mData = FirebaseDatabase.getInstance();

    }

    private void assignLinks() {
        confirmDialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createNewDepartment();
            }
        });
    }

    private void createNewDepartment() {
        String sDept = mNewDepartment.getText().toString();

        if (isEmpty(sDept)) {
            Log.d(TAG, "onClick: adding new department to the list.");
            DatabaseReference reference = mData.getReference();
            reference
                    .child(getString(R.string.dbnode_departments))
                    .child(mNewDepartment.getText().toString())
                    .setValue(mNewDepartment.getText().toString());

            getDialog().dismiss();

            ((AdminActivity) getActivity()).getDepartments();
        }
    }

    /**
     * Return true if the @param is null
     *
     * @param string
     * @return
     */
    private boolean isEmpty(String string) {
        return !string.equals("");
    }

    private void makeToast(String input) {
        Toast.makeText(mContext, input, Toast.LENGTH_SHORT).show();
    }
}

















