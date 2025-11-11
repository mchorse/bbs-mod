package mchorse.bbs_mod.forms.forms.utils;

import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;
// Numeric parsing is handled via MapType helpers; no direct NumberType/NumericType import required

/**
 * Unified structure light settings representing whether blocks emit light and
 * with what intensity (0–15).
 */
public class StructureLightSettings
{
    public boolean enabled;
    public int intensity;

    public StructureLightSettings()
    {
        this(false, 0);
    }

    public StructureLightSettings(boolean enabled, int intensity)
    {
        this.enabled = enabled;
        this.intensity = Math.max(0, Math.min(15, intensity));
    }

    public StructureLightSettings copy()
    {
        return new StructureLightSettings(this.enabled, this.intensity);
    }

    public void fromData(BaseType data)
    {
        if (data instanceof MapType map)
        {
            this.enabled = map.getBool("enabled");
            int i = map.getInt("intensity", 0);
            this.intensity = Math.max(0, Math.min(15, i));
        }
    }

    public BaseType toData()
    {
        MapType map = new MapType();
        map.putBool("enabled", this.enabled);
        map.putInt("intensity", this.intensity);
        return map;
    }
}