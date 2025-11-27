package mchorse.bbs_mod.ui.framework.elements.input.list;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.audio.SoundLikeManager;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.colors.Colors;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * List used specifically to render liked sounds with missing asset detection.
 */
public class UILikedSoundList extends UIList<SoundLikeManager.LikedSound>
{
    private final UIIcon unlikeIcon;
    private Consumer<SoundLikeManager.LikedSound> unlikeCallback;

    public UILikedSoundList(Consumer<List<SoundLikeManager.LikedSound>> callback)
    {
        super(callback);
        this.scroll.scrollItemSize = UIStringList.DEFAULT_HEIGHT;
        this.unlikeIcon = new UIIcon(Icons.DISLIKE, null);
    }

    public void setSounds(List<SoundLikeManager.LikedSound> sounds)
    {
        this.list.clear();

        if (sounds != null)
        {
            this.list.addAll(sounds);
        }

        this.update();
    }

    public List<SoundLikeManager.LikedSound> getSounds()
    {
        return Collections.unmodifiableList(this.list);
    }

    public void setUnlikeCallback(Consumer<SoundLikeManager.LikedSound> callback)
    {
        this.unlikeCallback = callback;
    }

    @Override
    protected void renderElementPart(UIContext context, SoundLikeManager.LikedSound element, int i, int x, int y, boolean hover, boolean selected)
    {
        if (element == null)
        {
            return;
        }

        Link link = Link.create(element.getPath());
        boolean exists = BBSMod.getProvider().getFile(link) != null;
        int textColor = exists ? (hover ? Colors.HIGHLIGHT : Colors.WHITE) : Colors.RED;

        int iconX = this.area.x + this.area.w - 20;
        int iconY = y + (this.scroll.scrollItemSize - 16) / 2;

        boolean hoverIcon = context.mouseX >= iconX && context.mouseX < iconX + 16 &&
            context.mouseY >= iconY && context.mouseY < iconY + 16;

        this.unlikeIcon.iconColor(hoverIcon ? Colors.WHITE : Colors.GRAY);
        this.unlikeIcon.area.set(iconX, iconY, 16, 16);
        this.unlikeIcon.render(context);

        int maxWidth = this.area.w - 8 - 20;
        String display = element.getDisplayName();
        int textWidth = context.batcher.getFont().getWidth(display);
        if (textWidth > maxWidth)
        {
            display = truncateText(context, display, maxWidth);
        }

        context.batcher.textShadow(display, x + 4, y + (this.scroll.scrollItemSize - context.batcher.getFont().getHeight()) / 2, textColor);
    }

    @Override
    public boolean subMouseClicked(UIContext context)
    {
        if (this.area.isInside(context) && context.mouseButton == 0)
        {
            int index = this.scroll.getIndex(context.mouseX, context.mouseY);
            SoundLikeManager.LikedSound element = this.getElementAt(index);

            if (element != null)
            {
                int iconX = this.area.x + this.area.w - 20;
                int iconY = this.area.y + index * this.scroll.scrollItemSize - (int) this.scroll.getScroll() + (this.scroll.scrollItemSize - 16) / 2;

                if (context.mouseX >= iconX && context.mouseX < iconX + 16 &&
                    context.mouseY >= iconY && context.mouseY < iconY + 16)
                {
                    if (this.unlikeCallback != null)
                    {
                        this.unlikeCallback.accept(element);
                    }

                    return true;
                }
            }
        }

        return super.subMouseClicked(context);
    }

    private String truncateText(UIContext context, String text, int maxWidth)
    {
        String ellipsis = "...";
        int ellipsisWidth = context.batcher.getFont().getWidth(ellipsis);

        if (ellipsisWidth >= maxWidth)
        {
            return "";
        }

        int availableWidth = maxWidth - ellipsisWidth;
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < text.length(); i++)
        {
            String test = result.toString() + text.charAt(i);
            int testWidth = context.batcher.getFont().getWidth(test);

            if (testWidth > availableWidth)
            {
                break;
            }

            result.append(text.charAt(i));
        }

        return result.toString() + ellipsis;
    }
}
