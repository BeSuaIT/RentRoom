package com.example.timphongtro.Activity;

import static android.widget.Toast.LENGTH_SHORT;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.timphongtro.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class LoginActivity extends AppCompatActivity {
    private EditText editTextEmail;
    private EditText editTextPassword;
    private Button btnSignIn;
    private FirebaseAuth mAuth;
    private FirebaseUser userData;
    private DatabaseReference userRef;
    private HashMap<String, Object> userMap;
    private ArrayList<String> emails;
    private CredentialManager credentialManager;
    private GetGoogleIdOption googleIdOption;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        credentialManager = CredentialManager.create(this);

        googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(true)
                .setServerClientId(getString(R.string.default_web_client_id))
                .build();

        editTextEmail = findViewById(R.id.txtemail);
        editTextPassword = findViewById(R.id.txtpassword);
        TextView textViewRegister = findViewById(R.id.textviewDangky);
        btnSignIn = findViewById(R.id.btnDangnhap);
        TextView txtViewForgotPassword = findViewById(R.id.txtViewForgotPassword);
        ImageView imgGoogleSignIn = findViewById(R.id.imgGoogleSignin);
        ImageView imgFacebookSignIn = findViewById(R.id.imgFacebookSignin);

        btnSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = editTextEmail.getText().toString();
                String password = editTextPassword.getText().toString();
                if (TextUtils.isEmpty(email)) {
                    Toast.makeText(getApplicationContext(), "Vui lòng nhập Email.", LENGTH_SHORT).show();
                    return;
                }
                if (TextUtils.isEmpty(password)) {
                    Toast.makeText(getApplicationContext(), "Vui lòng nhập Mật khẩu.", LENGTH_SHORT).show();
                    return;
                }
                mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(getApplicationContext(), "Đăng nhập thành công", LENGTH_SHORT).show();
                            Intent i = new Intent(LoginActivity.this, MainActivity.class);
                            startActivity(i);
                            btnSignIn.setEnabled(false);
                        } else {
                            Toast.makeText(getApplicationContext(), "Đăng nhập không thành công", LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });

        imgGoogleSignIn.setOnClickListener(v -> signInWithCredentialManager());

        imgFacebookSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "In Developing", LENGTH_SHORT).show();
            }
        });

        textViewRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(i);
                finish();
            }
        });

        txtViewForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
                startActivity(i);
                finish();
            }
        });
    }

    private void signInWithCredentialManager() {
        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            credentialManager.getCredentialAsync(
                    this,
                    request,
                    null,
                    getMainExecutor(),
                    new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                        @Override
                        public void onResult(GetCredentialResponse result) {
                            Credential credential = result.getCredential();

                            if (credential instanceof CustomCredential) {
                                if (GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(
                                        credential.getType())) {
                                    GoogleIdTokenCredential googleIdTokenCredential =
                                            GoogleIdTokenCredential.createFrom(
                                                    ((CustomCredential) credential).getData());
                                    String idToken = googleIdTokenCredential.getIdToken();
                                    firebaseAuthWithGoogle(idToken);
                                }
                            }
                        }

                        @Override
                        public void onError(@NonNull GetCredentialException e) {
                            Toast.makeText(LoginActivity.this,
                                    "Sign in failed: " + e.getMessage(),
                                    LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential authCredential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(authCredential).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                userData = mAuth.getCurrentUser();
                if (userData != null) {
                    userMap = new HashMap<>();
                    userMap.put("uid", userData.getUid());
                    userMap.put("email", userData.getEmail());
                    userMap.put("name", userData.getDisplayName());

                    userRef = FirebaseDatabase.getInstance().getReference("users");
                    emails = new ArrayList<>();
                    userRef.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                for (DataSnapshot user : snapshot.getChildren()) {
                                    String email = user.child("email").getValue(String.class);
                                    if (email != null) {
                                        emails.add(email);
                                    }
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                        }
                    });

                    userRef.child(Objects.requireNonNull(userData.getUid())).updateChildren(userMap)
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(LoginActivity.this, "Đăng nhập thành công", LENGTH_SHORT).show();
                                Intent i = new Intent(LoginActivity.this, MainActivity.class);
                                startActivity(i);
                                finish();
                            });
                }
            } else {
                Toast.makeText(LoginActivity.this, "Đăng nhập thất bại", LENGTH_SHORT).show();
            }
        });
    }
}