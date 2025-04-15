package com.example.timphongtro.Utils;

import android.text.TextUtils;
import android.util.Patterns;
import java.util.regex.Pattern;

public class Validator {
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "[a-zA-Z0-9\\+\\.\\_\\%\\-\\+]{1,256}" +
                    "\\@" +
                    "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
                    "(" +
                    "\\." +
                    "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
                    ")+"
    );

    public static boolean isValidEmail(String email) {
        if (TextUtils.isEmpty(email)) {
            return false;
        }

        // Basic pattern check
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return false;
        }

        // Additional stricter validation
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return false;
        }

        // Check common disposable email domains
        String domain = email.substring(email.indexOf("@") + 1).toLowerCase();
        return !isDisposableEmailDomain(domain);
    }

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