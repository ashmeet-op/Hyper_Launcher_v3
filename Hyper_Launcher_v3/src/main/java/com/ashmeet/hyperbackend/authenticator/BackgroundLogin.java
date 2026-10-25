package com.ashmeet.hyperbackend.authenticator;

import androidx.annotation.NonNull;

import com.ashmeet.hyperbackend.authenticator.listener.LoginListener;
import com.ashmeet.hyperbackend.authenticator.accounts.Account;

public interface BackgroundLogin {
    void createAccount(@NonNull LoginListener loginListener, String code);
    void refreshAccount(@NonNull LoginListener loginListener, Account account);
    interface Creator {
        BackgroundLogin create();
    }
}
