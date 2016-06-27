package com.alebit.hlsdownloader.download;

import com.alebit.hlsdownloader.playlist.PlaylistManager;
import com.iheartradio.m3u8.Encoding;
import com.iheartradio.m3u8.Format;
import com.iheartradio.m3u8.PlaylistWriter;
import com.iheartradio.m3u8.data.EncryptionData;
import com.iheartradio.m3u8.data.MediaPlaylist;
import com.iheartradio.m3u8.data.Playlist;
import com.iheartradio.m3u8.data.TrackData;
import org.apache.commons.io.FileUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Alec on 2016/6/26.
 */
public class HLSDownloader {
    private PlaylistManager playlistManager;
    private List<TrackData> tracks;
    private Path path;
    private Path partPath;
    private Path jsonPath;
    private String filename;
    private JSONObject statusJSON;
    private int progress = -1;

    public HLSDownloader(PlaylistManager playlistManager, Path path) {
        this.playlistManager = playlistManager;
        tracks = playlistManager.getTracks();
        this.path = path;
        int dotSite = path.getFileName().toString().lastIndexOf(".");
        if (dotSite > 0) {
            filename = path.getFileName().toString().substring(0, dotSite);
        } else {
            filename = path.getFileName().toString();
        }
        prepare();
    }

    public void prepare() {
        try {
            path.getParent().toFile().mkdirs();
            jsonPath = Paths.get(path.getParent().toString() + File.separator + filename + "_part" + File.separator + "status.json");
            jsonPath.getParent().toFile().mkdirs();
            partPath = jsonPath.getParent();
            Path playlistPath = Paths.get(path.getParent().toString() + File.separator + filename + "_part" + File.separator + filename + ".m3u8");
            writeToPlaylist(playlistPath);
            statusJSON = readJSON(jsonPath);
            if (statusJSON != null) {
                try {
                    long CRC = FileUtils.checksumCRC32(playlistPath.toFile());
                    if (statusJSON.containsKey("CRC32")) {
                        long oldCRC = (long) statusJSON.get("CRC32");
                        statusJSON.replace("CRC32", CRC);
                        if (CRC == oldCRC) {
                            progress = (int) (long) statusJSON.get("Progress");
                        }
                    } else {
                        statusJSON.put("CRC32", CRC);
                    }
                } catch (Exception e) {
                }
            }
            BufferedWriter videoWriter = new BufferedWriter(new FileWriter(path.toFile(), true));
            BufferedWriter jsonWriter = new BufferedWriter(new FileWriter(jsonPath.toFile(), true));
            jsonWriter.close();
            downloadHLS(progress);
        } catch (IOException e) {
            System.out.println("Write permission not allowed: " + e.getMessage());
            System.exit(-1);
        }
    }

    private JSONObject readJSON(Path jsonPath) {
        try {
            JSONParser parser = new JSONParser();
            return (JSONObject) parser.parse(new FileReader(jsonPath.toFile()));
        } catch (FileNotFoundException e) {
        } catch (ParseException e) {
            System.out.println("status.json is broken. Create new file.");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void writeToPlaylist(Path path) {
        try {
            MediaPlaylist mediaPlaylist = playlistManager.getMediaPlaylist();
            mediaPlaylist = modifyMediaPlaylist(mediaPlaylist);
            OutputStream playlistStream = new FileOutputStream(path.toFile());
            PlaylistWriter playlistWriter = new PlaylistWriter(playlistStream, Format.EXT_M3U, Encoding.UTF_8);
            Playlist playlist = new Playlist.Builder().withCompatibilityVersion(playlistManager.getPlaylistVersion()).withMediaPlaylist(mediaPlaylist).build();
            playlistWriter.write(playlist);
            playlistStream.close();
        } catch (IOException e) {
            System.out.println("Write permission not allowed: " + e.getMessage());
            System.exit(-1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private MediaPlaylist modifyMediaPlaylist(MediaPlaylist mediaPlaylist) {
        List<TrackData> tracks = mediaPlaylist.getTracks();
        List<TrackData> modifiedTracks = new ArrayList<TrackData>();
        for (TrackData trackData: tracks) {
            String uri = trackData.getUri();
            int dotSite = uri.lastIndexOf("?");
            if (dotSite > 0) {
                uri = uri.substring(0, dotSite);
            }
            if (trackData.hasEncryptionData()) {
                EncryptionData encryptionData = trackData.getEncryptionData();
                String[] filenameArr = encryptionData.getUri().split("/");
                encryptionData = encryptionData.buildUpon().withUri(filenameArr[filenameArr.length-1]).build();
                trackData = trackData.buildUpon().withUri(uri).withEncryptionData(encryptionData).build();
            } else {
                trackData = trackData.buildUpon().withUri(uri).build();
            }
            modifiedTracks.add(trackData);
        }
        mediaPlaylist = mediaPlaylist.buildUpon().withTracks(modifiedTracks).build();
        return mediaPlaylist;
    }

    private void downloadHLS(int index) {
        DownloadManager downloadManager = new DownloadManager();
        for (int i = index + 1; i < tracks.size(); i++) {
            System.out.println("Downloading video: " + Math.round(i / tracks.size()) + "% (" + (i+1) + "/" + tracks.size() + ")");
            boolean downStatus = downloadManager.download(playlistManager.getPreURL() + tracks.get(i).getUri(), partPath.toString() + File.separator);
            if (!downStatus) {
                System.out.println("Download Failed. Please try again later.");
                System.exit(-1);
            }
            setProgress(i);
        }
    }

    private void setProgress(int index) {
        try {
            if (statusJSON == null) {
                statusJSON = new JSONObject();
            }
            if (statusJSON.containsKey("Progress")) {
                statusJSON.replace("Progress", index);
            } else {
                statusJSON.put("Progress", index);
            }
            BufferedWriter jsonWriter = new BufferedWriter(new FileWriter(jsonPath.toFile()));
            jsonWriter.write(statusJSON.toJSONString());
            jsonWriter.flush();
            jsonWriter.close();
        } catch (IOException e) {
            System.out.println("Cannot open status.json");
        }
    }
}
