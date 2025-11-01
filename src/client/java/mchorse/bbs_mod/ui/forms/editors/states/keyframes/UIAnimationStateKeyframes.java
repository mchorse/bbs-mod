package mchorse.bbs_mod.ui.forms.editors.states.keyframes;

import mchorse.bbs_mod.camera.utils.TimeUtils;
import mchorse.bbs_mod.ui.film.UIClips;
import mchorse.bbs_mod.ui.forms.editors.UIFormEditor;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.utils.keyframes.Keyframe;

import java.util.function.Consumer;

public class UIAnimationStateKeyframes extends UIKeyframes
{
    public UIFormEditor editor;

    public UIAnimationStateKeyframes(UIFormEditor delegate, Consumer<Keyframe> callback)
    {
        super(callback);

        this.editor = delegate;
    }

    public int getOffset()
    {
        if (this.editor == null)
        {
            return 0;
        }

        return this.editor.getCursor();
    }

    @Override
    protected void selectNextKeyframe(int direction)
    {
        super.selectNextKeyframe(direction);

        Keyframe keyframe = this.getGraph().getSelected();

        if (keyframe != null)
        {
            this.editor.setCursor((int) keyframe.getTick());
        }
    }

    @Override
    protected void moveNoKeyframes(UIContext context)
    {
        if (this.editor != null)
        {
            this.editor.setCursor(Math.max(0, (int) Math.round(this.fromGraphX(context.mouseX))));
        }
    }

    @Override
    protected void renderBackground(UIContext context)
    {
        super.renderBackground(context);

        if (this.editor != null)
        {
            int cx = this.toGraphX(this.getOffset());
            String label = TimeUtils.formatTime(this.getOffset()) + "/" + TimeUtils.formatTime(this.getDuration());

            UIClips.renderCursor(context, label, this.area, cx - 1);
        }
    }
}