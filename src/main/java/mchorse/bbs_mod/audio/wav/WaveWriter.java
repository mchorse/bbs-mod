package mchorse.bbs_mod.audio.wav;

import mchorse.bbs_mod.audio.Wave;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class WaveWriter
{
    public static void write(File file, Wave wave) throws IOException
    {
        write(new FileOutputStream(file), wave);
    }

    public static void write(OutputStream stream, Wave wave) throws IOException
    {
        writeString(stream, "RIFF");
        /* RIFF chunk size calculation */
        writeInt(stream, 36 + wave.data.length);
        writeString(stream, "WAVE");

        writeString(stream, "fmt ");
        writeInt(stream, 16);
        writeShort(stream, wave.audioFormat);
        writeShort(stream, wave.numChannels);

        writeInt(stream, wave.sampleRate);
        writeInt(stream, wave.byteRate);

        writeShort(stream, wave.blockAlign);
        writeShort(stream, wave.bitsPerSample);

        writeString(stream, "data");
        writeInt(stream, wave.data.length);
        stream.write(wave.data);

        stream.close();
    }

    private static void writeString(OutputStream stream, String string) throws IOException
    {
        byte[] bytes = new byte[string.length()];

        for (int i = 0; i < bytes.length; i++)
        {
            bytes[i] = (byte) string.charAt(i);
        }

        stream.write(bytes);
    }

    private static void writeInt(OutputStream stream, int integer) throws IOException
    {
        byte[] bytes = new byte[4];

        bytes[0] = (byte) (integer & 0xff);
        bytes[1] = (byte) ((integer >> 8) & 0xff);
        bytes[2] = (byte) ((integer >> 16) & 0xff);
        bytes[3] = (byte) ((integer >> 24) & 0xff);

        stream.write(bytes);
    }

    private static void writeShort(OutputStream stream, int integer) throws IOException
    {
        byte[] bytes = new byte[2];

        bytes[0] = (byte) (integer & 0xff);
        bytes[1] = (byte) ((integer >> 8) & 0xff);

        stream.write(bytes);
    }
}