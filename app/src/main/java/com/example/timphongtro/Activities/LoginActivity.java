package com.example.timphongtro.Activities;

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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Arrays;
import java.util.HashMap;

public class LoginActivity extends AppCompatActivity {
    private EditText emailEditText, passwordEditText;
    private TextView registerTextView, forgotPasswordTextView;
    private ImageView googleSignInImageView, facebookSignInImageView;
    private Button loginButton;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference;
    private CredentialManager credentialManager;
    private GetGoogleIdOption getGoogleIdOption;
    private CallbackManager callbackManager;
    private LoginManager loginManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initFirebase();
        initViews();
        checkCurrentUser();
        setupListeners();
    }

    private void initFirebase() {
        firebaseAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("Users");
        credentialManager = CredentialManager.create(this);
        callbackManager = CallbackManager.Factory.create();
        loginManager = LoginManager.getInstance();

        getGoogleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(true)
                .setServerClientId(getString(R.string.default_web_client_id))
                .build();
    }

    private void initViews() {
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        registerTextView = findViewById(R.id.registerTextView);
        loginButton = findViewById(R.id.loginButton);
        forgotPasswordTextView = findViewById(R.id.forgotPasswordText);
        googleSignInImageView = findViewById(R.id.googleSignInImageView);
        facebookSignInImageView = findViewById(R.id.facebookSignInImageView);
    }

    private void checkCurrentUser() {
        if (firebaseAuth.getCurrentUser() != null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    private void setupListeners() {
        loginButton.setOnClickListener(v -> handleEmailPasswordSignIn());
        googleSignInImageView.setOnClickListener(v -> signInWithCredentialManager());
        facebookSignInImageView.setOnClickListener(v -> signInWithFacebook());
        registerTextView.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
        forgotPasswordTextView.setOnClickListener(v -> startActivity(new Intent(this, ForgotPasswordActivity.class)));
    }

    private void handleEmailPasswordSignIn() {
        String email = emailEditText.getText().toString();
        String password = passwordEditText.getText().toString();

        if (!validateInput(email, password)) return;

        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().getUser() != null) {
                        if (task.getResult().getUser().isEmailVerified()) {
                            showToast("Đăng nhập thành công");
                            startActivity(new Intent(this, MainActivity.class));
                            finish();
                        } else {
                            showToast("Email chưa được xác minh");
                            firebaseAuth.signOut();
                        }
                    } else {
                        showToast(task.getException() instanceof FirebaseAuthInvalidCredentialsException ?
                                "Email hoặc mật khẩu không chính xác" : "Đăng nhập không thành công");
                    }
                });
    }

    private boolean validateInput(String email, String password) {
        if (TextUtils.isEmpty(email)) {
            emailEditText.setError("Vui lòng nhập Email");
            emailEditText.requestFocus();
            return false;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Email không hợp lệ");
            emailEditText.requestFocus();
            return false;
        }
        if (TextUtils.isEmpty(password) || password.length() < 6) {
            passwordEditText.setError("Mật khẩu phải có ít nhất 6 ký tự");
            passwordEditText.requestFocus();
            return false;
        }
        return true;
    }

    private void saveUserToDatabase(FirebaseUser user) {
        HashMap<String, Object> userMap = new HashMap<>();
        userMap.put("uid", user.getUid());
        userMap.put("email", user.getEmail());
        userMap.put("name", user.getDisplayName());
        userMap.put("phone", "");
        userMap.put("permission", "user");
        userMap.put("createdAt", System.currentTimeMillis());

        databaseReference.child(user.getUid()).setValue(userMap)
                .addOnSuccessListener(unused -> {
                    showToast("Đăng nhập thành công");
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    showToast("Lỗi lưu dữ liệu: " + e.getMessage());
                    firebaseAuth.signOut();
                });
    }

    private void showToast(String message) {
        Toast.makeText(this, message, LENGTH_SHORT).show();
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
                            if (credential instanceof CustomCredential &&
                                    GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(credential.getType())) {
                                GoogleIdTokenCredential googleCredential = GoogleIdTokenCredential.createFrom(credential.getData());
                                firebaseAuthWithGoogle(googleCredential.getIdToken());
                            }
                        }

                        @Override
                        public void onError(@NonNull GetCredentialException e) {
                            showToast("Đăng nhập thất bại: " + e.getMessage());
                        }
                    });
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful() && task.getResult().getUser() != null) {
                        saveUserToDatabase(task.getResult().getUser());
                    } else {
                        showToast("Đăng nhập thất bại");
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
                showToast("Đăng nhập Facebook đã bị hủy");
            }

            @Override
            public void onError(@NonNull FacebookException e) {
                showToast("Đăng nhập Facebook thất bại: " + e.getMessage());
            }
        });
    }

    private void handleFacebookAccessToken(AccessToken token) {
        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful() && task.getResult().getUser() != null) {
                        saveUserToDatabase(task.getResult().getUser());
                    } else {
                        String errorMessage = task.getException() != null ?
                                task.getException().getMessage() :
                                "Lỗi không xác định";
                        showToast("Đăng nhập thất bại: " + errorMessage);
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }
}