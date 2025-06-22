package com.example.music_chenqianyu.model;

public class HomeResponse {
    private int code;
    private String msg;
    private HomeData data;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public HomeData getData() {
        return data;
    }

    public void setData(HomeData data) {
        this.data = data;
    }
}