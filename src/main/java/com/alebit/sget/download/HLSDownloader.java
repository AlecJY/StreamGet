package com.alebit.sget.download;

import com.alebit.sget.decrypt.HLSDecrypter;
import com.alebit.sget.playlist.PlaylistManager;
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
import java.util.Scanner;

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
    private boolean raw;

    public HLSDownloader(PlaylistManager playlistManager, Path path, boolean raw) {
        if (path.getParent() == null) {
            path = Paths.get("." + File.separator + path.toString());
        }
        this.raw = raw;
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
                        if (CRC == oldCRC) {
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
                        statusJSON.put("CRC32", CRC);
                    }
                } catch (Exception e) {
                }
            } else if (path.toFile().exists()) {
                System.out.println("File exists. Continue?(Y/N)");
                Scanner scanner = new Scanner(System.in);
                String chosen = scanner.next();
                if (!(chosen.toLowerCase().equals("y") || chosen.toLowerCase().equals("yes"))) {
                    System.exit(0);
                }
            }
            long CRC = FileUtils.checksumCRC32(playlistPath.toFile());
            if (statusJSON == null) {
                statusJSON = new JSONObject();
                statusJSON.put("CRC32", CRC);
            } else if (!statusJSON.containsKey("CRC32")) {
                statusJSON.put("CRC32", CRC);
            } else {
                statusJSON.replace("CRC32", CRC);
            }
            BufferedWriter videoWriter = new BufferedWriter(new FileWriter(path.toFile(), true));
            BufferedWriter jsonWriter = new BufferedWriter(new FileWriter(jsonPath.toFile(), true));
            jsonWriter.close();
            downloadHLS(progress);
            videoWriter.close();
            videoWriter = new BufferedWriter(new FileWriter(path.toFile()));
            videoWriter.close();
            System.out.println("Converting video...");
            new HLSDecrypter(path, raw);
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
            System.err.println("Write permission not allowed: " + e.getMessage());
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
                System.out.println("Downloading encrypt key...");
                /* if (tracks.get(0).getEncryptionData().getUri().contains("://")) {
                    downs = downloadManager.download(tracks.get(0).getEncryptionData().getUri(), partPath.toString() + File.separator);
                } else {
                    downs = downloadManager.download(playlistManager.getPreURL() + tracks.get(0).getEncryptionData().getUri(), partPath.toString() + File.separator);
                }
                if (!downs) {
                    System.err.println("Download Failed. Please try again later.");
                    System.exit(-1);
                } */
                setKeyProgress(true);
            }
        }
        for (int i = index + 1; i < tracks.size(); i++) {
            System.out.println("Downloading video: " + Math.round(((float)i / (float)tracks.size())*100) + "% (" + (i+1) + "/" + tracks.size() + ")");
            boolean downStatus = downloadManager.download(playlistManager.getPreURL() + tracks.get(i).getUri(), partPath.toString() + File.separator);
            if (!downStatus) {
                System.err.println("Download Failed. Please try again later.");
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
