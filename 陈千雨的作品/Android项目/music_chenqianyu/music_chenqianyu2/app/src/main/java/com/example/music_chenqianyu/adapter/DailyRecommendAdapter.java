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

public class DailyRecommendAdapter extends RecyclerView.Adapter<DailyRecommendAdapter.DailyRecommendViewHolder> {

    private final List<MusicInfo> dailyRecommendList;

    public DailyRecommendAdapter(List<MusicInfo> dailyRecommendList) {
        this.dailyRecommendList = dailyRecommendList;
    }

    @NonNull
    @Override
    public DailyRecommendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.daily_recommend_item, parent, false);
        return new DailyRecommendViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DailyRecommendViewHolder holder, int position) {
        MusicInfo music = dailyRecommendList.get(position);

        Glide.with(holder.itemView.getContext())
                .load(music.getCoverUrl())
                .placeholder(R.drawable.ic_placeholder)
                .error(R.drawable.ic_error)
                .transition(DrawableTransitionOptions.withCrossFade(500))
                .into(holder.dailyImage);

        holder.musicTitle.setText(music.getMusicName());
        holder.musicAuthor.setText(music.getAuthor());

        // 修改为只显示音乐名称
        holder.itemView.setOnClickListener(v -> {
//            Toast.makeText(holder.itemView.getContext(),
//                    music.getMusicName(),
//                    Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(holder.itemView.getContext(), PlayerActivity.class);
            intent.putParcelableArrayListExtra("playList", new ArrayList<>(dailyRecommendList));
            intent.putExtra("currentPosition", position);
            holder.itemView.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return dailyRecommendList.size();
    }

    static class DailyRecommendViewHolder extends RecyclerView.ViewHolder {
        final ImageView dailyImage;
        final TextView musicTitle;
        final TextView musicAuthor;

        DailyRecommendViewHolder(@NonNull View itemView) {
            super(itemView);
            dailyImage = itemView.findViewById(R.id.dailyImage);
            musicTitle = itemView.findViewById(R.id.musicTitle);
            musicAuthor = itemView.findViewById(R.id.musicAuthor);
        }
    }
}