package mchorse.bbs_mod.ui.rendering;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.rendering.RenderQueueExecutor;
import mchorse.bbs_mod.rendering.RenderQueueItem;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIList;
import mchorse.bbs_mod.utils.colors.Colors;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Render queue item list.
 *
 * Displays render queue items with drag-and-drop reordering support
 * and status color indication (green=success, red=failed).
 */
public class UIRenderQueueItemList extends UIList<RenderQueueItem>
{
    private BiConsumer<Integer, Integer> swapCallback;

    public UIRenderQueueItemList(Consumer<List<RenderQueueItem>> callback)
    {
        super(callback);

        this.sorting();
        this.background();
    }

    public UIRenderQueueItemList swapCallback(BiConsumer<Integer, Integer> callback)
    {
        this.swapCallback = callback;

        return this;
    }

    /**
     * Set list with a mutable copy to allow drag-and-drop reordering.
     */
    public void setListMutable(List<RenderQueueItem> list)
    {
        if (list == null)
        {
            return;
        }

        this.list = new ArrayList<>(list);
        this.update();
    }

    @Override
    protected void handleSwap(int from, int to)
    {
        /* Perform swap on local mutable list */
        if (from >= 0 && from < this.list.size() && to >= 0 && to < this.list.size())
        {
            RenderQueueItem item = this.list.remove(from);

            this.list.add(to, item);
            this.setIndex(to);

            /* Notify callback to sync with data model */
            if (this.swapCallback != null)
            {
                this.swapCallback.accept(from, to);
            }
        }
    }

    @Override
    protected void renderElementPart(UIContext context, RenderQueueItem element, int i, int x, int y, boolean hover, boolean selected)
    {
        int statusColor = this.getStatusColor(element.status.get());
        String filmId = element.filmId.get();
        String displayText = filmId.isEmpty() ? "---" : filmId;

        /* Render status indicator */
        if (statusColor != 0)
        {
            context.batcher.box(x, y, x + 3, y + this.scroll.scrollItemSize, statusColor);
        }

        /* Render film name */
        int textX = statusColor != 0 ? x + 7 : x + 4;
        int textY = y + (this.scroll.scrollItemSize - context.batcher.getFont().getHeight()) / 2;
        int textColor = hover ? Colors.HIGHLIGHT : Colors.WHITE;

        context.batcher.textShadow(displayText, textX, textY, textColor);

        /* Render progress for currently rendering item */
        if (element.status.get() == RenderQueueItem.STATUS_RENDERING)
        {
            RenderQueueExecutor executor = BBSModClient.getRenderQueueExecutor();

            if (executor != null && executor.isRunning() && executor.getCurrentItem() == element)
            {
                String progressText = UIKeys.RENDER_QUEUE_RENDERING.format(executor.getProgressString()).get();
                int progressWidth = context.batcher.getFont().getWidth(progressText);
                int progressX = x + this.area.w - progressWidth - 8;

                context.batcher.textShadow(progressText, progressX, textY, Colors.YELLOW);
            }
        }
    }

    @Override
    protected String elementToString(UIContext context, int i, RenderQueueItem element)
    {
        String filmId = element.filmId.get();

        return filmId.isEmpty() ? "---" : filmId;
    }

    /**
     * Get color for render status.
     *
     * @param status Status code (0=pending, 1=rendering, 2=success, 3=failed)
     * @return Color for the status, or 0 if no indicator needed
     */
    private int getStatusColor(int status)
    {
        if (status == RenderQueueItem.STATUS_RENDERING)
        {
            return Colors.YELLOW | Colors.A100;
        }
        else if (status == RenderQueueItem.STATUS_SUCCESS)
        {
            return Colors.GREEN | Colors.A100;
        }
        else if (status == RenderQueueItem.STATUS_FAILED)
        {
            return Colors.RED | Colors.A100;
        }

        return 0;
    }
}
