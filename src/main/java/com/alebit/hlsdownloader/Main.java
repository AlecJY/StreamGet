package com.alebit.hlsdownloader;

import com.alebit.hlsdownloader.playlist.PlaylistManager;
import com.alebit.hlsdownloader.download.HLSDownloader;
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
        String url = args[0];
        PlaylistManager playlistManager = new PlaylistManager(url);
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
        Path path = Paths.get(args[1]);
        HLSDownloader downloader = new HLSDownloader(playlistManager, path);
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

    public static void main(String[] args) {
        new Main(args);
    }
}
