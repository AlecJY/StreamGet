package com.alebit.sget;

import com.alebit.sget.playlist.DASH.DASHPlaylistManager;
import com.alebit.sget.playlist.DASH.Representation;
import com.alebit.sget.playlist.PlaylistManager;
import com.alebit.sget.download.StreamDownloader;
import com.alebit.sget.playlist.Subtitle;
import com.alebit.sget.plugin.PluginManager;
import com.iheartradio.m3u8.data.Resolution;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;


/**
 * Created by Alec on 2016/6/26.
 */
public class Main {
    private Scanner scanner = new Scanner(System.in);

    public Main(String[] args) {
        PluginManager pluginManager = new PluginManager(args);
        args = pluginManager.getArgs();
        if (args.length == 2) {
            String[] formatArgs = new String[6];
            formatArgs[0] = args[0];
            formatArgs[1] = args[1];
            formatArgs[2] = "false";
            formatArgs[3] = "0";
            formatArgs[4] = "0";
            formatArgs[5] = "0";
            args = formatArgs;
        } else if (args.length == 3) {
            String[] formatArgs = new String[6];
            formatArgs[0] = args[0];
            formatArgs[1] = args[1];
            formatArgs[2] = args[2];
            formatArgs[3] = "0";
            formatArgs[4] = "0";
            formatArgs[5] = "0";
            args = formatArgs;
        } else if (args.length == 5) {
            String[] formatArgs = new String[6];
            formatArgs[0] = args[0];
            formatArgs[1] = args[1];
            formatArgs[2] = args[2];
            formatArgs[3] = args[3];
            formatArgs[4] = args[4];
            formatArgs[5] = "0";
            args = formatArgs;
        } else if (args.length != 6) {
            System.err.println("Wrong arguments");
            System.exit(-1);
        }
        String url = args[0];
        int video = Integer.parseInt(args[3]);
        int audio = Integer.parseInt(args[4]);
        int subtitle = Integer.parseInt(args[5]);
        List<Subtitle> subtitleList = null;
        PlaylistManager playlistManager = new PlaylistManager(url);
        if (playlistManager.isDASH()) {
            DASHPlaylistManager dashPlaylistManager = playlistManager.getDASHPlaylist();
            if (dashPlaylistManager.getAudioRepresentations().length > 1) {
                Representation[] audioRepresentations = dashPlaylistManager.getAudioRepresentations();
                int index = chooseAudio(audioRepresentations, audio);
                dashPlaylistManager.chooseAudioRepresentation(index);
            }
            if (dashPlaylistManager.getVideoRepresentations().length > 1) {
                Representation[] videoRepresentations = dashPlaylistManager.getVideoRepresentations();
                int index = chooseVideo(videoRepresentations, video);
                dashPlaylistManager.chooseVideoRepresentation(index);
            }
        } else {
            while (true) {
                if (playlistManager.hasMasterPlaylist()) {
                    int index = 0;
                    if (playlistManager.getPlaylistResolution() != null && playlistManager.getPlaylistResolution().size() > 1) {
                        index = chooseResolution(playlistManager.getPlaylistResolution(), video);
                    }
                    if (playlistManager.getPlaylistData(index).getUri().contains("://")) {
                        url = playlistManager.getPlaylistData(index).getUri();
                    } else {
                        url = playlistManager.getPreURL().concat(playlistManager.getPlaylistData(index).getUri());
                    }
                    if (playlistManager.getSubtitles().size() > 0) {
                        if (subtitle == 0) {
                            if (wantSubtitles()) {
                                subtitleList = playlistManager.getSubtitles();
                            }
                        } else if (subtitle == 2) {
                            subtitleList = playlistManager.getSubtitles();
                        }
                    }
                    playlistManager = new PlaylistManager(url);
                } else {
                    break;
                }
            }
        }
        Path path = Paths.get(args[1]);
        boolean raw = Boolean.parseBoolean(args[2]);
        StreamDownloader downloader = new StreamDownloader(playlistManager, subtitleList, path, raw);
    }

    private int chooseResolution(List<Resolution> resolutionList, int video) {
        if (video != 0) {
            if (video < 0) {
                int[] resolutions = new int[resolutionList.size()];
                for (int i = 0; i < resolutionList.size(); i++) {
                    try {
                        resolutions[i] = resolutionList.get(i).height * resolutionList.get(i).width;
                    } catch (Exception e) {
                        resolutions[i] = 0;
                    }
                }
                if (video == -1) {
                    return getMinIndex(resolutions);
                } else {
                    return getMaxIndex(resolutions);
                }
            } else {
                if (resolutionList.size() >= video) {
                    return video - 1;
                } else {
                    System.out.println("[Warning] Illegal video track number. Please choose manually.\n");
                }
            }
        }
        String chosen;
        int chosenNum;
        while (true) {
            System.out.println("Please choose video resolution:");
            for (int i = 0; i < resolutionList.size(); i++) {
                System.out.println("[" + (i + 1) + "]: " + resolutionList.get(i).width + "x" + resolutionList.get(i).height);
            }
            System.out.print("\n=> ");
            chosen = scanner.next();
            try {
                chosenNum = Integer.parseInt(chosen);
                if (chosenNum < 1 || chosenNum > resolutionList.size()) {
                    System.out.println("Illegal input. Please choose again.\n");
                } else {
                    chosenNum--;
                    break;
                }
            } catch (NumberFormatException e) {
                System.out.println("Illegal input. Please choose again.\n");
            }
        }
        return chosenNum;
    }

    private boolean wantSubtitles() {
        String chosen;
        while (true) {
            System.out.println("\nThis video contains subtitles, do you want to download? (Y/N)");
            System.out.print("=> ");
            chosen = scanner.next().toLowerCase();
            switch (chosen) {
                case "y":
                case "yes":
                    return true;

                case "n":
                case "no":
                    return false;

                default:
                    System.out.println("Illegal input. Please choose again.\n");

            }
        }
    }

    private int chooseAudio(Representation[] audioRepresentations, int audio) {
        if (audio != 0) {
            if (audio < 0) {
                int[] bandwiths = new int[audioRepresentations.length];
                for (int i = 0; i < audioRepresentations.length; i++) {
                    try {
                        bandwiths[i] = Integer.parseInt(audioRepresentations[i].getBandwidth());
                    } catch (Exception e) {
                        bandwiths[i] = 0;
                    }
                }
                if (audio == -1) {
                    return getMinIndex(bandwiths);
                } else {
                    return getMaxIndex(bandwiths);
                }
            } else {
                if (audioRepresentations.length >= audio) {
                    return audio - 1;
                } else {
                    System.out.println("[Warning] Illegal audio track number. Please choose manually.\n");
                }
            }
        }
        while (true) {
            System.out.println("Please choose audio tracks:");
            for (int i = 0; i < audioRepresentations.length; i++) {
                if (audioRepresentations[i].getAudioSamplingRate().equals("")) {
                    System.out.println("[" + (i + 1) + "]: Bandwidth: " + audioRepresentations[i].getBandwidth());
                } else {
                    System.out.println("[" + (i + 1) + "]: Bandwidth:" + audioRepresentations[i].getBandwidth() + ", SamplingRate:" + audioRepresentations[i].getAudioSamplingRate());
                }
            }
            System.out.print("\n=> ");
            String chosen = scanner.next();
            int chosenNum;
            try {
                chosenNum = Integer.parseInt(chosen);
                if (chosenNum < 1 || chosenNum > audioRepresentations.length) {
                    System.out.println("Illegal input. Please choose again\n");
                } else {
                    System.out.println();
                    chosenNum--;
                    return chosenNum;
                }
            } catch (NumberFormatException e) {
                System.out.println("Illegal input. Please choose again.\n");
            }
        }
    }

    private int chooseVideo(Representation[] videoRepresentations, int video) {
        if (video != 0) {
            if (video < 0) {
                int[] bandwiths = new int[videoRepresentations.length];
                for (int i = 0; i < videoRepresentations.length; i++) {
                    try {
                        bandwiths[i] = Integer.parseInt(videoRepresentations[i].getBandwidth());
                    } catch (Exception e) {
                        bandwiths[i] = 0;
                    }
                }
                if (video == -1) {
                    return getMinIndex(bandwiths);
                } else {
                    return getMaxIndex(bandwiths);
                }
            } else {
                if (videoRepresentations.length >= video) {
                    return video - 1;
                } else {
                    System.out.println("[Warning] Illegal video track number. Please choose manually.\n");
                }
            }
        }
        while (true) {
            System.out.println("Please choose video resolution:");
            for (int i = 0; i < videoRepresentations.length; i++) {
                System.out.println("[" + (i + 1) + "]: " + videoRepresentations[i].getWidth() + "x" + videoRepresentations[i].getHeight());
            }
            System.out.print("\n=> ");
            String chosen = scanner.next();
            int chosenNum;
            try {
                chosenNum = Integer.parseInt(chosen);
                if (chosenNum < 1 || chosenNum > videoRepresentations.length) {
                    System.out.println("Illegal input. Please choose again\n");
                } else {
                    System.out.println();
                    chosenNum--;
                    return chosenNum;
                }
            } catch (NumberFormatException e) {
                System.out.println("Illegal input. Please choose again.\n");
            }
        }
    }

    private int getMinIndex(int num[]) {
        int minNum = num[0];
        int minIndex = 0;
        for (int i = 1; i < num.length; i++) {
            if (num[i] < minNum) {
                minNum = num[i];
                minIndex = i;
            }
        }
        return minIndex;
    }

    private int getMaxIndex(int num[]) {
        int maxNum = num[num.length - 1];
        int maxIndex = num.length - 1;
        for (int i = num.length - 2; i >= 0; i--) {
            if (num[i] > maxNum) {
                maxNum = num[i];
                maxIndex = i;
            }
        }
        return maxIndex;
    }

    public static void main(String[] args) {
        new Main(args);
    }
}
