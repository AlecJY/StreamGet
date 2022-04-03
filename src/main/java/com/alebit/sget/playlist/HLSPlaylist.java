package com.alebit.sget.playlist;

import com.alebit.sget.utils.Utils;
import com.alebit.sget.data.Header;
import io.lindstrom.m3u8.model.*;
import io.lindstrom.m3u8.parser.MasterPlaylistParser;
import io.lindstrom.m3u8.parser.MediaPlaylistParser;
import io.lindstrom.m3u8.parser.ParsingMode;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class HLSPlaylist implements Playlist {
    private static final MasterPlaylistParser masterPlaylistParser = new MasterPlaylistParser(ParsingMode.LENIENT);
    private static final MediaPlaylistParser mediaPlaylistParser = new MediaPlaylistParser(ParsingMode.LENIENT);

    // The priority of the audio track auto-selection
    private static final int AUDIO_GROUP = 3;
    private static final int AUDIO_DEFAULT = 2;
    private static final int AUDIO_AUTOSELECT = 1;

    private MasterPlaylist masterPlaylist;
    private MediaPlaylist mediaPlaylist;
    private MediaPlaylist audioPlaylist;
    private List<SubtitleInfo> subtitlePlaylists;
    private final URI playlistURI;
    private URI mediaPlaylistURI;
    private URI audioPlaylistURI;
    private final Header[] headers;
    private List<URI> videoSegmentKeyList;

    private int chosenVideoTrack = -1;
    private int chosenAudioTrack = -1;
    private boolean downloadSubtitles;

    HLSPlaylist(byte[] data, URI uri, Header[] headers) throws IOException {
        try {
            // Try master playlist parser first
            InputStream inputStream = new ByteArrayInputStream(data);
            masterPlaylist = masterPlaylistParser.readPlaylist(inputStream);
        } catch (Exception e) {
            // If it is failed, try media playlist parser
            InputStream inputStream = new ByteArrayInputStream(data);
            mediaPlaylist = mediaPlaylistParser.readPlaylist(inputStream);
            mediaPlaylistURI = uri;
        }
        playlistURI = uri;
        this.headers = headers;

    }

    public HLSPlaylist(URI uri, Header[] headers) throws IOException {
        URLConnection connection = uri.toURL().openConnection();
        for (Header header: headers) {
            connection.setRequestProperty(header.getName(), header.getValue());
        }
        try (InputStream connectionInputStream = connection.getInputStream()) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            IOUtils.copy(connectionInputStream, outputStream);

            try {
                // Try master playlist parser first
                InputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
                masterPlaylist = masterPlaylistParser.readPlaylist(inputStream);
            } catch (Exception e) {
                // If it is failed, try media playlist parser
                InputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
                mediaPlaylist = mediaPlaylistParser.readPlaylist(inputStream);
                mediaPlaylistURI = uri;
            }
        }
        playlistURI = uri;
        this.headers = headers;
    }

    @Override
    public PlaylistType getType() {
        return PlaylistType.HLS;
    }

    @Override
    public List<Long> getVideoTracksBandwidth() {
        if (masterPlaylist != null) {
            ArrayList<Long> bandwidthList = new ArrayList<>();
            for (Variant variant: masterPlaylist.variants()) {
                bandwidthList.add(variant.bandwidth());
            }
            return bandwidthList;
        }
        return null;
    }

    @Override
    public List<Optional<Resolution>> getVideoTracksResolution() {
        if (masterPlaylist != null) {
            ArrayList<Optional<Resolution>> resolutionList = new ArrayList<>();
            for (Variant variant: masterPlaylist.variants()) {
                resolutionList.add(variant.resolution());
            }
            return resolutionList;
        }
        return null;
    }

    @Override
    public List<String> getAudioTracksName() {
        if (masterPlaylist != null) {
            ArrayList<String> nameList = new ArrayList<>();
            ArrayList<String> groupList = new ArrayList<>();
            String lastGroupId = null;
            boolean showGroupId = false;
            for (AlternativeRendition alternativeRendition: masterPlaylist.alternativeRenditions()) {
                if (alternativeRendition.type() == MediaType.AUDIO) {
                    nameList.add(alternativeRendition.name());
                    groupList.add(alternativeRendition.groupId());

                    // If there are different group ids, show group ids as port of name
                    if (lastGroupId == null) {
                        lastGroupId = alternativeRendition.groupId();
                    } else {
                        if (!alternativeRendition.groupId().equals(lastGroupId)) {
                            showGroupId = true;
                        }
                    }
                }
            }
            if (showGroupId) {
                for (int i = 0; i < nameList.size(); i++) {
                    String name = nameList.get(i);
                    nameList.set(i, name.concat(" (" + groupList.get(i) + ")"));
                }
            }
            return nameList;
        }
        return null;
    }

    @Override
    public void chooseVideoTrack(int index) {
        chosenVideoTrack = index;
        Variant variant = masterPlaylist.variants().get(index);
        URI childPlaylistURI = playlistURI.resolve(variant.uri());
        try {
            HLSPlaylist childPlaylist = PlaylistParser.parseHLS(childPlaylistURI, headers);
            if (childPlaylist == null || childPlaylist.mediaPlaylist == null) {
                throw new RuntimeException("Can't get HLS media playlist");
            }
            mediaPlaylist = childPlaylist.mediaPlaylist;
            mediaPlaylistURI = childPlaylistURI;
        } catch (UnsupportedPlaylistException e) {
            throw new RuntimeException("Can't get HLS media playlist");
        }
    }

    @Override
    public void chooseAudioTrack(int index) {
        int i;
        AlternativeRendition selectedAlternativeRendition = null;
        for (i = 0; i < masterPlaylist.alternativeRenditions().size(); i++) {
            AlternativeRendition alternativeRendition = masterPlaylist.alternativeRenditions().get(i);
            if (alternativeRendition.type() == MediaType.AUDIO) {
                if (index == 0) {
                    selectedAlternativeRendition = alternativeRendition;
                    break;
                }
                index--;
            }
        }
        chosenAudioTrack = i;
        if (selectedAlternativeRendition == null) {
            throw new RuntimeException("The audio track index out of range");
        }
        if (!selectedAlternativeRendition.uri().isPresent()) {
            return;
        }
        URI childPlaylistURI = playlistURI.resolve(selectedAlternativeRendition.uri().get());
        try {
            HLSPlaylist childPlaylist = PlaylistParser.parseHLS(childPlaylistURI, headers);
            if (childPlaylist == null || childPlaylist.mediaPlaylist == null) {
                throw new RuntimeException("Can't get HLS media playlist");
            }
            audioPlaylist = childPlaylist.mediaPlaylist;
            audioPlaylistURI = childPlaylistURI;
        } catch (UnsupportedPlaylistException e) {
            throw new RuntimeException("Can't get HLS media playlist");
        }
    }

    @Override
    public void autoChooseAudioTrack(Quality quality) {
        int chosenId = -1;
        int priority = -1;
        for (int i = 0; i < masterPlaylist.alternativeRenditions().size(); i++) {
            AlternativeRendition alternativeRendition = masterPlaylist.alternativeRenditions().get(i);
            if (alternativeRendition.type() == MediaType.AUDIO) {
                int arPriority = 0;
                if (chosenVideoTrack >= 0) {
                    Variant variant = masterPlaylist.variants().get(chosenVideoTrack);
                    if (variant.audio().isPresent()) {
                        if (alternativeRendition.groupId().equals(variant.audio().get())) {
                            arPriority = setBit(arPriority, AUDIO_GROUP);
                        }
                    }
                }
                if (alternativeRendition.defaultRendition().isPresent()) {
                    if (alternativeRendition.defaultRendition().get()) {
                        arPriority = setBit(arPriority, AUDIO_DEFAULT);
                    }
                }
                if (alternativeRendition.autoSelect().isPresent()) {
                    if (alternativeRendition.defaultRendition().get()) {
                        arPriority = setBit(arPriority, AUDIO_AUTOSELECT);
                    }
                }
                if (arPriority > priority) {
                    priority = arPriority;
                    chosenId = i;
                }
            }
        }
        if (chosenId >= 0) {
            chooseAudioTrack(chosenId);
        }
    }

    private int setBit(int num, int n) {
        return num | 1 << n;
    }

    @Override
    public boolean hasSubtitles() {
        if (masterPlaylist != null) {
            for (AlternativeRendition alternativeRendition: masterPlaylist.alternativeRenditions()) {
                if (alternativeRendition.type() == MediaType.SUBTITLES) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void setDownloadSubtitles(boolean enable) {
        downloadSubtitles = enable;

        subtitlePlaylists = new ArrayList<>();

        for (int i = 0; i < masterPlaylist.alternativeRenditions().size(); i++) {
            AlternativeRendition alternativeRendition = masterPlaylist.alternativeRenditions().get(i);
            if (alternativeRendition.type() == MediaType.SUBTITLES) {
                URI childPlaylistURI = playlistURI.resolve(alternativeRendition.uri().get());
                try {
                    HLSPlaylist childPlaylist = PlaylistParser.parseHLS(childPlaylistURI, headers);
                    if (childPlaylist == null || childPlaylist.mediaPlaylist == null) {
                        throw new RuntimeException("Can't get HLS media playlist");
                    }
                    subtitlePlaylists.add(new SubtitleInfo(alternativeRendition.name(), childPlaylist.mediaPlaylist, childPlaylistURI));
                } catch (UnsupportedPlaylistException e) {
                    throw new RuntimeException("Can't get HLS media playlist");
                }
            }
        }
    }

    @Override
    public boolean shouldDownloadSubtitle() {
        return downloadSubtitles;
    }

    @Override
    public List<Subtitle> getSubtitles() {
        ArrayList<Subtitle> subtitles = new ArrayList<>();
        for (SubtitleInfo subtitleInfo: subtitlePlaylists) {
            if (subtitleInfo.playlist.mediaSegments().size() != 1) {
                System.err.println("Unsupport subtitle format. Ignore");
                continue;
            }
            URI uri = subtitleInfo.uri.resolve(subtitleInfo.playlist.mediaSegments().get(0).uri());
            subtitles.add(new Subtitle(subtitleInfo.name, uri));
        }
        return subtitles;
    }

    @Override
    public boolean isMultiTrackPlaylist() {
        List<String> audioTracksName = getAudioTracksName();
        return audioTracksName != null && audioTracksName.size() > 0;
    }

    @Override
    public void savePlaylist(Path path) {
        if (audioPlaylist == null) {
            MediaPlaylist rewrittenPlaylist = mediaPlaylistRewrite(mediaPlaylist, mediaPlaylistURI);
            byte[] data = mediaPlaylistParser.writePlaylistAsBytes(rewrittenPlaylist);
            try (FileOutputStream playlistStream = new FileOutputStream(path.toFile())) {
                playlistStream.write(data);
            } catch (IOException e) {
                System.err.println("Write permission not allowed: " + e.getMessage());
                System.exit(-1);
            }
        } else {
            MasterPlaylist rewrittenMasterPlaylist = masterPlaylistRewrite(masterPlaylist);
            MediaPlaylist rewrittenVideoPlaylist = mediaPlaylistRewrite(mediaPlaylist, mediaPlaylistURI);
            MediaPlaylist rewrittenAudioPlaylist = mediaPlaylistRewrite(audioPlaylist, audioPlaylistURI);
            Path videoPlaylistPath = path.getParent().resolve("video").resolve("index.m3u8");
            videoPlaylistPath.getParent().toFile().mkdirs();
            Path audioPlaylistPath = path.getParent().resolve("audio").resolve("index.m3u8");
            audioPlaylistPath.getParent().toFile().mkdirs();
            try (FileOutputStream playlistStream = new FileOutputStream(path.toFile())) {
                byte[] data = masterPlaylistParser.writePlaylistAsBytes(rewrittenMasterPlaylist);
                playlistStream.write(data);
            } catch (IOException e) {
                System.err.println("Write permission not allowed: " + e.getMessage());
                System.exit(-1);
            }
            try (FileOutputStream playlistStream = new FileOutputStream(videoPlaylistPath.toFile())) {
                byte[] data = mediaPlaylistParser.writePlaylistAsBytes(rewrittenVideoPlaylist);
                playlistStream.write(data);
            } catch (IOException e) {
                System.err.println("Write permission not allowed: " + e.getMessage());
                System.exit(-1);
            }
            try (FileOutputStream playlistStream = new FileOutputStream(audioPlaylistPath.toFile())) {
                byte[] data = mediaPlaylistParser.writePlaylistAsBytes(rewrittenAudioPlaylist);
                playlistStream.write(data);
            } catch (IOException e) {
                System.err.println("Write permission not allowed: " + e.getMessage());
                System.exit(-1);
            }
        }
    }

    @Override
    public URI getVideoInitializationSegment() {
        if (mediaPlaylist.mediaSegments().get(0).segmentMap().isPresent()) {
            String uri = mediaPlaylist.mediaSegments().get(0).segmentMap().get().uri();
            return mediaPlaylistURI.resolve(uri);
        }
        return null;
    }

    @Override
    public int getVideoSegmentsSize() {
        return mediaPlaylist.mediaSegments().size();
    }

    @Override
    public URI getVideoSegmentURI(int index) {
        String uri = mediaPlaylist.mediaSegments().get(index).uri();
        return mediaPlaylistURI.resolve(uri);
    }

    @Override
    public URI getAudioInitializationSegment() {
        if (audioPlaylist != null) {
            if (audioPlaylist.mediaSegments().get(0).segmentMap().isPresent()) {
                String uri = audioPlaylist.mediaSegments().get(0).segmentMap().get().uri();
                return audioPlaylistURI.resolve(uri);
            }
        }
        return null;
    }

    @Override
    public int getAudioSegmentsSize() {
        if (audioPlaylist != null) {
            return audioPlaylist.mediaSegments().size();
        }
        return 0;
    }

    @Override
    public URI getAudioSegmentURI(int index) {
        String uri = audioPlaylist.mediaSegments().get(index).uri();
        return audioPlaylistURI.resolve(uri);
    }

    @Override
    public byte[] getHash() {
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            buffer.write(mediaPlaylistParser.writePlaylistAsBytes(mediaPlaylistWithoutQueries(mediaPlaylist)));
            if (audioPlaylist != null) {
                buffer.write(mediaPlaylistParser.writePlaylistAsBytes(mediaPlaylistWithoutQueries(audioPlaylist)));
            }
            if (subtitlePlaylists != null) {
                for (SubtitleInfo subtitleInfo: subtitlePlaylists) {
                    buffer.write(mediaPlaylistParser.writePlaylistAsBytes(mediaPlaylistWithoutQueries(subtitleInfo.playlist)));
                }
            }
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(buffer.toByteArray());
        } catch (IOException | NoSuchAlgorithmException e) {
            System.err.println("Cannot use SHA-256. File checker disabled.");
        }
        return new byte[0];
    }

    private MediaPlaylist mediaPlaylistRewrite(MediaPlaylist playlist, URI playlistURI) {
        List<URI> keyList = getVideoSegmentKeyList();
        List<MediaSegment> segments = playlist.mediaSegments();
        ArrayList<MediaSegment> rewrittenSegments = new ArrayList<>();
        for (int i = 0; i < segments.size(); i++) {
            MediaSegment segment = segments.get(i);
            Optional<SegmentKey> segmentKey = segment.segmentKey();
            if (segmentKey.isPresent()) {
                if (segmentKey.get().uri().isPresent()) {
                    URI keyURI = playlistURI.resolve(segmentKey.get().uri().get());
                    int keyIndex = keyList.indexOf(keyURI);
                    segmentKey = Optional.of(SegmentKey.builder()
                            .from(segmentKey.get())
                            .uri(keyIndex + ".key")
                            .build());

                }
            }
            Optional<SegmentMap> segmentMap = segment.segmentMap();
            if (segmentMap.isPresent()) {
                String initURI = segmentMap.get().uri();
                segmentMap = Optional.of(SegmentMap.builder()
                        .from(segmentMap.get())
                        .uri("init" + Utils.getExtensionFromURI(initURI))
                        .build());
            }
            MediaSegment rewrittenSegment = MediaSegment.builder()
                    .from(segment)
                    .uri(i + Utils.getExtensionFromURI(segment.uri()))
                    .segmentKey(segmentKey)
                    .segmentMap(segmentMap)
                    .build();
            rewrittenSegments.add(rewrittenSegment);
        }

        return MediaPlaylist.builder()
                .from(playlist)
                .mediaSegments(rewrittenSegments)
                .build();
    }

    private MediaPlaylist mediaPlaylistWithoutQueries(MediaPlaylist playlist) {
        List<MediaSegment> segments = playlist.mediaSegments();
        ArrayList<MediaSegment> rewrittenSegments = new ArrayList<>();
        for (int i = 0; i < segments.size(); i++) {
            MediaSegment segment = segments.get(i);
            Optional<SegmentKey> segmentKey = segment.segmentKey();
            if (segmentKey.isPresent()) {
                if (segmentKey.get().uri().isPresent()) {
                    segmentKey = Optional.of(SegmentKey.builder()
                            .from(segmentKey.get())
                            .uri(uriWithoutQueries(segmentKey.get().uri().get()))
                            .build());

                }
            }
            Optional<SegmentMap> segmentMap = segment.segmentMap();
            if (segmentMap.isPresent()) {
                segmentMap = Optional.of(SegmentMap.builder()
                        .from(segmentMap.get())
                        .uri(uriWithoutQueries(segmentMap.get().uri()))
                        .build());
            }
            MediaSegment rewrittenSegment = MediaSegment.builder()
                    .from(segment)
                    .uri(uriWithoutQueries(segment.uri()))
                    .segmentKey(segmentKey)
                    .segmentMap(segmentMap)
                    .build();
            rewrittenSegments.add(rewrittenSegment);
        }

        return MediaPlaylist.builder()
                .from(playlist)
                .mediaSegments(rewrittenSegments)
                .build();
    }

    private String uriWithoutQueries(String uri) {
        try {
            URI origUri = new URI(uri);
            URI newUri = new URI(origUri.getScheme(), origUri.getAuthority(), origUri.getPath(), null, null);
            return newUri.toString();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return "";
    }

    private MasterPlaylist masterPlaylistRewrite(MasterPlaylist playlist) {
        ArrayList<Variant> variants = new ArrayList<>();
        Variant variant = Variant.builder()
                .from(masterPlaylist.variants().get(chosenVideoTrack))
                .uri("video/index.m3u8")
                .build();
        variants.add(variant);

        ArrayList<AlternativeRendition> alternativeRenditions = new ArrayList<>();

        AlternativeRendition audioAlternativeRendition = masterPlaylist.alternativeRenditions().get(chosenAudioTrack);
        AlternativeRendition rewrittenAudioAlternativeRendition = AlternativeRendition.builder()
                .from(audioAlternativeRendition)
                .uri("audio/index.m3u8")
                .build();
        alternativeRenditions.add(rewrittenAudioAlternativeRendition);

        return MasterPlaylist.builder()
                .from(playlist)
                .variants(variants)
                .alternativeRenditions(alternativeRenditions)
                .build();
    }

    public boolean hasMasterPlaylist() {
        return masterPlaylist != null;
    }

    public List<URI> getVideoSegmentKeyList() {
        if (videoSegmentKeyList == null) {
            ArrayList<URI> keyList = new ArrayList<>();
            for (MediaSegment segment : mediaPlaylist.mediaSegments()) {
                Optional<SegmentKey> segmentKey = segment.segmentKey();
                if (segmentKey.isPresent()) {
                    if (segmentKey.get().method() == KeyMethod.AES_128) {
                        if (segmentKey.get().uri().isPresent()) {
                            URI uri = mediaPlaylistURI.resolve(segmentKey.get().uri().get());
                            if (!keyList.contains(uri)) {
                                keyList.add(uri);
                            }
                        }
                    }
                }
            }
            videoSegmentKeyList = keyList;
            return keyList;
        }
        return videoSegmentKeyList;
    }

    public int getVideoSegmentKeyId(int index) {
        MediaSegment segment = mediaPlaylist.mediaSegments().get(index);
        if (segment.segmentKey().isPresent()) {
            if (segment.segmentKey().get().uri().isPresent()) {
                URI keyURI = mediaPlaylistURI.resolve(segment.segmentKey().get().uri().get());
                return videoSegmentKeyList.indexOf(keyURI);
            }
        }
        return -1;
    }

    public byte[] getVideoSegmentKeyIv(int index) {
        MediaSegment segment = mediaPlaylist.mediaSegments().get(index);
        if (segment.segmentKey().isPresent()) {
            if (segment.segmentKey().get().iv().isPresent()) {
                String iv = segment.segmentKey().get().iv().get();
                try {
                    return Hex.decodeHex(iv.substring(2));
                } catch (DecoderException e) {
                    System.err.println("Wrong IV format");
                    e.printStackTrace();
                    System.exit(-1);
                }
            } else {
                if (segment.segmentKey().get().keyFormat().isPresent() && segment.segmentKey().get().keyFormat().get().equals("identity")) {
                    ByteBuffer buffer = ByteBuffer.allocate(16);
                    buffer.order(ByteOrder.BIG_ENDIAN);
                    buffer.putLong(getVideoSegmentMediaSequenceNumber(index));
                    return buffer.array();
                } else {
                    return new byte[16];
                }
            }
        }
        return null;
    }

    public long getVideoSegmentMediaSequenceNumber(int index) {
        return mediaPlaylist.mediaSequence() + index;
    }

    public List<URI> getAudioSegmentKeyList() {
        ArrayList<URI> keyList = new ArrayList<>();
        if (audioPlaylist != null) {
            for (MediaSegment segment : audioPlaylist.mediaSegments()) {
                Optional<SegmentKey> segmentKey = segment.segmentKey();
                if (segmentKey.isPresent()) {
                    if (segmentKey.get().method() == KeyMethod.AES_128) {
                        if (segmentKey.get().uri().isPresent()) {
                            URI uri = audioPlaylistURI.resolve(segmentKey.get().uri().get());
                            if (!keyList.contains(uri)) {
                                keyList.add(uri);
                            }
                        }
                    }
                }
            }
        }
        return keyList;
    }

    private class SubtitleInfo {
        private String name;
        private MediaPlaylist playlist;
        private URI uri;

        public SubtitleInfo(String name, MediaPlaylist playlist, URI uri) {
            this.name = name;
            this.playlist = playlist;
            this.uri = uri;
        }

        public String getName() {
            return name;
        }

        public MediaPlaylist getPlaylist() {
            return playlist;
        }

        public URI getUri() {
            return uri;
        }
    }
}
