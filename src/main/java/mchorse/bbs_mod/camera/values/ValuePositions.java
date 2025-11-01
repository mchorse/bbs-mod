package mchorse.bbs_mod.camera.values;

import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.settings.values.IValueListener;
import mchorse.bbs_mod.settings.values.core.ValueList;

import java.util.List;

public class ValuePositions extends ValueList<ValuePosition>
{
    public ValuePositions(String id)
    {
        super(id);
    }

    /* Setters */

    public void add(Position position)
    {
        this.preNotify();

        this.add(new ValuePosition("", position));
        this.sync();

        this.postNotify();
    }

    public void add(int index, Position position)
    {
        if (index >= this.list.size())
        {
            this.add(position);

            return;
        }

        this.preNotify(IValueListener.FLAG_UNMERGEABLE);

        this.list.add(index, new ValuePosition("", position));
        this.sync();

        this.postNotify(IValueListener.FLAG_UNMERGEABLE);
    }

    public void move(int index, int to)
    {
        this.preNotify(IValueListener.FLAG_UNMERGEABLE);

        this.list.add(index, this.list.remove(to));
        this.sync();

        this.postNotify(IValueListener.FLAG_UNMERGEABLE);
    }

    public void remove(int index)
    {
        this.preNotify(IValueListener.FLAG_UNMERGEABLE);

        this.list.remove(index);
        this.sync();

        this.postNotify(IValueListener.FLAG_UNMERGEABLE);
    }

    public void set(List<Position> positions)
    {
        this.preNotify();
        this.list.clear();

        for (Position position : positions)
        {
            this.add(position.copy());
        }

        this.sync();
        this.postNotify();
    }

    public void reset()
    {
        this.preNotify();
        this.list.clear();
        this.postNotify();
    }

    /* Getters */

    public Position get(int index)
    {
        return this.list.get(index).get();
    }

    public int size()
    {
        return this.list.size();
    }

    @Override
    protected ValuePosition create(String id)
    {
        return new ValuePosition(id);
    }
}