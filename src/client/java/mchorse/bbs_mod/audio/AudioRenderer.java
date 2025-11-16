package mchorse.bbs_mod.audio;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.audio.wav.WaveWriter;
import mchorse.bbs_mod.camera.clips.misc.AudioClip;
import mchorse.bbs_mod.camera.utils.TimeUtils;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import mchorse.bbs_mod.utils.StringUtils;
import mchorse.bbs_mod.utils.colors.Colors;
import org.lwjgl.system.MemoryUtil;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AudioRenderer
{
    public static void renderAll(Batcher2D batcher, List<AudioClip> clips, float tick, int x, int y, int w, int h, int sw, int sh)
    {
        if (!BBSSettings.audioWaveformVisible.get())
        {
            return;
        }

        for (AudioClip clip : clips)
        {
            SoundBuffer audio = BBSModClient.getSounds().get(clip.audio.get(), true);

            if (audio != null && audio.getWaveform() != null && clip.isInside((int) tick))
            {
                renderWaveform(batcher, audio, clip, tick, x, y, w, h, sw, sh);

                y += h + 8;
            }
        }
    }

    public static void renderWaveform(Batcher2D batcher, SoundBuffer audio, AudioClip clip, float tick, int x, int y, int w, int h, int sw, int sh)
    {
        final float brightness = 0.45F;
        int half = w / 2;

        /* Draw background */
        batcher.gradientVBox(x + 2, y + 2, x + w - 2, y + h, 0, Colors.A50);
        batcher.box(x + 1, y, x + 2, y + h, 0xaaffffff);
        batcher.box(x + w - 2, y, x + w - 1, y + h, 0xaaffffff);
        batcher.box(x, y + h - 1, x + w, y + h, 0xffffffff);

        batcher.clip(x + 2, y + 2, w - 4, h - 4, sw, sh);

        Waveform wave = audio.getWaveform();

        float duration = w / (float) wave.getPixelsPerSecond();
        float playback = TimeUtils.toSeconds(tick - clip.tick.get() + clip.offset.get());
        int offset = (int) (playback * wave.getPixelsPerSecond());
        int waveW = wave.getWidth();

        /* Draw the waveform */
        int runningOffset = waveW - offset;

        if (runningOffset > 0)
        {
            wave.render(batcher, Colors.WHITE, x + half, y, half, h, playback, playback + duration / 2);
        }

        /* Draw the passed waveform */
        if (offset > 0)
        {
            int color = Colors.COLOR.set(brightness, brightness, brightness, 1F).getARGBColor();

            wave.render(batcher, color, x, y, half, h, playback - duration / 2, playback);
        }

        batcher.unclip(sw, sh);

        batcher.box(x + half, y + 1, x + half + 1, y + h - 1, 0xff57f52a);

        FontRenderer fontRenderer = batcher.getFont();

        if (BBSSettings.audioWaveformFilename.get())
        {
            batcher.textCard(audio.getId().toString(), x + 8, y + h / 2 - 4, 0xffffff, 0x99000000);
        }

        if (BBSSettings.audioWaveformTime.get())
        {
            int milliseconds = (int) (tick % 20 == 0 ? 0 : tick % 20 * 5D);

            String tickLabel = tick + "t (" + (int) playback + "." + StringUtils.leftPad(String.valueOf(milliseconds), 2, "0") + "s)";

            batcher.textCard(tickLabel, x + w - 8 - fontRenderer.getWidth(tickLabel), y + h / 2 - 4, 0xffffff, 0x99000000);
        }
    }

    public static boolean renderAudio(File file, List<AudioClip> clips, int totalDuration, int sampleRate)
    {
        float total = totalDuration / 20F;
        Map<AudioClip, Wave> map = new HashMap<>();


        for (AudioClip clip : clips)
        {
            if (!clip.enabled.get())
            {
                continue;
            }

            try
            {
                Wave wave = AudioReader.read(BBSMod.getProvider(), clip.audio.get());
                map.put(clip, wave);
                
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        if (map.isEmpty())
        {
            return false;
        }

        int byteRate = sampleRate * 2;
        int totalBytes = (int) Math.ceil(total * byteRate);
        // 确保缓冲区大小足够容纳所有音频数据，并保持字节对齐
        byte[] bytes = new byte[totalBytes + (totalBytes % 2)];
        Wave finalWave = new Wave(1, 1, sampleRate, 16, bytes);
        ByteBuffer buffer = MemoryUtil.memAlloc(2);
        

        for (AudioClip clip : clips)
        {
            try
            {
                Wave wave = map.get(clip);
                if (wave != null) {
                    
                    finalWave.add(buffer, wave,
                        TimeUtils.toSeconds(clip.tick.get()),
                        TimeUtils.toSeconds(clip.offset.get()),
                        TimeUtils.toSeconds(clip.duration.get())
                    );
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        MemoryUtil.memFree(buffer);

        try
        {
            WaveWriter.write(file, finalWave);

            return true;
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return false;
    }
}