package com.google.android.material.snackbar;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

public abstract class TransientBottom<F extends TransientBottom> extends BaseTransientBottomBar {

    protected TransientBottom(
            @NonNull final ViewGroup parent,
            @NonNull final View content,
            @NonNull final com.google.android.material.snackbar.ContentViewCallback contentViewCallback) {
        super(parent, content, contentViewCallback);
    }

    @Override
    boolean shouldAnimate() {
        return true;
    }
}
