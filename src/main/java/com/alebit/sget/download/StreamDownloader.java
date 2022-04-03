package com.alebit.sget.download;

import com.alebit.sget.playlist.HLSPlaylist;
import com.alebit.sget.playlist.Subtitle;
import com.alebit.sget.utils.Utils;
import com.alebit.sget.data.Header;
import com.alebit.sget.data.Status;
import com.alebit.sget.decrypt.HLSDecrypter;
import com.alebit.sget.playlist.PlaylistType;
import com.alebit.sget.playlist.Playlist;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.*;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Created by Alec on 2016/6/26.
 */
public class StreamDownloader {
    private Playlist playlist;
    private Header[] headers;
    private Path path;
    private Path partPath;
    private Path jsonPath;
    private String filename;
    private Status status;
    private int progress = -1;
    private boolean raw;

    public StreamDownloader(Playlist playlist, Header[] headers, Path path, boolean raw) {
        if (path.getParent() == null) {
            path = Paths.get("." + File.separator + path);
        }
        this.raw = raw;
        this.playlist = playlist;
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

    private void prepare() {
        try {
            if (playlist.isMultiTrackPlaylist()) {
                jsonPath = path.getParent().resolve(filename).resolve("status.json");
            } else {
                jsonPath = path.getParent().resolve(filename + "_part").resolve("status.json");
            }
            partPath = jsonPath.getParent();
            Path playlistPath;
            if (playlist.getType() == PlaylistType.DASH) {
                playlistPath = jsonPath.getParent().resolve(filename + ".mpd");
            } else {
                if (playlist.isMultiTrackPlaylist()) {
                    playlistPath = jsonPath.getParent().resolve(filename + ".m3u8");
                } else {
                    playlistPath = path.getParent().resolve(filename + "_part").resolve(filename + ".m3u8");
                }
            }

            status = readJSON(jsonPath);
            Boolean shaMatch = false;
            if (status != null) {
                if (Arrays.equals(playlist.getHash(), status.getSha256())) {
                    shaMatch = true;
                    progress = status.getProgress();
                }
            }
            if (!shaMatch && path.toFile().exists()) {
                System.out.println("File exists. Continue?(Y/N)");
                Scanner scanner = new Scanner(System.in);
                String chosen = scanner.next();
                if (!(chosen.equalsIgnoreCase("y") || chosen.equalsIgnoreCase("yes"))) {
                    System.exit(0);
                }
            }

            path.getParent().toFile().mkdirs();
            jsonPath.getParent().toFile().mkdirs();

            playlist.savePlaylist(playlistPath);
            if (status == null) {
                status = new Status();
            }
            status.setSha256(playlist.getHash());
            BufferedWriter videoWriter = null;
            if (!playlist.isMultiTrackPlaylist()) {
                videoWriter = new BufferedWriter(new FileWriter(path.toFile(), false));
            }
            BufferedWriter jsonWriter = new BufferedWriter(new FileWriter(jsonPath.toFile(), true));
            jsonWriter.close();
            if (playlist.shouldDownloadSubtitle()) {
                downloadSubtitles();
            }
            download(progress, !status.hasKey(), !status.initSeg());
            if (!playlist.isMultiTrackPlaylist()) {
                videoWriter.close();
                System.out.println("Converting video...");
                new HLSDecrypter(path, raw);
            } else {
                jsonPath.toFile().delete();
                System.out.println("Successfully download video!");
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

    private void downloadSubtitles() {
        DownloadManager downloadManager = new DownloadManager(headers);
        boolean downStatus;
        List<Subtitle> subtitles = playlist.getSubtitles();
        for (Subtitle subtitle: subtitles) {
            String ext = Utils.getExtensionFromURI(subtitle.getUri());
            System.out.println("Downloading subtitle \"" + filename + "." + subtitle.getName() + ext + "\"");
            downStatus = downloadManager.download(subtitle.getUri(), path.getParent(), filename + "." + subtitle.getName() + ext);
            if (!downStatus) {
                System.err.println("Download Failed. Please try again later.");
                System.exit(-1);
            }
        }
    }

    private void download(int index, boolean downloadKey, boolean downloadInitSeg) {
        DownloadManager downloadManager = new DownloadManager(headers);

        int videoSegSize = playlist.getVideoSegmentsSize();
        int audioSegSize = playlist.getAudioSegmentsSize();
        int segSize = Math.max(audioSegSize, videoSegSize);

        Path videoPath;
        if (playlist.getType() == PlaylistType.HLS && audioSegSize == 0) {
            videoPath = partPath;
        } else {
            videoPath = partPath.resolve("video");
        }

        // Download hls AES-128 keys
        if (playlist.getType() == PlaylistType.HLS && downloadKey) {
            HLSPlaylist hlsPlaylist = (HLSPlaylist) playlist;
            List<URI> videoKeyList = hlsPlaylist.getVideoSegmentKeyList();
            List<URI> audioKeyList = hlsPlaylist.getAudioSegmentKeyList();

            if (videoKeyList.size() != 0 || audioKeyList.size() !=0) {
                System.out.println("Downloading key...");
            }

            for (int i = 0; i < videoKeyList.size(); i++) {
                boolean downStatus = downloadManager.download(videoKeyList.get(i), videoPath, i + ".key");
                if (!downStatus) {
                    System.err.println("Download Failed. Please try again later.");
                    System.exit(-1);
                }
            }
            for (int i = 0; i < audioKeyList.size(); i++) {
                boolean downStatus = downloadManager.download(audioKeyList.get(i), partPath.resolve("audio"), i + ".key");
                if (!downStatus) {
                    System.err.println("Download Failed. Please try again later.");
                    System.exit(-1);
                }
            }
            if (videoKeyList.size() != 0 || audioKeyList.size() !=0) {
                setKeyProgress();
            }
        }

        // Download initialization segments
        if (downloadInitSeg) {
            URI videoInit = playlist.getVideoInitializationSegment();
            if (videoInit != null) {
                boolean downStatus = downloadManager.download(videoInit, videoPath, "init" + Utils.getExtensionFromURI(videoInit));
                if (!downStatus) {
                    System.err.println("Download Failed. Please try again later.");
                    System.exit(-1);
                }
            }
            URI audioInit = playlist.getAudioInitializationSegment();
            if (audioInit != null) {
                boolean downStatus = downloadManager.download(audioInit, partPath.resolve("audio"), "init" + Utils.getExtensionFromURI(audioInit));
                if (!downStatus) {
                    System.err.println("Download Failed. Please try again later.");
                    System.exit(-1);
                }
            }
            if (videoInit != null || audioInit != null) {
                setInitSegProgress();
            }
        }

        for (int i = index + 1; i < segSize; i++) {
            System.out.println("Downloading video: " + Math.round(((float)i / (float)segSize)*100) + "% (" + (i+1) + "/" + segSize + ")");
            if (i < videoSegSize) {
                URI videoSeg = playlist.getVideoSegmentURI(i);
                boolean downStatus = downloadManager.download(videoSeg, videoPath, i + Utils.getExtensionFromURI(videoSeg));
                if (!downStatus) {
                    System.err.println("Download Failed. Please try again later.");
                    System.exit(-1);
                }
            }
            if (i < audioSegSize) {
                URI audioSeg = playlist.getAudioSegmentURI(i);
                boolean downStatus = downloadManager.download(audioSeg, partPath.resolve("audio"), i + Utils.getExtensionFromURI(audioSeg));
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

    private void setInitSegProgress() {
        try {
            status.initSeg(true);
            Utils.getJsonObjectMapper().writeValue(jsonPath.toFile(), status);
        } catch (IOException e) {
            System.out.println("Cannot open status.json");
        }
    }
}
