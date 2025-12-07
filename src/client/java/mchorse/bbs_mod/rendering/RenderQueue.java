package mchorse.bbs_mod.rendering;

import mchorse.bbs_mod.settings.values.core.ValueGroup;
import mchorse.bbs_mod.utils.CollectionUtils;

/**
 * Render queue data class.
 *
 * Contains a list of render queue items for batch video rendering.
 */
public class RenderQueue extends ValueGroup
{
    public final RenderQueueItems items = new RenderQueueItems("items");

    public RenderQueue()
    {
        super("");

        this.add(this.items);
    }

    public RenderQueueItem addItem()
    {
        RenderQueueItem item = new RenderQueueItem(String.valueOf(this.items.getList().size()));

        this.items.preNotify();
        this.items.add(item);
        this.items.postNotify();

        return item;
    }

    public void removeItem(RenderQueueItem item)
    {
        int index = CollectionUtils.getIndex(this.items.getList(), item);

        if (CollectionUtils.inRange(this.items.getList(), index))
        {
            this.items.preNotify();
            this.items.getListInternal().remove(index);
            this.items.sync();
            this.items.postNotify();
        }
    }

    public void moveItem(int fromIndex, int toIndex)
    {
        if (!CollectionUtils.inRange(this.items.getList(), fromIndex) ||
            !CollectionUtils.inRange(this.items.getList(), toIndex))
        {
            return;
        }

        this.items.preNotify();

        RenderQueueItem item = this.items.getListInternal().remove(fromIndex);

        this.items.getListInternal().add(toIndex, item);
        this.items.sync();

        this.items.postNotify();
    }

    public void resetAllStatus()
    {
        for (RenderQueueItem item : this.items.getList())
        {
            item.status.set(RenderQueueItem.STATUS_PENDING);
        }
    }

    /**
     * Check if a film with the given ID already exists in the queue.
     */
    public boolean containsFilm(String filmId)
    {
        if (filmId == null || filmId.isEmpty())
        {
            return false;
        }

        for (RenderQueueItem item : this.items.getList())
        {
            if (filmId.equals(item.filmId.get()))
            {
                return true;
            }
        }

        return false;
    }
}
