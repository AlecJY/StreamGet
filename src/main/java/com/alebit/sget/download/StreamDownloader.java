package com.alebit.sget.download;

import com.alebit.sget.Utils;
import com.alebit.sget.data.Header;
import com.alebit.sget.data.Status;
import com.alebit.sget.decrypt.HLSDecrypter;
import com.alebit.sget.playlist.dash.DASHPlaylistManager;
import com.alebit.sget.playlist.PlaylistManager;
import com.alebit.sget.playlist.Subtitle;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.iheartradio.m3u8.Encoding;
import com.iheartradio.m3u8.Format;
import com.iheartradio.m3u8.PlaylistWriter;
import com.iheartradio.m3u8.data.EncryptionData;
import com.iheartradio.m3u8.data.MediaPlaylist;
import com.iheartradio.m3u8.data.Playlist;
import com.iheartradio.m3u8.data.TrackData;

import java.io.*;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Created by Alec on 2016/6/26.
 */
public class StreamDownloader {
    private PlaylistManager playlistManager;
    private DASHPlaylistManager dashPlaylistManager;
    private Header[] headers;
    private List<TrackData> tracks;
    private List<Subtitle> subtitles;
    private Path path;
    private Path partPath;
    private Path jsonPath;
    private String filename;
    private Status status;
    private int progress = -1;
    private boolean raw;

    public StreamDownloader(PlaylistManager playlistManager, List<Subtitle> subtitles, Header[] headers, Path path, boolean raw) {
        if (path.getParent() == null) {
            path = Paths.get("." + File.separator + path);
        }
        if (playlistManager.isDASH()) {
            dashPlaylistManager = playlistManager.getDASHPlaylist();
        }
        this.raw = raw;
        this.playlistManager = playlistManager;
        if (!playlistManager.isDASH()) {
            tracks = playlistManager.getTracks();
        }
        this.subtitles = subtitles;
        this.headers = headers;
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
            if (playlistManager.isDASH()) {
                jsonPath = path.getParent().resolve(filename).resolve("status.json");
            } else {
                jsonPath = path.getParent().resolve(filename + "_part").resolve("status.json");
            }
            jsonPath.getParent().toFile().mkdirs();
            partPath = jsonPath.getParent();
            Path playlistPath;
            if (playlistManager.isDASH()) {
                playlistPath = jsonPath.getParent().resolve(filename + ".mpd");
                writeToDashPlaylist(playlistPath);
            } else {
                playlistPath = path.getParent().resolve(filename + "_part").resolve(filename + ".m3u8");
                writeToPlaylist(playlistPath);
            }

            status = readJSON(jsonPath);
            Boolean shaMatch = false;
            if (status != null) {
                try {
                    MessageDigest digest = MessageDigest.getInstance("SHA-256");
                    byte[] sha = digest.digest(Files.readAllBytes(playlistPath));
                    if (Arrays.equals(sha, status.getSha256())) {
                        shaMatch = true;
                        progress = status.getProgress();
                    }
                } catch (NoSuchAlgorithmException e) {
                    System.err.println("Cannot use SHA-256. File checker disabled.");
                } catch (Exception e) {
                }
            }
            if (!shaMatch && !playlistManager.isDASH() && path.toFile().exists()) {
                System.out.println("File exists. Continue?(Y/N)");
                Scanner scanner = new Scanner(System.in);
                String chosen = scanner.next();
                if (!(chosen.toLowerCase().equals("y") || chosen.toLowerCase().equals("yes"))) {
                    System.exit(0);
                }
            }
            byte[] sha = new byte[0];
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                sha = digest.digest(Files.readAllBytes(playlistPath));
            } catch (NoSuchAlgorithmException e) {
                System.err.println("Cannot use SHA-256. File checker disabled.");
            }
            if (status == null) {
                status = new Status();
            }
            status.setSha256(sha);
            BufferedWriter videoWriter = null;
            if (!playlistManager.isDASH()) {
                videoWriter = new BufferedWriter(new FileWriter(path.toFile(), false));
            }
            BufferedWriter jsonWriter = new BufferedWriter(new FileWriter(jsonPath.toFile(), true));
            jsonWriter.close();
            if (subtitles != null) {
                downloadSubtitles();
            }
            if (playlistManager.isDASH()) {
                downloadDASH(progress);
                jsonPath.toFile().delete();
                System.out.println("Successfully download video!");
            } else {
                downloadHLS(progress);
                videoWriter.close();
                System.out.println("Converting video...");
                new HLSDecrypter(path, raw);
            }
        } catch (IOException e) {
            System.err.println("Write permission not allowed: " + e.getMessage());
            System.exit(-1);
        }
    }

    private Status readJSON(Path jsonPath) {
        try {
            Status status = Utils.getJsonObjectMapper().readValue(jsonPath.toFile(), Status.class);
            if (status.getSha256() == null) {
                System.out.println("\nstatus.json is broken. Create new file.");
                return null;
            }
            return status;
        } catch (FileNotFoundException e) {
        } catch (JsonProcessingException e) {
            System.out.println("\nstatus.json is broken. Create new file.");
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
            System.err.println("Write permission not allowed: " + e.getMessage());
            System.exit(-1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void writeToDashPlaylist(Path path) {
        dashPlaylistManager.saveManifest(path);
    }

    private MediaPlaylist modifyMediaPlaylist(MediaPlaylist mediaPlaylist) {
        List<TrackData> tracks = mediaPlaylist.getTracks();
        List<TrackData> modifiedTracks = new ArrayList<TrackData>();
        for (TrackData trackData: tracks) {
            String uri = trackData.getUri();
            int dotSite = uri.lastIndexOf("?");
            if (dotSite > 0) {
                uri = uri.substring(0, dotSite);
                uri = uri.substring(uri.lastIndexOf("/") + 1);
            } else {
                uri = uri.substring(uri.lastIndexOf("/") + 1);
            }
            if (trackData.hasEncryptionData()) {
                EncryptionData encryptionData = trackData.getEncryptionData();
                String filename;
                if (encryptionData.getUri().contains("?")) {
                    filename = encryptionData.getUri().substring(0, encryptionData.getUri().lastIndexOf("?"));
                    filename = filename.substring(filename.lastIndexOf("/") + 1);
                } else {
                    filename = encryptionData.getUri().substring(encryptionData.getUri().lastIndexOf("/") + 1);
                }
                encryptionData = encryptionData.buildUpon().withUri(filename).build();
                trackData = trackData.buildUpon().withUri(uri).withEncryptionData(encryptionData).build();
            } else {
                trackData = trackData.buildUpon().withUri(uri).build();
            }
            modifiedTracks.add(trackData);
        }

        mediaPlaylist = mediaPlaylist.buildUpon().withTracks(modifiedTracks).build();
        return mediaPlaylist;
    }

    private void downloadSubtitles() {
        DownloadManager downloadManager = new DownloadManager(headers);
        boolean downStatus;
        for (Subtitle subtitle: subtitles) {
            PlaylistManager subtitlePlaylist = new PlaylistManager(subtitle.getUri(), headers);
            if (!subtitlePlaylist.hasMediaPlaylist()) {
                System.err.println("Unsupported subtitle format");
                System.exit(-1);
            }
            if (subtitlePlaylist.getTracks().size() != 1) {
                System.err.println("Unsupported subtitle format");
                System.exit(-1);
            }
            String uri = subtitlePlaylist.getTracks().get(0).getUri();
            if (uri.contains("?")) {
                uri = uri.substring(0, uri.indexOf("?"));
            }
            uri = uri.substring(uri.lastIndexOf("/") + 1);
            String ext = "";
            if (uri.contains(".")) {
                ext = uri.substring(uri.lastIndexOf("."));
            }
            System.out.println("Downloading subtitle \"" + filename + "." + subtitle.getName() + ext + "\"");
            URI subtitleURI = subtitlePlaylist.resolveURI(subtitlePlaylist.getTracks().get(0).getUri());
            downStatus = downloadManager.download(subtitleURI, path.getParent(), filename + "." + subtitle.getName() + ext);
            if (!downStatus) {
                System.err.println("Download Failed. Please try again later.");
                System.exit(-1);
            }
        }
    }

    private void downloadHLS(int index) {
        DownloadManager downloadManager = new DownloadManager(headers);
        if (tracks.get(0).hasEncryptionData()) {
            boolean hasKey = status.hasKey();
            if (!hasKey) {
                System.out.println("Downloading key...");
                boolean downs = downloadManager.download(playlistManager.resolveURI(tracks.get(0).getEncryptionData().getUri()), partPath);
                if (!downs) {
                    System.err.println("Download Failed. Please try again later.");
                    System.exit(-1);
                }
                setKeyProgress();
            }
        }
        for (int i = index + 1; i < tracks.size(); i++) {
            System.out.println("Downloading video: " + Math.round(((float)i / (float)tracks.size())*100) + "% (" + (i+1) + "/" + tracks.size() + ")");
            boolean downStatus = downloadManager.download(playlistManager.resolveURI(tracks.get(i).getUri()), partPath);
            if (!downStatus) {
                System.err.println("Download Failed. Please try again later.");
                System.exit(-1);
            }
            setProgress(i);
        }
    }

    private void downloadDASH(int index) {
        DownloadManager downloadManager = new DownloadManager(headers);
        if (dashPlaylistManager.getAudioInitializationURI() != null) {
            boolean downStatus = downloadManager.download(dashPlaylistManager.getAudioInitializationURI(), partPath.resolve(dashPlaylistManager.audioID()));
            if (!downStatus) {
                System.err.println("Download Failed. Please try again later.");
                System.exit(-1);
            }
        }
        if (dashPlaylistManager.getVideoInitializationURI() != null) {
            boolean downStatus = downloadManager.download(dashPlaylistManager.getVideoInitializationURI(), partPath.resolve(dashPlaylistManager.videoID()));
            if (!downStatus) {
                System.err.println("Download Failed. Please try again later.");
                System.exit(-1);
            }
        }
        int audioSegNum = dashPlaylistManager.getAudioSegNumber();
        int videoSegNum = dashPlaylistManager.getVideoSegNumber();
        int segNum = audioSegNum > videoSegNum? audioSegNum: videoSegNum;
        for (int i = index + 1; i < segNum; i++) {
            System.out.println("Downloading video: " + Math.round(((float)i / (float)segNum)*100) + "% (" + (i+1) + "/" + segNum + ")");
            if (i < audioSegNum) {
                boolean downStatus = downloadManager.download(dashPlaylistManager.getAudioSegURI(i), partPath.resolve(dashPlaylistManager.audioID()));
                if (!downStatus) {
                    System.err.println("Download Failed. Please try again later.");
                    System.exit(-1);
                }
            }
            if (i < videoSegNum) {
                boolean downStatus = downloadManager.download(dashPlaylistManager.getVideoSegURI(i), partPath.resolve(dashPlaylistManager.videoID()));
                if (!downStatus) {
                    System.err.println("Download Failed. Please try again later.");
                    System.exit(-1);
                }
            }
            setProgress(i);
        }
    }

    private void setProgress(int index) {
        try {
            status.setProgress(index);
            Utils.getJsonObjectMapper().writeValue(jsonPath.toFile(), status);
        } catch (IOException e) {
            System.out.println("Cannot open status.json");
        }
    }

    private void setKeyProgress() {
        try {
            status.hasKey(true);
            Utils.getJsonObjectMapper().writeValue(jsonPath.toFile(), status);
        } catch (IOException e) {
            System.out.println("Cannot open status.json");
        }
    }
}
