package mchorse.bbs_mod.utils;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.ui.utils.UIUtils;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;
import sun.misc.Unsafe;

import java.io.File;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class VideoRecorder
{
    private Process process;
    private WritableByteChannel channel;
    private boolean recording;

    private ByteBuffer buffer;
    private int textureId = -1;
    private int textureWidth;
    private int textureHeight;
    private int counter;

    public int serverTicks;
    public int lastServerTicks;

    public boolean isRecording()
    {
        return this.recording;
    }

    public int getTextureId()
    {
        return this.textureId;
    }

    public int getCounter()
    {
        return this.counter;
    }

    private int[] pbos;
    private int pboIndex;

    /**
     * Start recording the video using ffmpeg
     */
    public void startRecording(File audioFile, int textureId, int width, int height)
    {
        if (this.recording)
        {
            return;
        }

        this.counter = 0;
        this.textureId = textureId;
        this.textureWidth = width;
        this.textureHeight = height;

        int size = width * height * 3;

        if (this.buffer == null)
        {
            this.buffer = MemoryUtil.memAlloc(size);
        }

        try
        {
            File movies = BBSRendering.getVideoFolder();

            movies.mkdirs();

            Path path = Paths.get(movies.toString());
            String movieName = StringUtils.createTimestampFilename();
            String params = audioFile == null
                ? BBSSettings.videoSettings.arguments.get()
                : BBSSettings.videoSettings.argumentsAudio.get();
            StringBuilder filters = new StringBuilder("vflip");
            float frameRate = (float) BBSRendering.getVideoFrameRate();

            int motionBlur = BBSRendering.getMotionBlur();

            for (int i = 0; i < motionBlur; i++)
            {
                filters.append(",tblend=all_mode=average,framestep=2");
            }

            params = params.replace("%WIDTH%", String.valueOf(width));
            params = params.replace("%HEIGHT%", String.valueOf(height));
            params = params.replace("%FPS%", String.valueOf(frameRate));
            params = params.replace("%NAME%", movieName);
            params = params.replace("%FILTERS%", filters.toString());

            if (audioFile != null)
            {
                params = params.replace("%AUDIO_TRACK%", "\"" + audioFile.getAbsolutePath() + "\"");
            }

            List<String> args = new ArrayList<String>();

            File encoder = FFMpegUtils.getFFMPEG();

            args.add(encoder.getAbsolutePath());
            args.addAll(Arrays.asList(params.split(" ")));

            System.out.println("Recording video with following arguments: " + args);

            this.pbos = new int[2];
            this.pboIndex = 0;

            for (int i = 0; i < 2; i++)
            {
                this.pbos[i] = GL30.glGenBuffers();

                GL30.glBindBuffer(GL30.GL_PIXEL_PACK_BUFFER, this.pbos[i]);
                GL30.glBufferData(GL30.GL_PIXEL_PACK_BUFFER, size, GL30.GL_STREAM_READ);
            }

            GL30.glBindBuffer(GL30.GL_PIXEL_PACK_BUFFER, 0);

            ProcessBuilder builder = new ProcessBuilder(args);
            File log = path.resolve(movieName.concat(".log")).toFile();

            if (!BBSSettings.videoEncoderLog.get())
            {
                log = BBSMod.getSettingsPath("video.log");
            }

            builder.directory(path.toFile());
            builder.redirectErrorStream(true);
            builder.redirectOutput(log);

            this.process = builder.start();

            /**
             * Java wraps the process output stream into a BufferedOutputStream,
             *
             * but its little buffer is just slowing everything down with the
             * huge amount of data we're dealing here, so unwrap it with this little
             * hack.
             */
            OutputStream os = this.process.getOutputStream();
            Unsafe unsafe = UnsafeUtils.getUnsafe();

            if (os instanceof FilterOutputStream)
            {
                try
                {
                    Field outField = FilterOutputStream.class.getDeclaredField("out");

                    os = (OutputStream) unsafe.getObject(os, unsafe.objectFieldOffset(outField));
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }

            this.channel = Channels.newChannel(os);
            this.recording = true;

            UIUtils.playClick(2F);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        this.serverTicks = this.lastServerTicks = 0;
    }

    /**
     * Stop recording
     */
    public void stopRecording()
    {
        if (!this.recording)
        {
            return;
        }

        if (this.pbos != null)
        {
            for (int pbo : this.pbos)
            {
                GL30.glDeleteBuffers(pbo);
            }
        }

        this.pbos = null;
        this.textureId = -1;

        if (this.buffer != null)
        {
            MemoryUtil.memFree(this.buffer);

            this.buffer = null;
        }

        try
        {
            if (this.channel != null && this.channel.isOpen())
            {
                this.channel.close();
            }

            this.channel = null;
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
        }

        try
        {
            if (this.process != null)
            {
                this.process.waitFor(1, TimeUnit.MINUTES);
                this.process.destroy();
            }

            this.process = null;
        }
        catch (InterruptedException ex)
        {
            ex.printStackTrace();
        }

        this.recording = false;

        UIUtils.playClick(0.5F);

        this.serverTicks = this.lastServerTicks = 0;
    }

    /**
     * Record a frame
     */
    public void recordFrame()
    {
        if (!this.recording)
        {
            return;
        }

        try
        {
            int pbo = this.pboIndex;
            int nextPbo = (this.pboIndex + 1) % this.pbos.length;

            GL30.glPixelStorei(GL30.GL_PACK_ALIGNMENT, 1);
            GL30.glBindBuffer(GL30.GL_PIXEL_PACK_BUFFER, this.pbos[pbo]);
            GL30.glBindTexture(GL30.GL_TEXTURE_2D, this.textureId);
            GL30.glGetTexImage(GL30.GL_TEXTURE_2D, 0, GL30.GL_BGR, GL30.GL_UNSIGNED_BYTE, 0);

            GL30.glBindBuffer(GL30.GL_PIXEL_PACK_BUFFER, this.pbos[nextPbo]);

            ByteBuffer mappedBuffer = GL30.glMapBuffer(GL30.GL_PIXEL_PACK_BUFFER, GL30.GL_READ_ONLY);

            if (mappedBuffer != null && this.counter != 0)
            {
                this.channel.write(mappedBuffer);
            }

            GL30.glUnmapBuffer(GL30.GL_PIXEL_PACK_BUFFER);
            GL30.glBindBuffer(GL30.GL_PIXEL_PACK_BUFFER, 0);

            this.pboIndex = nextPbo;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        this.counter += 1;
    }

    /**
     * Toggle recording of the video
     */
    public void toggleRecording(int textureId, int textureWidth, int textureHeight)
    {
        if (this.recording)
        {
            this.stopRecording();
        }
        else
        {
            this.startRecording(null, textureId, textureWidth, textureHeight);
        }

        UIUtils.playClick();
    }
}