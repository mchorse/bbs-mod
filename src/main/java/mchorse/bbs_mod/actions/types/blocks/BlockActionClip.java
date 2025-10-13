package mchorse.bbs_mod.actions.types.blocks;

import mchorse.bbs_mod.actions.types.ActionClip;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;

public abstract class BlockActionClip extends ActionClip
{
    public final ValueInt x = new ValueInt("x", 0);
    public final ValueInt y = new ValueInt("y", 0);
    public final ValueInt z = new ValueInt("z", 0);

    public BlockActionClip()
    {
        super();

        this.add(this.x);
        this.add(this.y);
        this.add(this.z);
    }

    @Override
    public void shift(double dx, double dy, double dz)
    {
        super.shift(dx, dy, dz);

        this.x.set((int) (this.x.get() + dx));
        this.y.set((int) (this.y.get() + dy));
        this.z.set((int) (this.z.get() + dz));
    }
}