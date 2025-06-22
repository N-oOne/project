package com.example.music_chenqianyu.adapter;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.music_chenqianyu.PlayerActivity;
import com.example.music_chenqianyu.R;
import com.example.music_chenqianyu.model.MusicInfo;

import java.util.ArrayList;
import java.util.List;

public class HorizontalCardAdapter extends RecyclerView.Adapter<HorizontalCardAdapter.HorizontalCardViewHolder> {

    private final List<MusicInfo> horizontalCardList;

    public HorizontalCardAdapter(List<MusicInfo> horizontalCardList) {
        this.horizontalCardList = horizontalCardList;
    }

    @NonNull
    @Override
    public HorizontalCardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.horizontal_card_item, parent, false);
        return new HorizontalCardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HorizontalCardViewHolder holder, int position) {
        int actualPosition = position % horizontalCardList.size();
        MusicInfo music = horizontalCardList.get(actualPosition);

        Glide.with(holder.itemView.getContext())
                .load(music.getCoverUrl())
                .placeholder(R.drawable.ic_placeholder)
                .error(R.drawable.ic_error)
                .transition(DrawableTransitionOptions.withCrossFade(500))
                .into(holder.cardImage);

        holder.musicTitle.setText(music.getMusicName());
        holder.musicAuthor.setText(music.getAuthor());

        // 点击整个item的监听
        holder.itemView.setOnClickListener(v -> {
//            Toast.makeText(holder.itemView.getContext(),
//                    music.getMusicName(),
//                    Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(holder.itemView.getContext(), PlayerActivity.class);
            intent.putParcelableArrayListExtra("playList", new ArrayList<>(horizontalCardList));
            intent.putExtra("currentPosition", actualPosition);
            holder.itemView.getContext().startActivity(intent);

        });

        // 保留加号点击功能
        holder.addButton.setOnClickListener(v -> {
            Toast.makeText(holder.itemView.getContext(),
                    "将\"" + music.getMusicName() + "\"添加到音乐列表",
                    Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public int getItemCount() {
        return horizontalCardList.isEmpty() ? 0 : Integer.MAX_VALUE;
    }

    static class HorizontalCardViewHolder extends RecyclerView.ViewHolder {
        final ImageView cardImage;
        final TextView musicTitle;
        final TextView musicAuthor;
        final ImageView addButton;

        HorizontalCardViewHolder(@NonNull View itemView) {
            super(itemView);
            cardImage = itemView.findViewById(R.id.cardImage);
            musicTitle = itemView.findViewById(R.id.musicTitle);
            musicAuthor = itemView.findViewById(R.id.musicAuthor);
            addButton = itemView.findViewById(R.id.addButton);
        }
    }
}