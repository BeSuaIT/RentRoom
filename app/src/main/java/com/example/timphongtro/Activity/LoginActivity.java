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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.timphongtro.R;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;

public class LoginActivity extends AppCompatActivity {
    private EditText emailEditText, passwordEditText;
    private TextView registerTextView, forgotPasswordTextView;
    private ImageView googleSignInImageView, facebookSignInImageView;
    private Button loginButton;
    private FirebaseAuth firebaseAuth;
    private FirebaseUser firebaseUser;
    private DatabaseReference databaseReference;
    private HashMap<String, Object> userMap;
    private ArrayList<String> emails;
    private CredentialManager credentialManager;
    private GetGoogleIdOption getGoogleIdOption;
    private CallbackManager callbackManager;
    private LoginManager loginManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance();

        // Initialize Firebase Database with explicit URL
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        databaseReference = database.getReference("Users");

        credentialManager = CredentialManager.create(this);

        // Initialize Facebook callback manager
        callbackManager = CallbackManager.Factory.create();
        loginManager = LoginManager.getInstance();

        // Check if user is already logged in
        getGoogleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(true)
                .setServerClientId(getString(R.string.default_web_client_id))
                .build();

        // Check if user is already logged in
        if (firebaseAuth.getCurrentUser() != null) {
            Intent i = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(i);
            finish();
        }

        // Initialize UI elements
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        registerTextView = findViewById(R.id.registerTextView);
        loginButton = findViewById(R.id.loginButton);
        forgotPasswordTextView = findViewById(R.id.forgotPasswordText);
        googleSignInImageView = findViewById(R.id.googleSignInImageView);
        facebookSignInImageView = findViewById(R.id.facebookSignInImageView);

        loginButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString();
            String password = passwordEditText.getText().toString();

            // Validate input
            if (TextUtils.isEmpty(email)) {
                emailEditText.setError("Vui lòng nhập Email");
                emailEditText.requestFocus();
                return;
            }

            // Validate email format
            if (!isValidEmail(email)) {
                emailEditText.setError("Email không hợp lệ");
                emailEditText.requestFocus();
                return;
            }

            // Validate password
            if (TextUtils.isEmpty(password)) {
                passwordEditText.setError("Vui lòng nhập Mật khẩu");
                passwordEditText.requestFocus();
                return;
            }

            // Validate password length
            if (password.length() < 6) {
                passwordEditText.setError("Mật khẩu phải có ít nhất 6 ký tự");
                passwordEditText.requestFocus();
                return;
            }
            firebaseAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    FirebaseUser user = firebaseAuth.getCurrentUser();
                    if (user != null && user.isEmailVerified()) {
                        Toast.makeText(getApplicationContext(), "Đăng nhập thành công", LENGTH_SHORT).show();
                        Intent i = new Intent(LoginActivity.this, MainActivity.class);
                        startActivity(i);
                        loginButton.setEnabled(false);
                    } else {
                        Toast.makeText(getApplicationContext(), "Email chưa được xác minh. Vui lòng kiểm tra email của bạn.", LENGTH_SHORT).show();
                        firebaseAuth.signOut();
                    }
                } else {
                    String errorMessage = "Đăng nhập không thành công";
                    Exception error = task.getException();
                    if (error != null) {
                        if (error instanceof FirebaseAuthInvalidCredentialsException) {
                            errorMessage = "Email hoặc mật khẩu không chính xác";
                        }
                    }
                    Toast.makeText(getApplicationContext(), errorMessage, LENGTH_SHORT).show();
                }
            });
        });

        googleSignInImageView.setOnClickListener(v -> signInWithCredentialManager());

        facebookSignInImageView.setOnClickListener(v -> signInWithFacebook());

        registerTextView.setOnClickListener(v -> {
            Intent i = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(i);
        });

        forgotPasswordTextView.setOnClickListener(v -> {
            Intent i = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
            startActivity(i);
        });
    }

    private boolean isValidEmail(String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private void signInWithCredentialManager() {
        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(getGoogleIdOption)
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
                                                    credential.getData());
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
        firebaseAuth.signInWithCredential(authCredential).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                firebaseUser = firebaseAuth.getCurrentUser();
                if (firebaseUser != null) {
                    userMap = new HashMap<>();
                    userMap.put("uid", firebaseUser.getUid());
                    userMap.put("email", firebaseUser.getEmail());
                    userMap.put("name", firebaseUser.getDisplayName());
                    userMap.put("phone", "");
                    userMap.put("permission", "user");
                    userMap.put("createdAt", System.currentTimeMillis());

                    emails = new ArrayList<>();
                    databaseReference.addValueEventListener(new ValueEventListener() {
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

                    databaseReference.child(Objects.requireNonNull(firebaseUser.getUid())).updateChildren(userMap)
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

    private void signInWithFacebook() {
        loginManager.logInWithReadPermissions(this, Arrays.asList("email", "public_profile"));
        loginManager.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {


            @Override
            public void onSuccess(LoginResult loginResult) {
                handleFacebookAccessToken(loginResult.getAccessToken());
            }

            @Override
            public void onCancel() {
                Toast.makeText(LoginActivity.this,
                        "Đăng nhập Facebook đã bị hủy",
                        LENGTH_SHORT).show();
            }

            @Override
            public void onError(@NonNull FacebookException e) {
                Toast.makeText(LoginActivity.this,
                        "Đăng nhập Facebook thất bại: " + e.getMessage(),
                        LENGTH_SHORT).show();
            }
        });
    }

    private void handleFacebookAccessToken(AccessToken token) {
        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        firebaseUser = firebaseAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            // Create user data map
                            userMap = new HashMap<>();
                            userMap.put("uid", firebaseUser.getUid());
                            userMap.put("email", firebaseUser.getEmail());
                            userMap.put("name", firebaseUser.getDisplayName());
                            userMap.put("phone", "");
                            userMap.put("permission", "user");
                            userMap.put("createdAt", System.currentTimeMillis());

                            // Get database reference
                            DatabaseReference userRef = FirebaseDatabase.getInstance()
                                    .getReference()
                                    .child("users")
                                    .child(firebaseUser.getUid());

                            // Set value instead of update to ensure all data is written
                            userRef.setValue(userMap)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(LoginActivity.this,
                                                "Đăng nhập thành công",
                                                LENGTH_SHORT).show();
                                        Intent i = new Intent(LoginActivity.this, MainActivity.class);
                                        startActivity(i);
                                        finish();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(LoginActivity.this,
                                                "Lỗi lưu dữ liệu: " + e.getMessage(),
                                                LENGTH_SHORT).show();
                                        firebaseAuth.signOut();
                                    });
                        }
                    } else {
                        Toast.makeText(LoginActivity.this,
                                "Đăng nhập thất bại: " +
                                        (task.getException() != null ? task.getException().getMessage() : "Unknown error"),
                                LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }
}