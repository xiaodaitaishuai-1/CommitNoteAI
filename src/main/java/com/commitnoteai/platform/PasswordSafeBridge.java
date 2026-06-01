package com.commitnoteai.platform;

import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.ide.passwordSafe.PasswordSafe;

public final class PasswordSafeBridge {
    private PasswordSafeBridge() {
    }

    public static String getPassword(CredentialAttributes attributes) {
        return PasswordSafe.getInstance().getPassword(attributes);
    }

    public static void setPassword(CredentialAttributes attributes, String password) {
        PasswordSafe.getInstance().setPassword(attributes, password);
    }
}
