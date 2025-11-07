package mchorse.bbs_mod.utils.keyframes.factories;

import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.forms.forms.utils.Anchor;
import mchorse.bbs_mod.utils.interps.IInterp;

public class AnchorKeyframeFactory implements IKeyframeFactory<Anchor>
{
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
        Anchor anchor = value.copy();

        anchor.previous = value.previous == null ? null : value.previous.copy();
        anchor.x = value.x;

        return anchor;
    }

    @Override
    public Anchor interpolate(Anchor preA, Anchor a, Anchor b, Anchor postB, IInterp interpolation, float x)
    {
        Anchor anchor = b.copy();

        anchor.previous = a.copy();
        anchor.x = interpolation.interpolate(0F, 1F, x);

        return anchor;
    }
}