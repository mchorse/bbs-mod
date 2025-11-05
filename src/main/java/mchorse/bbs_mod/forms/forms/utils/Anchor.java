package mchorse.bbs_mod.forms.forms.utils;

import mchorse.bbs_mod.data.IMapSerializable;
import mchorse.bbs_mod.data.types.MapType;

public class Anchor implements IMapSerializable
{
    public static final int NO_ATTACHMENT = -1;

    public int replay = NO_ATTACHMENT;
    public String attachment = "";
    public boolean translate = false;
    public boolean scale = false;

    /* Interpolation data */
    public Anchor previous;
    public float x;

    public Anchor()
    {}

    public Anchor(int replay, String attachment, boolean translate, boolean scale)
    {
        this.replay = replay;
        this.attachment = attachment;
        this.translate = translate;
        this.scale = scale;
    }

    public boolean isFadeIn()
    {
        return this.previous != null && this.replay != Anchor.NO_ATTACHMENT && this.previous.replay == Anchor.NO_ATTACHMENT;
    }

    public boolean isFadeOut()
    {
        return this.previous != null && this.replay == Anchor.NO_ATTACHMENT && this.previous.replay != Anchor.NO_ATTACHMENT;
    }

    public Anchor copy()
    {
        return new Anchor(this.replay, this.attachment, this.translate, this.scale);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (super.equals(obj))
        {
            return true;
        }

        if (obj instanceof Anchor anchor)
        {
            return this.replay == anchor.replay
                && this.attachment.equals(anchor.attachment)
                && this.translate == anchor.translate
                && this.scale == anchor.scale;
        }

        return false;
    }

    @Override
    public void fromData(MapType data)
    {
        this.replay = data.getInt("actor");
        this.attachment = data.getString("attachment");
        this.translate = data.getBool("translate", false);
        this.scale = data.getBool("scale", false);
    }

    @Override
    public void toData(MapType data)
    {
        data.putInt("actor", this.replay);
        data.putString("attachment", this.attachment);
        data.putBool("translate", this.translate);
        data.putBool("scale", this.scale);
    }
}
