package com.alebit.sget;

import com.alebit.sget.playlist.DASH.DASHPlaylistManager;
import com.alebit.sget.playlist.DASH.Representation;
import com.alebit.sget.playlist.PlaylistManager;
import com.alebit.sget.download.StreamDownloader;
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
            String[] formatArgs = new String[3];
            formatArgs[0] = args[0];
            formatArgs[1] = args[1];
            formatArgs[2] = "false";
            args = formatArgs;
        } else if (args.length != 3) {
            System.err.println("Wrong arguments");
            System.exit(-1);
        }
        String url = args[0];
        PlaylistManager playlistManager = new PlaylistManager(url);
        if (playlistManager.isDASH()) {
            DASHPlaylistManager dashPlaylistManager = playlistManager.getDASHPlaylist();
            if (dashPlaylistManager.getAudioRepresentations().length > 1) {
                Representation[] audioRepresentations = dashPlaylistManager.getAudioRepresentations();
                int index = chooseAudio(audioRepresentations);
                dashPlaylistManager.chooseAudioRepresentation(index);
            }
            if (dashPlaylistManager.getVideoRepresentations().length > 1) {
                Representation[] videoRepresentations = dashPlaylistManager.getVideoRepresentations();
                int index = chooseVideo(videoRepresentations);
                dashPlaylistManager.chooseVideoRepresentation(index);
            }
        } else {
            while (true) {
                if (playlistManager.hasMasterPlaylist()) {
                    int index = 0;
                    if (playlistManager.getPlaylistResolution() != null && playlistManager.getPlaylistResolution().size() > 1) {
                        index = chooseResolution(playlistManager.getPlaylistResolution());
                    }
                    url = playlistManager.getPreURL().concat(playlistManager.getPlaylistData(index).getUri());
                    playlistManager = new PlaylistManager(url);
                } else {
                    break;
                }
            }
        }
        Path path = Paths.get(args[1]);
        boolean raw = Boolean.parseBoolean(args[2]);
        StreamDownloader downloader = new StreamDownloader(playlistManager, path, raw);
    }

    private int chooseResolution(List<Resolution> resolutionList) {
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

    private int chooseAudio(Representation[] audioRepresentations) {
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

    private int chooseVideo(Representation[] videoRepresentations) {
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

    public static void main(String[] args) {
        new Main(args);
    }
}
