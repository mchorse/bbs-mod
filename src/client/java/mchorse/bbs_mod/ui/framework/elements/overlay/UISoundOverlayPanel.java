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
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.utils.UIDataUtils;
import mchorse.bbs_mod.ui.utils.icons.Icons;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class UISoundOverlayPanel extends UIStringOverlayPanel
{
    public UIAudioPlayer player;
    private UIContext context;
    private int renderCounter = 0;

    // Get available sound events from audio assets
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
        this(callback, null);
    }

    public UISoundOverlayPanel(Consumer<Link> callback, UIContext context)
    {
        super(UIKeys.OVERLAYS_SOUNDS_MAIN, getSoundEvents(), null);
        this.context = context;

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

        // Add right-click context menu for adding vanilla sounds
        this.strings.context((menu) -> {
            menu.action(Icons.ADD, UIKeys.ADD_VANILLA_SOUND, () -> {
                this.openVanillaSoundSelector();
            });
        });
    }

    // Opens the vanilla sound selector overlay panel
    private void openVanillaSoundSelector()
    {
        if (this.context != null)
        {
            UIVanillaSoundOverlayPanel vanillaPanel = new UIVanillaSoundOverlayPanel(
                (soundName) -> {
                    // Refresh the sound list after selecting a vanilla sound
                    this.refreshSoundList();
                },
                true,
                this.context,
                this
            );
            
            UIOverlay.addOverlay(this.context, vanillaPanel);
        }
    }

    // Refresh the sound list by reloading sound events
    public void refreshSoundList()
    {
        // Clear the sound cache first
        BBSModClient.getSounds().deleteSounds();
        
        // Clear current list
        this.strings.list.clear();
        
        // Reload sound events
        Set<String> soundEvents = getSoundEvents();
        this.strings.list.add(soundEvents);
        this.strings.list.sort();
        
        // Add "None" option back
        this.strings.list.getList().add(0, UIKeys.GENERAL_NONE.get());
        this.strings.list.update();
        
        // Reapply current filter if any
        this.strings.filter(this.strings.search.getText(), true);
    }

    @Override
    public void render(UIContext context)
    {
        super.render(context);

        // Show right-click animation if the list is empty (only has "None" option)
        List<String> items = this.strings.list.getList();
        boolean onlyHasNone = items.size() == 1 && items.get(0).equals(UIKeys.GENERAL_NONE.get());
        
        if (onlyHasNone || items.isEmpty())
        {
            UIDataUtils.renderRightClickHere(context, this.strings.area);
        }
    }
}