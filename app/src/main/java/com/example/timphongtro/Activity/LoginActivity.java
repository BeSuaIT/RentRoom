package com.example.timphongtro.Activity;

import static android.widget.Toast.LENGTH_SHORT;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.timphongtro.R;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
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
    private GoogleSignInClient googleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        editTextEmail = findViewById(R.id.txtemail);
        editTextPassword = findViewById(R.id.txtpassword);
        TextView textViewRegister = findViewById(R.id.textviewDangky);
        btnSignIn = findViewById(R.id.btnDangnhap);
        TextView txtViewForgotPassword = findViewById(R.id.txtViewForgotPassword);
        ImageView imgGoogleSignIn = findViewById(R.id.imgGoogleSignin);
        ImageView imgGuest = findViewById(R.id.imgGuest);
        ImageView imgFacebookSignIn = findViewById(R.id.imgFacebookSignin);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account != null) {
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }

        ActivityResultLauncher<Intent> googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    try {
                        GoogleSignInAccount account1 = task.getResult(ApiException.class);
                        if (account1 != null) {
                            String idToken = account1.getIdToken();
                            if (idToken != null) {
                                firebaseAuthWithGoogle(idToken);
                            } else {
                                Toast.makeText(LoginActivity.this, "Không lấy được ID token", LENGTH_SHORT).show();
                            }
                        }
                    } catch (ApiException e) {
                        Toast.makeText(LoginActivity.this, "Lỗi đăng nhập Google: " + e.getMessage(), LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                });

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

        imgGoogleSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent signInIntent = googleSignInClient.getSignInIntent();
                googleSignInLauncher.launch(signInIntent);
            }
        });

        imgFacebookSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "In Developing", LENGTH_SHORT).show();
            }
        });

        imgGuest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent main = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(main);
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