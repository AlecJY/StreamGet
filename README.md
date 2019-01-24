# StreamGet
Resumable HLS and MPEG-DASH Downloader

## Usage
```
Usage: StreamGet [OPTIONS] [-o|--output] FILENAME URL
 FILENAME       The output video name
 URL            Media Stream playlist URL

Option:
 -h     --help  Print this help text
 -r     --raw   Not delete raw video stream (no effect to MPEG-DASH)
 -v     --video [l | lowest | h | highest | NUMBER]     Select video track
                [l | lowest]    Select lowest resolution video
                [h | highest]   Select highest resolution video
                [NUMBER]        Select video number (Start from 1)
 -a     --audio [l | lowest | h | highest | NUMBER]     Select audio track (no effect to HLS)
                [l | lowest]    Select lowest quality audio
                [h | highest]   Select highest quality audio
                [NUMBER]        Select audio number (Start from 1)
 -s     --subtitle      Download subtitles if the video contains (no effect to MPEG-DASH)
 -e     --no-subtitle   Don't download subtitles
 -d     --header        Send http requests with the custom header
                        You can set this option more than once for multiple headers
```

## Output Video Format
### HLS
The output video format may not be standard MPEG-TS format, so some video player cannot play this format. You can convert videos using ffmpeg or other video converter.

### MPEG-DASH (experimental)
Download all video and audio segments into a folder only. You need a media player which is support  MPEG-DASH to play, such as VLC Media Player or other javascript-based media player.


## Plugins
You can use plugins to help you fetch playlist url more easily.

### Plugin Capabilities

| Plugin Version | StreamGet Version         |
| -------------- | ------------------------- |
| 1.0            | 1.0                       |
| 1.1            | 1.1 and later             |
| 1.2            | 2.1 and later             |
| 1.3            | 2.2 and later             |
| 1.4            | 2.3 and later             |

### Use plugins
Add a folder called "plugins", then put a StreamGet jar plugin into folder

### Make your own plugins
See [plugin example](example/StreamGet-PluginExample)
