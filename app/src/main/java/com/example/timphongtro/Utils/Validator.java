package com.example.timphongtro.Utils;

import android.text.TextUtils;
import android.util.Patterns;
import java.util.regex.Pattern;

/**
 * Lớp tiện ích cung cấp các phương thức kiểm tra tính hợp lệ của dữ liệu nhập vào
 * Hiện tại hỗ trợ việc xác thực địa chỉ email
 */
public class Validator {
    /**
     * Mẫu regex chi tiết hơn để kiểm tra địa chỉ email
     * Kiểm tra định dạng username@domain.tld
     */
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "[a-zA-Z0-9\\+\\.\\_\\%\\-\\+]{1,256}" +
                    "\\@" +
                    "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
                    "(" +
                    "\\." +
                    "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
                    ")+"
    );

    /**
     * Kiểm tra tính hợp lệ của địa chỉ email
     * Thực hiện kiểm tra theo nhiều bước:
     * 1. Kiểm tra email không rỗng
     * 2. Kiểm tra email khớp với mẫu cơ bản của Android
     * 3. Kiểm tra email với mẫu regex chi tiết hơn
     * 4. Kiểm tra tên miền không phải là dịch vụ email dùng một lần
     *
     * @param email Địa chỉ email cần kiểm tra
     * @return true nếu email hợp lệ, false nếu không hợp lệ
     */
    public static boolean isValidEmail(String email) {
        if (TextUtils.isEmpty(email)) {
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return false;
        }

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return false;
        }

        String domain = email.substring(email.indexOf("@") + 1).toLowerCase();
        return !isDisposableEmailDomain(domain);
    }

    /**
     * Kiểm tra xem tên miền có phải là dịch vụ email dùng một lần không
     * Danh sách các tên miền phổ biến cung cấp dịch vụ email dùng một lần
     * 
     * @param domain Tên miền cần kiểm tra
     * @return true nếu là dịch vụ email dùng một lần, false nếu không phải
     */
    private static boolean isDisposableEmailDomain(String domain) {
        String[] disposableDomains = {
                "tempmail.com", "temp-mail.org", "guerrillamail.com",
                "sharklasers.com", "mailinator.com", "yopmail.com"
        };

        for (String disposableDomain : disposableDomains) {
            if (domain.contains(disposableDomain)) {
                return true;
            }
        }
        return false;
    }
}