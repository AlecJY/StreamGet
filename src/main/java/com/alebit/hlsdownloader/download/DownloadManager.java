package com.alebit.hlsdownloader.download;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

/**
 * Created by Alec on 2016/6/26.
 */
public class DownloadManager {
    public boolean download(String url, String filePath) {
        try {
            URL loc = new URL(url);
            String[] urlArr = url.split("/");
            String filename = urlArr[urlArr.length - 1];
            int dotSite = filename.lastIndexOf("?");
            if (dotSite > 0) {
                filename = filename.substring(0, dotSite);
            }
            File file = new File(filePath + filename);
            FileUtils.copyURLToFile(loc, file, 20000, 20000);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
