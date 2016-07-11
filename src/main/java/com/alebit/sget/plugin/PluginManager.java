package com.alebit.sget.plugin;

import org.apache.commons.io.FileUtils;

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
        if (files != null && files.size() > 0) {
            for (File file: files) {
                try {
                    PluginLoader pluginLoader = new PluginLoader(file.toURI().toURL());
                    if (pluginLoader.getPluginLoaderVersion().equals("1.1")) {
                        args = pluginLoader.invokeClass(pluginLoader.getPluginClassName(), args);
                    }
                    break;
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            String[] newArgs = new String[3];
            newArgs[2] = "false";
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
                System.out.println(" URL     \tHTTP Live Stream playlist URL");
                System.out.println();
                System.out.println("Option: ");
                System.out.println(" -h\t--help\tPrint this help text");
                System.out.println(" -r\t--raw\tNot delete raw video stream");
                System.exit(0);
            }
            args = newArgs;
        }
        this.args = args;
    }

    public String[] getArgs() {
        return args;
    }
}
