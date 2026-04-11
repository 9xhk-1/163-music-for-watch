package com.qinghe.music163pro.model;

import java.io.Serializable;

/**
 * Model for MV search/detail data.
 */
public class MvInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    private long id;
    private String name;
    private String artist;
    private String coverUrl;
    private long durationMs;
    private long playCount;

    public MvInfo() {}

    public MvInfo(long id, String name, String artist, String coverUrl, long durationMs, long playCount) {
        this.id = id;
        this.name = name;
        this.artist = artist;
        this.coverUrl = coverUrl;
        this.durationMs = durationMs;
        this.playCount = playCount;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }

    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }

    public long getDurationMs() { return durationMs; }
    public void setDurationMs(long durationMs) { this.durationMs = durationMs; }

    public long getPlayCount() { return playCount; }
    public void setPlayCount(long playCount) { this.playCount = playCount; }

    @Override
    public String toString() {
        return name != null ? name : "";
    }
}
