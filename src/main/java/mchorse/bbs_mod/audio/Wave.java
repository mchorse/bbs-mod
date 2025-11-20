package mchorse.bbs_mod.audio;

import mchorse.bbs_mod.audio.wav.WaveCue;
import mchorse.bbs_mod.audio.wav.WaveList;
import mchorse.bbs_mod.utils.MathUtils;
import org.lwjgl.openal.AL10;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class Wave
{
    public int audioFormat;
    public int numChannels;
    public int sampleRate;
    public int byteRate;
    public int blockAlign;
    public int bitsPerSample;

    public byte[] data;

    public List<WaveList> lists = new ArrayList<>();
    public List<WaveCue> cues = new ArrayList<>();

    public Wave(int audioFormat, int numChannels, int sampleRate, int bitsPerSample, byte[] data)
    {
        int bytesPerSample = bitsPerSample / 8;
        int byteRate = sampleRate * numChannels * bytesPerSample;
        int blockAlign = numChannels * bytesPerSample;

        this.audioFormat = audioFormat;
        this.numChannels = numChannels;
        this.sampleRate = sampleRate;
        this.byteRate = byteRate;
        this.blockAlign = blockAlign;
        this.bitsPerSample = bitsPerSample;
        this.data = data;
    }

    public Wave(int audioFormat, int numChannels, int sampleRate, int byteRate, int blockAlign, int bitsPerSample, byte[] data)
    {
        this.audioFormat = audioFormat;
        this.numChannels = numChannels;
        this.sampleRate = sampleRate;
        this.byteRate = byteRate;
        this.blockAlign = blockAlign;
        this.bitsPerSample = bitsPerSample;
        this.data = data;
    }

    public int getBytesPerSample()
    {
        return this.bitsPerSample / 8;
    }

    public float getDuration()
    {
        return this.data.length / (float) this.numChannels / (float) this.getBytesPerSample() / (float) this.sampleRate;
    }

    public int getALFormat()
    {
        int bytes = this.getBytesPerSample();

        if (bytes == 1)
        {
            if (this.numChannels == 2)
            {
                return AL10.AL_FORMAT_STEREO8;
            }
            else if (this.numChannels == 1)
            {
                return AL10.AL_FORMAT_MONO8;
            }
        }
        else if (bytes == 2)
        {
            if (this.numChannels == 2)
            {
                return AL10.AL_FORMAT_STEREO16;
            }
            else if (this.numChannels == 1)
            {
                return AL10.AL_FORMAT_MONO16;
            }
        }

        throw new IllegalStateException("Current WAV file has unusual configuration... channels: " + this.numChannels + ", BPS: " + bytes);
    }

    public int getScanRegion(float pixelsPerSecond)
    {
        return (int) (this.sampleRate / pixelsPerSecond) * this.getBytesPerSample() * this.numChannels;
    }

    public Wave convertTo16()
    {
        final int bytes = 16 / 8;

        int c = this.data.length / this.numChannels / this.getBytesPerSample();
        int byteRate = this.sampleRate * this.numChannels * bytes;
        byte[] data = new byte[c * this.numChannels * bytes];
        boolean isFloat = this.getBytesPerSample() == 4;

        Wave wave = new Wave(this.audioFormat, this.numChannels, this.sampleRate, byteRate, bytes * this.numChannels, 16, data);

        ByteBuffer sample = MemoryUtil.memAlloc(4);
        ByteBuffer dataBuffer = MemoryUtil.memAlloc(data.length);

        for (int i = 0; i < c * this.numChannels; i++)
        {
            sample.clear();

            for (int j = 0; j < this.getBytesPerSample(); j++)
            {
                sample.put(this.data[i * this.getBytesPerSample() + j]);
            }

            if (isFloat)
            {
                sample.flip();
                float floatValue = sample.getFloat();

                /* Bit depth conversion for float */
                floatValue = Math.max(-1.0f, Math.min(1.0f, floatValue));
                dataBuffer.putShort((short) (floatValue * Short.MAX_VALUE));
            }
            else
            {
                sample.put((byte) 0);
                sample.flip();
                int intValue = sample.getInt();
                
                /* Bit depth conversion for integer */
                if (this.bitsPerSample == 24)
                {
                    double scaledValue = intValue / 8388608.0 * Short.MAX_VALUE;

                    dataBuffer.putShort((short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, (long) scaledValue)));
                }
                else if (this.bitsPerSample == 32)
                {
                    double scaledValue = intValue / 2147483648.0 * Short.MAX_VALUE;

                    dataBuffer.putShort((short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, (long) scaledValue)));
                }
                else
                {
                    double maxOriginalValue = Math.pow(2, this.bitsPerSample - 1) - 1;
                    double scaledValue = intValue / maxOriginalValue * Short.MAX_VALUE;

                    dataBuffer.putShort((short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, (long) scaledValue)));
                }
            }
        }

        dataBuffer.flip();
        dataBuffer.get(data);

        MemoryUtil.memFree(sample);
        MemoryUtil.memFree(dataBuffer);

        wave.lists = this.lists;
        wave.cues = this.cues;

        return wave;
    }

    public float[] getCues()
    {
        float[] cues = new float[this.cues.size()];
        int i = 0;

        for (WaveCue cue : this.cues)
        {
            cues[i] = cue.position / (float) this.sampleRate;

            i += 1;
        }

        return cues;
    }

    /**
     * Improved sample rate conversion with linear interpolation
     * Reduces aliasing distortion and frequency loss during audio mixing
     * Fixed stereo to mono conversion and sample position calculation
     */
    public void add(ByteBuffer buffer, Wave wave, float offset, float shift, float duration)
    {
        int waveStart = this.truncate((int) (shift * wave.byteRate));
        int start = this.truncate((int) (offset * this.byteRate));
        int end = this.truncate((int) ((offset + duration) * this.byteRate));

        end = this.truncate(Math.min(end, this.data.length));

        /* Calculate sample rate ratio for conversion */
        float ratio = (float) wave.sampleRate / (float) this.sampleRate;
        
        
        /* Use linear interpolation for better quality when sample rates differ */
        boolean useLinearInterpolation = Math.abs(ratio - 1.0f) > 0.01f;

        /* Calculate step size based on channel count and sample rate ratio */
        int targetStep = this.numChannels * this.getBytesPerSample();
        int sourceStep = wave.numChannels * wave.getBytesPerSample();

        for (int i = 0; start + i < end; i += targetStep)
        {
            /* Fixed sample position calculation to prevent duration doubling
             * Account for channel count difference in stereo to mono conversion */
            float sampleIndex = (i / targetStep) * ratio;
            float exactPos = waveStart + sampleIndex * sourceStep;
            int a = this.truncate((int) exactPos);
            int b = start + i;

            /* Ensure we don't go beyond the source audio data range */
            int requiredSourceBytes = useLinearInterpolation ? sourceStep * 2 : sourceStep;

            if (a + requiredSourceBytes - 1 >= wave.data.length)
            {
                break;
            }

            /* Ensure we don't go beyond the target audio data range */
            if (b + targetStep - 1 >= this.data.length)
            {
                break;
            }

            int waveShort;
            
            if (useLinearInterpolation)
            {
                /* Linear interpolation for better sample rate conversion */
                waveShort = this.getLinearInterpolatedSample(wave, exactPos);
            }
            else
            {
                /* Direct sample access when sample rates match
                 * Handle stereo to mono conversion by averaging channels */
                if (wave.numChannels == 2 && this.numChannels == 1)
                {
                    /* Stereo to mono conversion: average left and right channels */
                    buffer.position(0);
                    buffer.put(wave.data[a]);
                    buffer.put(wave.data[a + 1]);

                    short leftSample = buffer.getShort(0);
                    
                    buffer.position(0);
                    buffer.put(wave.data[a + 2]);
                    buffer.put(wave.data[a + 3]);

                    short rightSample = buffer.getShort(0);
                    
                    waveShort = (leftSample + rightSample) / 2;
                }
                else
                {
                    /* Direct sample access for same channel configuration */
                    buffer.position(0);
                    buffer.put(wave.data[a]);
                    buffer.put(wave.data[a + 1]);
                    waveShort = buffer.getShort(0);
                }
            }

            buffer.position(0);
            buffer.put(this.data[b]);
            buffer.put(this.data[b + 1]);

            int bytesShort = buffer.getShort(0);
            
            /* Improved audio mixing algorithm with smart volume normalization
             * Convert to float for precise calculations */
            float waveFloat = waveShort / (float) Short.MAX_VALUE;
            float bytesFloat = bytesShort / (float) Short.MAX_VALUE;
            
            /* Calculate sum and check for clipping */
            float sum = waveFloat + bytesFloat;
            
            /* Apply smart normalization only when clipping would occur */
            float mixedFloat;

            if (sum > 1F || sum < -1F)
            {
                /* Dynamic normalization to preserve as much volume as possible */
                float absSum = Math.abs(sum);
                float normalizationFactor = 1F / absSum;

                /* Slight headroom */
                mixedFloat = sum * normalizationFactor * 0.95F;
            }
            else
            {
                /* No clipping, use direct sum to preserve volume */
                mixedFloat = sum;
            }
            
            /* Convert back to short */
            int finalShort = (int) (mixedFloat * Short.MAX_VALUE);

            buffer.putShort(0, (short) MathUtils.clamp(finalShort, Short.MIN_VALUE, Short.MAX_VALUE));

            this.data[b + 1] = buffer.get(1);
            this.data[b] = buffer.get(0);
        }
        
    }
    
    /**
     * Linear interpolation between samples for high-quality sample rate conversion
     * Reduces aliasing and preserves high-frequency content better than nearest neighbor
     * Fixed fraction calculation and stereo to mono handling
     */
    private int getLinearInterpolatedSample(Wave wave, float exactPos)
    {
        int bytesPerSample = wave.getBytesPerSample();
        int sourceStep = wave.numChannels * bytesPerSample;
        
        /* Calculate base index aligned to sample boundaries */
        int baseIndex = this.truncate((int) exactPos);
        /* Fixed fraction calculation to account for actual sample size */
        float fraction = (exactPos - baseIndex) / sourceStep;

        /* Ensure we have valid sample positions */
        int requiredBytes = sourceStep * 2; // Need two samples for interpolation

        if (baseIndex + requiredBytes - 1 >= wave.data.length || baseIndex < 0)
        {
            /* Return silence if out of bounds */
            return 0;
        }

        /* Get the two surrounding samples for interpolation */
        ByteBuffer buffer = MemoryUtil.memAlloc(4);
        
        /* First sample (handle stereo to mono conversion if needed) */
        buffer.position(0);

        short sample1;

        if (wave.numChannels == 2 && this.numChannels == 1)
        {
            /* Stereo to mono: average left and right channels */
            buffer.put(wave.data[baseIndex]);
            buffer.put(wave.data[baseIndex + 1]);

            short leftSample1 = buffer.getShort(0);
            
            buffer.position(0);
            buffer.put(wave.data[baseIndex + 2]);
            buffer.put(wave.data[baseIndex + 3]);

            short rightSample1 = buffer.getShort(0);
            
            sample1 = (short) ((leftSample1 + rightSample1) / 2);
        }
        else
        {
            /* Direct sample access */
            buffer.put(wave.data[baseIndex]);
            buffer.put(wave.data[baseIndex + 1]);

            sample1 = buffer.getShort(0);
        }
        
        /* Second sample (handle stereo to mono conversion if needed) */
        buffer.position(0);

        int nextSampleIndex = baseIndex + sourceStep;
        short sample2;

        if (wave.numChannels == 2 && this.numChannels == 1)
        {
            /* Stereo to mono: average left and right channels */
            buffer.put(wave.data[nextSampleIndex]);
            buffer.put(wave.data[nextSampleIndex + 1]);

            short leftSample2 = buffer.getShort(0);
            
            buffer.position(0);
            buffer.put(wave.data[nextSampleIndex + 2]);
            buffer.put(wave.data[nextSampleIndex + 3]);

            short rightSample2 = buffer.getShort(0);
            
            sample2 = (short) ((leftSample2 + rightSample2) / 2);
        }
        else
        {
            /* Direct sample access */
            buffer.put(wave.data[nextSampleIndex]);
            buffer.put(wave.data[nextSampleIndex + 1]);

            sample2 = buffer.getShort(0);
        }
        
        MemoryUtil.memFree(buffer);
        
        /* Linear interpolation: sample1 + (sample2 - sample1) * fraction */
        float interpolated = sample1 + (sample2 - sample1) * fraction;

        return (int) MathUtils.clamp(interpolated, Short.MIN_VALUE, Short.MAX_VALUE);
    }

    private int truncate(int offset)
    {
        return offset - offset % 2;
    }
}