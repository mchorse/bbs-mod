package mchorse.bbs_mod.forms.states;

import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.utils.interps.Lerps;

public class StatePlayer
{
    private AnimationState state;
    private int tick;

    public StatePlayer(AnimationState state)
    {
        this.state = state;
    }

    public boolean canBeRemoved()
    {
        if (this.state.main.get())
        {
            return false;
        }

        return this.tick >= this.state.duration.get();
    }

    public void update()
    {
        this.tick += 1;

        if (this.state.main.get() && this.tick >= this.state.duration.get())
        {
            this.tick = 0;
        }
    }

    public void assignValues(Form form, float transition)
    {
        float t = this.tick + transition;
        float blend = Lerps.envelope(t, 0, this.state.fadeIn.get(), this.state.duration.get() - this.state.fadeOut.get(), this.state.duration.get());

        this.state.properties.applyProperties(form, t, blend);
    }

    public void resetValues(Form form)
    {
        this.state.properties.resetProperties(form);
    }
}