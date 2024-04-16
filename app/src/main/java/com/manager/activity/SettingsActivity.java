package com.manager.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;

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
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class SettingsActivity extends AppCompatActivity {

    FirebaseAuth userAuth;
    private static final int REQ_ONE_TAP_LOGIN = 100;
    TextView userName,userMail;
    Button userSign;
    ImageButton user_profile_pic,back;
    Boolean logInStatus;
    String USERID,UserName,UserEmail;
    ProgressDialog sPG;

    SignInClient oneTapClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);

        back = findViewById(R.id.back);
        user_profile_pic = findViewById(R.id.user_profile_pic);
        userName = findViewById(R.id.userName);
        userMail = findViewById(R.id.userMail);
        userSign = findViewById(R.id.userSign);

        oneTapClient = Identity.getSignInClient(this);

        sPG = new ProgressDialog(this);
        sPG.setTitle("Signing You In...");
        sPG.setMessage("Please Wait");

        userAuth = FirebaseAuth.getInstance();
        FirebaseUser user = userAuth.getCurrentUser();
        if (user!= null){
            USERID = user.getUid();
            UserName = user.getDisplayName();
            UserEmail = user.getEmail();
            logInStatus =  true;

            userName.setText(UserName);
            userMail.setText(UserEmail);

            userSign.setText("Sign Out");
            userSign.setOnClickListener(v -> {
                userAuth.signOut();
                finish();
            });


        }else {
            logInStatus = false;

            userSign.setText("Sign In");
            userSign.setOnClickListener(v -> SignIn());
        }

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        back.setOnClickListener(v -> finish());
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
        }

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

        Task<BeginSignInResult> signInTask = oneTapClient.beginSignIn(signInRequest);
        signInTask
                .addOnSuccessListener(new OnSuccessListener<BeginSignInResult>() {
                    @Override
                    public void onSuccess(BeginSignInResult beginSignInResult) {
                        try {
                            sPG.show();
                            startIntentSenderForResult(signInTask.getResult().getPendingIntent().getIntentSender(), REQ_ONE_TAP_LOGIN, null, 0, 0, 0, null);
                        } catch (IntentSender.SendIntentException e) {
                            Toast.makeText(SettingsActivity.this, "failed", Toast.LENGTH_SHORT).show();
                            throw new RuntimeException(e);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(SettingsActivity.this, "Failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_ONE_TAP_LOGIN) {
            try {
                SignInCredential credential = oneTapClient.getSignInCredentialFromIntent(data);
                String idToken = credential.getGoogleIdToken();
                AuthCredential firebaseCredential = GoogleAuthProvider.getCredential(idToken, null);
                userAuth.signInWithCredential(firebaseCredential)
                        .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    Log.d("SignIn Status", "signInWithCredential:success");
                                    FirebaseUser user = userAuth.getCurrentUser();
                                    sPG.dismiss();
                                    Toast.makeText(SettingsActivity.this, "Login Success", Toast.LENGTH_SHORT).show();
//                                    updateUI(user);
                                } else {
                                    sPG.dismiss();
                                    Toast.makeText(SettingsActivity.this, "Login Failed", Toast.LENGTH_SHORT).show();
                                    Log.w("SignIn Status", "signInWithCredential:failure", task.getException());
//                                    updateUI(null);
                                }
                            }
                        });

            } catch (ApiException e) {
                Log.e("SignIn Status", "One Tap sign-in failed", e);
                Toast.makeText(this, "One Tap sign-in failed", Toast.LENGTH_SHORT).show();
            }
        }
    }



}