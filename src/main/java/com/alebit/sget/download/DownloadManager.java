package com.alebit.sget.download;

import com.alebit.sget.data.Header;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.net.URI;
import java.net.URLConnection;
import java.nio.file.Path;

/**
 * Created by Alec on 2016/6/26.
 */
public class DownloadManager {
    private Header[] headers;

    public DownloadManager(Header[] headers) {
        this.headers = headers;
    }

    public boolean download(URI uri, Path filePath) {
        String filename = uri.getPath();
        filename = filename.substring(filename.lastIndexOf("/") + 1);
        return download(uri, filePath, filename);
    }

    public boolean download(URI uri, Path filePath, String filename) {
        try {
            File file = filePath.resolve(filename).toFile();
            URLConnection connection = uri.toURL().openConnection();
            connection.setConnectTimeout(60000);
            connection.setReadTimeout(60000);
            for (Header header: headers) {
                connection.setRequestProperty(header.getName(), header.getValue());
            }
            FileUtils.copyInputStreamToFile(connection.getInputStream(), file);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
