package com.alebit.sget.download;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

/**
 * Created by Alec on 2016/6/26.
 */
public class DownloadManager {
    private ArrayList<String[]> headers;

    public DownloadManager(ArrayList<String[]> headers) {
        this.headers = headers;
    }

    public boolean download(String url, String filePath) {
        String filename = url;
        if (filename.contains("?")) {
            filename = filename.substring(0, filename.indexOf("?"));
        }
        filename = filename.substring(filename.lastIndexOf("/") + 1);
        return download(url, filePath, filename);
    }

    public boolean download(String url, String filePath, String filename) {
        try {
            URL loc = new URL(url);
            File file = new File(filePath + filename);
            URLConnection connection = loc.openConnection();
            connection.setConnectTimeout(60000);
            connection.setReadTimeout(60000);
            for (String[] header: headers) {
                connection.setRequestProperty(header[0], header[1]);
            }
            FileUtils.copyInputStreamToFile(connection.getInputStream(), file);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
