package mchorse.bbs_mod.utils.keyframes.factories;

import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.forms.properties.AnchorProperty;
import mchorse.bbs_mod.utils.interps.IInterp;

public class AnchorKeyframeFactory implements IKeyframeFactory<AnchorProperty.Anchor>
{
    private AnchorProperty.Anchor i = new AnchorProperty.Anchor();

    @Override
    public AnchorProperty.Anchor fromData(BaseType data)
    {
        AnchorProperty.Anchor anchor = new AnchorProperty.Anchor();

        anchor.fromData(data.asMap());

        return anchor;
    }

    @Override
    public BaseType toData(AnchorProperty.Anchor value)
    {
        return value.toData();
    }

    @Override
    public AnchorProperty.Anchor createEmpty()
    {
        return new AnchorProperty.Anchor();
    }

    @Override
    public AnchorProperty.Anchor copy(AnchorProperty.Anchor value)
    {
        AnchorProperty.Anchor anchor = new AnchorProperty.Anchor();

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
    public AnchorProperty.Anchor interpolate(AnchorProperty.Anchor preA, AnchorProperty.Anchor a, AnchorProperty.Anchor b, AnchorProperty.Anchor postB, IInterp interpolation, float x)
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