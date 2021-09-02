package com.alebit.sget.playlist;

import com.alebit.sget.data.Header;
import com.alebit.sget.playlist.DASH.DASHPlaylistManager;
import com.iheartradio.m3u8.*;
import com.iheartradio.m3u8.data.*;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Alec on 2016/6/26.
 */
public class PlaylistManager {
    private InputStream inputStream;
    private ByteArrayOutputStream outputStream;
    private URI playlistURI;
    private Playlist playlist;
    private MasterPlaylist masterPlaylist;
    private MediaPlaylist mediaPlaylist;
    private List<PlaylistData> masterPlaylistData;
    private byte[] iv;
    private int version;
    private boolean playlistType = false; // false: m3u8 true: MPEG-DASH
    private DASHPlaylistManager dashPlaylistManager;

    public PlaylistManager(String url, Header[] headers) {
        try {
            playlistURI = new URI(url);
            URLConnection connection = playlistURI.toURL().openConnection();
            for (Header header: headers) {
                connection.setRequestProperty(header.getName(), header.getValue());
            }
            inputStream = connection.getInputStream();
            outputStream = new ByteArrayOutputStream();
            IOUtils.copy(inputStream, outputStream);
            inputStream = new ByteArrayInputStream(outputStream.toByteArray());
            PlaylistParser parser = new PlaylistParser(inputStream, Format.EXT_M3U, Encoding.UTF_8, ParsingMode.LENIENT);
            playlist = parser.parse();

            version = playlist.getCompatibilityVersion();
            masterPlaylist = playlist.getMasterPlaylist();
            if (masterPlaylist != null) {
                masterPlaylistData = masterPlaylist.getPlaylists();
            }
            mediaPlaylist = playlist.getMediaPlaylist();

            setIV();
        } catch (IOException e) {
            System.err.println("Invalid URL: " + url);
            System.exit(-1);
        } catch (ParseException e) {
            if (e.getInput().startsWith("<?xml")) {
                playlistType = true;
                inputStream = new ByteArrayInputStream(outputStream.toByteArray());
                dashPlaylistManager = new DASHPlaylistManager(inputStream);
                dashPlaylistManager.setURI(url);
            } else {
                e.printStackTrace();
                System.err.println("Invalid m3u8 playlist");
                System.exit(-1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public PlaylistManager(File file) {
        playlistURI = file.toURI();
        try {
            inputStream = new FileInputStream(file);
            PlaylistParser parser = new PlaylistParser(inputStream, Format.EXT_M3U, Encoding.UTF_8, ParsingMode.LENIENT);
            playlist = parser.parse();

            version = playlist.getCompatibilityVersion();
            masterPlaylist = playlist.getMasterPlaylist();
            if (masterPlaylist != null) {
                masterPlaylistData = masterPlaylist.getPlaylists();
            }
            mediaPlaylist = playlist.getMediaPlaylist();

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

    public List<Subtitle> getSubtitles() {
        ArrayList<Subtitle> subtitles = new ArrayList<>();
        if (hasMasterPlaylist()) {
            for (MediaData mediaData: masterPlaylist.getMediaData()) {
                if (mediaData.getGroupId().equals("subs")) {
                    String subtitleURI = mediaData.getUri();
                    Subtitle subtitle = new Subtitle(mediaData.getName(), mediaData.getLanguage(), subtitleURI);
                    subtitles.add(subtitle);
                }
            }
        }
        return subtitles;
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

    public void close() {
        try {
            inputStream.close();
        } catch (IOException e) {

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

    public boolean isDASH() {
        return playlistType;
    }

    public URI resolveURI(String uri) {
        return playlistURI.resolve(uri);
    }

    public DASHPlaylistManager getDASHPlaylist() {
        return dashPlaylistManager;
    }
}
