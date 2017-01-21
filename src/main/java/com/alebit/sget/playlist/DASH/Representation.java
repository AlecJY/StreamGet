package com.alebit.sget.playlist.DASH;

/**
 * Created by alec on 2017/1/17.
 */
public class Representation {
    public static boolean VIDEO = false;
    public static boolean AUDIO = true;
    private boolean type;
    private String bandwidth;
    private String audioSamplingRate;
    private String width;
    private String height;
    private String id;

    public Representation(String id, String bandwidth, boolean type) {
        this.id = id;
        this.bandwidth = bandwidth;
        this.type = type;
    }

    public Representation(String id, String bandwidth, String audioSamplingRate) {
        this.id = id;
        this.bandwidth = bandwidth;
        this.type = AUDIO;
        this.audioSamplingRate = audioSamplingRate;
    }

    public Representation(String id, String bandwidth, String width, String height) {
        this.id = id;
        this.bandwidth = bandwidth;
        this.type = VIDEO;
        this.width = width;
        this.height = height;
    }

    public boolean getType() {
        return type;
    }

    public String getAudioSamplingRate() {
        return audioSamplingRate;
    }

    public String getWidth() {
        return width;
    }

    public String getHeight() {
        return height;
    }

    public String getBandwidth() {
        return bandwidth;
    }
}
