package mchorse.bbs_mod.ui.utils.resizers;

import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.utils.Area;

/**
 * Flex class
 * 
 * This class is used to define resizing behavior for a 
 * {@link UIElement}.
 */
public class Flex implements IResizer
{
    public final Unit x = new Unit();
    public final Unit y = new Unit();
    public final Unit w = new Unit();
    public final Unit h = new Unit();

    public IResizer relative;
    public IResizer post;

    /* IResizer implementation */

    @Override
    public void preApply(Area area)
    {}

    @Override
    public void apply(Area area)
    {
        if (this.post != null)
        {
            this.post.preApply(area);
        }

        area.w = this.getW();
        area.h = this.getH();
        area.x = this.getX();
        area.y = this.getY();

        if (this.post != null)
        {
            this.post.apply(area);
        }
    }

    @Override
    public void postApply(Area area)
    {
        if (this.post != null)
        {
            this.post.postApply(area);
        }
    }

    @Override
    public void add(UIElement parent, UIElement child)
    {
        if (this.post != null)
        {
            this.post.add(parent, child);
        }
    }

    @Override
    public void remove(UIElement parent, UIElement child)
    {
        if (this.post != null)
        {
            this.post.remove(parent, child);
        }
    }

    @Override
    public int getX()
    {
        int value = this.x.offset;

        if (this.relative != null)
        {
            value += this.relative.getX();

            if (this.x.value != 0)
            {
                value += (int) (this.relative.getW() * this.x.value);
            }
        }

        if (this.x.anchor != 0)
        {
            value -= this.x.anchor * this.getW();
        }

        return value;
    }

    @Override
    public int getY()
    {
        int value = this.y.offset;

        if (this.relative != null)
        {
            value += this.relative.getY();

            if (this.y.value != 0)
            {
                value += (int) (this.relative.getH() * this.y.value);
            }
        }

        if (this.y.anchor != 0)
        {
            value -= this.y.anchor * this.getH();
        }

        return value;
    }

    @Override
    public int getW()
    {
        if (this.w.target != null)
        {
            int w = this.w.targetAnchor == 0 ? 0 : (int) (this.w.target.getW() * this.w.targetAnchor);

            return this.w.normalize((this.w.target.getX() + w) - this.getX() + this.w.offset);
        }

        int value = this.post == null ? 0 : this.post.getW();

        if (value != 0)
        {
            return value;
        }

        value = this.w.offset;

        if (this.relative != null && this.w.value != 0)
        {
            value += (int) (this.relative.getW() * this.w.value);
        }

        if (this.w.max > 0)
        {
            value = Math.min(value, this.w.max);
        }

        if (this.w.min > 0)
        {
            value = Math.max(value, this.w.min);
        }

        return value;
    }

    @Override
    public int getH()
    {
        if (this.h.target != null)
        {
            int h = this.h.targetAnchor == 0 ? 0 : (int) (this.h.target.getH() * this.h.targetAnchor);

            return this.h.normalize((this.h.target.getY() + h) - this.getY() + this.h.offset);
        }

        int value = this.post == null ? 0 : this.post.getH();

        if (value != 0)
        {
            return value;
        }

        value = this.h.offset;

        if (this.relative != null && this.h.value != 0)
        {
            value += (int) (this.relative.getH() * this.h.value);
        }

        if (this.h.max > 0)
        {
            value = Math.min(value, this.h.max);
        }

        if (this.h.min > 0)
        {
            value = Math.max(value, this.h.min);
        }

        return value;
    }

    /**
     * Unit class
     */
    public static class Unit
    {
        public int offset;
        public float value;
        public int min;
        public int max;
        public float anchor;
        public IResizer target;
        public float targetAnchor;

        public void reset()
        {
            this.value = 0F;
            this.offset = 0;
            this.max = 0;
            this.min = 0;
            this.anchor = 0F;
            this.target = null;
            this.targetAnchor = 0F;
        }

        public void set(float value)
        {
            this.set(value, 0);
        }

        public void set(float value, int offset)
        {
            this.value = value;
            this.offset = offset;

            /* Reset target */
            this.target = null;
            this.targetAnchor = 0;
        }

        public int normalize(int value)
        {
            if (this.min > 0) value = Math.max(value, this.min);
            if (this.max > 0) value = Math.min(value, this.max);

            return value;
        }
    }
}