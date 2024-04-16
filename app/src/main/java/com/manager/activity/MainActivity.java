package com.manager.activity;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;

import com.google.android.gms.auth.api.identity.BeginSignInRequest;
import com.google.android.gms.auth.api.identity.BeginSignInResult;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.auth.api.identity.SignInClient;
import com.google.android.gms.auth.api.identity.SignInCredential;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.View;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.Timestamp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "OneTapLoginActivity";
    private static final int REQ_ONE_TAP_LOGIN = 100;
    SignInClient oneTapClient;
    FirebaseAuth firebaseAuth;
    FirebaseFirestore FSdb;
    CheckBox markTaskDone,modTaskDone;
    TextView taskVIewTitle;
    EditText task_title,taskEditTitle;
    Button taskTime,addExp,modtaskTime,modTask,delTask;
    ImageButton closeView;
    SimpleDateFormat dateFormat;
    FloatingActionButton addTask;
    String USERID;
    Timestamp setTim;
    TableLayout Entries;
    Date dt;ProgressDialog sPG;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();
        firebaseAppCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance());

        sPG = new ProgressDialog(this);
        sPG.setTitle("Signing You In...");
        sPG.setMessage("Please Wait");


        firebaseAuth = FirebaseAuth.getInstance();
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        if (firebaseUser != null) {
            // When user already sign in redirect to profile activity
            USERID = firebaseUser.getUid();
            Handler h = new Handler();
            h.postDelayed(new Runnable() {
                @Override
                public void run() {
                    getTasks();
                }
            },1000);

//            startActivity(new Intent(MainActivity.this, ProfileActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }else {
            ProgressDialog lPG = new ProgressDialog(this);
            lPG.setTitle("Initiating One-Tap");
            lPG.setMessage("Please Wait");
            lPG.show();
            Handler h = new Handler();
            h.postDelayed(new Runnable() {
                @Override
                public void run() {
                    lPG.dismiss();
                    SignIn();
                }
            },1000);
        }

        FSdb = FirebaseFirestore.getInstance();

        Toolbar toolbar = findViewById(R.id.appbar);
        setSupportActionBar(toolbar);

        addTask = findViewById(R.id.addTask);
        Entries = findViewById(R.id.Entries);

        oneTapClient = Identity.getSignInClient(this);
        dateFormat = new SimpleDateFormat("dd-MM-yyyy hh:mm a");


        addTask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addTaskDialog();
            }
        });

    }

    public void SignIn(){

        BeginSignInRequest signInRequest = BeginSignInRequest.builder()
                .setGoogleIdTokenRequestOptions(
                        BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                                .setSupported(true)
                                .setServerClientId(getString(R.string.default_web_client_id))
                                .setFilterByAuthorizedAccounts(true)
                                .build())
                .build();

        Task<com.google.android.gms.auth.api.identity.BeginSignInResult> signInTask = oneTapClient.beginSignIn(signInRequest);
        signInTask
                .addOnSuccessListener(new OnSuccessListener<BeginSignInResult>() {
                    @Override
                    public void onSuccess(BeginSignInResult beginSignInResult) {
                        try {
                            sPG.show();
                            startIntentSenderForResult(signInTask.getResult().getPendingIntent().getIntentSender(), REQ_ONE_TAP_LOGIN, null, 0, 0, 0, null);
                        } catch (IntentSender.SendIntentException e) {
                            Toast.makeText(MainActivity.this, "failed", Toast.LENGTH_SHORT).show();
                            throw new RuntimeException(e);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(MainActivity.this, "Failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public void addTaskDialog(){

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this)
                .setView(R.layout.add_task);
        AlertDialog alertDialog = builder.create();
        alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        alertDialog.show();
        task_title = alertDialog.findViewById(R.id.task_title);
        markTaskDone = alertDialog.findViewById(R.id.markTaskDone);
        addExp = alertDialog.findViewById(R.id.addExp);
        taskTime = alertDialog.findViewById(R.id.taskTime);

        final Timestamp[] setTime = {null};

        taskTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickDateTime(taskTime);
            }
        });

        addExp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int okay = 1;
                String title = task_title.getText().toString();
                String date = taskTime.getText().toString();
                if (title.trim().isEmpty()){
                    task_title.setError("*Required");
                    okay = 0;
                }
                if(date == getString(R.string.setTime)){
                    taskTime.setError("*Required");
                    okay = 0;
                }
                if (okay==1){
                    boolean taskStatus = false;
                    if(markTaskDone.isChecked()){
                        taskStatus = true;
                    }
                    Timestamp crrT = new Timestamp(Calendar.getInstance().getTime());
                    TaskModel task = new TaskModel(title,taskStatus,setTim,crrT);

                    try {
                        assert setTim!= null;
                        FSdb.collection("users").document(USERID).collection("tasks").add(task)
                                .addOnSuccessListener(documentReference -> {
                                    Toast.makeText(MainActivity.this, "Task added with ID: " + documentReference.getId(), Toast.LENGTH_SHORT).show();
                                    Log.d(TAG, "Task added with ID: " + documentReference.getId());
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(MainActivity.this, "Error adding task"+ e, Toast.LENGTH_LONG).show();
                                    Log.w(TAG, "Error adding task", e);
                                });
                    }catch (Exception e){
                        Toast.makeText(MainActivity.this, "Error while adding task: "+ e, Toast.LENGTH_SHORT).show();
                        Log.d(TAG,"Error while adding task: "+ e);
                    }

                    alertDialog.dismiss();
                    refreshTasks();
                }
            }
        });
    }

    public void pickDateTime(Button timeSet) {
        final Calendar c = Calendar.getInstance();
        final Timestamp[] setT = {null};
        DatePickerDialog.OnDateSetListener dateListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                c.set(Calendar.YEAR,year);
                c.set(Calendar.MONTH,month);
                c.set(Calendar.DAY_OF_MONTH,dayOfMonth);

                TimePickerDialog.OnTimeSetListener timeListener = new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        c.set(Calendar.HOUR_OF_DAY,hourOfDay);
                        c.set(Calendar.MINUTE,minute);

//                        SimpleDateFormat date = new SimpleDateFormat("dd-MM-YY hh:mm a");
                        dt = c.getTime();
                        setTim = new Timestamp(dt);
                        timeSet.setText(dateFormat.format(dt));
                    }
                };
                new TimePickerDialog(MainActivity.this,timeListener,c.get(Calendar.HOUR_OF_DAY),c.get(Calendar.MINUTE),false).show();
            }
        };
        new DatePickerDialog(MainActivity.this,dateListener,c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH)).show();
    }

    public void getTasks(){
//        Toast.makeText(this, USERID, Toast.LENGTH_LONG).show();
        ProgressDialog dg = new ProgressDialog(this);
        dg.setTitle("Loading Tasks");
        dg.setMessage("Please Wait");
        dg.show();
        FSdb.collection("users").document(USERID).collection("tasks").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots){

                        Boolean done = doc.getBoolean("done");
                        if (done == false){
                            String taskId = doc.getId();
                            String title = doc.getString("title");
                            Timestamp createdAt = doc.getTimestamp("createdAt");
                            Timestamp dueTime = doc.getTimestamp("dueTime");
                            assert createdAt != null;
                            String cr = dateFormat.format(createdAt.toDate());
                            assert dueTime != null;
                            String due = dateFormat.format(dueTime.toDate());
//                            String due = dateFormat.format(Calendar.getInstance().getTime());
                            rowGenerator(title,due,createdAt,taskId);
                        }
                    }
                    dg.dismiss();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Some Error", Toast.LENGTH_SHORT).show();
                    dg.dismiss();
                });

    }

    public void rowGenerator(String title,String dueAt, Timestamp createdAt,String taskID){
        TableRow tr = new TableRow(this);

        TextView Title = new TextView(this);
        Title.setWidth((int) getResources().getDimension(R.dimen.TITLE_WIDTH));
        Title.setGravity(Gravity.CENTER);
        Title.setPadding(10,10,10,10);
        Title.setTextSize(20);

        TextView DueAt = new TextView(this);
        DueAt.setWidth((int) getResources().getDimension(R.dimen.DUE_DATE_WIDTH));
        DueAt.setGravity(Gravity.CENTER);
        DueAt.setPadding(10,10,10,10);
        DueAt.setTextSize(20);

        Title.setText(title);
        DueAt.setText(dueAt);

        tr.addView(Title);tr.addView(DueAt);
//        tr.addView(CreatedAt);
        tr.setContentDescription(taskID);
        tr.setBackground(getDrawable(R.drawable.round_bg));
        tr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                taskView(taskID,title,dueAt,createdAt);
//                Toast.makeText(MainActivity.this, taskID, Toast.LENGTH_SHORT).show();
            }
        });

        View space = new View(this);
        space.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, 5)); // 5sp space

        Entries.addView(tr);
        Entries.addView(space);
    }

    public void taskView(String taskID,String title,String dueAt,Timestamp createdAt){
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this)
                .setView(R.layout.task_viewer);
        AlertDialog alertDialog = builder.create();
        alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        alertDialog.show();
        taskVIewTitle = alertDialog.findViewById(R.id.taskVIewTitle);
        taskEditTitle = alertDialog.findViewById(R.id.taskEditTitle);
        modTaskDone = alertDialog.findViewById(R.id.modTaskDone);
        modtaskTime = alertDialog.findViewById(R.id.modtaskTime);
        modTask = alertDialog.findViewById(R.id.modTask);
        closeView = alertDialog.findViewById(R.id.closeView);
        delTask = alertDialog.findViewById(R.id.delTask);

        taskVIewTitle.setText(title);
        modtaskTime.setText(dueAt);

        closeView.setOnClickListener(v -> alertDialog.dismiss());
        modtaskTime.setOnClickListener(v -> pickDateTime(modtaskTime));

        modTask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String Tasktitle = "";
                String modtitle = taskEditTitle.getText().toString();
                if (modtitle.isEmpty()){
                    Tasktitle = taskVIewTitle.getText().toString();
                }else {
                    Tasktitle = modtitle;
                }

                Date modDate = null;
                try {
                    modDate = dateFormat.parse(modtaskTime.getText().toString());
                } catch (ParseException e) {
                    Toast.makeText(MainActivity.this, "Some Error", Toast.LENGTH_SHORT).show();
                    throw new RuntimeException(e);
                }
                setTim = new Timestamp(modDate);

                boolean modStatus = false;
                if (modTaskDone.isChecked()){
                    modStatus = true;
                }

//                TaskModel modTask = new TaskModel(Tasktitle,modStatus,setTim,createdAt);
                Map<String,Object> modTask = new HashMap<>();
                modTask.put("title",Tasktitle);
                modTask.put("done",modStatus);
                modTask.put("dueTime",setTim);
                modTask.put("createdAt",createdAt);


                FSdb.collection("users").document(USERID).collection("tasks").document(taskID).update(modTask)
                        .addOnSuccessListener(documentReference -> {
                            Toast.makeText(MainActivity.this, "Task Updated ", Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "Task Updated: ");
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(MainActivity.this, "Error updating task"+ e, Toast.LENGTH_LONG).show();
                            Log.w(TAG, "Error updating task", e);
                        });

                refreshTasks();
                alertDialog.dismiss();

//                Toast.makeText(MainActivity.this, modDate.toString(), Toast.LENGTH_SHORT).show();
            }
        });

        delTask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FSdb.collection("users").document(USERID).collection("tasks").document(taskID).delete()
                        .addOnSuccessListener(documentReference -> {
                            Toast.makeText(MainActivity.this, "Task Deleted", Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "Task Deleted: ");
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(MainActivity.this, "Error deleting task"+ e, Toast.LENGTH_LONG).show();
                            Log.w(TAG, "Error deleting task", e);
                        });
                refreshTasks();
                alertDialog.dismiss();
            }
        });
    }


    public void refreshTasks(){
        Entries.removeAllViews();
        getTasks();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_ONE_TAP_LOGIN) {
            try {
                SignInCredential credential = oneTapClient.getSignInCredentialFromIntent(data);
                String idToken = credential.getGoogleIdToken();
                AuthCredential firebaseCredential = GoogleAuthProvider.getCredential(idToken, null);
                firebaseAuth.signInWithCredential(firebaseCredential)
                        .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    Log.d(TAG, "signInWithCredential:success");
                                    FirebaseUser user = firebaseAuth.getCurrentUser();
                                    sPG.dismiss();
                                    Toast.makeText(MainActivity.this, "Login Success", Toast.LENGTH_SHORT).show();
//                                    updateUI(user);
                                    USERID = user.getUid();
                                    getTasks();
                                } else {
                                    sPG.dismiss();
                                    Toast.makeText(MainActivity.this, "Login Failed", Toast.LENGTH_SHORT).show();
                                    Log.w(TAG, "signInWithCredential:failure", task.getException());
//                                    updateUI(null);
                                }
                            }
                        });

            } catch (ApiException e) {
                Log.e(TAG, "One Tap sign-in failed", e);
                Toast.makeText(this, "One Tap sign-in failed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public class TaskModel {
        private String title;
        private boolean isDone;
        private Timestamp dueTime;
        private Timestamp createdAt;
        public String getTitle() {
            return title;
        }
        public boolean isDone() {
            return isDone;
        }
        public Timestamp getDueTime() {
            return dueTime;
        }
        public Timestamp getCreatedAt() {
            return createdAt;
        }

        public TaskModel(String title, boolean isDone, Timestamp dueTime, Timestamp createdAt) {
            this.title = title;
            this.isDone = isDone;
            this.dueTime = dueTime;
            this.createdAt = createdAt;
        }

        public void setTitle(String title) {
            this.title = title;
        }
        public void setDone(boolean done) {
            isDone = done;
        }
        public void setDueTime(Timestamp dueTime) {
            this.dueTime = dueTime;
        }
        public void setCreatedAt(Timestamp createdAt) {
            this.createdAt = createdAt;
        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent launchSetting = new Intent(this,SettingsActivity.class);
            startActivity(launchSetting);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}