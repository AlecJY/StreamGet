package com.alebit.sget.pluginexample;

import java.util.Scanner;

public class PluginMain {
    //  Plugin loader will "pluginMain" static method with a string array return value
    public static String[] pluginMain(String[] args) {
        // the variable "args" will get arguments input from program
        // then do what you want to do below
        Scanner scn = new Scanner(System.in);
        System.out.println("Please specify the video URL you want to download");
        System.out.print("=> ");
        String videoURL = scn.nextLine();
        System.out.println("\nPlease specify the file name you want of the video");
        System.out.print("=> ");
        String videoLocation = scn.nextLine();
        System.out.println("\nSave raw file? [Y/N]");
        System.out.print("=> ");
        String raw = scn.nextLine();
        if (raw.toLowerCase().equals("yes") || raw.toLowerCase().equals("y")) {
            raw = "true";
        } else {
            raw = "false";
        }
        System.out.println();
        // new a string array with three elements to save return arguments
        String[] rArgs = new String[3];
        rArgs[0] = videoURL; // first element save video URL (only accept HLS and DASH URL)
        rArgs[1] = videoLocation; // second element save video filename and location
        rArgs[2] = raw; // third element save whether user want to save raw file or not (use string "true" or "false")
        return rArgs; // return the arguments
    }
}
