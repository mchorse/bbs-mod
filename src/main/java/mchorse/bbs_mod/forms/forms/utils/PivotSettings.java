package mchorse.bbs_mod.forms.forms.utils;

import mchorse.bbs_mod.data.IMapSerializable;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.data.DataStorageUtils;
import org.joml.Vector4f;

/**
 * PivotSettings
 *
 * Tipo compuesto para una sola pista de tiempo que encapsula:
 * - auto: si el pivote se calcula automáticamente
 * - pivot: vector X/Y/Z (W sin uso) cuando auto es falso
 */
public class PivotSettings implements IMapSerializable
{
    public boolean auto = true;
    public Vector4f pivot = new Vector4f();

    public PivotSettings() {}

    public PivotSettings(boolean auto, float x, float y, float z)
    {
        this.auto = auto;
        this.pivot.set(x, y, z, 0f);
    }

    @Override
    public void toData(MapType data)
    {
        data.putBool("auto", this.auto);
        data.put("pivot", DataStorageUtils.vector4fToData(this.pivot));
    }

    @Override
    public void fromData(MapType data)
    {
        this.auto = data.getBool("auto");
        if (data.has("pivot"))
        {
            this.pivot = DataStorageUtils.vector4fFromData(data.get("pivot").asList());
        }
    }
}