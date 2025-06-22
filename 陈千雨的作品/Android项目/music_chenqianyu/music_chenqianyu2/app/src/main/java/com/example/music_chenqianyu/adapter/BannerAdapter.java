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

public class BannerAdapter extends RecyclerView.Adapter<BannerAdapter.BannerViewHolder> {

    private final List<MusicInfo> bannerMusicList;
    private static final int LOOP_MULTIPLIER = 1000;

    public BannerAdapter(List<MusicInfo> bannerMusicList) {
        this.bannerMusicList = bannerMusicList;
    }

    @Override
    public int getItemCount() {
        if (bannerMusicList.isEmpty()) return 0;
        return bannerMusicList.size() * LOOP_MULTIPLIER;
    }

    public int getRealItemCount() {
        return bannerMusicList.size();
    }

    @NonNull
    @Override
    public BannerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.banner_item, parent, false);
        return new BannerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BannerViewHolder holder, int position) {
        if (bannerMusicList.isEmpty()) return;

        int realPosition = position % bannerMusicList.size();
        MusicInfo music = bannerMusicList.get(realPosition);

        Glide.with(holder.itemView.getContext())
                .load(music.getCoverUrl())
                .placeholder(R.drawable.ic_placeholder)
                .error(R.drawable.ic_error)
                .transition(DrawableTransitionOptions.withCrossFade(500))
                .into(holder.bannerImage);

        holder.musicTitle.setText(music.getMusicName());
        holder.musicAuthor.setText(music.getAuthor());

        // 点击整个item的监听
        holder.itemView.setOnClickListener(v -> {
//            Toast.makeText(holder.itemView.getContext(),
//                    music.getMusicName(), // 只显示音乐名称
//                    Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(holder.itemView.getContext(), PlayerActivity.class);
            intent.putExtra("playList", new ArrayList<>(bannerMusicList)); // 传递副本
            intent.putExtra("currentPosition", realPosition);
            holder.itemView.getContext().startActivity(intent);
        });

        // 点击加号的监听（保留原有功能）
        holder.addButton.setOnClickListener(v -> {
            Toast.makeText(holder.itemView.getContext(),
                    "将\"" + music.getMusicName() + "\"添加到音乐列表",
                    Toast.LENGTH_SHORT).show();

        });
    }


    static class BannerViewHolder extends RecyclerView.ViewHolder {
        final ImageView bannerImage;
        final TextView musicTitle;
        final TextView musicAuthor;
        final ImageView addButton;

        BannerViewHolder(@NonNull View itemView) {
            super(itemView);
            bannerImage = itemView.findViewById(R.id.bannerImage);
            musicTitle = itemView.findViewById(R.id.musicTitle);
            musicAuthor = itemView.findViewById(R.id.musicAuthor);
            addButton = itemView.findViewById(R.id.addButton);
        }
    }
}