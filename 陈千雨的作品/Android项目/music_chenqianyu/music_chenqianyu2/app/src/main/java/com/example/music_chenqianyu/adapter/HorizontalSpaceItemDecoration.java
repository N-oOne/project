package com.example.music_chenqianyu.adapter;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class HorizontalSpaceItemDecoration extends RecyclerView.ItemDecoration {
    private final int horizontalSpace;

    public HorizontalSpaceItemDecoration(int horizontalSpace) {
        this.horizontalSpace = horizontalSpace;
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                               @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        outRect.left = horizontalSpace / 2;
        outRect.right = horizontalSpace / 2;
    }
}