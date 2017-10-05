# StreamGet Plugin
## Format
StreamGet plugin use jar format with compiled java class and manifest with plugin information

### Code format
Create a java class like below

```
public class AnyNameYouLike {
    //  Plugin loader will "pluginMain" static method with a string array return value
    public static String[] pluginMain(String[] args) {
        // the variable "args" will get arguments input from program
        // then do what you want to do below
        ...

        // new a string array with three elements to save return arguments
        String[] rArgs = new String[3];
        rArgs[0] = videoURL; // first element save video URL (only accept HLS and DASH URL)
        rArgs[1] = videoLocation; // second element save video filename and location
        rArgs[2] = raw; // third element save whether user want to save raw file or not (use string "true" or "false")
        return rArgs; // return the arguments
    }
}
```

### Manifest Format
Add these information into META-INF/MANIFEST.MF

```
Plugin-Class: entry.point.of.your.main.class // the entry point of your plugin
Plugin-Name: Your Plugin Name // the name of your plugin
Plugin-Version: 1.0 // the version of your plugin
Plugin-Loader-Version: 1.1 // The version of StreamGet plugin loader
```

## Example
This directory contains an example project of StreamGet plugin. You can use "./gradlew jar" to build this plugin.