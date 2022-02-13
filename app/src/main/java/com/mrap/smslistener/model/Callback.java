package com.mrap.smslistener.model;

public interface Callback<T> {
    void onCallback(T args);
}
