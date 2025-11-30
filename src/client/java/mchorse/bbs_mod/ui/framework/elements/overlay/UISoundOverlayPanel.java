package mchorse.bbs_mod.ui.framework.elements.overlay;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.audio.AudioCacheManager;
import mchorse.bbs_mod.audio.AudioReader;
import mchorse.bbs_mod.audio.ColorCode;
import mchorse.bbs_mod.audio.SoundLikeManager;
import mchorse.bbs_mod.audio.SoundLikeManager.LikedSound;
import mchorse.bbs_mod.audio.SoundManager;
import mchorse.bbs_mod.audio.SoundPlayer;
import mchorse.bbs_mod.audio.Wave;
import mchorse.bbs_mod.audio.ogg.VorbisReader;
import mchorse.bbs_mod.audio.wav.WaveReader;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.screenplay.UIAudioPlayer;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.input.list.UILikeableStringList;
import mchorse.bbs_mod.ui.framework.elements.input.list.UILikedSoundList;
import mchorse.bbs_mod.ui.framework.elements.input.list.UISearchList;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIVanillaSoundList;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.colors.Colors;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Overlay panel for picking sounds, importing vanilla sounds, and browsing liked sounds.
 * Split into three tabs represented by left-hand side buttons.
 */
public class UISoundOverlayPanel extends UIStringOverlayPanel
{
    private static final int PLAYER_HEIGHT = 24;

    public UIAudioPlayer player;

    private final SoundLikeManager likeManager;
    private final UIIcon folderButton;
    private final UIIcon addButton;
    private final UIIcon likeButton;

    private UISearchList<String> vanillaSounds;
    private UIVanillaSoundList vanillaSoundList;
    private UISearchList<LikedSound> likedSounds;
    private UILikedSoundList likedSoundList;

    private ViewMode currentMode = null;
    private final UIContext context;
    private final Consumer<Link> originalCallback;
    private String selectedSound;

    public UISoundOverlayPanel(Consumer<Link> callback)
    {
        this(callback, null);
    }

    public UISoundOverlayPanel(Consumer<Link> callback, UIContext context)
    {
        super(UIKeys.OVERLAYS_SOUNDS_MAIN, getSoundEvents(), null);

        AudioCacheManager.getInstance().clearAllCache();

        this.context = context;
        this.originalCallback = callback;
        this.likeManager = new SoundLikeManager();

        /* Replace the default list with the like-aware list */
        this.content.remove(this.strings);

        UILikeableStringList likeableList = new UILikeableStringList((list) ->
        {
            if (!list.isEmpty())
            {
                this.pickAudio(list.get(0));
            }
        }, this.likeManager);

        likeableList.scroll.scrollSpeed *= 2;
        likeableList.setEditCallback((soundName) -> {
            if (this.context == null)
            {
                return;
            }
            
            UIPromptOverlayPanel renamePanel = new UIPromptOverlayPanel(
                UIKeys.GENERAL_RENAME,
                UIKeys.GENERAL_RENAME,
                (newName) -> this.renameAudio(soundName, newName)
            );
            renamePanel.text.setText(soundName);
            renamePanel.text.filename();
            
            UIOverlay.addOverlay(this.context, renamePanel);
        });
        
        likeableList.setRemoveCallback((soundName) ->
        {
            if (this.context == null)
            {
                return;
            }
            
            UIConfirmOverlayPanel confirmPanel = new UIConfirmOverlayPanel(
                UIKeys.GENERAL_REMOVE,
                UIKeys.GENERAL_REMOVE,
                (confirmed) -> {
                    if (confirmed) {
                        this.deleteAudio(soundName);
                    }
                }
            );
            
            UIOverlay.addOverlay(this.context, confirmPanel);
        });

        likeableList.setRefreshCallback(this::refreshLikedList);

        this.strings = new UISearchList<>(likeableList);
        this.strings.label(UIKeys.GENERAL_SEARCH).full(this.content).x(6).w(1F, -12);
        this.content.add(this.strings);

        this.player = new UIAudioPlayer();
        this.content.add(this.player);
        this.player.relative(this.content).x(6).w(1F, -12).h(PLAYER_HEIGHT).y(0);

        this.vanillaSoundList = new UIVanillaSoundList((list) ->
        {
            if (!list.isEmpty())
            {
                this.pickAudio(list.get(0));
            }
        }, this.likeManager);
        this.vanillaSoundList.setDownloadCallback((soundLink) ->
        {
            this.refreshSoundList();
            this.refreshVanillaSoundList();
            this.refreshLikedList();
        });
        this.vanillaSoundList.setLikeToggleCallback(() ->
        {
            this.refreshVanillaSoundList();
            this.refreshLikedList();
        });

        this.vanillaSounds = new UISearchList<>(this.vanillaSoundList);
        this.vanillaSounds.label(UIKeys.GENERAL_SEARCH).full(this.content).x(6).w(1F, -12);
        this.vanillaSounds.setVisible(false);
        this.content.add(this.vanillaSounds);

        this.likedSoundList = new UILikedSoundList((list) ->
        {
            if (!list.isEmpty())
            {
                this.pickAudio(list.get(0).getPath());
            }
        });
        this.likedSoundList.setUnlikeCallback((sound) ->
        {
            this.likeManager.setSoundLiked(sound.getPath(), sound.getDisplayName(), false);
            this.refreshLikedList();
        });
        this.likedSounds = new UISearchList<>(this.likedSoundList);
        this.likedSounds.label(UIKeys.GENERAL_SEARCH).full(this.content).x(6).w(1F, -12);
        this.likedSounds.setVisible(false);
        this.content.add(this.likedSounds);

        this.strings.y(PLAYER_HEIGHT).h(1F, -PLAYER_HEIGHT);
        this.vanillaSounds.y(PLAYER_HEIGHT).h(1F, -PLAYER_HEIGHT);
        this.likedSounds.y(PLAYER_HEIGHT).h(1F, -PLAYER_HEIGHT);

        this.folderButton = new UIIcon(Icons.FOLDER, (b) -> this.switchToMode(ViewMode.FOLDER));
        this.folderButton.tooltip(UIKeys.OVERLAYS_SOUNDS_FOLDER_MODE);

        this.addButton = new UIIcon(Icons.ADD, (b) -> this.switchToMode(ViewMode.ADD));
        this.addButton.tooltip(UIKeys.OVERLAYS_SOUNDS_ADD_MODE);

        this.likeButton = new UIIcon(Icons.HEART_ALT, (b) -> this.switchToMode(ViewMode.LIKE));
        this.likeButton.tooltip(UIKeys.OVERLAYS_SOUNDS_LIKE_MODE);

        this.icons.add(this.folderButton, this.addButton, this.likeButton);

        this.callback(this::pickAudio);

        this.refreshSoundList();
        this.refreshVanillaSoundList();
        this.refreshLikedList();
        this.switchToMode(ViewMode.FOLDER);
    }

    private static Set<String> getSoundEvents()
    {
        Set<String> locations = new HashSet<>();

        for (Link link : BBSMod.getProvider().getLinksFromPath(Link.assets("audio")))
        {
            String pathLower = link.path.toLowerCase();
            boolean supported = pathLower.endsWith(".wav") || pathLower.endsWith(".ogg");

            if (supported)
            {
                locations.add(link.toString());
            }
        }

        return locations;
    }

    private void switchToMode(ViewMode mode)
    {
        if (this.currentMode == mode)
        {
            return;
        }

        this.stopCurrentPlayback();

        this.currentMode = mode;

        this.updateButtonStates();

        switch (mode)
        {
            case FOLDER:
                this.showFolderMode();
            break;

            case ADD:
                this.showAddMode();
            break;

            case LIKE:
                this.showLikeMode();
            break;
        }

        this.updateListSelections();
    }

    private void showFolderMode()
    {
        this.strings.setVisible(true);
        this.vanillaSounds.setVisible(false);

        if (this.likedSounds != null)
        {
            this.likedSounds.setVisible(false);
        }

        UILikeableStringList list = (UILikeableStringList) this.strings.list;

        list.setShowOnlyLiked(false);
        list.setShowEditRemoveButtons(true);

        this.refreshSoundList();
        list.update();
    }

    private void showAddMode()
    {
        this.strings.setVisible(false);
        this.vanillaSounds.setVisible(true);

        if (this.likedSounds != null)
        {
            this.likedSounds.setVisible(false);
        }

        this.vanillaSounds.resize();
        this.content.resize();

        this.refreshVanillaSoundList();
    }

    private void showLikeMode()
    {
        this.strings.setVisible(false);
        this.vanillaSounds.setVisible(false);

        if (this.likedSounds != null)
        {
            this.likedSounds.setVisible(true);
        }

        UILikeableStringList list = (UILikeableStringList) this.strings.list;

        list.setShowOnlyLiked(true);
        list.setShowEditRemoveButtons(false);

        this.refreshLikedList();
        list.update();
    }

    private void updateButtonStates()
    {
        this.folderButton.active(this.currentMode == ViewMode.FOLDER);
        this.addButton.active(this.currentMode == ViewMode.ADD);
        this.likeButton.active(this.currentMode == ViewMode.LIKE);
    }

    private void refreshVanillaSoundList()
    {
        if (this.vanillaSoundList == null)
        {
            return;
        }

        this.vanillaSoundList.refresh();

        if (this.vanillaSounds != null)
        {
            String filter = this.vanillaSounds.search.getText();

            this.vanillaSounds.filter(filter, true);
            this.vanillaSounds.resize();
        }
    }

    private void refreshLikedList()
    {
        if (this.likedSoundList == null)
        {
            return;
        }

        this.likedSoundList.setSounds(this.likeManager.getLikedSounds());

        if (this.likedSounds != null)
        {
            String filter = this.likedSounds.search.getText();

            this.likedSounds.filter(filter, true);
            this.likedSounds.resize();
        }
    }

    public void refreshSoundList()
    {
        Set<String> soundEvents = getSoundEvents();

        UILikeableStringList list = (UILikeableStringList) this.strings.list;
        list.getList().clear();

        List<String> sorted = new ArrayList<>(soundEvents);

        sorted.sort(null);
        list.getList().addAll(sorted);
        list.getList().add(0, UIKeys.GENERAL_NONE.get());
        list.update();
        list.sort();

        String filter = this.strings.search.getText();

        this.strings.filter(filter, true);
        this.strings.resize();
        this.refreshLikedList();
    }

    private void pickAudio(String audio)
    {
        this.selectedSound = audio;

        if (audio == null || audio.isEmpty() || audio.equals(UIKeys.GENERAL_NONE.get()))
        {
            if (this.originalCallback != null)
            {
                this.originalCallback.accept(null);
            }

            return;
        }

        try
        {
            SoundManager sounds = BBSModClient.getSounds();
            Link link = null;
            Wave wave = null;

            SoundPlayer current = this.player.getPlayer();

            if (current != null)
            {
                current.stop();
            }

            if (this.currentMode == ViewMode.ADD && !audio.startsWith("assets:"))
            {
                File tempFile = this.vanillaSoundList.getTemporaryFileForSound(audio);

                if (tempFile != null && tempFile.exists())
                {
                    try (java.io.FileInputStream fis = new java.io.FileInputStream(tempFile))
                    {
                        String pathLower = tempFile.getName().toLowerCase();
                        
                        if (pathLower.endsWith(".wav"))
                        {
                            wave = new WaveReader().read(fis);
                        }
                        else if (pathLower.endsWith(".ogg"))
                        {
                            Link tempLink = new Link("cache", tempFile.getName());

                            wave = VorbisReader.read(tempLink, fis);
                        }
                    }
                }
            }
            else
            {
                link = Link.create(audio);

                if (BBSMod.getProvider().getFile(link) == null)
                {
                    return;
                }

                wave = AudioReader.read(BBSMod.getProvider(), link);
            }

            if (wave != null)
            {
                List<ColorCode> colorCodes = link != null ? sounds.readColorCodes(link) : new ArrayList<>();

                if (wave.getBytesPerSample() > 2)
                {
                    wave = wave.convertTo16();
                }

                this.player.loadAudio(wave, colorCodes);

                SoundPlayer newPlayer = this.player.getPlayer();

                if (newPlayer != null)
                {
                    BBSModClient.getSounds().deleteSounds();
                    newPlayer.play();
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        this.updateListSelections();

        if (this.originalCallback != null)
        {
            if (audio.startsWith("assets:"))
            {
                this.originalCallback.accept(Link.create(audio));
            }
            else
            {
                String downloaded = this.findDownloadedSoundInAddMode(audio);

                if (downloaded != null)
                {
                    this.originalCallback.accept(Link.create(downloaded));
                }
            }
        }
    }

    private void updateListSelections()
    {
        if (this.selectedSound == null)
        {
            return;
        }

        if (this.strings.list instanceof UILikeableStringList list)
        {
            int index = list.getList().indexOf(this.selectedSound);

            if (index >= 0)
            {
                list.setIndex(index);
            }
        }

        if (this.vanillaSoundList != null)
        {
            int index = this.vanillaSoundList.getList().indexOf(this.selectedSound);

            if (index >= 0)
            {
                this.vanillaSoundList.setIndex(index);
            }
        }

        if (this.likedSoundList != null)
        {
            List<LikedSound> liked = this.likedSoundList.getSounds();

            for (int i = 0; i < liked.size(); i++)
            {
                if (liked.get(i).getPath().equals(this.selectedSound))
                {
                    this.likedSoundList.setIndex(i);

                    break;
                }
            }
        }
    }

    private String findDownloadedSoundInAddMode(String displayName)
    {
        File gameDir = BBSMod.getGameFolder();
        File audioDir = new File(gameDir, "config/bbs/assets/audio");

        if (!audioDir.exists() || !audioDir.isDirectory())
        {
            return null;
        }

        String originalName = displayName;

        if (originalName.startsWith("Music: ") || originalName.startsWith("Sound: "))
        {
            originalName = originalName.substring(7);
        }

        File exactMatch = new File(audioDir, originalName + ".ogg");

        if (exactMatch.exists())
        {
            return "assets:audio/" + originalName + ".ogg";
        }

        for (int suffix = 1; suffix < 100; suffix++)
        {
            File file = new File(audioDir, originalName + "_" + suffix + ".ogg");

            if (file.exists())
            {
                return "assets:audio/" + originalName + "_" + suffix + ".ogg";
            }
        }

        return null;
    }

    @Override
    public void render(UIContext context)
    {
        super.render(context);

        switch (this.currentMode)
        {
            case FOLDER:
                this.renderFolderEmptyState(context);
            break;

            case ADD:
                this.renderAddEmptyState(context);
            break;

            case LIKE:
                this.renderLikeEmptyState(context);
            break;
        }
    }

    private void renderFolderEmptyState(UIContext context)
    {}

    private void renderAddEmptyState(UIContext context)
    {}

    private void renderLikeEmptyState(UIContext context)
    {}

    @Override
    protected void renderBackground(UIContext context)
    {
        super.renderBackground(context);

        if (this.folderButton.isActive())
        {
            this.folderButton.area.render(context.batcher, BBSSettings.primaryColor(Colors.A100));
        }

        if (this.addButton.isActive())
        {
            this.addButton.area.render(context.batcher, BBSSettings.primaryColor(Colors.A100));
        }

        if (this.likeButton.isActive())
        {
            this.likeButton.area.render(context.batcher, BBSSettings.primaryColor(Colors.A100));
        }
    }

    private void renameAudio(String oldName, String newName)
    {
        if (oldName == null || newName == null || oldName.equals(newName))
        {
            return;
        }

        String oldFileName = oldName.replace("assets:audio/", "").replace(".ogg", "");
        String newFileName = newName.replace("assets:audio/", "").replace(".ogg", "");

        File oldFile = new File(BBSMod.getAssetsFolder(), "audio/" + oldFileName + ".ogg");
        File newFile = new File(BBSMod.getAssetsFolder(), "audio/" + newFileName + ".ogg");

        if (newFile.exists())
        {
            return;
        }

        if (!oldFile.exists())
        {
            return;
        }

        if (oldFile.renameTo(newFile))
        {
            if (this.likeManager.isSoundLiked(oldName))
            {
                this.likeManager.removeSound(oldName);
                this.likeManager.setSoundLiked("assets:audio/" + newFileName + ".ogg", newFileName, true);
            }

            this.refreshSoundList();
            this.refreshVanillaSoundList();
            this.refreshLikedList();
        }
    }

    private void deleteAudio(String soundName)
    {
        if (soundName == null)
        {
            return;
        }

        String fileName = soundName.replace("assets:audio/", "").replace(".ogg", "");
        File audioFile = new File(BBSMod.getAssetsFolder(), "audio/" + fileName + ".ogg");

        if (audioFile.exists() && audioFile.delete())
        {
            this.likeManager.removeSound(soundName);

            if (BBSModClient.getSounds() != null)
            {
                BBSModClient.getSounds().stop(Link.assets("audio/" + fileName + ".ogg"));
            }

            this.refreshSoundList();
            this.refreshVanillaSoundList();
            this.refreshLikedList();
        }
    }

    private void stopCurrentPlayback()
    {
        if (this.player != null)
        {
            SoundPlayer current = this.player.getPlayer();

            if (current != null)
            {
                current.stop();
            }
        }

        AudioCacheManager.getInstance().cleanupInvalidCache();
    }

    @Override
    public void onClose()
    {
        super.onClose();

        this.stopCurrentPlayback();
        AudioCacheManager.getInstance().clearAllCache();
    }

    private enum ViewMode
    {
        FOLDER, ADD, LIKE;
    }
}