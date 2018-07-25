package com.alebit.sget.playlist;

public class Subtitle {
    private String name;
    private String lang;
    private String Uri;

    public Subtitle(String name, String lang, String Uri) {
        this.name = name;
        this.lang = lang;
        this.Uri = Uri;
    }

    public String getName() {
        return name;
    }

    public String getLang() {
        return lang;
    }

    public String getUri() {
        return Uri;
    }
}
