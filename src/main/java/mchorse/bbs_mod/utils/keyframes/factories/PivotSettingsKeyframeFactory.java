package mchorse.bbs_mod.utils.keyframes.factories;

import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.forms.forms.utils.PivotSettings;
import mchorse.bbs_mod.utils.interps.IInterp;

/**
 * Fábrica de keyframes para PivotSettings (auto + vector pivote)
 */
public class PivotSettingsKeyframeFactory implements IKeyframeFactory<PivotSettings>
{
    private final PivotSettings i = new PivotSettings();

    @Override
    public PivotSettings fromData(BaseType data)
    {
        PivotSettings settings = new PivotSettings();

        if (data.isMap())
        {
            settings.fromData(data.asMap());
        }
        else if (data.isList())
        {
            // Compatibilidad con datos antiguos donde pivot era Vector4f
            org.joml.Vector4f vec = mchorse.bbs_mod.data.DataStorageUtils.vector4fFromData(data.asList());
            settings.auto = false;
            settings.pivot.set(vec);
        }

        return settings;
    }

    @Override
    public BaseType toData(PivotSettings value)
    {
        MapType map = new MapType();
        value.toData(map);
        return map;
    }

    @Override
    public PivotSettings createEmpty()
    {
        return new PivotSettings();
    }

    @Override
    public PivotSettings copy(PivotSettings value)
    {
        PivotSettings copy = new PivotSettings();
        copy.auto = value.auto;
        copy.pivot.set(value.pivot);
        return copy;
    }

    @Override
    public PivotSettings interpolate(PivotSettings preA, PivotSettings a, PivotSettings b, PivotSettings postB, IInterp interpolation, float x)
    {
        // Comportamiento: auto es discreto (tomar el valor de "a"),
        // y el vector pivote se interpola cuando auto es falso en ambos lados.
        this.i.auto = a.auto;

        float ax = a.pivot.x;
        float ay = a.pivot.y;
        float az = a.pivot.z;
        float bx = b.pivot.x;
        float by = b.pivot.y;
        float bz = b.pivot.z;

        if (!a.auto && !b.auto)
        {
            this.i.pivot.x = (float) interpolation.interpolate(IInterp.context.set(preA.pivot.x, ax, bx, postB.pivot.x, x));
            this.i.pivot.y = (float) interpolation.interpolate(IInterp.context.set(preA.pivot.y, ay, by, postB.pivot.y, x));
            this.i.pivot.z = (float) interpolation.interpolate(IInterp.context.set(preA.pivot.z, az, bz, postB.pivot.z, x));
        }
        else
        {
            // Si en algún lado es auto, mantener el valor de "a" sin interpolación
            this.i.pivot.set(ax, ay, az, 0f);
        }

        return this.i;
    }
}