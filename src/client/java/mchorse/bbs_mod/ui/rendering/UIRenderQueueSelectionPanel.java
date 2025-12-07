package mchorse.bbs_mod.ui.rendering;

import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.rendering.RenderQueue;
import mchorse.bbs_mod.rendering.RenderQueueManager;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.dashboard.panels.overlay.UICRUDOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.utils.DataPath;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Render queue selection panel.
 *
 * Displays a list of render queues and allows creating, renaming, and deleting queues.
 */
public class UIRenderQueueSelectionPanel extends UICRUDOverlayPanel
{
    protected RenderQueueManager manager;
    protected Consumer<RenderQueue> queueCallback;
    protected BiConsumer<String, RenderQueue> queueWithNameCallback;

    private String lastSelectedName;
    private boolean requireDoubleClick = true;

    public UIRenderQueueSelectionPanel(IKey title, RenderQueueManager manager, Consumer<RenderQueue> queueCallback)
    {
        super(title, null);

        this.manager = manager;
        this.queueCallback = queueCallback;

        this.callback = (name) -> this.handleSelection(name);

        this.icons.remove(this.dupe);
        this.refreshList();
    }

    public UIRenderQueueSelectionPanel(IKey title, RenderQueueManager manager, BiConsumer<String, RenderQueue> queueWithNameCallback)
    {
        super(title, null);

        this.manager = manager;
        this.queueWithNameCallback = queueWithNameCallback;

        this.callback = (name) -> this.handleSelection(name);

        this.icons.remove(this.dupe);
        this.refreshList();
    }

    /**
     * Set to single click mode (default is double click).
     */
    public UIRenderQueueSelectionPanel singleClick()
    {
        this.requireDoubleClick = false;

        return this;
    }

    private void handleSelection(String name)
    {
        if (name == null || name.isEmpty())
        {
            return;
        }

        boolean shouldOpen = !this.requireDoubleClick || name.equals(this.lastSelectedName);

        if (shouldOpen)
        {
            RenderQueue queue = this.manager.load(name);

            if (queue != null)
            {
                if (this.queueCallback != null)
                {
                    this.queueCallback.accept(queue);
                }

                if (this.queueWithNameCallback != null)
                {
                    this.queueWithNameCallback.accept(name, queue);
                }
            }
        }

        this.lastSelectedName = name;
    }

    public void refreshList()
    {
        this.namesList.fill(this.manager.getKeys());
    }

    @Override
    protected void addNewData(String name, MapType data)
    {
        if (name == null || name.trim().isEmpty())
        {
            this.getContext().notifyError(UIKeys.PANELS_MODALS_EMPTY);

            return;
        }

        String trimmedName = name.trim();

        if (this.manager.exists(trimmedName))
        {
            this.getContext().notifyError(UIKeys.RENDER_QUEUE_NAME_EXISTS);

            return;
        }

        RenderQueue queue = this.manager.create(trimmedName);

        if (queue != null)
        {
            this.manager.save(trimmedName, queue);
            this.namesList.addFile(trimmedName);

            if (this.queueCallback != null)
            {
                this.queueCallback.accept(queue);
            }
        }
    }

    @Override
    protected void addNewFolder(String path)
    {
        if (path == null || path.trim().isEmpty())
        {
            this.getContext().notifyError(UIKeys.PANELS_MODALS_EMPTY);

            return;
        }

        if (this.manager.addFolder(path))
        {
            this.refreshList();
        }
    }

    @Override
    protected void dupeData(String name)
    {
        /* Duplicate is disabled for render queues */
    }

    @Override
    protected void renameData(UIIcon element)
    {
        if (!this.namesList.isSelected())
        {
            this.getContext().notifyError(UIKeys.RENDER_QUEUE_NO_SELECTION);

            return;
        }

        super.renameData(element);
    }

    @Override
    protected void renameData(String name)
    {
        if (name == null || name.trim().isEmpty())
        {
            this.getContext().notifyError(UIKeys.PANELS_MODALS_EMPTY);

            return;
        }

        DataPath current = this.namesList.getCurrentFirst();

        if (current == null || current.toString().isEmpty())
        {
            this.getContext().notifyError(UIKeys.RENDER_QUEUE_NO_SELECTION);

            return;
        }

        String oldName = current.toString();
        String newName = name.trim();

        if (oldName.equals(newName))
        {
            return;
        }

        if (this.manager.exists(newName))
        {
            this.getContext().notifyError(UIKeys.RENDER_QUEUE_NAME_EXISTS);

            return;
        }

        if (this.manager.rename(oldName, newName))
        {
            this.refreshList();
            this.namesList.setCurrentFile(newName);
        }
    }

    @Override
    protected void renameFolder(String name)
    {
        if (name == null || name.trim().isEmpty())
        {
            this.getContext().notifyError(UIKeys.PANELS_MODALS_EMPTY);

            return;
        }

        DataPath current = this.namesList.getCurrentFirst();

        if (current == null)
        {
            this.getContext().notifyError(UIKeys.RENDER_QUEUE_NO_SELECTION);

            return;
        }

        String oldPath = current.toString();
        String newPath = name.trim();

        if (this.manager.renameFolder(oldPath, newPath))
        {
            this.refreshList();
        }
    }

    @Override
    protected void removeData(UIIcon element)
    {
        if (!this.namesList.isSelected())
        {
            this.getContext().notifyError(UIKeys.RENDER_QUEUE_NO_SELECTION);

            return;
        }

        super.removeData(element);
    }

    @Override
    protected void removeData()
    {
        DataPath current = this.namesList.getCurrentFirst();

        if (current == null || current.toString().isEmpty())
        {
            this.getContext().notifyError(UIKeys.RENDER_QUEUE_NO_SELECTION);

            return;
        }

        String name = current.toString();

        if (this.manager.delete(name))
        {
            this.refreshList();
        }
    }

    @Override
    protected void removeFolder()
    {
        DataPath current = this.namesList.getCurrentFirst();

        if (current == null)
        {
            this.getContext().notifyError(UIKeys.RENDER_QUEUE_NO_SELECTION);

            return;
        }

        String path = current.toString();

        if (this.manager.deleteFolder(path))
        {
            this.refreshList();
        }
    }
}
