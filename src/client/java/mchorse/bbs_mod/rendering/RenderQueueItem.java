package mchorse.bbs_mod.rendering;

import mchorse.bbs_mod.settings.values.core.ValueGroup;
import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.settings.values.ui.ValueVideoSettings;

/**
 * Render queue item data class.
 *
 * Represents a single item in the render queue, containing the film ID,
 * independent video settings, and render status.
 */
public class RenderQueueItem extends ValueGroup
{
    public static final int STATUS_PENDING = 0;
    public static final int STATUS_RENDERING = 1;
    public static final int STATUS_SUCCESS = 2;
    public static final int STATUS_FAILED = 3;

    public final ValueString filmId = new ValueString("filmId", "");
    public final ValueVideoSettings settings = new ValueVideoSettings("settings");
    public final ValueInt status = new ValueInt("status", STATUS_PENDING, 0, 3);

    public RenderQueueItem(String id)
    {
        super(id);

        this.add(this.filmId);
        this.add(this.settings);
        this.add(this.status);
    }
}
