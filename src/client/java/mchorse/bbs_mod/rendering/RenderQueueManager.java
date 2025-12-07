package mchorse.bbs_mod.rendering;

import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.utils.manager.BaseManager;
import mchorse.bbs_mod.utils.manager.storage.CompressedDataStorage;

import java.io.File;
import java.util.function.Supplier;

/**
 * Render queue manager.
 *
 * Manages loading, saving, deleting, and renaming of render queues.
 * Data is stored in config/bbs/data/rendering/ folder.
 */
public class RenderQueueManager extends BaseManager<RenderQueue>
{
    public RenderQueueManager(Supplier<File> folder)
    {
        super(folder);

        this.storage = new CompressedDataStorage();
    }

    @Override
    protected RenderQueue createData(String id, MapType mapType)
    {
        RenderQueue queue = new RenderQueue();

        if (mapType != null)
        {
            queue.fromData(mapType);
        }

        return queue;
    }

    @Override
    protected String getExtension()
    {
        return ".dat";
    }

    /**
     * Rename a queue with duplicate name check.
     *
     * @param from Original queue name
     * @param to New queue name
     * @return true if rename succeeded, false if target name already exists or rename failed
     */
    @Override
    public boolean rename(String from, String to)
    {
        if (this.exists(to))
        {
            return false;
        }

        return super.rename(from, to);
    }

    /**
     * Create a new queue with duplicate name check.
     *
     * @param id Queue name
     * @return Created queue, or null if name already exists
     */
    public RenderQueue createIfNotExists(String id)
    {
        if (this.exists(id))
        {
            return null;
        }

        return this.create(id);
    }

    /**
     * Save a render queue to file.
     *
     * @param name Queue name
     * @param queue Queue to save
     * @return true if save succeeded
     */
    public boolean save(String name, RenderQueue queue)
    {
        return this.save(name, (MapType) queue.toData());
    }
}
