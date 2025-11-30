package mchorse.bbs_mod.ui.dashboard;

import com.mojang.blaze3d.systems.RenderSystem;
import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.camera.Camera;
import mchorse.bbs_mod.camera.OrbitCamera;
import mchorse.bbs_mod.camera.controller.OrbitCameraController;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.events.register.RegisterDashboardPanelsEvent;
import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.settings.ui.UISettingsOverlayPanel;
import mchorse.bbs_mod.ui.Keys;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.dashboard.panels.IFlightSupported;
import mchorse.bbs_mod.ui.dashboard.panels.UIDashboardPanel;
import mchorse.bbs_mod.ui.dashboard.panels.UIDashboardPanels;
import mchorse.bbs_mod.ui.dashboard.textures.UITextureManagerPanel;
import mchorse.bbs_mod.ui.dashboard.utils.UIGraphPanel;
import mchorse.bbs_mod.ui.dashboard.utils.UIOrbitCamera;
import mchorse.bbs_mod.ui.dashboard.utils.UIOrbitCameraKeys;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.framework.UIBaseMenu;
import mchorse.bbs_mod.ui.framework.UIRenderingContext;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIMessageOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.model_blocks.UIModelBlockPanel;
import mchorse.bbs_mod.ui.morphing.UIMorphingPanel;
import mchorse.bbs_mod.ui.particles.UIParticleSchemePanel;
import mchorse.bbs_mod.ui.selectors.UISelectorsOverlayPanel;
import mchorse.bbs_mod.ui.supporters.UISupportersPanel;
import mchorse.bbs_mod.ui.utility.UIUtilityOverlayPanel;
import mchorse.bbs_mod.ui.utility.audio.UIAudioEditorPanel;
import mchorse.bbs_mod.ui.utils.UIChalkboard;
import mchorse.bbs_mod.ui.utils.UIUtils;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.Direction;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.colors.Colors;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public class UIDashboard extends UIBaseMenu
{
    private UIDashboardPanels panels;

    public UIIcon settings;
    public UIIcon selectors;

    /* Camera data */
    public final UIOrbitCamera orbitUI = new UIOrbitCamera();
    public final UIOrbitCameraKeys orbitKeysUI = new UIOrbitCameraKeys(this);
    public final OrbitCamera orbit = this.orbitUI.orbit;
    public final OrbitCameraController camera = new OrbitCameraController(this.orbit, 5);

    private UISettingsOverlayPanel settingsPanel;
    private Perspective lastPerspective = Perspective.FIRST_PERSON;

    private UIChalkboard chalkboard;

    public UIDashboard()
    {
        super();

        this.orbitUI.setControl(true);

        /* Setup panels */
        this.panels = new UIDashboardPanels();
        this.panels.getEvents().register(UIDashboardPanels.PanelEvent.class, (e) ->
        {
            this.orbitUI.setControl(this.panels.isFlightSupported());

            if (this.panels.panel instanceof IFlightSupported panel)
            {
                this.orbit.setFovRoll(panel.supportsRollFOVControl());
            }

            this.copyCurrentEntityCamera();
        });
        this.panels.full(this.viewport);
        this.registerPanels();

        BBSMod.events.post(new RegisterDashboardPanelsEvent(this));

        this.main.add(this.panels);

        this.settingsPanel = new UISettingsOverlayPanel();

        this.settings = new UIIcon(Icons.SETTINGS, (b) ->
        {
            UIOverlay.addOverlayRight(this.context, this.settingsPanel, 240);
        });
        this.settings.tooltip(UIKeys.CONFIG_TITLE, Direction.TOP);
        this.selectors = new UIIcon(Icons.PROPERTIES, (b) ->
        {
            UIOverlay.addOverlayRight(this.context, new UISelectorsOverlayPanel(), 240);
        });
        this.selectors.tooltip(UIKeys.SELECTORS_TITLE, Direction.TOP);
        this.chalkboard = new UIChalkboard();
        this.chalkboard.full(this.getRoot());

        this.panels.pinned.add(this.settings, this.selectors);
        this.getRoot().prepend(this.orbitUI);
        this.getRoot().add(this.orbitKeysUI);
        this.getRoot().add(this.chalkboard);

        /* Register keys */
        IKey category = UIKeys.DASHBOARD_CATEGORY;

        this.main.keys().register(Keys.CYCLE_PANELS, this::cyclePanels).category(category);
        this.overlay.keys().register(Keys.TOGGLE_VISIBILITY, () ->
        {
            if (this.panels.panel.canToggleVisibility())
            {
                this.main.toggleVisible();
            }
        }).category(category);
        this.overlay.keys().register(Keys.OPEN_UTILITY_PANEL, () ->
        {
            if (UIOverlay.has(this.context))
            {
                return;
            }

            UIOverlay.addOverlay(this.context, new UIUtilityOverlayPanel(UIKeys.UTILITY_TITLE, null), 240, 160);
        });

        this.showAnnoyingPopups();
    }

    private void showAnnoyingPopups()
    {
        if (BBSRendering.isOptifinePresent())
        {
            UIOverlay.addOverlay(this.context, new UIMessageOverlayPanel(
                UIKeys.DASHBOARD_OPTIFINE_EW_TITLE,
                UIKeys.DASHBOARD_OPTIFINE_EW_DESCRIPTION
            ));
        }
    }

    public void copyCurrentEntityCamera()
    {
        Entity cameraEntity = MinecraftClient.getInstance().getCameraEntity();
        Vec3d eyePos = cameraEntity.getEyePos();
        Camera camera = new Camera();

        camera.position.set(eyePos.getX(), eyePos.getY(), eyePos.getZ());
        camera.rotation.set(MathUtils.toRad(cameraEntity.getPitch()), MathUtils.toRad(cameraEntity.getHeadYaw() - 180), 0);
        camera.fov = MathUtils.toRad(MinecraftClient.getInstance().options.getFov().getValue().floatValue());

        this.orbit.setup(camera);
        this.camera.setup(BBSModClient.getCameraController().camera, 0F);
    }

    private void cyclePanels()
    {
        List<UIDashboardPanel> panels = this.panels.panels;

        int direction = Window.isShiftPressed() ? -1 : 1;
        int index = panels.indexOf(this.panels.panel);
        int newIndex = MathUtils.cycler(index + direction, panels);

        this.setPanel(panels.get(newIndex));
        UIUtils.playClick();
    }

    public UIDashboardPanels getPanels()
    {
        return this.panels;
    }

    @Override
    public boolean canPause()
    {
        return this.panels.panel != null && this.panels.panel.canPause();
    }

    @Override
    public boolean canRefresh()
    {
        return this.panels.panel != null && this.panels.panel.canRefresh();
    }

    @Override
    public void onOpen(UIBaseMenu oldMenu)
    {
        super.onOpen(oldMenu);

        this.lastPerspective = MinecraftClient.getInstance().options.getPerspective();

        MinecraftClient.getInstance().options.setPerspective(Perspective.FIRST_PERSON);

        if (oldMenu != this)
        {
            this.panels.open();
            this.setPanel(this.panels.panel);
        }

        BBSModClient.getCameraController().add(this.camera);
    }

    @Override
    public void onClose(UIBaseMenu nextMenu)
    {
        super.onClose(nextMenu);

        if (nextMenu != this)
        {
            this.panels.close();
        }

        this.orbit.reset();
        BBSModClient.getCameraController().remove(this.camera);

        MinecraftClient.getInstance().options.setPerspective(this.lastPerspective);
    }

    @Override
    protected void closeMenu()
    {
        super.closeMenu();

        if (!this.main.isVisible())
        {
            this.main.setVisible(true);
        }
    }

    protected void registerPanels()
    {
        this.panels.registerPanel(new UISupportersPanel(this), UIKeys.SUPPORTERS_TITLE, Icons.USER);
        this.panels.registerPanel(new UIMorphingPanel(this), UIKeys.MORPHING_TITLE, Icons.MORPH);
        this.panels.registerPanel(new UIFilmPanel(this), UIKeys.FILM_TITLE, Icons.FILM);
        this.panels.registerPanel(new UIModelBlockPanel(this), UIKeys.MODEL_BLOCKS_TITLE, Icons.BLOCK);
        this.panels.registerPanel(new UIParticleSchemePanel(this), UIKeys.PANELS_PARTICLES, Icons.PARTICLE).marginLeft(10);
        this.panels.registerPanel(new UITextureManagerPanel(this), UIKeys.TEXTURES_TOOLTIP, Icons.MATERIAL);
        this.panels.registerPanel(new UIAudioEditorPanel(this), UIKeys.AUDIO_TITLE, Icons.SOUND);
        this.panels.registerPanel(new UIGraphPanel(this), UIKeys.GRAPH_TOOLTIP, Icons.GRAPH);

        if (FabricLoader.getInstance().isDevelopmentEnvironment())
        {
            this.panels.registerPanel(new UIDebugPanel(this), IKey.raw("Sandbox"), Icons.CODE);
        }

        this.setPanel(this.getPanel(UISupportersPanel.class));
    }

    public <T> T getPanel(Class<T> clazz)
    {
        return this.panels.getPanel(clazz);
    }

    public void setPanel(UIDashboardPanel panel)
    {
        this.panels.setPanel(panel);
    }

    @Override
    public void update()
    {
        super.update();

        if (this.panels.panel != null)
        {
            this.panels.panel.update();
        }
    }

    @Override
    protected void preRenderMenu(UIRenderingContext context)
    {
        if (!this.main.isVisible())
        {
            if (this.panels.panel != null)
            {
                this.panels.panel.renderPanelBackground(this.context);
            }

            return;
        }

        if (this.panels.panel != null && this.panels.panel.needsBackground())
        {
            this.background(context);
        }
        else
        {
            context.batcher.gradientVBox(0, 0, this.width, this.height / 8, Colors.A25, 0);
            context.batcher.gradientVBox(0, this.height - this.height / 8, this.width, this.height, 0, Colors.A25);
        }
    }

    private void background(UIRenderingContext context)
    {
        Link background = BBSSettings.backgroundImage.get();
        int color = BBSSettings.backgroundColor.get();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        if (background == null)
        {
            context.batcher.box(0, 0, this.width, this.height, color);
        }
        else
        {
            context.batcher.texturedBox(context.getTextures().getTexture(background), color, 0, 0, this.width, this.height, 0, 0, this.width, this.height, this.width, this.height);
        }
    }

    @Override
    public void startRenderFrame(float tickDelta)
    {
        super.startRenderFrame(tickDelta);

        if (this.panels.panel != null)
        {
            this.panels.panel.startRenderFrame(tickDelta);
        }
    }

    public void renderInWorld(WorldRenderContext context)
    {
        super.renderInWorld(context);

        if (this.panels.panel != null)
        {
            this.panels.panel.renderInWorld(context);
        }
    }
}
