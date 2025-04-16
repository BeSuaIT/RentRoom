package com.example.timphongtro.BroadcastReceiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.widget.Toast;

import androidx.annotation.NonNull;

public class NetworkChangeReceiver extends BroadcastReceiver {
    private boolean isInitialStickyBroadcast = true;
    private boolean lastNetworkState = false;

    @Override
    public void onReceive(Context context, Intent intent) {
        // Skip initial broadcast
        if (isInitialStickyBroadcast) {
            isInitialStickyBroadcast = false;
            return;
        }

        boolean isCurrentlyConnected = isNetworkAvailable(context);
        
        // Only show notification when state actually changes
        if (isCurrentlyConnected != lastNetworkState) {
            if (isCurrentlyConnected) {
                Toast.makeText(context, "Đã kết nối tới mạng", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "Vui lòng kiểm tra lại mạng", Toast.LENGTH_SHORT).show();
            }
            lastNetworkState = isCurrentlyConnected;
        }
    }

    private boolean isNetworkAvailable(@NonNull Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            return false;
        }

        Network network = connectivityManager.getActiveNetwork();
        if (network == null) {
            return false;
        }

        NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(network);
        return networkCapabilities != null && 
               (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR));
    }
}