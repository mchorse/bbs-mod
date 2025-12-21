package mchorse.bbs_mod.ui.film;

import com.mojang.blaze3d.systems.RenderSystem;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.audio.AudioRenderer;
import mchorse.bbs_mod.camera.Camera;
import mchorse.bbs_mod.camera.clips.misc.AudioClip;
import mchorse.bbs_mod.camera.controller.RunnerCameraController;
import mchorse.bbs_mod.camera.data.Angle;
import mchorse.bbs_mod.camera.data.Point;
import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.camera.utils.TimeUtils;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.film.Films;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.settings.ui.UIVideoSettingsOverlayPanel;
import mchorse.bbs_mod.ui.Keys;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.dashboard.panels.UIDashboardPanels;
import mchorse.bbs_mod.ui.film.controller.UIOnionSkinContextMenu;
import mchorse.bbs_mod.ui.film.utils.UICameraUtils;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIMessageFolderOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIMessageOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.framework.elements.utils.EventPropagation;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.UIUtils;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.ui.utils.keys.KeyCodes;
import mchorse.bbs_mod.utils.Direction;
import mchorse.bbs_mod.utils.FFMpegUtils;
import mchorse.bbs_mod.utils.ScreenshotRecorder;
import mchorse.bbs_mod.utils.StringUtils;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.clips.Clips;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.joml.Vectors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import org.joml.Vector2i;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class UIFilmPreview extends UIElement
{
    private List<AudioClip> clips = new ArrayList<>();
    private UIFilmPanel panel;

    public UIElement icons;

    public UIIcon replays;
    public UIIcon onionSkin;
    public UIIcon plause;
    public UIIcon teleport;
    public UIIcon flight;
    public UIIcon control;
    public UIIcon perspective;
    public UIIcon recordReplay;
    public UIIcon recordVideo;

    public UIFilmPreview(UIFilmPanel filmPanel)
    {
        this.panel = filmPanel;

        this.icons = UI.row(0, 0);
        this.icons.row().resize();
        this.icons.relative(this).x(0.5F).y(1F).anchor(0.5F, 1F);

        /* Preview buttons */
        this.replays = new UIIcon(Icons.EDITOR, (b) -> this.openReplays());
        this.replays.tooltip(UIKeys.FILM_REPLAY_TITLE);
        this.onionSkin = new UIIcon(Icons.ONION_SKIN, (b) -> this.openOnionSkin());
        this.onionSkin.tooltip(UIKeys.FILM_CONTROLLER_ONION_SKIN_TITLE);
        this.plause = new UIIcon(() -> this.panel.isRunning() ? Icons.PAUSE : Icons.PLAY, (b) -> this.panel.togglePlayback());
        this.plause.tooltip(UIKeys.CAMERA_EDITOR_KEYS_EDITOR_PLAUSE);
        this.plause.context((menu) ->
        {
            menu.action(Icons.PLAY, UIKeys.CAMERA_EDITOR_KEYS_EDITOR_PLAY_FILM, () ->
            {
                if (!this.panel.checkShowNoCamera())
                {
                    this.panel.dashboard.closeThisMenu();

                    Films.playFilm(this.panel.getData().getId(), true);
                }
            });

            menu.action(Icons.PAUSE, UIKeys.CAMERA_EDITOR_KEYS_EDITOR_FREEZE_PAUSED, !this.panel.getController().isPaused(), () ->
            {
                this.panel.getController().setPaused(!this.panel.getController().isPaused());
            });
        });
        this.teleport = new UIIcon(Icons.MOVE_TO, (b) -> this.panel.teleportToCamera());
        this.teleport.tooltip(UIKeys.FILM_TELEPORT_TITLE);
        this.teleport.context((menu) ->
        {
            menu.action(Icons.MOVE_TO, UIKeys.FILM_TELEPORT_CONTEXT_PLAYER, this.panel.playerToCamera, () -> this.panel.playerToCamera = !this.panel.playerToCamera);
            menu.action(Icons.COPY, UIKeys.CAMERA_PANELS_CONTEXT_COPY_POSITION, () ->
            {
                Position current = new Position(this.panel.getCamera());

                Map<String, Double> map = new LinkedHashMap<>();

                UICameraUtils.copyPoint(map, current.point);
                UICameraUtils.copyAngle(map, current.angle);

                Window.setClipboard(UICameraUtils.mapToString(map));
            });

            Map<String, Double> map = UICameraUtils.stringToMap(Window.getClipboard());

            if (!map.isEmpty())
            {
                menu.action(Icons.PASTE, UIKeys.CAMERA_PANELS_CONTEXT_PASTE_POSITION, () ->
                {
                    Position position = new Position();
                    Point point = UICameraUtils.createPoint(map);
                    Angle angle = UICameraUtils.createAngle(map);

                    if (point != null && angle != null)
                    {
                        position.point.set(point);
                        position.angle.set(angle);
                    }

                    this.panel.cameraEditor.editClip(position);
                });
            }
        });
        this.flight = new UIIcon(Icons.PLANE, (b) -> this.panel.toggleFlight());
        this.flight.tooltip(UIKeys.CAMERA_EDITOR_KEYS_MODES_FLIGHT);
        this.control = new UIIcon(Icons.POSE, (b) -> this.panel.getController().toggleControl());
        this.control.tooltip(UIKeys.FILM_CONTROLLER_KEYS_TOGGLE_CONTROL);
        this.perspective = new UIIcon(this.panel.getController()::getOrbitModeIcon, (b) -> this.panel.getController().toggleOrbitMode());
        this.perspective.tooltip(UIKeys.FILM_CONTROLLER_KEYS_CHANGE_CAMERA_MODE);
        this.recordReplay = new UIIcon(Icons.SPHERE, (b) -> this.panel.getController().pickRecording());
        this.recordReplay.tooltip(UIKeys.FILM_REPLAY_RECORD);
        this.recordReplay.context((menu) ->
        {
            menu.action(Icons.DOWNLOAD, UIKeys.FILM_CONTROLLER_KEYS_TOGGLE_INSTANT_KEYFRAMES, this.panel.getController().isInstantKeyframes(), () ->
            {
                this.panel.getController().toggleInstantKeyframes();
            });
        });
        this.recordVideo = new UIIcon(Icons.VIDEO_CAMERA, (b) ->
        {
            if (this.panel.checkShowNoCamera())
            {
                return;
            }

            if (!FFMpegUtils.checkFFMPEG())
            {
                UIMessageOverlayPanel panel = new UIMessageOverlayPanel(UIKeys.GENERAL_WARNING, UIKeys.GENERAL_FFMPEG_ERROR_DESCRIPTION);
                UIIcon guide = new UIIcon(Icons.HELP, (bb) -> UIUtils.openWebLink(UIKeys.GENERAL_FFMPEG_ERROR_GUIDE_LINK.get()));

                guide.tooltip(UIKeys.GENERAL_FFMPEG_ERROR_GUIDE, Direction.LEFT);
                panel.icons.add(guide);

                UIOverlay.addOverlay(this.getContext(), panel);

                return;
            }

            this.panel.recorder.startRecording(this.panel.getData().camera.calculateDuration(), BBSRendering.getTexture());
        });
        this.recordVideo.tooltip(UIKeys.CAMERA_TOOLTIPS_RECORD);
        this.recordVideo.context((menu) ->
        {
            menu.action(Icons.CAMERA, UIKeys.FILM_SCREENSHOT, () ->
            {
                ScreenshotRecorder recorder = BBSModClient.getScreenshotRecorder();
                Texture texture = BBSRendering.getTexture();

                recorder.takeScreenshot(Window.isAltPressed() ? null : recorder.getScreenshotFile(), texture.id, texture.width, texture.height);

                UIMessageFolderOverlayPanel overlayPanel = new UIMessageFolderOverlayPanel(
                    UIKeys.FILM_SCREENSHOT_TITLE,
                    UIKeys.FILM_SCREENSHOT_DESCRIPTION,
                    recorder.getScreenshots()
                );

                UIOverlay.addOverlay(this.getContext(), overlayPanel);
            });

            menu.action(Icons.FILM, UIKeys.CAMERA_TOOLTIPS_OPEN_VIDEOS, () -> this.panel.recorder.openMovies());
            menu.action(Icons.GEAR, UIKeys.CAMERA_TOOLTIPS_OPEN_VIDEO_SETTINGS, () -> UIOverlay.addOverlay(this.getContext(), new UIVideoSettingsOverlayPanel(BBSSettings.videoSettings)));

            menu.action(Icons.SOUND, UIKeys.FILM_RENDER_AUDIO, this::renderAudio);
            menu.action(Icons.REFRESH, UIKeys.FILM_RESET_REPLAYS, this.panel.recorder.resetReplays, () ->
            {
                this.panel.recorder.resetReplays = !this.panel.recorder.resetReplays;
            });
        });

        this.icons.add(this.replays, this.onionSkin, this.plause, this.teleport, this.flight, this.control, this.perspective, this.recordReplay, this.recordVideo);
        this.add(this.icons);
    }

    public void openReplays()
    {
        UIOverlay overlay = UIOverlay.addOverlayLeft(this.getContext(), this.panel.replayEditor.replays, 360);

        overlay.eventPropagataion(EventPropagation.PASS);
    }

    public void openOnionSkin()
    {
        this.getContext().replaceContextMenu(new UIOnionSkinContextMenu(this.panel, this.panel.getController().getOnionSkin()));
    }

    private void renderAudio()
    {
        Clips camera = this.panel.getData().camera;
        List<AudioClip> audioClips = camera.getClips(AudioClip.class);

        String name = StringUtils.createTimestampFilename() + ".wav";
        File videos = BBSRendering.getVideoFolder();
        UIContext context = this.getContext();
        Vector2i range = BBSSettings.editorLoop.get() ? this.panel.getLoopingRange() : new Vector2i();

        if (AudioRenderer.renderAudio(new File(videos, name), audioClips, camera.calculateDuration(), 48000, TimeUtils.toSeconds(range.x), TimeUtils.toSeconds(range.y)))
        {
            UIOverlay.addOverlay(context, new UIMessageFolderOverlayPanel(UIKeys.GENERAL_SUCCESS, UIKeys.FILM_RENDER_AUDIO_SUCCESS, videos));
        }
        else
        {
            UIOverlay.addOverlay(context, new UIMessageOverlayPanel(UIKeys.GENERAL_ERROR, UIKeys.FILM_RENDER_AUDIO_ERROR));
        }
    }

    public Area getViewport()
    {
        int width = BBSRendering.getVideoWidth();
        int height = BBSRendering.getVideoHeight();
        int w = this.area.w;
        int h = this.area.h;

        Camera camera = new Camera();

        camera.copy(this.panel.getWorldCamera());
        camera.updatePerspectiveProjection(width, height);

        Vector2i size = Vectors.resize(width / (float) height, w, h);
        Area area = new Area();

        area.setSize(size.x, size.y);
        area.setPos(this.area.mx() - area.w / 2, this.area.my() - area.h / 2);

        return area;
    }

    @Override
    protected boolean subMouseClicked(UIContext context)
    {
        Area area = this.getViewport();

        if (area.isInside(context))
        {
            return this.panel.replayEditor.clickViewport(context, area);
        }

        return super.subMouseClicked(context);
    }

    @Override
    public void render(UIContext context)
    {
        Texture texture = BBSRendering.getTexture();
        Area area = this.getViewport();
        Camera camera = this.panel.getCamera();

        camera.copy(this.panel.getWorldCamera());
        camera.view.set(this.panel.lastView);
        camera.projection.set(this.panel.lastProjection);
        context.batcher.flush();

        if (texture != null)
        {
            context.batcher.texturedBox(texture.id, Colors.WHITE, area.x, area.y, area.w, area.h, 0, texture.height, texture.width, 0, texture.width, texture.height);
        }

        this.renderCursor(context);

        /* Render rule of thirds */
        if (BBSSettings.editorRuleOfThirds.get())
        {
            int guidesColor = BBSSettings.editorGuidesColor.get();

            context.batcher.box(area.x + area.w / 3 - 1, area.y, area.x + area.w / 3, area.y + area.h, guidesColor);
            context.batcher.box(area.x + area.w - area.w / 3, area.y, area.x + area.w - area.w / 3 + 1, area.y + area.h, guidesColor);

            context.batcher.box(area.x, area.y + area.h / 3 - 1, area.x + area.w, area.y + area.h / 3, guidesColor);
            context.batcher.box(area.x, area.y + area.h - area.h / 3, area.x + area.w, area.y + area.h - area.h / 3 + 1, guidesColor);
        }

        if (BBSSettings.editorCenterLines.get())
        {
            int guidesColor = BBSSettings.editorGuidesColor.get();
            int x = area.mx();
            int y = area.my();

            context.batcher.box(area.x, y, area.ex(), y + 1, guidesColor);
            context.batcher.box(x, area.y, x + 1, area.ey(), guidesColor);
        }

        if (BBSSettings.editorCrosshair.get())
        {
            int x = area.mx() + 1;
            int y = area.my() + 1;

            context.batcher.box(x - 4, y - 1, x + 3, y, Colors.setA(Colors.WHITE, 0.5F));
            context.batcher.box(x - 1, y - 4, x, y + 3, Colors.setA(Colors.WHITE, 0.5F));
        }

        this.panel.getController().renderHUD(context, area);

        if (this.panel.replayEditor.isVisible())
        {
            RunnerCameraController runner = this.panel.getRunner();
            int w = (int) (area.w * BBSSettings.audioWaveformWidth.get());
            int x = area.x(0.5F, w);
            float tick = this.panel.getCursor() + (runner.isRunning() ? context.getTransition() : 0);

            this.clips.clear();

            for (Clip clip : this.panel.getData().camera.get())
            {
                if (clip instanceof AudioClip)
                {
                    this.clips.add((AudioClip) clip);
                }
            }

            AudioRenderer.renderAll(context.batcher, this.clips, tick, x, area.y + 10, w, BBSSettings.audioWaveformHeight.get(), context.menu.width, context.menu.height);
        }

        Area a = this.icons.area;

        /* Render icon bar */
        context.batcher.gradientVBox(a.x, a.y, a.ex(), a.ey(), 0, Colors.A50);

        if (this.panel.isFlying()) UIDashboardPanels.renderHighlight(context.batcher, this.flight.area);
        if (this.panel.getController().isControlling()) UIDashboardPanels.renderHighlight(context.batcher, this.control.area);
        if (this.panel.getController().isRecording()) UIDashboardPanels.renderHighlight(context.batcher, this.recordReplay.area);
        if (this.panel.recorder.isRecording()) UIDashboardPanels.renderHighlight(context.batcher, this.recordVideo.area);
        if (this.panel.getController().getOnionSkin().enabled.get()) UIDashboardPanels.renderHighlight(context.batcher, this.onionSkin.area);
        if (this.panel.getController().isControlling())
        {
            String s = UIKeys.FILM_CONTROLLER_CONTROL_MODE_TOOLTIP.format(KeyCodes.getName(Keys.FILM_CONTROLLER_TOGGLE_CONTROL.getMainKey())).get();
            int w = context.batcher.getFont().getWidth(s);
            int height = context.batcher.getFont().getHeight();

            context.batcher.textCard(s, a.mx(w), a.y - height - 5);
        }

        context.batcher.clip(this.area, context);
        super.render(context);
        context.batcher.unclip(context);
    }

    private void renderCursor(UIContext context)
    {
        net.minecraft.client.render.Camera mcCamera = MinecraftClient.getInstance().gameRenderer.getCamera();
        MatrixStack stack = RenderSystem.getModelViewStack();

        stack.push();

        stack.multiplyPositionMatrix(context.batcher.getContext().getMatrices().peek().getPositionMatrix());
        stack.translate(area.x + 16, area.ey() - 12, 0F);
        stack.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(mcCamera.getPitch()));
        stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(mcCamera.getYaw()));
        stack.scale(-1F, -1F, -1F);
        RenderSystem.applyModelViewMatrix();
        RenderSystem.renderCrosshair(10);

        stack.pop();
        RenderSystem.applyModelViewMatrix();
    }
}