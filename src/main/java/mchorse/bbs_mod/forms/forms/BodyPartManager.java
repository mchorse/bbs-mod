package mchorse.bbs_mod.forms.forms;

import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.settings.values.core.ValueList;

public class BodyPartManager extends ValueList<BodyPart>
{
    public BodyPartManager(String id)
    {
        super(id);
    }

    public Form getOwner()
    {
        return this.parent instanceof Form form ? form : null;
    }

    public void addBodyPart(BodyPart part)
    {
        this.preNotify();
        this.add(part);
        this.sync();
        this.postNotify();
    }

    public void removeBodyPart(BodyPart part)
    {
        this.preNotify();

        if (this.list.remove(part))
        {
            this.sync();
        }

        this.postNotify();
    }

    public void moveBodyPart(BodyPart part, int index)
    {
        this.preNotify();

        if (this.list.remove(part))
        {
            this.list.add(index, part);
        }

        this.sync();
        this.postNotify();
    }

    public void update(IEntity target)
    {
        for (BodyPart part : this.list)
        {
            part.update(target);
        }
    }

    @Override
    protected BodyPart create(String id)
    {
        return new BodyPart(id);
    }
}