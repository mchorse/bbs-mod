package mchorse.bbs_mod.ui.framework.elements.input.list;

import mchorse.bbs_mod.audio.SoundLikeManager;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.colors.Colors;

import java.util.List;
import java.util.function.Consumer;

/**
 * 支持like/dislike按钮的字符串列表
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
    
    /**
     * 设置是否只显示like的音频
     */
    public void setShowOnlyLiked(boolean showOnlyLiked)
    {
        this.showOnlyLiked = showOnlyLiked;
    }
    
    /**
     * 获取是否只显示like的音频
     */
    public boolean isShowOnlyLiked()
    {
        return this.showOnlyLiked;
    }
    
    /**
     * 切换是否只显示like的音频
     */
    public void toggleShowOnlyLiked()
    {
        this.showOnlyLiked = !this.showOnlyLiked;
    }
    
    private String getDisplayText(String element)
    {
        String display = this.likeManager.getDisplayName(element);

        return display != null ? display : element;
    }
    
    @Override
    protected void renderElementPart(UIContext context, String element, int i, int x, int y, boolean hover, boolean selected)
    {
        // 在showOnlyLiked模式下，i是可见索引，需要获取实际的元素
        if (this.showOnlyLiked)
        {
            element = this.getVisibleElement(i);
            if (element == null)
            {
                return;
            }
        }

        // 特殊处理"None"选项，始终显示且不显示like按钮
        boolean isNoneOption = element.equals(UIKeys.GENERAL_NONE.get());

        // 计算文本宽度，为按钮留出空间（如果是None选项则不需要）
        String displayText = this.getDisplayText(element);
        int textWidth = context.batcher.getFont().getWidth(displayText);
        int buttonSpace = 0;
        if (!isNoneOption) {
            if (this.showEditRemoveButtons) {
                buttonSpace = 60; // edit + remove + like = 3 * 20 = 60像素
            } else {
                buttonSpace = 20; // 只有like按钮 = 20像素
            }
        }
        int maxWidth = this.area.w - 8 - buttonSpace;

        // 如果文本太长，进行截断
        if (textWidth > maxWidth)
        {
            displayText = truncateText(context, displayText, maxWidth);
        }

        // 绘制文本
        context.batcher.textShadow(displayText, x + 4, y + (this.scroll.scrollItemSize - context.batcher.getFont().getHeight()) / 2, hover ? Colors.HIGHLIGHT : Colors.WHITE);

        // 如果是None选项，不绘制like按钮
        if (isNoneOption)
        {
            return;
        }

        // 绘制按钮（从右到左：like, remove, edit）
        int currentIconX = this.area.x + this.area.w - 20;
        int iconY = y + (this.scroll.scrollItemSize - 16) / 2;

        // 绘制like按钮
        boolean isLiked = this.likeManager.isSoundLiked(element);
        boolean isHoverOnLike = this.area.isInside(context) && context.mouseX >= currentIconX && context.mouseX < currentIconX + 16 &&
                context.mouseY >= iconY && context.mouseY < iconY + 16;

        this.likeButton.both(isLiked ? Icons.DISLIKE : Icons.LIKE);
        this.likeButton.iconColor(isHoverOnLike || isLiked ? Colors.WHITE : Colors.GRAY);
        this.likeButton.area.set(currentIconX, iconY, 16, 16);
        this.likeButton.render(context);

        // 如果启用了edit和remove按钮，则绘制它们
        if (this.showEditRemoveButtons) {
            // Remove按钮
            currentIconX -= 20;
            boolean isHoverOnRemove = this.area.isInside(context) && context.mouseX >= currentIconX && context.mouseX < currentIconX + 16 &&
                    context.mouseY >= iconY && context.mouseY < iconY + 16;

            this.removeButton.iconColor(isHoverOnRemove ? Colors.WHITE : Colors.GRAY);
            this.removeButton.area.set(currentIconX, iconY, 16, 16);
            this.removeButton.render(context);

            // Edit按钮
            currentIconX -= 20;
            boolean isHoverOnEdit = this.area.isInside(context) && context.mouseX >= currentIconX && context.mouseX < currentIconX + 16 &&
                    context.mouseY >= iconY && context.mouseY < iconY + 16;

            this.editButton.iconColor(isHoverOnEdit ? Colors.WHITE : Colors.GRAY);
            this.editButton.area.set(currentIconX, iconY, 16, 16);
            this.editButton.render(context);
        }
    }
    
    /**
     * 截断文本以适应最大宽度
     */
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

        // 计算行的y坐标（与渲染时一致）
        int y = this.area.y + scrollIndex * this.scroll.scrollItemSize - (int) this.scroll.getScroll();
        int iconY = y + (this.scroll.scrollItemSize - 16) / 2;
        int likeIconX = this.area.x + this.area.w - 20;

        if (context.mouseX >= likeIconX && context.mouseX < likeIconX + 16 &&
            context.mouseY >= iconY && context.mouseY < iconY + 16)
        {
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
        if (context.mouseX >= buttonAreaStartX && context.mouseX < this.area.x + this.area.w &&
            context.mouseY >= iconY && context.mouseY < iconY + 16)
        {
            return false;
        }

        this.current.clear();
        this.current.add(actualIndex);

        if (this.callback != null)
        {
            java.util.List<String> selected = new java.util.ArrayList<>();
            selected.add(element);
            this.callback.accept(selected);
        }

        return true;
    }
    
    @Override
    public int renderElement(UIContext context, String element, int i, int index, boolean postDraw)
    {
        // This check is now handled by the custom renderList method, but we keep it
        // as a safeguard in case renderElement is called directly.
        boolean isNoneOption = element.equals(UIKeys.GENERAL_NONE.get());
        
        if (this.showOnlyLiked && !this.likeManager.isSoundLiked(element) && !isNoneOption)
        {
            return i;
        }
        
        int result = super.renderElement(context, element, i, index, postDraw);
        return result;
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
    
    /**
     * 获取当前显示的元素数量（考虑showOnlyLiked过滤）
     */
    public int getVisibleElementCount()
    {
        if (!this.showOnlyLiked)
        {
            return this.list.size();
        }

        int count = 0;
        for (String element : this.list)
        {
            // 特殊处理"None"选项，始终显示
            boolean isNoneOption = element.equals(UIKeys.GENERAL_NONE.get());
            if (this.likeManager.isSoundLiked(element) || isNoneOption)
            {
                count++;
            }
        }

        return count;
    }
    
    /**
     * 获取指定索引处的可见元素（考虑showOnlyLiked过滤）
     */
    public String getVisibleElement(int visibleIndex)
    {
        if (!this.showOnlyLiked)
        {
            return this.list.get(visibleIndex);
        }
        
        int currentIndex = 0;
        for (String element : this.list)
        {
            // 特殊处理"None"选项，始终显示
            boolean isNoneOption = element.equals(UIKeys.GENERAL_NONE.get());
            
            // 如果只显示like的音频且当前音频没有被like，并且不是"None"选项，则跳过
            if (this.showOnlyLiked && !this.likeManager.isSoundLiked(element) && !isNoneOption)
            {
                continue;
            }
            
            if (currentIndex == visibleIndex)
            {
                return element;
            }
            currentIndex++;
        }
        
        return null;
    }
    
    /**
     * 设置刷新回调，当like状态改变时调用
     */
    public void setRefreshCallback(Runnable callback)
    {
        this.refreshCallback = callback;
    }

    /**
     * 设置编辑回调
     */
    public void setEditCallback(Consumer<String> callback)
    {
        this.editCallback = callback;
    }

    /**
     * 设置删除回调
     */
    public void setRemoveCallback(Consumer<String> callback)
    {
        this.removeCallback = callback;
    }

    /**
     * 设置是否显示编辑和删除按钮（文件夹模式使用）
     */
    public void setShowEditRemoveButtons(boolean show)
    {
        this.showEditRemoveButtons = show;
    }
    
    @Override
    public void update()
    {
        // 根据是否显示只喜欢的音频来计算正确的列表大小
        int size;
        if (this.showOnlyLiked)
        {
            size = this.getVisibleElementCount();
        }
        else
        {
            size = this.list.size();
        }
        
        this.scroll.setSize(size);
        this.scroll.clamp();
    }
}
