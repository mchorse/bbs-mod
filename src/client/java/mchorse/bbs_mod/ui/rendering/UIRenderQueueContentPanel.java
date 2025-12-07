package mchorse.bbs_mod.ui.rendering;

import mchorse.bbs_mod.rendering.RenderQueue;
import mchorse.bbs_mod.rendering.RenderQueueItem;
import mchorse.bbs_mod.rendering.RenderQueueManager;
import mchorse.bbs_mod.settings.ui.UIVideoSettingsOverlayPanel;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlayPanel;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.colors.Colors;

import java.util.function.Consumer;

/**
 * Render queue content panel.
 *
 * Displays the items in a render queue with controls for managing
 * and executing the render queue.
 */
public class UIRenderQueueContentPanel extends UIOverlayPanel
{
    private RenderQueueManager manager;
    private RenderQueue queue;
    private String queueName;

    public UIRenderQueueItemList itemList;
    public UIIcon settingsButton;
    public UIIcon removeButton;
    public UIIcon playButton;
    public UIIcon resetButton;

    private Consumer<RenderQueueItem> playCallback;

    public UIRenderQueueContentPanel(RenderQueueManager manager, RenderQueue queue, String queueName)
    {
        super(UIKeys.RENDER_QUEUE_CONTENT_TITLE);

        this.manager = manager;
        this.queue = queue;
        this.queueName = queueName;

        this.setupUI();
        this.refreshList();
    }

    public UIRenderQueueContentPanel playCallback(Consumer<RenderQueueItem> callback)
    {
        this.playCallback = callback;

        return this;
    }

    private void setupUI()
    {
        /* Create item list */
        this.itemList = new UIRenderQueueItemList((items) -> this.updateButtonStates());
        this.itemList.swapCallback((from, to) -> this.handleSwap(from, to));
        this.itemList.full(this.content);

        /* Create buttons */
        this.settingsButton = new UIIcon(Icons.GEAR, (b) -> this.openSettings());
        this.settingsButton.tooltip(UIKeys.RENDER_QUEUE_SETTINGS_TOOLTIP);

        this.removeButton = new UIIcon(Icons.REMOVE, (b) -> this.removeSelected());
        this.removeButton.tooltip(UIKeys.RENDER_QUEUE_REMOVE_TOOLTIP);

        this.playButton = new UIIcon(Icons.PLAY, (b) -> this.startRendering());
        this.playButton.tooltip(UIKeys.RENDER_QUEUE_PLAY_TOOLTIP);

        this.resetButton = new UIIcon(Icons.REFRESH, (b) -> this.resetStatus());
        this.resetButton.tooltip(UIKeys.RENDER_QUEUE_RESET_TOOLTIP);

        /* Add buttons to icons bar */
        this.icons.add(this.resetButton);
        this.icons.add(this.playButton);
        this.icons.add(this.settingsButton);
        this.icons.add(this.removeButton);

        this.content.add(this.itemList);

        this.updateButtonStates();
    }

    public void refreshList()
    {
        this.itemList.setListMutable(this.queue.items.getList());
        this.updateButtonStates();
    }

    private void updateButtonStates()
    {
        boolean hasSelection = this.itemList.isSelected();
        boolean hasItems = !this.queue.items.getList().isEmpty();

        this.settingsButton.setEnabled(hasSelection);
        this.removeButton.setEnabled(hasSelection);
        this.playButton.setEnabled(hasSelection);
        this.resetButton.setEnabled(hasItems);
    }

    private void handleSwap(int fromIndex, int toIndex)
    {
        if (fromIndex != toIndex && fromIndex >= 0 && toIndex >= 0)
        {
            this.queue.moveItem(fromIndex, toIndex);
            this.saveQueue();
        }
    }

    private void openSettings()
    {
        RenderQueueItem item = this.itemList.getCurrentFirst();

        if (item == null)
        {
            return;
        }

        UIVideoSettingsOverlayPanel panel = new UIVideoSettingsOverlayPanel(item.settings);

        panel.onClose((e) -> this.saveQueue());

        UIOverlay.addOverlay(this.getContext(), panel, 300, 0.8F);
    }

    private void removeSelected()
    {
        RenderQueueItem item = this.itemList.getCurrentFirst();

        if (item == null)
        {
            return;
        }

        this.queue.removeItem(item);
        this.saveQueue();
        this.refreshList();
    }

    private void startRendering()
    {
        RenderQueueItem item = this.itemList.getCurrentFirst();

        if (item == null)
        {
            return;
        }

        if (this.playCallback != null)
        {
            this.playCallback.accept(item);
        }
    }

    private void resetStatus()
    {
        this.queue.resetAllStatus();
        this.saveQueue();
        this.refreshList();
    }

    private void saveQueue()
    {
        if (this.manager != null && this.queueName != null)
        {
            this.manager.save(this.queueName, this.queue);
        }
    }

    @Override
    public void render(UIContext context)
    {
        super.render(context);

        /* Render empty message if queue is empty */
        if (this.queue.items.getList().isEmpty())
        {
            String message = UIKeys.RENDER_QUEUE_EMPTY_QUEUE.get();
            int textWidth = context.batcher.getFont().getWidth(message);
            int x = this.content.area.mx() - textWidth / 2;
            int y = this.content.area.my() - context.batcher.getFont().getHeight() / 2;

            context.batcher.textShadow(message, x, y, Colors.GRAY);
        }
    }
}
