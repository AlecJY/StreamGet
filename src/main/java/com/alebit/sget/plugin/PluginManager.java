package com.alebit.sget.plugin;

import org.apache.commons.io.FileUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.File;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;

/**
 * Created by Alec on 2016/6/27.
 */
public class PluginManager {
    String[] args;
    public PluginManager(String[] args) {
        Path jarPath = null;
        try {
            jarPath = Paths.get(PluginManager.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            if (jarPath.toFile().isDirectory()) {
                jarPath = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        File pluginsDir;
        if (jarPath == null) {
            pluginsDir = new File("." + File.separator + "plugins");
        } else {
            pluginsDir = new File(jarPath.getParent().getParent().toString() + File.separator + "plugins");
        }
        Collection<File> files = null;
        if (pluginsDir.exists()) {
            files = FileUtils.listFiles(pluginsDir, new String[]{"jar"}, true);
        }
        boolean execPlugin = false;
        if (files != null && files.size() > 0) {
            for (File file: files) {
                try {
                    PluginLoader pluginLoader = new PluginLoader(file.toURI().toURL());
                    if (pluginLoader.getPluginLoaderVersion().equals("1.1") || pluginLoader.getPluginLoaderVersion().equals("1.2") || pluginLoader.getPluginLoaderVersion().equals("1.3") || pluginLoader.getPluginLoaderVersion().equals("1.4")) {
                        args = pluginLoader.invokeClass(pluginLoader.getPluginClassName(), args);
                        execPlugin = true;
                        break;
                    } else {
                        System.err.println("StreamGet's version is too old to run \"" + pluginLoader.getPluginName() + "\"");
                    }
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (!execPlugin) {
                System.out.println("Fall back to run the default command line interface");
            }
        }
        if (!execPlugin) {
            String[] newArgs = new String[7];
            newArgs[2] = "false";
            newArgs[3] = "0";
            newArgs[4] = "0";
            newArgs[5] = "0";
            JSONArray headers = new JSONArray();
            try {
                for (int i = 0; i < args.length; i++) {
                    switch (args[i].charAt(0)) {
                        case '-':
                            switch (args[i].toLowerCase()) {
                                case "--output":
                                case "-o":
                                    if (args[i + 1].charAt(0) != '-') {
                                        newArgs[1] = args[i + 1];
                                        i++;
                                    } else {
                                        System.err.println("Wrong argument after \"" + args[i] + "\": " + args[i + 1]);
                                        throw new IllegalArgumentException();
                                    }
                                    break;
                                case "--raw":
                                case "-r":
                                    newArgs[2] = "true";
                                    break;
                                case "-v":
                                case "--video":
                                    switch (args[i+1].toLowerCase()) {
                                        case "l":
                                        case "lowest":
                                            newArgs[3] = "-1";
                                            break;
                                        case "h":
                                        case "highest":
                                            newArgs[3] = "-2";
                                            break;
                                        default:
                                        try {
                                            int resNum = Integer.parseInt(args[i + 1]);
                                            if (resNum < 1) {
                                                throw new IllegalArgumentException();
                                            }
                                            newArgs[3] = Integer.toString(resNum);
                                        } catch (Exception e) {
                                            System.err.println("Wrong argument after \"" + args[i] + "\": " + args[i + 1]);
                                            throw new IllegalArgumentException();
                                        }
                                    }
                                    i++;
                                    break;
                                case "-a":
                                case "--audio":
                                    switch (args[i+1].toLowerCase()) {
                                        case "l":
                                        case "lowest":
                                            newArgs[4] = "-1";
                                            break;
                                        case "h":
                                        case "highest":
                                            newArgs[4] = "-2";
                                            break;
                                        default:
                                            try {
                                                int resNum = Integer.parseInt(args[i + 1]);
                                                i++;
                                                if (resNum < 1) {
                                                    throw new IllegalArgumentException();
                                                }
                                                newArgs[4] = Integer.toString(resNum);
                                            } catch (Exception e) {
                                                System.err.println("Wrong argument after \"" + args[i] + "\": " + args[i + 1]);
                                                throw new IllegalArgumentException();
                                            }
                                    }
                                    i++;
                                    break;
                                case "--subtitle":
                                case "-s":
                                    newArgs[5] = "2";
                                    break;
                                case "-e":
                                case "--no-subtitle":
                                    newArgs[5] = "1";
                                    break;
                                case "--header":
                                case "-d":
                                    String[] splitHeader = args[i + 1].split(":", 2);
                                    if (splitHeader.length != 2) {
                                        System.err.println("Wrong argument after \"" + args[i] + "\": " + args[i + 1]);
                                        throw new IllegalArgumentException();
                                    }
                                    JSONObject header = new JSONObject();
                                    header.put("name", splitHeader[0].trim());
                                    header.put("value", splitHeader[1].trim());
                                    headers.add(header);
                                    i++;
                                    break;
                                case "--help":
                                case "-h":
                                    throw new IllegalArgumentException();
                                default:
                                    System.err.println("Unknown argument: " + args[i]);
                                    throw new IllegalArgumentException();
                            }
                            break;
                        default:
                            if (i == args.length - 1) {
                                newArgs[0] = args[i];
                            } else {
                                System.err.println("Wrong argument: " + args[i]);
                                throw new IllegalArgumentException();
                            }
                    }
                }
                if (newArgs[0] == null) {
                    System.err.println("Empty URL!");
                    throw new IllegalArgumentException();
                } else if (newArgs[1] == null) {
                    System.err.println("Empty filename!");
                    throw new IllegalArgumentException();
                }
            } catch (Exception e) {
                System.out.println("Usage: StreamGet [OPTIONS] [-o|--output] FILENAME URL");
                System.out.println(" FILENAME\tThe output video name");
                System.out.println(" URL     \tMedia Stream playlist URL");
                System.out.println();
                System.out.println("Option: ");
                System.out.println(" -h\t--help\tPrint this help text");
                System.out.println(" -r\t--raw\tNot delete raw video stream (no effect to MPEG-DASH)");
                System.out.println(" -v\t--video\t[l | lowest | h | highest | NUMBER]\tSelect video track");
                System.out.println("\t\t[l | lowest]\tSelect lowest resolution video");
                System.out.println("\t\t[h | highest]\tSelect highest resolution video");
                System.out.println("\t\t[NUMBER]\tSelect video number (Start from 1)");
                System.out.println(" -a\t--audio\t[l | lowest | h | highest | NUMBER]\tSelect audio track (no effect to HLS)");
                System.out.println("\t\t[l | lowest]\tSelect lowest quality audio");
                System.out.println("\t\t[h | highest]\tSelect highest quality audio");
                System.out.println("\t\t[NUMBER]\tSelect audio number (Start from 1)");
                System.out.println(" -s\t--subtitle\tDownload subtitles if the video contains (no effect to MPEG-DASH)");
                System.out.println(" -e\t--no-subtitle\tDon't download subtitles");
                System.out.println(" -d\t--header\tSend http requests with the custom header\n" +
                        "\t\t\tYou can set this option more than once for multiple headers");
                System.exit(0);
            }
            newArgs[6] = headers.toJSONString();
            args = newArgs;
        }
        this.args = args;
    }

    public String[] getArgs() {
        return args;
    }
}
