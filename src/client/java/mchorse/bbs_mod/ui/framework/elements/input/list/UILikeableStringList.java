package mchorse.bbs_mod.ui.framework.elements.input.list;

import mchorse.bbs_mod.audio.SoundLikeManager;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.colors.Colors;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * String list with like/dislike buttons
 */
public class UILikeableStringList extends UIStringList
{
    private SoundLikeManager likeManager;
    private boolean showOnlyLiked = false;
    private UIIcon likeButton;
    private UIIcon editButton;
    private UIIcon removeButton;
    private Runnable refreshCallback;
    private Consumer<String> editCallback;
    private Consumer<String> removeCallback;
    private boolean showEditRemoveButtons = false;
    
    public UILikeableStringList(Consumer<List<String>> callback, SoundLikeManager likeManager)
    {
        super(callback);
        this.likeManager = likeManager;
        this.likeButton = new UIIcon(Icons.LIKE, null);
        this.editButton = new UIIcon(Icons.EDIT, null);
        this.removeButton = new UIIcon(Icons.REMOVE, null);
    }
    
    public void setShowOnlyLiked(boolean showOnlyLiked)
    {
        this.showOnlyLiked = showOnlyLiked;
    }
    
    private String getDisplayText(String element)
    {
        String display = this.likeManager.getDisplayName(element);

        return display != null ? display : element;
    }
    
    @Override
    protected void renderElementPart(UIContext context, String element, int i, int x, int y, boolean hover, boolean selected)
    {
        /* In show-only-liked mode, 'i' is the visible index, map it to the actual element */
        if (this.showOnlyLiked)
        {
            element = this.getVisibleElement(i);

            if (element == null)
            {
                return;
            }
        }

        boolean isNoneOption = element.equals(UIKeys.GENERAL_NONE.get());
        String displayText = this.getDisplayText(element);
        int textWidth = context.batcher.getFont().getWidth(displayText);
        int buttonSpace = 0;

        if (!isNoneOption)
        {
            buttonSpace = this.showEditRemoveButtons
                ? 60 /* edit + remove + like = 60px */
                : 20; /* like button only = 20px */
        }

        int maxWidth = this.area.w - 8 - buttonSpace;

        if (textWidth > maxWidth)
        {
            displayText = context.batcher.getFont().limitToWidth(displayText, maxWidth);
        }

        context.batcher.textShadow(displayText, x + 4, y + (this.scroll.scrollItemSize - context.batcher.getFont().getHeight()) / 2, hover ? Colors.HIGHLIGHT : Colors.WHITE);

        if (isNoneOption)
        {
            return;
        }

        int currentIconX = this.area.x + this.area.w - 20;
        int iconY = y + (this.scroll.scrollItemSize - 16) / 2;

        boolean isLiked = this.likeManager.isSoundLiked(element);
        boolean isHoverOnLike = this.area.isInside(context)
            && context.mouseX >= currentIconX
            && context.mouseX < currentIconX + 16
            && context.mouseY >= iconY
            && context.mouseY < iconY + 16;

        this.likeButton.both(isLiked ? Icons.DISLIKE : Icons.LIKE);
        this.likeButton.iconColor(isHoverOnLike || isLiked ? Colors.WHITE : Colors.GRAY);
        this.likeButton.area.set(currentIconX, iconY, 16, 16);
        this.likeButton.render(context);

        if (this.showEditRemoveButtons)
        {
            currentIconX -= 20;

            boolean isHoverOnRemove = this.area.isInside(context)
                && context.mouseX >= currentIconX
                && context.mouseX < currentIconX + 16
                && context.mouseY >= iconY
                && context.mouseY < iconY + 16;

            this.removeButton.iconColor(isHoverOnRemove ? Colors.WHITE : Colors.GRAY);
            this.removeButton.area.set(currentIconX, iconY, 16, 16);
            this.removeButton.render(context);

            currentIconX -= 20;
            boolean isHoverOnEdit = this.area.isInside(context)
                && context.mouseX >= currentIconX
                && context.mouseX < currentIconX + 16
                && context.mouseY >= iconY
                && context.mouseY < iconY + 16;

            this.editButton.iconColor(isHoverOnEdit ? Colors.WHITE : Colors.GRAY);
            this.editButton.area.set(currentIconX, iconY, 16, 16);
            this.editButton.render(context);
        }
    }
    
    @Override
    public boolean subMouseClicked(UIContext context)
    {
        if (!this.area.isInside(context) || context.mouseButton != 0)
        {
            return super.subMouseClicked(context);
        }

        int scrollIndex = this.scroll.getIndex(context.mouseX, context.mouseY);

        if (!this.exists(scrollIndex))
        {
            return super.subMouseClicked(context);
        }

        String element = this.showOnlyLiked ? this.getVisibleElement(scrollIndex) : this.list.get(scrollIndex);

        if (element == null)
        {
            return super.subMouseClicked(context);
        }

        int y = this.area.y + scrollIndex * this.scroll.scrollItemSize - (int) this.scroll.getScroll();
        int iconY = y + (this.scroll.scrollItemSize - 16) / 2;
        int likeIconX = this.area.x + this.area.w - 20;

        if (
            context.mouseX >= likeIconX &&
            context.mouseX < likeIconX + 16 &&
            context.mouseY >= iconY &&
            context.mouseY < iconY + 16
        ) {
            this.likeManager.toggleSoundLiked(element);

            if (this.refreshCallback != null)
            {
                this.refreshCallback.run();
            }

            return true;
        }

        if (this.showEditRemoveButtons)
        {
            int removeIconX = likeIconX - 20;
            if (context.mouseX >= removeIconX && context.mouseX < removeIconX + 16 &&
                context.mouseY >= iconY && context.mouseY < iconY + 16)
            {
                if (this.removeCallback != null)
                {
                    this.removeCallback.accept(element);
                }

                return true;
            }

            int editIconX = removeIconX - 20;
            if (context.mouseX >= editIconX && context.mouseX < editIconX + 16 &&
                context.mouseY >= iconY && context.mouseY < iconY + 16)
            {
                if (this.editCallback != null)
                {
                    this.editCallback.accept(element);
                }

                return true;
            }
        }

        if (!this.showOnlyLiked)
        {
            return super.subMouseClicked(context);
        }

        int actualIndex = this.list.indexOf(element);

        if (actualIndex < 0)
        {
            return false;
        }

        int buttonAreaStartX = this.area.x + this.area.w - 20;

        if (context.mouseX >= buttonAreaStartX && context.mouseX < this.area.x + this.area.w && context.mouseY >= iconY && context.mouseY < iconY + 16)
        {
            return false;
        }

        this.current.clear();
        this.current.add(actualIndex);

        if (this.callback != null)
        {
            List<String> selected = new ArrayList<>();

            selected.add(element);

            this.callback.accept(selected);
        }

        return true;
    }
    
    @Override
    public int renderElement(UIContext context, String element, int i, int index, boolean postDraw)
    {
        boolean isNoneOption = element.equals(UIKeys.GENERAL_NONE.get());
        
        if (this.showOnlyLiked && !this.likeManager.isSoundLiked(element) && !isNoneOption)
        {
            return i;
        }

        return super.renderElement(context, element, i, index, postDraw);
    }

    @Override
    public void renderList(UIContext context)
    {
        if (!this.showOnlyLiked)
        {
            super.renderList(context);

            return;
        }

        int visibleIndex = 0;

        for (int actualIndex = 0; actualIndex < this.list.size(); actualIndex++)
        {
            String element = this.list.get(actualIndex);
            boolean isNoneOption = element.equals(UIKeys.GENERAL_NONE.get());

            if (this.likeManager.isSoundLiked(element) || isNoneOption)
            {
                int nextVisibleIndex = this.renderElement(context, element, visibleIndex, actualIndex, false);

                if (nextVisibleIndex == -1)
                {
                    break;
                }
                
                visibleIndex = nextVisibleIndex;
            }
        }
    }
    
    public int getVisibleElementCount()
    {
        if (!this.showOnlyLiked)
        {
            return this.list.size();
        }

        int count = 0;

        for (String element : this.list)
        {
            boolean isNoneOption = element.equals(UIKeys.GENERAL_NONE.get());

            if (this.likeManager.isSoundLiked(element) || isNoneOption)
            {
                count += 1;
            }
        }

        return count;
    }
    
    public String getVisibleElement(int visibleIndex)
    {
        if (!this.showOnlyLiked)
        {
            return this.list.get(visibleIndex);
        }
        
        int currentIndex = 0;

        for (String element : this.list)
        {
            boolean isNoneOption = element.equals(UIKeys.GENERAL_NONE.get());
            
            if (this.showOnlyLiked && !this.likeManager.isSoundLiked(element) && !isNoneOption)
            {
                continue;
            }
            
            if (currentIndex == visibleIndex)
            {
                return element;
            }

            currentIndex += 1;
        }
        
        return null;
    }
    
    public void setRefreshCallback(Runnable callback)
    {
        this.refreshCallback = callback;
    }

    public void setEditCallback(Consumer<String> callback)
    {
        this.editCallback = callback;
    }

    public void setRemoveCallback(Consumer<String> callback)
    {
        this.removeCallback = callback;
    }

    public void setShowEditRemoveButtons(boolean show)
    {
        this.showEditRemoveButtons = show;
    }
    
    @Override
    public void update()
    {
        int size = this.showOnlyLiked ? this.getVisibleElementCount() : this.list.size();
        
        this.scroll.setSize(size);
        this.scroll.clamp();
    }
}
