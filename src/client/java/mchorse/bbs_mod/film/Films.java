package mchorse.bbs_mod.film;

import com.mojang.blaze3d.systems.RenderSystem;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.audio.AudioRenderer;
import mchorse.bbs_mod.camera.clips.misc.AudioClip;
import mchorse.bbs_mod.camera.controller.ICameraController;
import mchorse.bbs_mod.camera.controller.PlayCameraController;
import mchorse.bbs_mod.camera.controller.RunnerCameraController;
import mchorse.bbs_mod.camera.utils.TimeUtils;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.morphing.Morph;
import mchorse.bbs_mod.network.ClientNetwork;
import mchorse.bbs_mod.ui.ContentType;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.CollectionUtils;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Films
{
    private List<BaseFilmController> controllers = new ArrayList<BaseFilmController>();
    private Recorder recorder;

    public Map<String, Map<String, Integer>> actors = new HashMap<>();

    /* Static helpers */

    public static void playFilm(String filmId, boolean withCamera)
    {
        if (ClientNetwork.isIsBBSModOnServer())
        {
            ClientNetwork.sendToggleFilm(filmId, withCamera);
        }
        else
        {
            if (BBSModClient.getFilms().has(filmId))
            {
                stopFilm(filmId);
            }
            else
            {
                ContentType.FILMS.getRepository().load(filmId, (data) ->
                {
                    MinecraftClient.getInstance().execute(() -> playFilm((Film) data, withCamera));
                });
            }
        }
    }

    public static void playFilm(Film film, boolean withCamera)
    {
        FirstPersonFilmController filmController = new FirstPersonFilmController(film);

        if (withCamera && !film.hasFirstPerson())
        {
            PlayCameraController controller = new PlayCameraController(film.camera);

            controller.getContext().entities.putAll(filmController.getEntities());
            BBSModClient.getCameraController().add(controller);
        }

        BBSModClient.getFilms().add(filmController);
    }

    public static void pauseFilm(String filmId)
    {
        if (ClientNetwork.isIsBBSModOnServer())
        {
            ClientNetwork.sendPauseFilm(filmId);
        }
        else
        {
            if (BBSModClient.getFilms().has(filmId))
            {
                togglePauseFilm(filmId);
            }
        }
    }

    public static void togglePauseFilm(String filmId)
    {
        BaseFilmController controller = BBSModClient.getFilms().getController(filmId);

        if (controller != null)
        {
            controller.togglePause();
        }
    }

    public static void stopFilm(String filmId)
    {
        Film film = BBSModClient.getFilms().remove(filmId);
        ICameraController current = BBSModClient.getCameraController().getCurrent();

        if (film != null && current instanceof PlayCameraController play)
        {
            if (play.getContext().clips == film.camera)
            {
                if (BBSModClient.getCameraController().remove(play) instanceof RunnerCameraController controller)
                {
                    controller.getContext().shutdown();
                }
            }
        }
    }

    /* Instance API */

    public BaseFilmController getController(String filmId)
    {
        for (BaseFilmController controller : this.controllers)
        {
            if (controller.film.getId().equals(filmId))
            {
                return controller;
            }
        }

        return null;
    }

    public Recorder getRecorder()
    {
        return this.recorder;
    }

    public void startRecording(Film film, int replayId, int tick)
    {
        Morph morph = Morph.getMorph(MinecraftClient.getInstance().player);

        this.recorder = new Recorder(film, morph == null ? null : morph.getForm(), replayId, tick);

        if (ClientNetwork.isIsBBSModOnServer())
        {
            ClientNetwork.sendActionRecording(film.getId(), replayId, this.recorder.getTick(), this.recorder.countdown, true);
        }

        Replay replay = CollectionUtils.getSafe(film.replays.getList(), replayId);

        if (replay != null)
        {
            ClientNetwork.sendPlayerForm(replay.form.get());
        }
    }

    public Recorder stopRecording()
    {
        Recorder recorder = this.recorder;

        this.recorder = null;

        if (recorder != null)
        {
            for (KeyframeChannel<?> channel : recorder.keyframes.getChannels())
            {
                channel.simplify();
            }

            if (ClientNetwork.isIsBBSModOnServer())
            {
                ClientNetwork.sendActionRecording(recorder.film.getId(), recorder.exception, recorder.initialTick, 0, false);
            }

            recorder.shutdown();
        }

        return recorder;
    }

    public void add(BaseFilmController controller)
    {
        this.controllers.add(controller);
    }

    public boolean has(String filmId)
    {
        for (BaseFilmController controller : this.controllers)
        {
            if (controller.film.getId().equals(filmId))
            {
                return true;
            }
        }

        return false;
    }

    public Film remove(String id)
    {
        Iterator<BaseFilmController> it = this.controllers.iterator();

        while (it.hasNext())
        {
            BaseFilmController next = it.next();

            if (next.film.getId().equals(id))
            {
                next.shutdown();
                it.remove();

                return next.film;
            }
        }

        return null;
    }

    public void updateActors(String filmId, Map<String, Integer> actors)
    {
        this.actors.put(filmId, actors);
    }

    public void startRenderFrame(float transition)
    {
        if (this.recorder != null)
        {
            this.recorder.startRenderFrame(transition);
        }

        for (BaseFilmController controller : this.controllers)
        {
            controller.startRenderFrame(transition);
        }
    }

    public void update()
    {
        this.controllers.removeIf((film) ->
        {
            film.update();

            if (film.hasFinished())
            {
                film.shutdown();
            }

            return film.hasFinished();
        });

        if (this.recorder != null)
        {
            this.recorder.update();
        }
    }

    public void updateEndWorld()
    {
        for (BaseFilmController controller : this.controllers)
        {
            controller.updateEndWorld();
        }
    }

    public void render(WorldRenderContext context)
    {
        RenderSystem.enableDepthTest();

        for (BaseFilmController controller : this.controllers)
        {
            controller.render(context);
        }

        if (this.recorder != null)
        {
            this.recorder.render(context);
        }

        RenderSystem.disableDepthTest();
    }

    public void renderHud(Batcher2D batcher2D, float tickDelta)
    {
        Recorder recorder = BBSModClient.getFilms().getRecorder();

        if (recorder != null && BBSSettings.recordingOverlays.get())
        {
            String label = recorder.hasNotStarted() ?
                String.valueOf(TimeUtils.toSeconds(recorder.countdown)) :
                UIKeys.FILM_RECORDING.format(recorder.getTick()).get();
            int x = 5;
            int y = 5;
            int w = batcher2D.getFont().getWidth(label);

            batcher2D.box(x, y, x + 18 + w + 3, y + 16, Colors.A50);
            batcher2D.icon(Icons.SPHERE, Colors.RED | Colors.A100, x, y);
            batcher2D.textShadow(label, x + 18, y + 4);

            /* Render audio waveform */
            List<AudioClip> audioClips = new ArrayList<>();

            for (Clip clip : recorder.film.camera.get())
            {
                if (clip instanceof AudioClip)
                {
                    audioClips.add((AudioClip) clip);
                }
            }

            int sw = MinecraftClient.getInstance().getWindow().getScaledWidth();
            int sh = MinecraftClient.getInstance().getWindow().getScaledHeight();
            w = (int) (sw * BBSSettings.audioWaveformWidth.get());
            x = sw / 2 - w / 2;
            y = sh / 2 + 100;

            AudioRenderer.renderAll(batcher2D, audioClips, recorder.getTick() + tickDelta, x, y, w, BBSSettings.audioWaveformHeight.get(), sw, sh);
        }
    }

    public void reset()
    {
        controllers.clear();

        recorder = null;
    }
}