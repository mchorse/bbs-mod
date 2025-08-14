package mchorse.bbs_mod.ui.framework.elements.overlay;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.audio.AudioReader;
import mchorse.bbs_mod.audio.ColorCode;
import mchorse.bbs_mod.audio.SoundManager;
import mchorse.bbs_mod.audio.SoundPlayer;
import mchorse.bbs_mod.audio.Wave;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.screenplay.UIAudioPlayer;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class UISoundOverlayPanel extends UIStringOverlayPanel
{
    public UIAudioPlayer player;

    private static Set<String> getSoundEvents()
    {
        Set<String> locations = new HashSet<>();

        for (Link link : BBSMod.getProvider().getLinksFromPath(Link.assets("audio")))
        {
            String pathLower = link.path.toLowerCase();

            boolean supportedExtension = pathLower.endsWith(".wav") || pathLower.endsWith(".ogg");

            if (supportedExtension)
            {
                locations.add(link.toString());
            }
        }

        return locations;
    }

    public UISoundOverlayPanel(Consumer<Link> callback)
    {
        super(UIKeys.OVERLAYS_SOUNDS_MAIN, getSoundEvents(), null);

        this.callback((str) ->
        {
            if (callback != null)
            {
                Link link = Link.create(str);

                callback.accept(link);

                try
                {
                    SoundManager sounds = BBSModClient.getSounds();
                    Wave wave = AudioReader.read(BBSMod.getProvider(), link);
                    SoundPlayer player = this.player.getPlayer();

                    if (player != null)
                    {
                        player.stop();
                    }

                    List<ColorCode> colorCodes = sounds.readColorCodes(link);

                    if (wave.getBytesPerSample() > 2)
                    {
                        wave = wave.convertTo16();
                    }

                    this.player.loadAudio(wave, colorCodes);
                }
                catch (Exception e)
                {}
            }
        });

        this.player = new UIAudioPlayer();

        this.content.add(this.player);
        this.player.relative(this.content).x(6).w(1F, -12).h(20);
        this.strings.y(20).h(1F, -20);
    }
}