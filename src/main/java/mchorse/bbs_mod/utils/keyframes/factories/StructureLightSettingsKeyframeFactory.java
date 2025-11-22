package mchorse.bbs_mod.utils.keyframes.factories;

import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.forms.forms.utils.StructureLightSettings;
import mchorse.bbs_mod.utils.interps.IInterp;

public class StructureLightSettingsKeyframeFactory implements IKeyframeFactory<StructureLightSettings>
{
    private final StructureLightSettings i = new StructureLightSettings();

    @Override
    public StructureLightSettings fromData(BaseType data)
    {
        StructureLightSettings value = new StructureLightSettings();

        if (data.isMap())
        {
            value.fromData(data.asMap());
        }

        return value;
    }

    @Override
    public BaseType toData(StructureLightSettings value)
    {
        return value == null ? new MapType() : value.toData();
    }

    @Override
    public StructureLightSettings createEmpty()
    {
        return new StructureLightSettings();
    }

    @Override
    public StructureLightSettings copy(StructureLightSettings value)
    {
        return value == null ? null : value.copy();
    }

    @Override
    public StructureLightSettings interpolate(StructureLightSettings preA, StructureLightSettings a, StructureLightSettings b, StructureLightSettings postB, IInterp interpolation, float x)
    {
        // enabled: discreto, tomar el estado de "a"
        this.i.enabled = a.enabled;
        // intensity: interpolación numérica estándar con límites 0..15
        int y = (int) Math.round(interpolation.interpolate(IInterp.context.set(preA.intensity, a.intensity, b.intensity, postB.intensity, x)));
        if (y < 0) y = 0; else if (y > 15) y = 15;
        this.i.intensity = y;
        return this.i;
    }
}