package com.alebit.sget.decrypt;

import com.alebit.sget.data.Header;
import com.alebit.sget.playlist.HLSPlaylist;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;

/**
 * Created by Alec on 2016/6/27.
 */
public class HLSDecrypter {
    boolean raw;

    public HLSDecrypter(Path path, boolean raw) {
        this.raw = raw;
        String filename = setFilename(path);
        Path playlistPath = path.getParent().resolve(filename + "_part").resolve(filename + ".m3u8");
        Header[] headers = new Header[0];
        try {
            HLSPlaylist playlist = new HLSPlaylist(playlistPath.toUri(), headers);

            // Load keys
            List<URI> keyUris = playlist.getVideoSegmentKeyList();
            byte[][] keys = new byte[keyUris.size()][];
            for (int i = 0; i < keyUris.size(); i++) {
                try {
                    File file = new File(keyUris.get(i));
                    FileInputStream fileInputStream = new FileInputStream(file);
                    keys[i] = new byte[(int) file.length()];
                    fileInputStream.read(keys[i]);
                    fileInputStream.close();
                } catch (Exception e) {
                    System.err.println("Cannot get key.");
                    System.exit(-1);
                }
            }

            try (FileOutputStream outputStream = new FileOutputStream(path.toFile())) {
                DecryptManager decryptManager = new DecryptManager();
                // A workaround to deal with the bug in m3u8-parser that
                // EXT-X-KEY tag only applies to the first segment after it
                int previousKeyId = -1;
                int previousKeyIndex = -1;
                for (int i = 0; i < playlist.getVideoSegmentsSize(); i++) {
                    int keyId = playlist.getVideoSegmentKeyId(i);
                    if (keyId == -1 && previousKeyId == -1) {
                        try (FileInputStream inputStream = new FileInputStream(new File(playlist.getVideoSegmentURI(i)))) {
                            IOUtils.copyLarge(inputStream, outputStream);
                        } catch (IOException e) {
                            System.err.println("Cannot read cached video");
                            e.printStackTrace();
                            System.exit(-1);
                        }
                    } else {
                        previousKeyId = keyId == -1? previousKeyId: keyId;
                        previousKeyIndex = keyId == -1? previousKeyIndex: i;
                        decryptManager.setSecret(keys[previousKeyId], playlist.getVideoSegmentKeyIv(previousKeyIndex));
                        try (FileInputStream inputStream = new FileInputStream(new File(playlist.getVideoSegmentURI(i)))) {
                            decryptManager.decrypt(inputStream, outputStream);
                        } catch (IOException e) {
                            System.err.println("Cannot read cached video");
                            e.printStackTrace();
                            System.exit(-1);
                        }
                    }
                }
            } catch (IOException e) {
                System.err.println("Cannot write video");
                e.printStackTrace();
                System.exit(-1);
            }

            try {
                if (!raw) {
                    System.out.println("Deleteing temp file...");
                    FileUtils.deleteDirectory(playlistPath.getParent().toFile());
                }
            } catch (Exception e) {
                System.err.println("Delete " + playlistPath.getParent() + " failed");
                e.printStackTrace();
                System.exit(-1);
            }
            System.out.println("Successfully download video!");
        } catch (IOException e) {
            System.err.println("Cannot open playlist");
            System.exit(-1);
        }
    }

    private String setFilename(Path path) {
        String filename;
        int dotSite = path.getFileName().toString().lastIndexOf(".");
        if (dotSite > 0) {
            filename = path.getFileName().toString().substring(0, dotSite);
        } else {
            filename = path.getFileName().toString();
        }
        return filename;
    }
}
