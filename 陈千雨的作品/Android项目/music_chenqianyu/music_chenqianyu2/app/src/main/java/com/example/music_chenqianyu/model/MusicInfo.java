package com.example.music_chenqianyu.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

public class MusicInfo implements Parcelable {
    private int id;
    private String musicName;
    private String author;
    private String coverUrl;
    private String musicUrl;
    private String lyricUrl;

    public MusicInfo() {
        // 默认构造函数
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getMusicName() {
        return musicName;
    }

    public void setMusicName(String musicName) {
        this.musicName = musicName;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public String getMusicUrl() {
        return musicUrl;
    }

    public void setMusicUrl(String musicUrl) {
        this.musicUrl = musicUrl;
    }

    public String getLyricUrl() {
        return lyricUrl;
    }

    public void setLyricUrl(String lyricUrl) {
        this.lyricUrl = lyricUrl;
    }

    // ========== Parcelable 实现 ==========
    protected MusicInfo(Parcel in) {
        id = in.readInt();
        musicName = in.readString();
        author = in.readString();
        coverUrl = in.readString();
        musicUrl = in.readString();
        lyricUrl = in.readString();
    }

    public static final Creator<MusicInfo> CREATOR = new Creator<MusicInfo>() {
        @Override
        public MusicInfo createFromParcel(Parcel in) {
            return new MusicInfo(in);
        }

        @Override
        public MusicInfo[] newArray(int size) {
            return new MusicInfo[size];
        }
    };

    @Override
    public int describeContents() {
        return 0; // 一般返回0即可
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(musicName);
        dest.writeString(author);
        dest.writeString(coverUrl);
        dest.writeString(musicUrl);
        dest.writeString(lyricUrl);
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MusicInfo musicInfo = (MusicInfo) o;
        return id == musicInfo.id; // 使用原始id字段比较
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    // 添加toString()方便调试
    @Override
    public String toString() {
        return "MusicInfo{" +
                "id=" + id +
                ", musicName='" + musicName + '\'' +
                ", author='" + author + '\'' +
                ", coverUrl='" + coverUrl + '\'' +
                ", musicUrl='" + musicUrl + '\'' +
                ", lyricUrl='" + lyricUrl + '\'' +
                '}';
    }
}