package com.alebit.sget.playlist;

import com.alebit.sget.data.Header;
import io.lindstrom.m3u8.model.Resolution;

import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

// TODO: implement DASH playlist
public class DASHPlaylist implements Playlist {
    DASHPlaylist(InputStream inputStream, URI uri, Header[] headers) {

    }

    @Override
    public PlaylistType getType() {
        return PlaylistType.DASH;
    }

    @Override
    public List<Long> getVideoTracksBandwidth() {
        return null;
    }

    @Override
    public List<Optional<Resolution>> getVideoTracksResolution() {
        return null;
    }

    @Override
    public List<String> getAudioTracksName() {
        return null;
    }

    @Override
    public void chooseVideoTrack(int index) {

    }

    @Override
    public void chooseAudioTrack(int index) {

    }

    @Override
    public void autoChooseAudioTrack(Quality quality) {

    }

    @Override
    public boolean hasSubtitles() {
        return false;
    }

    @Override
    public void setDownloadSubtitles(boolean enable) {

    }

    @Override
    public boolean shouldDownloadSubtitle() {
        return false;
    }

    @Override
    public List<Subtitle> getSubtitles() {
        return null;
    }

    @Override
    public boolean isMultiTrackPlaylist() {
        return false;
    }

    @Override
    public void savePlaylist(Path path) {

    }

    @Override
    public URI getVideoInitializationSegment() {
        return null;
    }

    @Override
    public int getVideoSegmentsSize() {
        return 0;
    }

    @Override
    public URI getVideoSegmentURI(int index) {
        return null;
    }

    @Override
    public URI getAudioInitializationSegment() {
        return null;
    }

    @Override
    public int getAudioSegmentsSize() {
        return 0;
    }

    @Override
    public URI getAudioSegmentURI(int index) {
        return null;
    }

    @Override
    public byte[] getHash() {
        return new byte[0];
    }
}
