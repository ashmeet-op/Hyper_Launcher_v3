package com.ashmeet.hyperbackend.authenticator.listener;

import com.ashmeet.hyperbackend.authenticator.accounts.Account;

public interface LoginListener{
    void onLoginDone(Account account);
    void onLoginError(Throwable errorMessage);
    void onLoginProgress(int step);
    void setMaxLoginProgress(int max);
}
