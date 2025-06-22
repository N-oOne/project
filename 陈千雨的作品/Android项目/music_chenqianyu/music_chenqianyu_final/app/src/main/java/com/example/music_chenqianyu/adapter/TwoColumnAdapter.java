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
import com.example.music_chenqianyu.HomeActivity;
import com.example.music_chenqianyu.PlayerActivity;
import com.example.music_chenqianyu.R;
import com.example.music_chenqianyu.model.MusicInfo;

import java.util.ArrayList;
import java.util.List;

public class TwoColumnAdapter extends RecyclerView.Adapter<TwoColumnAdapter.TwoColumnViewHolder> {

    private final List<MusicInfo> twoColumnList;

    public TwoColumnAdapter(List<MusicInfo> twoColumnList) {
        this.twoColumnList = twoColumnList;
    }

    @NonNull
    @Override
    public TwoColumnViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.two_column_item, parent, false);
        return new TwoColumnViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TwoColumnViewHolder holder, int position) {
        MusicInfo music = twoColumnList.get(position);

        Glide.with(holder.itemView.getContext())
                .load(music.getCoverUrl())
                .placeholder(R.drawable.ic_placeholder)
                .error(R.drawable.ic_error)
                .transition(DrawableTransitionOptions.withCrossFade(500))
                .into(holder.twoColumnImage);

        holder.musicTitle.setText(music.getMusicName());
        holder.musicAuthor.setText(music.getAuthor());

        // 点击整个item的监听
        holder.itemView.setOnClickListener(v -> {
//            Toast.makeText(holder.itemView.getContext(),
//                    music.getMusicName(),
//                    Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(holder.itemView.getContext(), PlayerActivity.class);
            intent.putParcelableArrayListExtra("playList", new ArrayList<>(twoColumnList));
            intent.putExtra("currentPosition", position);
            holder.itemView.getContext().startActivity(intent);
        });

        // 保留加号点击功能
        holder.addButton.setOnClickListener(v -> {
            Toast.makeText(holder.itemView.getContext(),
                    "将\"" + music.getMusicName() + "\"添加到音乐列表",
                    Toast.LENGTH_SHORT).show();
            ((HomeActivity) holder.itemView.getContext()).addToPlaylist(music);
        });
    }

    @Override
    public int getItemCount() {
        return twoColumnList.size();
    }

    static class TwoColumnViewHolder extends RecyclerView.ViewHolder {
        final ImageView twoColumnImage;
        final TextView musicTitle;
        final TextView musicAuthor;
        final ImageView addButton;

        TwoColumnViewHolder(@NonNull View itemView) {
            super(itemView);
            twoColumnImage = itemView.findViewById(R.id.twoColumnImage);
            musicTitle = itemView.findViewById(R.id.musicTitle);
            musicAuthor = itemView.findViewById(R.id.musicAuthor);
            addButton = itemView.findViewById(R.id.addButton);
        }
    }
}