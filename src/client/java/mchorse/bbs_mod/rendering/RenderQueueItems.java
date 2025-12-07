package mchorse.bbs_mod.rendering;

import mchorse.bbs_mod.settings.values.core.ValueList;

import java.util.List;

/**
 * Render queue items list.
 *
 * A typed list container for RenderQueueItem objects.
 */
public class RenderQueueItems extends ValueList<RenderQueueItem>
{
    public RenderQueueItems(String id)
    {
        super(id);
    }

    /**
     * Get internal list for direct modification.
     * Use with caution - prefer using RenderQueue methods.
     */
    public List<RenderQueueItem> getListInternal()
    {
        return this.list;
    }

    @Override
    protected RenderQueueItem create(String id)
    {
        return new RenderQueueItem(id);
    }
}
