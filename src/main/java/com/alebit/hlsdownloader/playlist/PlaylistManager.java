package com.alebit.hlsdownloader.playlist;

import com.iheartradio.m3u8.Encoding;
import com.iheartradio.m3u8.Format;
import com.iheartradio.m3u8.PlaylistParser;
import com.iheartradio.m3u8.data.*;

import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Alec on 2016/6/26.
 */
public class PlaylistManager {
    private String playlistURL;
    private String preURL;
    private Playlist playlist;
    private MasterPlaylist masterPlaylist;
    private MediaPlaylist mediaPlaylist;
    private List<PlaylistData> masterPlaylistData;
    private byte[] iv;
    private int version;

    public PlaylistManager(String url) {
        playlistURL = url;
        try {
            InputStream inputStream = new URL(playlistURL).openStream();
            PlaylistParser parser = new PlaylistParser(inputStream, Format.EXT_M3U, Encoding.UTF_8);
            playlist = parser.parse();

            version = playlist.getCompatibilityVersion();
            masterPlaylist = playlist.getMasterPlaylist();
            if (masterPlaylist != null) {
                masterPlaylistData = masterPlaylist.getPlaylists();
            }
            mediaPlaylist = playlist.getMediaPlaylist();

            setPreURL(playlistURL);

            setIV();
        } catch (IOException e) {
            System.err.println("Invalid URL: " + e.getMessage());
            System.exit(-1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public PlaylistManager(File file) {
        playlistURL = file.getPath();
        try {
            InputStream inputStream = new FileInputStream(file);
            PlaylistParser parser = new PlaylistParser(inputStream, Format.EXT_M3U, Encoding.UTF_8);
            playlist = parser.parse();

            version = playlist.getCompatibilityVersion();
            masterPlaylist = playlist.getMasterPlaylist();
            if (masterPlaylist != null) {
                masterPlaylistData = masterPlaylist.getPlaylists();
            }
            mediaPlaylist = playlist.getMediaPlaylist();

            int dotSite = playlistURL.lastIndexOf(File.separator);
            if (dotSite > 0) {
                preURL = playlistURL.substring(0, ++dotSite);
            } else {
                System.err.println("File path format error");
                System.exit(-1);
            }

            setIV();
        } catch (IOException e) {
            System.err.println("Invalid URL: " + e.getMessage());
            System.exit(-1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<Resolution> getPlaylistResolution() {
        List<Resolution> resolutionList = new ArrayList();
        if (masterPlaylist != null) {
            for (PlaylistData playlistData: masterPlaylistData) {
                if (playlistData.hasStreamInfo()) {
                    resolutionList.add(playlistData.getStreamInfo().getResolution());
                } else {
                    resolutionList.add(null);
                }
            }
        }
        return resolutionList;
    }

    public PlaylistData getPlaylistData(int index) {
        return masterPlaylistData.get(index);
    }

    public String getPreURL() {
        return preURL;
    }

    public boolean hasMasterPlaylist() {
        if (masterPlaylist == null) {
            return false;
        } else {
            return true;
        }
    }

    public boolean hasMediaPlaylist() {
        if (mediaPlaylist == null) {
            return false;
        } else {
            return true;
        }
    }

    public List<TrackData> getTracks() {
        return mediaPlaylist.getTracks();
    }

    public MediaPlaylist getMediaPlaylist() {
        return mediaPlaylist;
    }

    public int getPlaylistVersion() {
        return version;
    }

    public byte[] getIV() {
        return iv;
    }

    private void setPreURL(String playlistURL) {
        int dotSite = playlistURL.lastIndexOf("/");
        if (dotSite > 0) {
            preURL = playlistURL.substring(0, ++dotSite);
        } else {
            System.err.println("URL format error");
            System.exit(-1);
        }
    }

    private void setIV() {
        if (hasMediaPlaylist()) {
            if (mediaPlaylist.getTracks().get(0).hasEncryptionData()) {
                if (mediaPlaylist.getTracks().get(0).getEncryptionData().hasInitializationVector()) {
                    iv = new byte[mediaPlaylist.getTracks().get(0).getEncryptionData().getInitializationVector().size()];
                    for (int i = 0; i < iv.length; i++) {
                        iv[i] = mediaPlaylist.getTracks().get(0).getEncryptionData().getInitializationVector().get(i);
                    }
                }
            }
        }
    }
}
