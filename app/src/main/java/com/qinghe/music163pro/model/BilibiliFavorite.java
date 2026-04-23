package com.qinghe.music163pro.model;

import java.io.Serializable;

public class BilibiliFavorite implements Serializable {
    private static final long serialVersionUID = 1L;

    private String bvid;
    private String title;
    private String owner;

    public BilibiliFavorite() {
    }

    public BilibiliFavorite(String bvid, String title, String owner) {
        this.bvid = bvid;
        this.title = title;
        this.owner = owner;
    }

    public String getBvid() {
        return bvid;
    }

    public void setBvid(String bvid) {
        this.bvid = bvid;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }
}
