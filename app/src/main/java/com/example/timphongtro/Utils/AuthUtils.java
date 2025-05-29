package com.example.timphongtro.Utils;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.timphongtro.Activities.LoginActivity;
import com.facebook.AccessToken;
import com.facebook.login.LoginManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

/**
 * Lớp tiện ích xử lý các chức năng liên quan đến xác thực người dùng
 */
public class AuthUtils {

    /**
     * Interface callback để xử lý kết quả kiểm tra tài khoản
     */
    public interface UserExistsCallback {
        void onResult(boolean exists);
    }

    /**
     * Lấy Activity từ Context
     * 
     * @param context Context bất kỳ
     * @return Activity hoặc null
     */
    private static Activity getActivity(Context context) {
        if (context == null) {
            return null;
        } else if (context instanceof Activity) {
            return (Activity) context;
        } else if (context instanceof ContextWrapper) {
            return getActivity(((ContextWrapper) context).getBaseContext());
        }
        return null;
    }

    /**
     * Hiển thị dialog thông báo yêu cầu đăng nhập từ một Context bất kỳ
     * 
     * @param context Context (có thể là Activity, Application, Service, hoặc ContextWrapper)
     * @param feature Tên tính năng yêu cầu đăng nhập
     * @param action Hành động người dùng muốn thực hiện (mặc định là "sử dụng")
     */
    public static void showLoginRequiredDialog(Context context, String feature, String action) {
        if (context == null) return;
        
        Activity activity = getActivity(context);
        if (activity == null) return;
        
        String message = "Bạn cần đăng nhập để " + (action != null ? action : "sử dụng") + " " + feature;
        
        new AlertDialog.Builder(activity)
            .setTitle("Yêu cầu đăng nhập")
            .setMessage(message)
            .setPositiveButton("Đăng nhập", (dialog, which) -> {
                Intent intent = new Intent(activity, LoginActivity.class);
                intent.putExtra("previous_activity", activity.getClass().getName());
                activity.startActivity(intent);
            })
            .setNegativeButton("Hủy", null)
            .show();
    }

    /**
     * Hiển thị dialog thông báo yêu cầu đăng nhập từ một Fragment
     * 
     * @param fragment Fragment hiện tại
     * @param feature Tên tính năng yêu cầu đăng nhập
     * @param action Hành động người dùng muốn thực hiện
     */
    public static void showLoginRequiredDialog(Fragment fragment, String feature, String action) {
        if (fragment == null || !fragment.isAdded()) return;
        
        AlertDialog.Builder builder = new AlertDialog.Builder(fragment.requireActivity());
        String message = "Bạn cần đăng nhập để " + (action != null ? action : "sử dụng") + " " + feature;
        
        builder.setTitle("Yêu cầu đăng nhập")
               .setMessage(message)
               .setPositiveButton("Đăng nhập", (dialog, which) -> {
                   Intent intent = new Intent(fragment.requireContext(), LoginActivity.class);
                   intent.putExtra("previous_activity", fragment.requireActivity().getClass().getName());
                   fragment.startActivity(intent);
               })
               .setNegativeButton("Hủy", null)
               .show();
    }

    /**
     * Hiển thị dialog thông báo yêu cầu đăng nhập từ Fragment với hành động mặc định
     * 
     * @param fragment Fragment hiện tại
     * @param feature Tên tính năng yêu cầu đăng nhập
     */
    public static void showLoginRequiredDialog(Fragment fragment, String feature) {
        showLoginRequiredDialog(fragment, feature, "sử dụng");
    }

    /**
     * Kiểm tra tài khoản người dùng có tồn tại trên Firebase Database hay không
     * Sử dụng khi khởi động app hoặc resume activity
     * 
     * @param context Context hiện tại
     * @param user FirebaseUser cần kiểm tra
     * @param callback Callback xử lý kết quả
     */
    public static void checkUserExistsOnStartup(Context context, FirebaseUser user, UserExistsCallback callback) {
        if (user == null) {
            callback.onResult(true);
            return;
        }

        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(user.getUid());
        
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean exists = snapshot.exists();
                
                if (!exists) {
                    // Tài khoản không tồn tại trên database
                    clearAllLoginData(context);
                    
                    Toast.makeText(context, 
                        "Tài khoản đã bị xóa khỏi hệ thống. Vui lòng đăng nhập lại", 
                        Toast.LENGTH_LONG).show();
                }
                
                callback.onResult(exists);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Nếu có lỗi mạng, cho phép tiếp tục để không block app
                callback.onResult(true);
            }
        });
    }

    /**
     * Kiểm tra tài khoản với xử lý chuyển hướng tự động
     * Sử dụng trong các activity yêu cầu đăng nhập bắt buộc
     * 
     * @param context Context hiện tại  
     * @param user FirebaseUser cần kiểm tra
     * @param callback Callback xử lý kết quả
     */
    public static void checkUserExists(Context context, FirebaseUser user, UserExistsCallback callback) {
        if (user == null) {
            callback.onResult(false);
            return;
        }

        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(user.getUid());
        
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean exists = snapshot.exists();
                callback.onResult(exists);
                
                if (!exists) {
                    clearAllLoginData(context);
                    
                    Toast.makeText(context, 
                        "Tài khoản không tồn tại. Vui lòng đăng nhập lại", 
                        Toast.LENGTH_LONG).show();
                    Activity activity = getActivity(context);
                    if (activity != null) {
                        Intent intent = new Intent(context, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        context.startActivity(intent);
                        activity.finish();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onResult(false);
            }
        });
    }

    /**
     * Xóa toàn bộ dữ liệu đăng nhập khỏi thiết bị
     * Bao gồm Firebase Auth, Facebook Login và SharedPreferences
     * 
     * @param context Context hiện tại
     */
    public static void clearAllLoginData(Context context) {
        try {
            FirebaseAuth.getInstance().signOut();
        } catch (Exception e) {
        }
        
        try {
            if (AccessToken.getCurrentAccessToken() != null) {
                LoginManager.getInstance().logOut();
            }
        } catch (Exception e) {
        }
        
        try {
            // Clear SharedPreferences nếu có lưu thông tin đăng nhập
            SharedPreferences prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
            prefs.edit().clear().apply();
            
            // Clear other common preference files
            SharedPreferences authPrefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE);
            authPrefs.edit().clear().apply();
            
            SharedPreferences loginPrefs = context.getSharedPreferences("login_prefs", Context.MODE_PRIVATE);
            loginPrefs.edit().clear().apply();
        } catch (Exception e) {
        }
        
        // Note: CredentialManager không cần clear thủ công vì nó tự động xóa khi Firebase Auth sign out
        // Google Credentials sẽ được xóa thông qua Firebase Auth signOut()
    }

    /**
     * Đăng xuất người dùng khỏi tất cả các dịch vụ
     * 
     * @param context Context hiện tại
     */
    public static void signOut(Context context) {
        clearAllLoginData(context);
        Activity activity = getActivity(context);
        if (activity != null) {
            Intent intent = new Intent(context, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            context.startActivity(intent);
            activity.finish();
        }
    }

    /**
     * Kiểm tra tài khoản có tồn tại hay không (đơn giản)
     * Chỉ trả về kết quả, không tự động xử lý
     * 
     * @param user FirebaseUser cần kiểm tra
     * @param callback Callback xử lý kết quả
     */
    public static void isUserExists(FirebaseUser user, UserExistsCallback callback) {
        if (user == null) {
            callback.onResult(false);
            return;
        }

        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(user.getUid());
        
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                callback.onResult(snapshot.exists());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onResult(false);
            }
        });
    }

    /**
     * Lấy FirebaseUser hiện tại
     * 
     * @return FirebaseUser hiện tại hoặc null
     */
    public static FirebaseUser getCurrentUser() {
        return FirebaseAuth.getInstance().getCurrentUser();
    }

    /**
     * Kiểm tra người dùng đã đăng nhập hay chưa
     * 
     * @return true nếu đã đăng nhập, false nếu chưa
     */
    public static boolean isUserLoggedIn() {
        return FirebaseAuth.getInstance().getCurrentUser() != null;
    }

    /**
     * Kiểm tra email đã được xác minh hay chưa
     * 
     * @return true nếu email đã được xác minh, false nếu chưa
     */
    public static boolean isEmailVerified() {
        FirebaseUser user = getCurrentUser();
        return user != null && user.isEmailVerified();
    }

    /**
     * Kiểm tra user đã đăng nhập và email đã được xác minh
     * 
     * @return true nếu user đã login và email verified, false nếu ngược lại
     */
    public static boolean isUserFullyAuthenticated() {
        return isUserLoggedIn() && isEmailVerified();
    }
}