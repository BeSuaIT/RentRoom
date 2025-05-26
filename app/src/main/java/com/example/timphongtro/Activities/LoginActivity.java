package com.example.timphongtro.Activities;

import static android.widget.Toast.LENGTH_SHORT;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;

import android.app.AlertDialog;
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
import com.example.timphongtro.Utils.Validator;
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
    private AlertDialog loadingDialog;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference;
    private CredentialManager credentialManager;
    private GetGoogleIdOption getGoogleIdOption;
    private CallbackManager callbackManager;
    private LoginManager loginManager;
    private static final String EXTRA_PREVIOUS_ACTIVITY = "previous_activity";
    private String previousActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        previousActivity = getIntent().getStringExtra(EXTRA_PREVIOUS_ACTIVITY);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                navigateBackToPreviousScreen();
            }
        });

        initFirebase();
        setupGoogleSignIn();
        initViews();
        checkCurrentUser();
    }

    private void navigateBackToPreviousScreen() {
        if (previousActivity != null) {
            try {
                Class<?> activityClass = Class.forName(previousActivity);
                Intent intent = new Intent(this, activityClass);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            } catch (ClassNotFoundException e) {
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            }
        } else {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        }
        finish();
    }

    private void initFirebase() {
        firebaseAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("Users");
        credentialManager = CredentialManager.create(this);
        callbackManager = CallbackManager.Factory.create();
        loginManager = LoginManager.getInstance();
    }

    private void setupGoogleSignIn() {
        getGoogleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
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

        loginButton.setOnClickListener(v -> handleEmailPasswordSignIn());
        googleSignInImageView.setOnClickListener(v -> signInWithCredentialManager());
        facebookSignInImageView.setOnClickListener(v -> signInWithFacebook());
        registerTextView.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
        forgotPasswordTextView.setOnClickListener(v -> startActivity(new Intent(this, ForgotPasswordActivity.class)));
    }

    private void checkCurrentUser() {
        if (firebaseAuth.getCurrentUser() != null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    private void handleEmailPasswordSignIn() {
        String email = emailEditText.getText().toString();
        String password = passwordEditText.getText().toString();

        if (!validateInput(email, password)) return;

        showLoadingDialog();
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    hideLoadingDialog();
                    if (task.isSuccessful() && task.getResult().getUser() != null) {
                        if (task.getResult().getUser().isEmailVerified()) {
                            handleSuccessfulLogin();
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
            setError(emailEditText, "Vui lòng nhập Email");
            return false;
        }
        if (!Validator.isValidEmail(email)) {
            setError(emailEditText, "Email không hợp lệ");
            return false;
        }
        if (TextUtils.isEmpty(password) || password.length() < 6) {
            setError(passwordEditText, "Mật khẩu phải có ít nhất 6 ký tự");
            return false;
        }
        return true;
    }

    private void signInWithCredentialManager() {
        showLoadingDialog();
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
                            try {
                                Credential credential = result.getCredential();
                                if (credential instanceof CustomCredential) {
                                    String type = credential.getType();
                                    if (GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(type)) {
                                        GoogleIdTokenCredential googleCredential =
                                                GoogleIdTokenCredential.createFrom(credential.getData());
                                        firebaseAuthWithGoogle(googleCredential.getIdToken());
                                    }
                                }
                            } catch (Exception e) {
                                hideLoadingDialog();
                                showToast("Lỗi xử lý đăng nhập: " + e.getMessage());
                            }
                        }

                        @Override
                        public void onError(@NonNull GetCredentialException e) {
                            hideLoadingDialog();
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
        showLoadingDialog();
        loginManager.logInWithReadPermissions(this, Arrays.asList("email", "public_profile"));
        loginManager.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                handleFacebookAccessToken(loginResult.getAccessToken());
            }

            @Override
            public void onCancel() {
                hideLoadingDialog();
                showToast("Đăng nhập Facebook đã bị hủy");
            }

            @Override
            public void onError(@NonNull FacebookException e) {
                hideLoadingDialog();
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

    private void saveUserToDatabase(FirebaseUser user) {
        databaseReference.child(user.getUid()).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (task.getResult().exists()) {
                    HashMap<String, Object> updates = new HashMap<>();
                    updates.put("email", user.getEmail());
                    updates.put("name", user.getDisplayName());

                    databaseReference.child(user.getUid()).updateChildren(updates)
                            .addOnSuccessListener(unused -> handleSuccessfulLogin())
                            .addOnFailureListener(e -> {
                                hideLoadingDialog();
                                showToast("Lỗi cập nhật dữ liệu: " + e.getMessage());
                                firebaseAuth.signOut();
                            });
                } else {
                    HashMap<String, Object> userMap = new HashMap<>();
                    userMap.put("uid", user.getUid());
                    userMap.put("email", user.getEmail());
                    userMap.put("name", user.getDisplayName());
                    userMap.put("phone", "");
                    userMap.put("role", "Người thuê");
                    userMap.put("createdAt", System.currentTimeMillis());

                    databaseReference.child(user.getUid()).setValue(userMap)
                            .addOnSuccessListener(unused -> handleSuccessfulLogin())
                            .addOnFailureListener(e -> {
                                hideLoadingDialog();
                                showToast("Lỗi lưu dữ liệu: " + e.getMessage());
                                firebaseAuth.signOut();
                            });
                }
            } else {
                hideLoadingDialog();
                showToast("Lỗi kiểm tra dữ liệu: " + task.getException().getMessage());
                firebaseAuth.signOut();
            }
        });
    }

    private void setError(EditText editText, String error) {
        editText.setError(error);
        editText.requestFocus();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, LENGTH_SHORT).show();
    }

    private void showLoadingDialog() {
        if (loadingDialog == null) {
            loadingDialog = new AlertDialog.Builder(this)
                    .setView(R.layout.progress_layout)
                    .setCancelable(false)
                    .create();
        }
        loadingDialog.show();
    }

    private void hideLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    private void handleSuccessfulLogin() {
        hideLoadingDialog();
        showToast("Đăng nhập thành công");
        navigateBackToPreviousScreen();
    }
}