package mchorse.bbs_mod.film.replays;

import mchorse.bbs_mod.settings.values.core.ValueList;
import mchorse.bbs_mod.utils.CollectionUtils;

public class Replays extends ValueList<Replay>
{
    public Replays(String id)
    {
        super(id);
    }

    public Replay addReplay()
    {
        Replay replay = new Replay(String.valueOf(this.list.size()));

        this.preNotify();
        this.add(replay);
        this.postNotify();

        return replay;
    }

    public void remove(Replay replay)
    {
        int index = CollectionUtils.getIndex(this.list, replay);

        if (CollectionUtils.inRange(this.list, index))
        {
            this.preNotify();
            this.list.remove(index);
            this.sync();
            this.postNotify();
        }
    }

    @Override
    protected Replay create(String id)
    {
        return new Replay(id);
    }
}