package mchorse.bbs_mod.audio.ogg;

import mchorse.bbs_mod.audio.Wave;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.utils.IOUtils;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.stb.STBVorbisInfo;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

public class VorbisReader
{
    public static Wave read(Link link, InputStream stream) throws IOException
    {
        try (
            STBVorbisInfo info = STBVorbisInfo.malloc();
            MemoryStack stack = MemoryStack.stackPush()
        ) {
            ByteBuffer buffer = IOUtils.readByteBuffer(stream, 8 * 1024);

            IntBuffer error = stack.mallocInt(1);
            long decoder = STBVorbis.stb_vorbis_open_memory(buffer, error, null);

            if (decoder == MemoryUtil.NULL)
            {
                throw new RuntimeException("Failed to read " + link.toString() + " Vorbis audio... Error code: " + error.get());
            }

            STBVorbis.stb_vorbis_get_info(decoder, info);

            int channels = info.channels();
            int sampleRate = info.sample_rate();
            int samplesLength = STBVorbis.stb_vorbis_stream_length_in_samples(decoder);
            int size = samplesLength * channels;


            ShortBuffer samples = MemoryUtil.memAllocShort(size);

            STBVorbis.stb_vorbis_get_samples_short_interleaved(decoder, channels, samples);
            STBVorbis.stb_vorbis_close(decoder);

            ByteBuffer byteBuffer = MemoryUtil.memAlloc(size * 2);

            for (int i = 0, c = samples.limit(); i < c; i++)
            {
                byteBuffer.putShort(samples.get());
            }

            byteBuffer.flip();

            byte[] finalBytes = new byte[byteBuffer.limit()];

            for (int i = 0, c = byteBuffer.limit(); i < c; i++)
            {
                finalBytes[i] = byteBuffer.get();
            }

            Wave wave = new Wave(1, channels, sampleRate, 16, finalBytes);
            

            MemoryUtil.memFree(buffer);
            MemoryUtil.memFree(samples);
            MemoryUtil.memFree(byteBuffer);

            return wave;
        }
    }
}