package com.ezerka.pingo.app;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.ezerka.pingo.R;
import com.ezerka.pingo.model.User;
import com.ezerka.pingo.model.fcm.Data;
import com.ezerka.pingo.model.fcm.FirebaseCloudMessage;
import com.ezerka.pingo.utility.EmployeesAdapter;
import com.ezerka.pingo.utility.FCM;
import com.ezerka.pingo.utility.VerticalSpacingDecorator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static com.ezerka.pingo.R.id.recyclerView;

/**
 * Created by User on 10/25/2017.
 */

public class AdminActivity extends AppCompatActivity {

    private static final String TAG = "AdminActivity";
    private static final String BASE_URL = "https://fcm.googleapis.com/fcm/";
    public static boolean isActivityRunning;
    //widgets
    private TextView mDepartments;
    private Button mAddDepartment, mSendMessage;
    private RecyclerView mRecyclerView;
    private EditText mMessage, mTitle;
    //vars
    private ArrayList<String> mDepartmentsList;
    private Set<String> mSelectedDepartments;
    private EmployeesAdapter mEmployeeAdapter;
    private ArrayList<User> mUsers;
    private Set<String> mTokens;
    private String mServerKey;

    private FirebaseDatabase mData;
    private FirebaseAuth mAuth;

    private Context mContext;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        assignViews();
        assignLinks();

        assignMethods();

        setupEmployeeList();
        hideSoftKeyboard();

    }

    private void assignViews() {
        mDepartments = findViewById(R.id.broadcast_departments);
        mAddDepartment = findViewById(R.id.add_department);
        mSendMessage = findViewById(R.id.btn_send_message);
        mRecyclerView = findViewById(recyclerView);
        mMessage = findViewById(R.id.input_message);
        mTitle = findViewById(R.id.input_title);

        mSelectedDepartments = new HashSet<>();
        mTokens = new HashSet<>();


        mAuth = FirebaseAuth.getInstance();
        mData = FirebaseDatabase.getInstance();

        mContext = getApplicationContext();

        mUsers = new ArrayList<>();
        mEmployeeAdapter = new EmployeesAdapter(mContext, mUsers);
    }

    private void assignLinks() {

        mDepartments.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: opening departments selector dialog.");

                AlertDialog.Builder builder = new AlertDialog.Builder(AdminActivity.this);
                builder.setIcon(R.drawable.ic_departments);
                builder.setTitle("Select Departments:");

                //create an array of the departments
                String[] departments = new String[mDepartmentsList.size()];
                for (int i = 0; i < mDepartmentsList.size(); i++) {
                    departments[i] = mDepartmentsList.get(i);
                }

                //get the departments that are already added to the list
                boolean[] checked = new boolean[mDepartmentsList.size()];
                for (int i = 0; i < mDepartmentsList.size(); i++) {
                    if (mSelectedDepartments.contains(mDepartmentsList.get(i))) {
                        checked[i] = true;
                    }
                }

                builder.setPositiveButton("done", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

                builder.setMultiChoiceItems(departments, checked, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        if (isChecked) {
                            Log.d(TAG, "onClick: adding " + mDepartmentsList.get(which) + " to the list.");
                            mSelectedDepartments.add(mDepartmentsList.get(which));
                        } else {
                            Log.d(TAG, "onClick: removing " + mDepartmentsList.get(which) + " from the list.");
                            mSelectedDepartments.remove(mDepartmentsList.get(which));

                        }
                    }
                });

                AlertDialog dialog = builder.create();
                dialog.show();

                dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        Log.d(TAG, "onDismiss: dismissing dialog and refreshing token list.");
                        getDepartmentTokens();
                    }
                });
            }
        });

        mAddDepartment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: opening dialog to add new department");
                NewDepartmentDialog dialog = new NewDepartmentDialog();
                dialog.show(getSupportFragmentManager(), getString(R.string.dialog_add_department));
            }
        });

        mSendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: attempting to send the message.");
                String message = mMessage.getText().toString();
                String title = mTitle.getText().toString();

                if (isEmpty(message) && isEmpty(title)) {

                    //send message
                    sendMessageToDepartment(title, message);

                    mMessage.setText("");
                    mTitle.setText("");
                } else {
                    makeToast("Fill out the title and message details");
                }
            }
        });

    }

    private void assignMethods() {
        getDepartments();
        getEmployeeList();
        getServerKey();
    }



    private void sendMessageToDepartment(String title, String message) {
        Log.d(TAG, "sendMessageToDepartment: sending message to selected departments.");

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        //create the interface
        FCM fcmAPI = retrofit.create(FCM.class);

        //attach the headers
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Authorization", "key=" + mServerKey);

        //send the message to all the tokens
        for (String token : mTokens) {

            Log.d(TAG, "sendMessageToDepartment: sending to token: " + token);
            Data data = new Data();
            data.setMessage(message);
            data.setTitle(title);
            data.setData_type(getString(R.string.data_type_admin_broadcast));
            FirebaseCloudMessage firebaseCloudMessage = new FirebaseCloudMessage();
            firebaseCloudMessage.setData(data);
            firebaseCloudMessage.setTo(token);

            Call<ResponseBody> call = fcmAPI.send(headers, firebaseCloudMessage);

            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                    Log.d(TAG, "onResponse: Server Response: " + response.toString());

                }

                @Override
                public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                    Log.e(TAG, "onFailure: Unable to send the message." + t.getMessage());
                    makeToast("Unable to send the message");
                }
            });
        }

    }

    /**
     * Retrieves the server key for the Firebase server.
     * This is required to send FCM messages.
     */
    private void getServerKey() {
        Log.d(TAG, "getServerKey: retrieving server key.");

        DatabaseReference reference = mData.getReference();

        Query query = reference.child(getString(R.string.dbnode_server))
                .orderByValue();
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.d(TAG, "onDataChange: got the server key.");
                DataSnapshot singleSnapshot = dataSnapshot.getChildren().iterator().next();
                mServerKey = Objects.requireNonNull(singleSnapshot.getValue()).toString();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.d(TAG, "getServerKey: Failed: " + databaseError.getDetails());
            }
        });
    }


    /**
     * Get all the tokens of the users who are in the selected departments
     */
    private void getDepartmentTokens() {
        Log.d(TAG, "getDepartmentTokens: searching for tokens.");
        mTokens.clear(); //clear current token list in case admin has change departments
        DatabaseReference reference = mData.getReference();

        for (String department : mSelectedDepartments) {
            Log.d(TAG, "getDepartmentTokens: department: " + department);

            Query query = reference.child(getString(R.string.dbnode_users))
                    .orderByChild(getString(R.string.field_department))
                    .equalTo(department);

            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        String token = snapshot.getValue(User.class).getMessaging_token();
                        Log.d(TAG, "onDataChange: got a token for user named: "
                                + snapshot.getValue(User.class).getName());
                        mTokens.add(token);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.d(TAG, "getDepartmentTokens: Failed: " + databaseError.getDetails());
                }
            });
        }
    }

    public void setDepartmentDialog(final User user) {
        Log.d(TAG, "setDepartmentDialog: setting the department of: " + user.getName());

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);

        builder.setIcon(R.drawable.ic_departments);
        builder.setTitle("Set a Department for " + user.getName() + ":");

        builder.setPositiveButton("done", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        //get the index of the department (if the user has a department assigned)
        int index = -1;
        for (int i = 0; i < mDepartmentsList.size(); i++) {
            if (mDepartmentsList.contains(user.getDepartment())) {
                index = i;
            }
        }

        final ListAdapter adapter = new ArrayAdapter<String>(mContext, android.R.layout.simple_list_item_1, mDepartmentsList);
        builder.setSingleChoiceItems(adapter, index, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                makeToast("Department Saved");
                DatabaseReference reference = mData.getReference();
                reference.child(getString(R.string.dbnode_users))
                        .child(user.getUser_id())
                        .child(getString(R.string.field_department))
                        .setValue(mDepartmentsList.get(which));
                dialog.dismiss();
                //refresh the list with the new information
                mUsers.clear();
                getEmployeeList();
            }
        });
        builder.show();
    }

    private void getEmployeeList() throws NullPointerException {
        Log.d(TAG, "getEmployeeList: getting a list of all employees");
        DatabaseReference reference = mData.getReference();

        Query query = reference.child(getString(R.string.dbnode_users));

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    User user = snapshot.getValue(User.class);
                    Log.d(TAG, "onDataChange: found a user: " + user.getName());
                    mUsers.add(user);
                }
                mEmployeeAdapter.notifyDataSetChanged();
                getDepartmentTokens();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.d(TAG, "getEmployeeList: Failed: " + databaseError.getDetails());
            }
        });
    }

    /**
     * Setup the list of employees
     */
    private void setupEmployeeList() {

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(mContext);
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.addItemDecoration(new VerticalSpacingDecorator(15));
        mRecyclerView.setAdapter(mEmployeeAdapter);
    }

    /**
     * Retrieve a list of departments that have been added to the database.
     */
    public void getDepartments() {
        mDepartmentsList = new ArrayList<>();
        DatabaseReference reference = mData.getReference();
        Query query = reference.child(getString(R.string.dbnode_departments));

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String department = snapshot.getValue().toString();
                    Log.d(TAG, "onDataChange: found a department: " + department);
                    mDepartmentsList.add(department);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d(TAG, "getDepartments: Failed: " + databaseError.getDetails());
            }
        });
    }

    private void makeToast(String input) {
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

    private void hideSoftKeyboard() {
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    private boolean isEmpty(String string) {
        return !string.equals("");
    }
}
