package com.alebit.sget.decrypt;

import com.alebit.sget.playlist.PlaylistManager;
import com.iheartradio.m3u8.data.TrackData;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.List;

/**
 * Created by Alec on 2016/6/27.
 */
public class HLSDecrypter {
    List<TrackData> tracks;
    boolean raw;

    public HLSDecrypter(Path path, boolean raw) {
        this.raw = raw;
        String filename = setFilename(path);
        Path playlistPath = path.getParent().resolve(filename + "_part").resolve(filename + ".m3u8");
        PlaylistManager playlistManager = new PlaylistManager(playlistPath.toFile());
        tracks = playlistManager.getTracks();
        if (tracks.get(0).hasEncryptionData()) {
            DecryptManager decryptManager = new DecryptManager();
            if (playlistManager.getIV() != null) {
                decryptManager.setSecret(new File(playlistManager.resolveURI(tracks.get(0).getEncryptionData().getUri())), playlistManager.getIV());
            } else {
                decryptManager.setSecret(new File(playlistManager.resolveURI(tracks.get(0).getEncryptionData().getUri())));
            }
            for (int i = 0; i < tracks.size(); i++) {
                TrackData trackData = tracks.get(i);
                decryptManager.decrypt(new File(playlistManager.resolveURI(trackData.getUri())), path.toFile(), true);
            }
        } else {
            for (int i = 0; i < tracks.size(); i++) {
                TrackData trackData = tracks.get(i);
                try (FileInputStream fileInputStream = new FileInputStream(new File(playlistManager.resolveURI(trackData.getUri())));
                     FileOutputStream fileOutputStream = new FileOutputStream(path.toFile(), true)) {
                    IOUtils.copyLarge(fileInputStream, fileOutputStream);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        try {
            if (!raw) {
                playlistManager.close();
                System.out.println("Deleteing temp file...");
                FileUtils.deleteDirectory(playlistPath.getParent().toFile());
            }
        } catch (Exception e) {
            System.err.println("Delete " + playlistPath.getParent() + " failed");
            e.printStackTrace();
            System.exit(-1);
        }
        System.out.println("Successfully download video!");
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
