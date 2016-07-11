package com.alebit.sget.plugin;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.net.MalformedURLException;
import java.util.Collection;

/**
 * Created by Alec on 2016/6/27.
 */
public class PluginManager {
    String[] args;
    public PluginManager(String[] args) {
        File pluginsDir = new File("." + File.separator + "plugins");
        pluginsDir.mkdirs();
        Collection<File> files = FileUtils.listFiles(pluginsDir, new String[]{"jar"}, true);
        if (files.size() > 0) {
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
        }
        this.args = args;
    }

    public String[] getArgs() {
        return args;
    }
}
