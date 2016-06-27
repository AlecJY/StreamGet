package com.alebit.hlsdownloader.decrypt;

import com.alebit.hlsdownloader.playlist.PlaylistManager;
import com.iheartradio.m3u8.data.TrackData;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Created by Alec on 2016/6/27.
 */
public class HLSDecrypter {
    List<TrackData> tracks;

    public HLSDecrypter(Path path) {
        String filename = setFilename(path);
        Path playlistPath = Paths.get(path.getParent().toString() + File.separator + filename + "_part" + File.separator + filename + ".m3u8");
        PlaylistManager playlistManager = new PlaylistManager(playlistPath.toFile());
        tracks = playlistManager.getTracks();
        if (tracks.get(0).hasEncryptionData()) {
            DecryptManager decryptManager = new DecryptManager();
            if (playlistManager.getIV() != null) {
                decryptManager.setSecret(new File(playlistManager.getPreURL() + tracks.get(0).getEncryptionData().getUri()), playlistManager.getIV());
            } else {
                decryptManager.setSecret(new File(playlistManager.getPreURL() + tracks.get(0).getEncryptionData().getUri()));
            }
            for (TrackData trackData : tracks) {
                decryptManager.decrypt(new File(playlistManager.getPreURL() + trackData.getUri()), path.toFile(), true);
            }
        } else {

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
