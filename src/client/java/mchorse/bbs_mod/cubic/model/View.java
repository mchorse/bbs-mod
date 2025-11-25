package mchorse.bbs_mod.cubic.model;

import mchorse.bbs_mod.data.IMapSerializable;
import mchorse.bbs_mod.data.types.MapType;

public class View implements IMapSerializable
{
    public String headBone = "head";
    public boolean pitch = true;
    public float constraint = 45F;

    @Override
    public void toData(MapType data)
    {
        data.putString("head_bone", this.headBone);
        data.putBool("pitch", this.pitch);
        data.putFloat("head_limit", this.constraint);
    }

    @Override
    public void fromData(MapType data)
    {
        this.headBone = data.getString("head", this.headBone);
        this.pitch = data.getBool("pitch", this.pitch);
        this.constraint = data.getFloat("head_limit", this.constraint);
    }
}