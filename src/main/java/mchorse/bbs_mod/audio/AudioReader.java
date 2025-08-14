package mchorse.bbs_mod.audio;

import mchorse.bbs_mod.audio.ogg.VorbisReader;
import mchorse.bbs_mod.audio.wav.WaveReader;
import mchorse.bbs_mod.resources.AssetProvider;
import mchorse.bbs_mod.resources.Link;

import java.io.InputStream;

public class AudioReader
{
    public static Wave read(AssetProvider provider, Link link) throws Exception
    {
        String pathLower = link.path.toLowerCase();

        if (!pathLower.endsWith(".wav") && !pathLower.endsWith(".ogg"))
        {
            return null;
        }

        /* System.out.println("Reading: " + link); */

        try (InputStream asset = provider.getAsset(link))
        {
            if (pathLower.endsWith(".wav"))
            {
                return new WaveReader().read(asset);
            }
            else if (pathLower.endsWith(".ogg"))
            {
                return VorbisReader.read(link, asset);
            }
        }

        throw new IllegalStateException("Given link " + link + " isn't a Wave or a Vorbis file!");
    }
}