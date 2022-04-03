package com.alebit.sget.playlist;

import java.net.URI;

public class Subtitle {
    private String name;
    private URI uri;

    public Subtitle(String name, URI uri) {
        this.name = name;
        this.uri = uri;
    }

    public String getName() {
        return name;
    }

    public URI getUri() {
        return uri;
    }
}
