# StreamGet
Resumable HLS Downloader

## Usage
```
StreamGet [OPTIONS] [-o|--output] FILENAME URL
 FILENAME	The output video name
 URL     	HTTP Live Stream playlist URL

Option: 
 -h	--help	Print this help text
 -r	--raw	Not delete raw video stream
```

## Output Video Format
The output video format isn't standard MPEG-TS format, so some video player cannot play this format. You can convert videos using ffmpeg or other video converter

## Plugins
You can use plugins to help you fetch m3u8 url more easily.

See plugin example (Not finish yet)
