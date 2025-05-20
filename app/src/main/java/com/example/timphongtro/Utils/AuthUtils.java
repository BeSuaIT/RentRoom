package com.example.timphongtro.Utils;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.timphongtro.Activities.LoginActivity;

/**
 * Lớp tiện ích xử lý các chức năng liên quan đến xác thực người dùng
 */
public class AuthUtils {

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
     * Hiển thị dialog thông báo yêu cầu đăng nhập với hành động mặc định
     * 
     * @param context Context bất kỳ
     * @param feature Tên tính năng yêu cầu đăng nhập
     */
    public static void showLoginRequiredDialog(Context context, String feature) {
        showLoginRequiredDialog(context, feature, "sử dụng");
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
}