package com.alebit.sget.playlist;

import com.alebit.sget.data.Header;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;

public class PlaylistParser {
    public static Playlist parse(String url, Header[] headers) throws UnsupportedPlaylistException {
        try {
            URI uri = new URI(url);
            URLConnection connection = uri.toURL().openConnection();
            for (Header header: headers) {
                connection.setRequestProperty(header.getName(), header.getValue());
            }
            try (InputStream inputStream = connection.getInputStream()) {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                IOUtils.copy(inputStream, outputStream);

                try {
                    return new HLSPlaylist(outputStream.toByteArray(), uri, headers);
                } catch (Exception e) {
                    try {
                        InputStream dataInputStream = new ByteArrayInputStream(outputStream.toByteArray());
                        return new DASHPlaylist(dataInputStream, uri, headers);
                    } catch (Exception ex) {
                        throw new UnsupportedPlaylistException("Unsupported playlist format");
                    }
                }
            }
        } catch (URISyntaxException | IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    static HLSPlaylist parseHLS(URI uri, Header[] headers) throws UnsupportedPlaylistException {
        try {
            URLConnection connection = uri.toURL().openConnection();
            for (Header header: headers) {
                connection.setRequestProperty(header.getName(), header.getValue());
            }
            InputStream inputStream = connection.getInputStream();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            IOUtils.copy(inputStream, outputStream);

            try {
                return new HLSPlaylist(outputStream.toByteArray(), uri, headers);
            } catch (Exception e) {
                throw new UnsupportedPlaylistException("Unsupported playlist format");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
