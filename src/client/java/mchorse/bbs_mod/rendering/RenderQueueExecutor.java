package mchorse.bbs_mod.rendering;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.settings.values.ui.ValueVideoSettings;
import mchorse.bbs_mod.ui.ContentType;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.rendering.UIRenderQueueContentPanel;
import mchorse.bbs_mod.ui.utils.UIUtils;
import mchorse.bbs_mod.utils.VideoRecorder;
import net.minecraft.client.MinecraftClient;

import java.util.List;
import java.util.function.Consumer;

/**
 * Render queue executor.
 *
 * Handles batch rendering of multiple films in a render queue.
 * Works with UIFilmPanel to properly render films using the editor's mechanism.
 */
public class RenderQueueExecutor
{
    private static final int DELAY_TICKS = 20;

    private RenderQueue queue;
    private RenderQueueManager manager;
    private String queueName;
    private UIFilmPanel filmPanel;
    private UIRenderQueueContentPanel contentPanel;

    private int currentIndex = -1;
    private boolean running;
    private boolean waitingForFilm;
    private boolean waitingForDelay;
    private int delayCounter;
    private int endTick;

    private Consumer<RenderQueueExecutor> onComplete;
    private Consumer<RenderQueueItem> onItemComplete;

    public RenderQueueExecutor(RenderQueue queue, RenderQueueManager manager, String queueName, UIFilmPanel filmPanel, UIRenderQueueContentPanel contentPanel)
    {
        this.queue = queue;
        this.manager = manager;
        this.queueName = queueName;
        this.filmPanel = filmPanel;
        this.contentPanel = contentPanel;
    }

    public RenderQueueExecutor onComplete(Consumer<RenderQueueExecutor> callback)
    {
        this.onComplete = callback;

        return this;
    }

    public RenderQueueExecutor onItemComplete(Consumer<RenderQueueItem> callback)
    {
        this.onItemComplete = callback;

        return this;
    }

    public boolean isRunning()
    {
        return this.running;
    }

    public int getCurrentIndex()
    {
        return this.currentIndex;
    }

    public RenderQueueItem getCurrentItem()
    {
        List<RenderQueueItem> items = this.queue.items.getList();

        if (this.currentIndex >= 0 && this.currentIndex < items.size())
        {
            return items.get(this.currentIndex);
        }

        return null;
    }

    /**
     * Get the total duration in ticks for the current rendering item.
     */
    public int getEndTick()
    {
        return this.endTick;
    }

    /**
     * Get the current tick position from the film panel.
     */
    public int getCurrentTick()
    {
        if (this.filmPanel != null)
        {
            return this.filmPanel.getCursor();
        }

        return 0;
    }

    /**
     * Get the rendering progress as a percentage (0.0 to 1.0).
     */
    public float getProgress()
    {
        if (this.endTick <= 0)
        {
            return 0F;
        }

        return Math.min(1F, (float) this.getCurrentTick() / (float) this.endTick);
    }

    /**
     * Get progress string for display (e.g., "50%").
     */
    public String getProgressString()
    {
        return String.format("%.0f%%", this.getProgress() * 100F);
    }

    /**
     * Start batch rendering from the specified index.
     */
    public void startFrom(int startIndex)
    {
        if (this.running)
        {
            return;
        }

        List<RenderQueueItem> items = this.queue.items.getList();

        if (startIndex < 0 || startIndex >= items.size())
        {
            return;
        }

        this.running = true;
        this.currentIndex = startIndex;
        this.waitingForFilm = false;
        this.waitingForDelay = false;
        this.delayCounter = 0;

        this.renderNext();
    }

    /**
     * Stop the batch rendering process.
     */
    public void stop()
    {
        if (!this.running)
        {
            return;
        }

        VideoRecorder recorder = BBSModClient.getVideoRecorder();

        if (recorder.isRecording())
        {
            recorder.stopRecording();
        }

        /* Stop film playback if running */
        if (this.filmPanel != null && this.filmPanel.isRunning())
        {
            this.filmPanel.togglePlayback();
        }

        this.running = false;
        this.waitingForFilm = false;
        this.waitingForDelay = false;
        this.currentIndex = -1;

        if (this.onComplete != null)
        {
            this.onComplete.accept(this);
        }
    }

    /**
     * Render the next item in the queue.
     */
    private void renderNext()
    {
        List<RenderQueueItem> items = this.queue.items.getList();

        if (this.currentIndex >= items.size())
        {
            this.stop();

            return;
        }

        RenderQueueItem item = items.get(this.currentIndex);

        this.updateStatus(item, RenderQueueItem.STATUS_RENDERING);

        String filmId = item.filmId.get();

        if (filmId == null || filmId.isEmpty())
        {
            this.updateStatus(item, RenderQueueItem.STATUS_FAILED);
            this.scheduleAdvanceToNext();

            return;
        }

        this.waitingForFilm = true;

        /* Load the film into the editor */
        ContentType.FILMS.getRepository().load(filmId, (data) ->
        {
            MinecraftClient.getInstance().execute(() -> this.onFilmLoaded((Film) data, item));
        });
    }

    /**
     * Called when a film is loaded.
     */
    private void onFilmLoaded(Film film, RenderQueueItem item)
    {
        this.waitingForFilm = false;

        if (!this.running)
        {
            return;
        }

        if (film == null)
        {
            this.updateStatus(item, RenderQueueItem.STATUS_FAILED);
            this.scheduleAdvanceToNext();

            return;
        }

        /* Fill the film panel with the loaded film */
        this.filmPanel.fill(film);

        /* Apply video settings */
        this.applyVideoSettings(item.settings);

        /* Calculate duration */
        this.endTick = film.camera.calculateDuration();

        if (this.endTick <= 0)
        {
            this.updateStatus(item, RenderQueueItem.STATUS_FAILED);
            this.scheduleAdvanceToNext();

            return;
        }

        /* Start recording using the film panel's recorder */
        try
        {
            Texture texture = BBSRendering.getTexture();
            int width = item.settings.width.get();
            int height = item.settings.height.get();

            /* Ensure dimensions are even */
            if (width % 2 == 1)
            {
                width -= 1;
            }

            if (height % 2 == 1)
            {
                height -= 1;
            }

            texture.setSize(width, height);

            this.filmPanel.recorder.startRecording(this.endTick, texture.id, width, height);

            UIUtils.playClick(2F);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            this.updateStatus(item, RenderQueueItem.STATUS_FAILED);
            this.scheduleAdvanceToNext();
        }
    }

    /**
     * Apply video settings from the item to the global settings.
     */
    private void applyVideoSettings(ValueVideoSettings settings)
    {
        BBSSettings.videoSettings.arguments.set(settings.arguments.get());
        BBSSettings.videoSettings.argumentsAudio.set(settings.argumentsAudio.get());
        BBSSettings.videoSettings.audio.set(settings.audio.get());
        BBSSettings.videoSettings.width.set(settings.width.get());
        BBSSettings.videoSettings.height.set(settings.height.get());
        BBSSettings.videoSettings.frameRate.set(settings.frameRate.get());
        BBSSettings.videoSettings.motionBlur.set(settings.motionBlur.get());
        BBSSettings.videoSettings.heldFrames.set(settings.heldFrames.get());
        BBSSettings.videoSettings.path.set(settings.path.get());
    }

    /**
     * Update the status of an item and save the queue.
     */
    private void updateStatus(RenderQueueItem item, int status)
    {
        item.status.set(status);
        this.saveQueue();

        if (this.onItemComplete != null && (status == RenderQueueItem.STATUS_SUCCESS || status == RenderQueueItem.STATUS_FAILED))
        {
            this.onItemComplete.accept(item);
        }
    }

    /**
     * Save the queue to disk.
     */
    private void saveQueue()
    {
        if (this.manager != null && this.queueName != null)
        {
            this.manager.save(this.queueName, this.queue);
        }
    }

    /**
     * Schedule advancing to the next item with a delay.
     */
    private void scheduleAdvanceToNext()
    {
        this.currentIndex++;
        this.waitingForDelay = true;
        this.delayCounter = 0;
    }

    /**
     * Called when recording completes for the current item.
     */
    public void onRecordingComplete(boolean success)
    {
        if (!this.running)
        {
            return;
        }

        RenderQueueItem item = this.getCurrentItem();

        if (item != null)
        {
            this.updateStatus(item, success ? RenderQueueItem.STATUS_SUCCESS : RenderQueueItem.STATUS_FAILED);
        }

        UIUtils.playClick(0.5F);

        this.scheduleAdvanceToNext();
    }

    /**
     * Update method to be called each tick.
     * Handles delayed transitions between items.
     */
    public void tick()
    {
        if (!this.running || !this.waitingForDelay)
        {
            return;
        }

        this.delayCounter++;

        if (this.delayCounter >= DELAY_TICKS)
        {
            this.waitingForDelay = false;
            this.delayCounter = 0;

            if (this.currentIndex < this.queue.items.getList().size())
            {
                this.renderNext();
            }
            else
            {
                this.stop();
            }
        }
    }
}
