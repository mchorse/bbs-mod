package mchorse.bbs_mod.utils.keyframes.factories;

import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.forms.forms.utils.Anchor;
import mchorse.bbs_mod.utils.interps.IInterp;

public class AnchorKeyframeFactory implements IKeyframeFactory<Anchor>
{
    private Anchor i = new Anchor();

    @Override
    public Anchor fromData(BaseType data)
    {
        Anchor anchor = new Anchor();

        anchor.fromData(data.asMap());

        return anchor;
    }

    @Override
    public BaseType toData(Anchor value)
    {
        return value.toData();
    }

    @Override
    public Anchor createEmpty()
    {
        return new Anchor();
    }

    @Override
    public Anchor copy(Anchor value)
    {
        Anchor anchor = new Anchor();

        anchor.actor = value.actor;
        anchor.attachment = value.attachment;
        anchor.translate = value.translate;
        anchor.scale = value.scale;
        anchor.previousActor = value.previousActor;
        anchor.previousAttachment = value.previousAttachment;
        anchor.previousTranslate = value.previousTranslate;
        anchor.previousScale = value.previousScale;
        anchor.x = value.x;

        return anchor;
    }

    @Override
    public Anchor interpolate(Anchor preA, Anchor a, Anchor b, Anchor postB, IInterp interpolation, float x)
    {
        this.i.actor = b.actor;
        this.i.attachment = b.attachment;
        this.i.translate = b.translate;
        this.i.scale = b.scale;

        this.i.previousActor = a.actor;
        this.i.previousAttachment = a.attachment;
        this.i.previousTranslate = a.translate;
        this.i.previousScale = a.scale;

        this.i.x = interpolation.interpolate(0F, 1F, x);

        return this.i;
    }
}