package mchorse.bbs_mod.forms.forms.utils;

import mchorse.bbs_mod.data.IMapSerializable;
import mchorse.bbs_mod.data.types.MapType;
import net.minecraft.util.Identifier;
import java.util.Objects;

public class ParticleSettings implements IMapSerializable
{
    public Identifier particle = Identifier.of("minecraft", "flame");
    public String arguments = "";

    @Override
    public void toData(MapType data)
    {
        data.putString("particle", this.particle.toString());
        data.putString("args", this.arguments);
    }

    @Override
    public void fromData(MapType data)
    {
        this.particle = new Identifier(data.getString("particle"));
        this.arguments = data.getString("args");
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (!(o instanceof ParticleSettings)) return false;
        ParticleSettings that = (ParticleSettings) o;
        return Objects.equals(this.particle, that.particle) && Objects.equals(this.arguments, that.arguments);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(this.particle, this.arguments);
    }
}