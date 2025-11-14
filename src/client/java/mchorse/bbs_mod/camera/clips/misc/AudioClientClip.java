package mchorse.bbs_mod.camera.clips.misc;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.audio.SoundPlayer;
import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.camera.utils.TimeUtils;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.clips.ClipContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AudioClientClip extends AudioClip
{
    public AudioClientClip()
    {
        super();
    }

    public static Map<Link, Float> getPlayback(ClipContext context)
    {
        return context.clipData.get("audio", ConcurrentHashMap::new);
    }

    public static void manageSounds(ClipContext context)
    {
        Map<Link, Float> playback = getPlayback(context);

        for (Map.Entry<Link, Float> entry : playback.entrySet())
        {
            float tickTime = entry.getValue();
            SoundPlayer player = BBSModClient.getSounds().playUnique(entry.getKey());

            if (player == null)
            {
                continue;
            }

            if (tickTime < 0 || tickTime >= player.getBuffer().getDuration())
            {
                if (player.isPlaying())
                {
                    player.pause();
                }

                continue;
            }

            float time = player.getPlaybackPosition();
            float diff = Math.abs(tickTime - time);

            if (context.playing && !player.isPlaying())
            {
                player.play();
            }
            else if (!context.playing && player.isPlaying())
            {
                player.pause();
            }

            if (diff > 0.05F)
            {
                player.setPlaybackPosition(tickTime);
            }
        }
    }

    @Override
    public boolean isGlobal()
    {
        return true;
    }

    @Override
    public void shutdown(ClipContext context)
    {
        Link link = this.audio.get();

        if (link != null)
        {
            BBSModClient.getSounds().stop(link);
        }
    }

    @Override
    protected void applyClip(ClipContext context, Position position)
    {
        Link link = this.audio.get();

        if (link != null)
        {
            SoundPlayer player = BBSModClient.getSounds().playUnique(link);

            if (player == null)
            {
                return;
            }

            float tickTime = (context.relativeTick + context.transition) / 20F;
            Map<Link, Float> playback = getPlayback(context);

            if (context.relativeTick >= this.duration.get() || tickTime < 0)
            {
                playback.putIfAbsent(link, -1F);
            }
            else
            {
                playback.put(link, TimeUtils.toSeconds(this.offset.get()) + tickTime);
            }
        }
    }

    @Override
    protected Clip create()
    {
        return new AudioClientClip();
    }
}