package com.alebit.sget.download;

import com.alebit.sget.decrypt.HLSDecrypter;
import com.alebit.sget.playlist.DASH.DASHPlaylistManager;
import com.alebit.sget.playlist.PlaylistManager;
import com.alebit.sget.playlist.Subtitle;
import com.iheartradio.m3u8.Encoding;
import com.iheartradio.m3u8.Format;
import com.iheartradio.m3u8.PlaylistWriter;
import com.iheartradio.m3u8.data.EncryptionData;
import com.iheartradio.m3u8.data.MediaPlaylist;
import com.iheartradio.m3u8.data.Playlist;
import com.iheartradio.m3u8.data.TrackData;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Scanner;

/**
 * Created by Alec on 2016/6/26.
 */
public class StreamDownloader {
    private PlaylistManager playlistManager;
    private DASHPlaylistManager dashPlaylistManager;
    private List<TrackData> tracks;
    private List<Subtitle> subtitles;
    private Path path;
    private Path partPath;
    private Path jsonPath;
    private String filename;
    private JSONObject statusJSON;
    private int progress = -1;
    private boolean raw;

    public StreamDownloader(PlaylistManager playlistManager, List<Subtitle> subtitles, Path path, boolean raw) {
        if (path.getParent() == null) {
            path = Paths.get("." + File.separator + path.toString());
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
                jsonPath = Paths.get(path.getParent().toString() + File.separator + filename + File.separator + "status.json");
            } else {
                jsonPath = Paths.get(path.getParent().toString() + File.separator + filename + "_part" + File.separator + "status.json");
            }
            jsonPath.getParent().toFile().mkdirs();
            partPath = jsonPath.getParent();
            Path playlistPath;
            if (playlistManager.isDASH()) {
                playlistPath = Paths.get(jsonPath.getParent().toString() + File.separator + filename + ".mpd");
                writeToDashPlaylist(playlistPath);
            } else {
                playlistPath = Paths.get(path.getParent().toString() + File.separator + filename + "_part" + File.separator + filename + ".m3u8");
                writeToPlaylist(playlistPath);
            }

            statusJSON = readJSON(jsonPath);
            if (statusJSON != null) {
                try {
                    MessageDigest digest = MessageDigest.getInstance("SHA-256");
                    byte[] shaByte = digest.digest(Files.readAllBytes(playlistPath));
                    String sha = Base64.getEncoder().encodeToString(shaByte);
                    if (statusJSON.containsKey("SHA256")) {
                        String oldSha = (String) statusJSON.get("SHA256");
                        if (sha.equals(oldSha)) {
                            progress = (int) (long) statusJSON.get("Progress");
                        } else {
                            System.out.println("File exists. Continue?(Y/N)");
                            Scanner scanner = new Scanner(System.in);
                            String chosen = scanner.nextLine();
                            if (chosen.toLowerCase().equals("y") || chosen.toLowerCase().equals("yes")) {
                                if (statusJSON.containsKey("Key")) {
                                    statusJSON.replace("Key", false);
                                }
                            } else {
                                System.exit(0);
                            }
                        }
                    } else {
                        System.out.println("File exists. Continue?(Y/N)");
                        Scanner scanner = new Scanner(System.in);
                        String chosen = scanner.nextLine();
                        if (chosen.toLowerCase().equals("y") || chosen.toLowerCase().equals("yes")) {
                            if (statusJSON.containsKey("Key")) {
                                statusJSON.replace("Key", false);
                            }
                        } else {
                            System.exit(0);
                        }
                        statusJSON.put("SHA256", sha);
                    }
                } catch (NoSuchAlgorithmException e) {
                    System.err.println("Cannot use SHA-256. File checker disabled.");
                } catch (Exception e) {
                }
            } else if (!playlistManager.isDASH() && path.toFile().exists()) {
                System.out.println("File exists. Continue?(Y/N)");
                Scanner scanner = new Scanner(System.in);
                String chosen = scanner.next();
                if (!(chosen.toLowerCase().equals("y") || chosen.toLowerCase().equals("yes"))) {
                    System.exit(0);
                }
            }
            String sha = "";
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] shaByte = digest.digest(Files.readAllBytes(playlistPath));
                sha = Base64.getEncoder().encodeToString(shaByte);
            } catch (NoSuchAlgorithmException e) {
                System.err.println("Cannot use SHA-256. File checker disabled.");
            }
            if (statusJSON == null) {
                statusJSON = new JSONObject();
                statusJSON.put("SHA256", sha);
            } else if (!statusJSON.containsKey("SHA256")) {
                statusJSON.put("SHA256", sha);
            } else {
                statusJSON.replace("SHA256", sha);
            }
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

    private JSONObject readJSON(Path jsonPath) {
        try {
            JSONParser parser = new JSONParser();
            return (JSONObject) parser.parse(new FileReader(jsonPath.toFile()));
        } catch (FileNotFoundException e) {
        } catch (ParseException e) {
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
                uri = uri.substring(uri.lastIndexOf("/") + 1, dotSite);
            } else {
                uri = uri.substring(uri.lastIndexOf("/") + 1, uri.length());
            }
            if (trackData.hasEncryptionData()) {
                EncryptionData encryptionData = trackData.getEncryptionData();
                String filename;
                if (encryptionData.getUri().contains("?")) {
                    filename = encryptionData.getUri().substring(encryptionData.getUri().lastIndexOf("/") + 1, encryptionData.getUri().lastIndexOf("?"));
                } else {
                    filename = encryptionData.getUri().substring(encryptionData.getUri().lastIndexOf("/") + 1, encryptionData.getUri().length());
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
        DownloadManager downloadManager = new DownloadManager();
        boolean downStatus;
        for (Subtitle subtitle: subtitles) {
            String uri;
            if (subtitle.getUri().contains("?")) {
                uri = subtitle.getUri().substring(0, subtitle.getUri().indexOf("?"));
            } else {
                uri = subtitle.getUri();
            }
            String ext = uri.substring(uri.lastIndexOf("."));
            System.out.println("Downloading subtitle \"" + filename + "." + subtitle.getName() + ext + "\"");
            PlaylistManager subtitlePlaylist = new PlaylistManager(subtitle.getUri());
            if (!subtitlePlaylist.hasMediaPlaylist()) {
                System.err.println("Unsupported subtitle format");
                System.exit(-1);
            }
            if (subtitlePlaylist.getTracks().size() != 1) {
                System.err.println("Unsupported subtitle format");
                System.exit(-1);
            }
            downStatus = downloadManager.download(subtitlePlaylist.getPreURL() + subtitlePlaylist.getTracks().get(0).getUri(), path.getParent().toString() + File.separator, filename + "." + subtitle.getName() + ext);
            if (!downStatus) {
                System.err.println("Download Failed. Please try again later.");
                System.exit(-1);
            }
        }
    }

    private void downloadHLS(int index) {
        DownloadManager downloadManager = new DownloadManager();
        if (tracks.get(0).hasEncryptionData()) {
            boolean status = false;
            boolean downs;
            if (statusJSON != null && statusJSON.containsKey("Key")) {
                try {
                    status = (boolean) statusJSON.get("Key");
                } catch (Exception e) {
                }
            }
            if (!status) {
                System.out.println("Downloading key...");
                if (tracks.get(0).getEncryptionData().getUri().contains("://")) {
                    downs = downloadManager.download(tracks.get(0).getEncryptionData().getUri(), partPath.toString() + File.separator);
                } else {
                    downs = downloadManager.download(playlistManager.getPreURL() + tracks.get(0).getEncryptionData().getUri(), partPath.toString() + File.separator);
                }
                if (!downs) {
                    System.err.println("Download Failed. Please try again later.");
                    System.exit(-1);
                }
                setKeyProgress(true);
            }
        }
        for (int i = index + 1; i < tracks.size(); i++) {
            System.out.println("Downloading video: " + Math.round(((float)i / (float)tracks.size())*100) + "% (" + (i+1) + "/" + tracks.size() + ")");
            boolean downStatus;
            if (tracks.get(i).getUri().contains("://")) {
                downStatus = downloadManager.download(tracks.get(i).getUri(), partPath.toString() + File.separator);
            } else {
                downStatus = downloadManager.download(playlistManager.getPreURL() + tracks.get(i).getUri(), partPath.toString() + File.separator);
            }
            if (!downStatus) {
                System.err.println("Download Failed. Please try again later.");
                System.exit(-1);
            }
            setProgress(i);
        }
    }

    private void downloadDASH(int index) {
        DownloadManager downloadManager = new DownloadManager();
        boolean downStatus = downloadManager.download(dashPlaylistManager.getAudioInitializationURI(), partPath.toString() + File.separator + dashPlaylistManager.audioID() + File.separator);
        if (!downStatus) {
            System.err.println("Download Failed. Please try again later.");
            System.exit(-1);
        }
        downStatus = downloadManager.download(dashPlaylistManager.getVideoInitializationURI(), partPath.toString() + File.separator + dashPlaylistManager.videoID() + File.separator);
        if (!downStatus) {
            System.err.println("Download Failed. Please try again later.");
            System.exit(-1);
        }
        int audioSegNum = dashPlaylistManager.getAudioSegNumber();
        int videoSegNum = dashPlaylistManager.getVideoSegNumber();
        int segNum = audioSegNum > videoSegNum? audioSegNum: videoSegNum;
        for (int i = index + 1; i < segNum; i++) {
            System.out.println("Downloading video: " + Math.round(((float)i / (float)segNum)*100) + "% (" + (i+1) + "/" + segNum + ")");
            if (i < audioSegNum) {
                downStatus = downloadManager.download(dashPlaylistManager.getAudioSegURI(i), partPath.toString() + File.separator + dashPlaylistManager.audioID() + File.separator);
                if (!downStatus) {
                    System.err.println("Download Failed. Please try again later.");
                    System.exit(-1);
                }
            }
            if (i < videoSegNum) {
                downStatus = downloadManager.download(dashPlaylistManager.getVideoSegURI(i), partPath.toString() + File.separator + dashPlaylistManager.videoID() + File.separator);
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

    private void setKeyProgress(boolean status) {
        try {
            if (statusJSON == null) {
                statusJSON = new JSONObject();
            }
            if (statusJSON.containsKey("Key")) {
                statusJSON.replace("Key", status);
            } else {
                statusJSON.put("Key", status);
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
