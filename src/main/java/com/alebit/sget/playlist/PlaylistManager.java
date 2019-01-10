package com.alebit.sget.playlist;

import com.alebit.sget.playlist.DASH.DASHPlaylistManager;
import com.iheartradio.m3u8.Encoding;
import com.iheartradio.m3u8.Format;
import com.iheartradio.m3u8.ParsingMode;
import com.iheartradio.m3u8.PlaylistParser;
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
    private String playlistURL;
    private String preURL;
    private Playlist playlist;
    private MasterPlaylist masterPlaylist;
    private MediaPlaylist mediaPlaylist;
    private List<PlaylistData> masterPlaylistData;
    private byte[] iv;
    private int version;
    private boolean playlistType = false; // false: m3u8 true: MPEG-DASH
    private DASHPlaylistManager dashPlaylistManager;

    public PlaylistManager(String url, ArrayList<String[]> headers) {
        playlistURL = url.replace("https", "http");
        try {
            URLConnection connection = new URL(playlistURL).openConnection();
            for (String[] header: headers) {
                connection.setRequestProperty(header[0], header[1]);
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

            setPreURL(playlistURL);

            setIV();
        } catch (IOException e) {
            System.err.println("Invalid URL: " + url);
            System.exit(-1);
        } catch (com.iheartradio.m3u8.ParseException e) {
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
        playlistURL = file.getPath();
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

    public List<Subtitle> getSubtitles() {
        ArrayList<Subtitle> subtitles = new ArrayList<>();
        if (hasMasterPlaylist()) {
            for (MediaData mediaData: masterPlaylist.getMediaData()) {
                if (mediaData.getGroupId().equals("subs")) {
                    Subtitle subtitle = new Subtitle(mediaData.getName(), mediaData.getLanguage(), removeDotDot(getPreURL(), mediaData.getUri()));
                    subtitles.add(subtitle);
                }
            }
        }
        return subtitles;
    }

    private String removeDotDot(String pUrl, String uri) {
        try {
            while (uri.startsWith("../")) {
                pUrl = pUrl.substring(0, pUrl.substring(0, pUrl.length() - 1).lastIndexOf("/") + 1);
                uri = uri.substring(uri.indexOf("/") + 1);
            }
        } catch (Exception e) {
            System.err.println("Invalid URL: " + pUrl + uri);
        }
        return pUrl + uri;
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

    public boolean isDASH() {
        return playlistType;
    }

    public DASHPlaylistManager getDASHPlaylist() {
        return dashPlaylistManager;
    }
}
