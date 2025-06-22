package com.example.music_chenqianyu.model;

import java.util.List;

public class ModuleConfig {
    private int moduleConfigId;
    private String moduleName;
    private int style;
    private List<MusicInfo> musicInfoList;

    public int getModuleConfigId() {
        return moduleConfigId;
    }

    public void setModuleConfigId(int moduleConfigId) {
        this.moduleConfigId = moduleConfigId;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public int getStyle() {
        return style;
    }

    public void setStyle(int style) {
        this.style = style;
    }

    public List<MusicInfo> getMusicInfoList() {
        return musicInfoList;
    }

    public void setMusicInfoList(List<MusicInfo> musicInfoList) {
        this.musicInfoList = musicInfoList;
    }
}