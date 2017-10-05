# StreamGet
Resumable HLS and MPEG-DASH Downloader

## Usage
```
StreamGet [OPTIONS] [-o|--output] FILENAME URL
 FILENAME	The output video name
 URL		Media Stream playlist URL

Option: 
 -h	--help	Print this help text
 -r	--raw	Not delete raw video stream (no effect to MPEG-DASH)
```

## Output Video Format
### HLS
The output video format isn't standard MPEG-TS format, so some video player cannot play this format. You can convert videos using ffmpeg or other video converter.

### MPEG-DASH (experimental)
Download all video and audio segments into a folder only. You need a media player which is support  MPEG-DASH to play, such as VLC Media Player or other javascript-based media player.


## Plugins
You can use plugins to help you fetch playlist url more easily.
### Use plugins
Add a folder called "plugins", then put a StreamGet jar plugin into folder

### Make your own plugins
See [plugin example](example/StreamGet-PluginExample)
