package mchorse.bbs_mod.ui.utility;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.BBSResources;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.client.BBSShaders;
import mchorse.bbs_mod.l10n.L10nUtils;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.dashboard.panels.UIDashboardPanels;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIScrollView;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIMessageFolderOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIStatusLogOverlayPanel;
import mchorse.bbs_mod.ui.utility.audio.UIAudioEditorPanel;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.UIUtils;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.Pair;
import mchorse.bbs_mod.utils.StringUtils;
import mchorse.bbs_mod.utils.resources.CDNAssetSyncService;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class UIUtilityOverlayPanel extends UIOverlayPanel
{
    public Runnable callback;

    public UIScrollView view;
    public UITrackpad width;
    public UITrackpad height;

    private Window window;

    public UIUtilityOverlayPanel(IKey title, Runnable callback)
    {
        super(title);

        this.window = MinecraftClient.getInstance().getWindow();
        this.callback = callback;

        this.view = UI.scrollView(5, 10, 140);
        this.view.full(this.content);

        UIButton openGameDirectory = new UIButton(UIKeys.UTILITY_OPEN_GAME_FOLDER, (b) -> this.openFolder(BBSMod.getGameFolder()));
        UIButton openAudioDirectory = new UIButton(UIKeys.UTILITY_OPEN_AUDIO_FOLDER, (b) -> this.openFolder(BBSMod.getAudioFolder()));
        UIButton openModelsDirectory = new UIButton(UIKeys.UTILITY_OPEN_MODELS_FOLDER, (b) -> this.openFolder(BBSMod.getAssetsPath("models")));

        UIIcon textures = new UIIcon(Icons.MATERIAL, (b) ->
        {
            this.print("Reloading textures!");
            BBSModClient.getTextures().delete();
            this.close();
        });
        textures.w(0).tooltip(UIKeys.UTILITY_RELOAD_TEXTURES);
        UIIcon language = new UIIcon(Icons.GLOBE, (b) ->
        {
            this.print("Reloading languages!");
            BBSModClient.getL10n().reload();
            this.close();
        });
        language.w(0).tooltip(UIKeys.UTILITY_RELOAD_LANG);
        UIIcon models = new UIIcon(Icons.POSE, (b) ->
        {
            this.print("Reloading models");
            BBSModClient.getModels().reload();
            this.close();
        });
        models.w(0).tooltip(UIKeys.UTILITY_RELOAD_MODELS);
        UIIcon sounds = new UIIcon(Icons.SOUND, (b) ->
        {
            this.print("Reloading sounds");
            BBSModClient.getSounds().deleteSounds();
            this.close();
        });
        sounds.w(0).tooltip(UIKeys.UTILITY_RELOAD_SOUNDS);
        UIIcon terrain = new UIIcon(Icons.TREE, (b) ->
        {
            this.print("Forcing chunk loader");
            // TODO: this.getContext().menu.bridge.get(IBridgeWorld.class).getWorld().chunks.buildChunks(BBS.getRender(), true);
            BBSShaders.setup();
            this.close();
        });
        terrain.w(0).tooltip(UIKeys.UTILITY_RELOAD_TERRAIN);

        this.width = new UITrackpad((v) ->
        {
            this.window.setWindowedSize((int) this.width.getValue(), (int) this.height.getValue());
        });
        this.height = new UITrackpad((v) ->
        {
            this.window.setWindowedSize((int) this.width.getValue(), (int) this.height.getValue());
        });

        this.width.delayedInput().limit(2, 4096, true).values(2, 1, 10).setValue(this.window.getWidth());
        this.height.delayedInput().limit(2, 4096, true).values(2, 1, 10).setValue(this.window.getHeight());

        UIButton analyze = new UIButton(UIKeys.UTILITY_ANALYZE_LANG, (b) -> this.analyzeLanguageStrings());
        UIButton compile = new UIButton(UIKeys.UTILITY_COMPILE_LANG, (b) -> this.compileLanguageStrings());
        UIButton langEditor = new UIButton(UIKeys.UTILITY_LANG_EDITOR, (b) -> this.openLangEditor());
        UIButton openAudioEditor = new UIButton(UIKeys.UTILITY_OPEN_AUDIO_EDITOR, (b) -> this.openAudioEditor());
        UIButton defaultCommands = new UIButton(UIKeys.UTILITY_EXECUTE_DEFAULT_COMMANDS, (b) -> this.executeDefaultCommands());

        UIButton cdnDownload = new UIButton(UIKeys.GENERAL_DOWNLOAD, (b) ->
        {
            UIStatusLogOverlayPanel panel = new UIStatusLogOverlayPanel(UIKeys.CDN_DOWNLOADING_TITLE);

            UIOverlay.addOverlay(this.getContext(), panel);

            Thread thread = new Thread(() ->
            {
                BBSResources.stopWatchdog();

                try
                {
                    CDNAssetSyncService syncService = new CDNAssetSyncService(BBSSettings.cdnUrl.get(), BBSMod.getAssetsFolder().toPath(), (p) ->
                    {
                        MinecraftClient.getInstance().execute(() -> panel.list.add(new Pair<>(p.a.color, p.b)));
                    });

                    syncService.syncOnce();
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }

                BBSResources.setupWatchdog();
                
                MinecraftClient.getInstance().execute(() ->
                {
                    BBSModClient.getTextures().delete();
                    BBSModClient.getSounds().deleteSounds();
                    BBSModClient.getModels().reload();
                });
            }, "CDNDownloadThread");

            thread.start();
        });

        UIButton cdnUpload = new UIButton(UIKeys.GENERAL_UPLOAD, (b) ->
        {
            UIStatusLogOverlayPanel panel = new UIStatusLogOverlayPanel(UIKeys.CDN_UPLOADING_TITLE);

            UIOverlay.addOverlay(this.getContext(), panel);

            Thread thread = new Thread(() ->
            {
                try
                {
                    CDNAssetSyncService syncService = new CDNAssetSyncService(BBSSettings.cdnUrl.get(), BBSMod.getAssetsFolder().toPath(), (p) ->
                    {
                        MinecraftClient.getInstance().execute(() -> panel.list.add(new Pair<>(p.a.color, p.b)));
                    });

                    syncService.pushChangedFiles(BBSSettings.cdnToken.get());
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }, "CDNUploadThread");

            thread.start();
        });

        this.view.add(UI.label(UIKeys.UTILITY_OPEN_FOLDER), UI.row(openGameDirectory, openModelsDirectory, openAudioDirectory).marginBottom(8));
        this.view.add(UI.label(UIKeys.UTILITY_RELOAD_LABEL), UI.row(textures, language, models, sounds, terrain));
        this.view.add(defaultCommands.marginBottom(8));
        this.view.add(UI.column(UI.label(UIKeys.UTILITY_RESIZE_WINDOW), UI.row(this.width, this.height)).marginBottom(8));
        this.view.add(UI.label(UIKeys.UTILITY_LANG_LABEL), UI.row(analyze, compile), langEditor.marginBottom(8));
        this.view.add(UI.label(UIKeys.UTILITY_AUDIO), openAudioEditor.marginBottom(8));
        this.view.add(UI.label(IKey.raw("CDN")), UI.row(cdnDownload, cdnUpload));
        this.content.add(this.view);
    }

    private void executeDefaultCommands()
    {
        List<String> commands = Arrays.asList(
            "gamerule doDaylightCycle false",
            "gamerule doWeatherCycle false",
            "gamerule doWardenSpawning false",
            "gamerule doMobSpawning false",
            "gamerule doTraderSpawning false",
            "gamerule randomTickSpeed 3"
        );

        for (String command : commands)
        {
            MinecraftClient.getInstance().player.networkHandler.sendCommand(command);
        }
    }

    private void openFolder(File gameFolder)
    {
        gameFolder.mkdirs();

        UIUtils.openFolder(gameFolder);
    }

    private void openLangEditor()
    {
        UIContext context = this.getContext();

        this.close();

        UIOverlay.addOverlay(context, new UILanguageEditorOverlayPanel(), 0.6F, 0.9F);
    }

    private void openAudioEditor()
    {
        UIContext context = this.getContext();

        this.close();

        for (UIDashboardPanels child : context.menu.getRoot().getChildren(UIDashboardPanels.class))
        {
            child.setPanel(child.getPanel(UIAudioEditorPanel.class));
        }
    }

    private void analyzeLanguageStrings()
    {
        this.print(L10nUtils.analyzeStrings(BBSModClient.getL10n()));
    }

    private void compileLanguageStrings()
    {
        L10nUtils.compile(BBSMod.getExportFolder(), BBSModClient.getL10n().getStrings());

        UIMessageFolderOverlayPanel panel = new UIMessageFolderOverlayPanel(UIKeys.GENERAL_SUCCESS, UIKeys.UTILITY_COMPILE_LANG_DESCRIPTION, BBSMod.getExportFolder());
        UIOverlay.addOverlay(this.getContext(), panel);
    }

    private void print(String string)
    {
        int longest = 0;
        String[] splits = string.split("\n");

        for (String s : splits)
        {
            longest = Math.max(s.length(), longest);
        }

        String separator = StringUtils.repeat("-", longest);

        System.out.println(separator);

        for (String s : splits)
        {
            System.out.println(s);
        }

        System.out.println(separator);
    }

    @Override
    public void resize()
    {
        super.resize();

        this.width.setValue(this.window.getWidth());
        this.height.setValue(this.window.getHeight());
    }

    @Override
    public void onClose()
    {
        super.onClose();

        if (this.callback != null)
        {
            this.callback.run();
        }
    }
}