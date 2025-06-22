package com.example.music_chenqianyu.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.music_chenqianyu.R;
import com.example.music_chenqianyu.model.MusicInfo;

import java.util.List;

public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder> {

    private List<MusicInfo> musicList;
    private OnItemClickListener listener;
    private int currentPlayingPosition = -1;

    public interface OnItemClickListener {
        void onItemClick(int position);
        void onDeleteClick(int position);
    }

    public PlaylistAdapter(List<MusicInfo> musicList, OnItemClickListener listener) {
        this.musicList = musicList;
        this.listener = listener;
    }

    public void setCurrentPlayingPosition(int position) {
        this.currentPlayingPosition = position;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PlaylistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_playlist, parent, false);
        return new PlaylistViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlaylistViewHolder holder, int position) {
        MusicInfo music = musicList.get(position);

        // 设置歌曲名称和艺术家
        holder.tvMusicName.setText(music.getMusicName());
        holder.tvArtist.setText(music.getAuthor());

        // 高亮显示当前播放的歌曲
        if (position == currentPlayingPosition) {
            holder.tvMusicName.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.blue));
            holder.tvArtist.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.blue));
        } else {
            holder.tvMusicName.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.black));
            holder.tvArtist.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.black));
        }

        // 隐藏播放模式TextView（现在在对话框顶部显示）
        holder.tvPlayMode.setVisibility(View.GONE);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(position);
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(position);
            }
        });
    }

    public void updatePlaylist(List<MusicInfo> newPlaylist) {
        this.musicList = newPlaylist;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return musicList != null ? musicList.size() : 0;
    }

    static class PlaylistViewHolder extends RecyclerView.ViewHolder {
        TextView tvMusicName;
        TextView tvArtist;
        TextView tvPlayMode;  // 保留但不使用
        ImageButton btnDelete;

        public PlaylistViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMusicName = itemView.findViewById(R.id.tvMusicName);
            tvArtist = itemView.findViewById(R.id.tvArtist);
            tvPlayMode = itemView.findViewById(R.id.tvPlayMode);  // 保留但不使用
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}