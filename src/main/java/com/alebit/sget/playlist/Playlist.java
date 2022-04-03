package com.alebit.sget.playlist;

import io.lindstrom.m3u8.model.Resolution;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public interface Playlist {
    public PlaylistType getType();

    public List<Long> getVideoTracksBandwidth();

    public List<Optional<Resolution>> getVideoTracksResolution();

    public List<String> getAudioTracksName();

    public void chooseVideoTrack(int index);

    public void chooseAudioTrack(int index);

    public void autoChooseAudioTrack(Quality quality);

    public boolean hasSubtitles();

    public void setDownloadSubtitles(boolean enable);

    public boolean shouldDownloadSubtitle();

    public List<Subtitle> getSubtitles();

    public boolean isMultiTrackPlaylist();

    public void savePlaylist(Path path);

    public URI getVideoInitializationSegment();

    public int getVideoSegmentsSize();

    public URI getVideoSegmentURI(int index);

    public URI getAudioInitializationSegment();

    public int getAudioSegmentsSize();

    public URI getAudioSegmentURI(int index);

    public byte[] getHash();
}
