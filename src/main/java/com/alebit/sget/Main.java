package com.alebit.sget;

import com.alebit.sget.data.Header;
import com.alebit.sget.playlist.*;
import com.alebit.sget.download.StreamDownloader;
import com.alebit.sget.plugin.PluginManager;
import com.alebit.sget.utils.Utils;
import io.lindstrom.m3u8.model.Resolution;



import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;


/**
 * Created by Alec on 2016/6/26.
 */
public class Main {
    private Scanner scanner = new Scanner(System.in);

    public Main(String[] args) {
        System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
        PluginManager pluginManager = new PluginManager(args);
        args = pluginManager.getArgs();
        if (args.length == 2) {
            String[] formatArgs = new String[7];
            formatArgs[0] = args[0];
            formatArgs[1] = args[1];
            formatArgs[2] = "false";
            formatArgs[3] = "0";
            formatArgs[4] = "0";
            formatArgs[5] = "0";
            formatArgs[6] = "[]";
            args = formatArgs;
        } else if (args.length == 3) {
            String[] formatArgs = new String[7];
            formatArgs[0] = args[0];
            formatArgs[1] = args[1];
            formatArgs[2] = args[2];
            formatArgs[3] = "0";
            formatArgs[4] = "0";
            formatArgs[5] = "0";
            formatArgs[6] = "[]";
            args = formatArgs;
        } else if (args.length == 5) {
            String[] formatArgs = new String[7];
            formatArgs[0] = args[0];
            formatArgs[1] = args[1];
            formatArgs[2] = args[2];
            formatArgs[3] = args[3];
            formatArgs[4] = args[4];
            formatArgs[5] = "0";
            formatArgs[6] = "[]";
            args = formatArgs;
        } else if (args.length == 6) {
            String[] formatArgs = new String[7];
            formatArgs[0] = args[0];
            formatArgs[1] = args[1];
            formatArgs[2] = args[2];
            formatArgs[3] = args[3];
            formatArgs[4] = args[4];
            formatArgs[5] = args[5];
            formatArgs[6] = "[]";
            args = formatArgs;
        } else if (args.length != 7) {
            System.err.println("Wrong arguments");
            System.exit(-1);
        }
        String url = args[0];
        int video = Integer.parseInt(args[3]);
        int audio = Integer.parseInt(args[4]);

        Header[] headers = null;
        try {
            headers = Utils.getJsonObjectMapper().readValue(args[6], Header[].class);
        } catch (Exception e) {
            System.err.println("Illegal header format");
            System.exit(-1);
        }

        try {
            int subtitle = Integer.parseInt(args[5]);
            Playlist playlist = PlaylistParser.parse(url, headers);

            if (playlist == null) {
                throw new UnsupportedPlaylistException("Unsupported playlist format");
            }

            if (playlist.getType() != PlaylistType.HLS || ((HLSPlaylist) playlist).hasMasterPlaylist()) {
                chooseVideo(playlist, video);
                chooseAudio(playlist, audio);
                wantSubtitles(playlist, subtitle);
            }
            Path path = Paths.get(args[1]);
            boolean raw = Boolean.parseBoolean(args[2]);
            new StreamDownloader(playlist, headers, path, raw);
        } catch (UnsupportedPlaylistException e) {
            e.printStackTrace();
        }
    }

    private void wantSubtitles(Playlist playlist, int subtitle) {
        if (!playlist.hasSubtitles()) {
            return;
        }

        switch (subtitle) {
            case 0:
                String chosen;
                while (true) {
                    System.out.println("\nThis video contains subtitles, do you want to download? (Y/N)");
                    System.out.print("=> ");
                    chosen = scanner.next().toLowerCase();
                    switch (chosen) {
                        case "y":
                        case "yes":
                            playlist.setDownloadSubtitles(true);
                            return;

                        case "n":
                        case "no":
                            playlist.setDownloadSubtitles(false);
                            return;
                        default:
                            System.out.println("Illegal input. Please choose again.\n");

                    }
                }
            case 1:
                playlist.setDownloadSubtitles(false);
                break;
            case 2:
                playlist.setDownloadSubtitles(true);
        }
    }

    private void chooseAudio(Playlist playlist, int audio) {
        if (playlist.getAudioTracksName().size() == 0) {
            // Bypass because of no audio tracks.
            return;
        } else if (playlist.getAudioTracksName().size() == 1) {
            // Auto select the only audio track.
            playlist.chooseAudioTrack(0);
            return;
        }

        if (audio != Quality.NONE.getValue()) {
            if (audio < Quality.NONE.getValue()) {
                // TODO: Auto select audio tracks
                throw new UnsupportedOperationException();
            } else {
                if (playlist.getAudioTracksName().size() >= audio) {
                    playlist.chooseAudioTrack(audio - 1);
                    return;
                } else {
                    System.out.println("[Warning] Illegal audio track number. Please choose manually.\n");
                }
            }
        }
        while (true) {
            System.out.println("Please choose audio tracks:");
            for (int i = 0; i < playlist.getAudioTracksName().size(); i++) {
                System.out.println("[" + (i + 1) + "]: " + playlist.getAudioTracksName().get(i));
            }
            System.out.print("\n=> ");
            String chosen = scanner.next();
            int chosenNum;
            try {
                chosenNum = Integer.parseInt(chosen);
                if (chosenNum < 1 || chosenNum > playlist.getAudioTracksName().size()) {
                    System.out.println("Illegal input. Please choose again\n");
                } else {
                    System.out.println();
                    chosenNum--;
                    playlist.chooseAudioTrack(chosenNum);
                    return;
                }
            } catch (NumberFormatException e) {
                System.out.println("Illegal input. Please choose again.\n");
            }
        }
    }

    private void chooseVideo(Playlist playlist, int video) {
        if (playlist.getVideoTracksBandwidth().size() == 0) {
            // Bypass because of no video tracks.
            return;
        } else if (playlist.getVideoTracksBandwidth().size() == 1) {
            // Auto select the only video track.
            playlist.chooseVideoTrack(0);
            return;
        }

        if (video != Quality.NONE.getValue()) {
            // If video quality is chosen
            if (video < Quality.NONE.getValue()) {
                // If video quality is chosen to max or min quality
                if (video == Quality.MIN.getValue()) {
                    int index = getMinIndex(playlist.getVideoTracksBandwidth());
                    playlist.chooseVideoTrack(index);
                    return;
                } else {
                    int index = getMaxIndex(playlist.getVideoTracksBandwidth());
                    playlist.chooseVideoTrack(index);
                    return;
                }
            } else {
                if (playlist.getVideoTracksBandwidth().size() >= video) {
                    playlist.chooseVideoTrack(video - 1);
                    return;
                } else {
                    System.out.println("[Warning] Illegal video track number. Please choose manually.\n");
                }
            }
        }

        // Show bandwidths if there are the same resolutions or no resolutions
        // Not show resolutions if there is no resolution
        List<Long> bandwidths = playlist.getVideoTracksBandwidth();
        List<Optional<Resolution>> resolutions = playlist.getVideoTracksResolution();
        ArrayList<String> texts = new ArrayList<>();
        boolean showBandwidths = false;
        boolean showResolutions = true;
        int noResolution = 0;

        for (Optional<Resolution> resolution: resolutions) {
            if (resolution.isPresent()) {
                String resolutionText = resolution.get().width() + "x" + resolution.get().height();
                if(texts.contains(resolutionText)) {
                    showBandwidths = true;
                }
                texts.add(resolutionText);
            } else {
                showBandwidths = true;
                texts.add("Unknown");
                noResolution++;
            }
        }
        if (noResolution == resolutions.size()) {
            showResolutions = false;
        }
        if (showBandwidths && showResolutions) {
            for (int i = 0; i < bandwidths.size(); i++) {
                String resolutionText = texts.get(i);
                texts.set(i, resolutionText + ", " + bandwidths.get(i));
            }
        } else if (showBandwidths) {
            texts.clear();
            for (Long bandwidth: bandwidths) {
                texts.add(bandwidth.toString());
            }
        }

        while (true) {
            System.out.println("Please choose the video track:");
            for (int i = 0; i < texts.size(); i++) {
                System.out.println("[" + (i + 1) + "]: " + texts.get(i));
            }
            System.out.print("\n=> ");
            String chosen = scanner.next();
            int chosenNum;
            try {
                chosenNum = Integer.parseInt(chosen);
                if (chosenNum < 1 || chosenNum > texts.size()) {
                    System.out.println("Illegal input. Please choose again\n");
                } else {
                    System.out.println();
                    chosenNum--;
                    playlist.chooseVideoTrack(chosenNum);
                    return;
                }
            } catch (NumberFormatException e) {
                System.out.println("Illegal input. Please choose again.\n");
            }
        }
    }

    private int getMinIndex(List<Long> num) {
        long minNum = num.indexOf(0);
        int minIndex = 0;
        for (int i = 1; i < num.size(); i++) {
            if (num.get(i) < minNum) {
                minNum = num.get(i);
                minIndex = i;
            }
        }
        return minIndex;
    }

    private int getMaxIndex(List<Long> num) {
        long maxNum = num.get(num.size() - 1);
        int maxIndex = num.size() - 1;
        for (int i = num.size() - 2; i >= 0; i--) {
            if (num.get(i) > maxNum) {
                maxNum = num.get(i);
                maxIndex = i;
            }
        }
        return maxIndex;
    }

    public static void main(String[] args) {
        new Main(args);
    }
}
