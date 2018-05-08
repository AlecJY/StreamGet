package com.alebit.sget.convert;

import org.jcodec.common.Codec;
import org.jcodec.common.io.ByteBufferSeekableByteChannel;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.containers.mp4.Brand;
import org.jcodec.containers.mp4.muxer.MP4Muxer;
import org.jcodec.containers.mps.MTSDemuxer;

import java.nio.ByteBuffer;

public class Converter {
    public byte[] convert(byte[] data) {
        ByteBuffer dataBuffer = ByteBuffer.wrap(data);
        MTSDemuxer.MTSPacket packet = MTSDemuxer.parsePacket(dataBuffer);
        SeekableByteChannel byteChannel = ByteBufferSeekableByteChannel.readFromByteBuffer(packet.payload);
        try {
            MP4Muxer muxer = MP4Muxer.createMP4Muxer(byteChannel, Brand.MP4);
            muxer.addAudioTrack(Codec.AAC, )
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
