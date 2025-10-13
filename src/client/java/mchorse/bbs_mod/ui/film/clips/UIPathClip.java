package mchorse.bbs_mod.ui.film.clips;

import mchorse.bbs_mod.camera.clips.overwrite.PathClip;
import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.camera.values.ValuePosition;
import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.IUIClipsDelegate;
import mchorse.bbs_mod.ui.film.clips.modules.UIAngleModule;
import mchorse.bbs_mod.ui.film.clips.modules.UIPointModule;
import mchorse.bbs_mod.ui.film.clips.modules.UIPointsModule;
import mchorse.bbs_mod.ui.film.utils.UICameraUtils;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.context.UIInterpolationContextMenu;
import mchorse.bbs_mod.ui.framework.tooltips.InterpolationTooltip;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.utils.MathUtils;

public class UIPathClip extends UIClip<PathClip>
{
    public UIPointModule point;
    public UIAngleModule angle;
    public UIButton interpPoint;
    public UIButton interpAngle;

    public UIPointsModule points;

    public ValuePosition position;

    public UIPathClip(PathClip clip, IUIClipsDelegate editor)
    {
        super(clip, editor);
    }

    @Override
    protected void registerUI()
    {
        super.registerUI();

        this.point = new UIPointModule(editor);
        this.angle = new UIAngleModule(editor);
        this.interpPoint = new UIButton(UIKeys.CAMERA_PANELS_POINT, (b) ->
        {
            this.getContext().replaceContextMenu(new UIInterpolationContextMenu(this.clip.interpolationPoint));
        });
        this.interpPoint.tooltip(new InterpolationTooltip(1F, 0.5F, () -> this.clip.interpolationPoint));
        this.interpAngle = new UIButton(UIKeys.CAMERA_PANELS_ANGLE, (b) ->
        {
            this.getContext().replaceContextMenu(new UIInterpolationContextMenu(this.clip.interpolationAngle));
        });
        this.interpAngle.tooltip(new InterpolationTooltip(1F, 0.5F, () -> this.clip.interpolationAngle));

        this.points = new UIPointsModule(this.editor, this::pickPoint);
        this.points.h(20);
    }

    @Override
    protected void registerPanels()
    {
        super.registerPanels();

        this.panels.add(UI.column(UIClip.label(UIKeys.CAMERA_PANELS_PATH_POINTS), this.points).marginTop(12));
        this.panels.add(UI.row(this.interpPoint, this.interpAngle).marginBottom(6));
        this.panels.add(this.point.marginTop(12), this.angle.marginTop(6));
        this.panels.context((menu) -> UICameraUtils.positionContextMenu(menu, editor, this.position));
    }

    private ValuePosition getPosition(int index)
    {
        BaseValue value = this.clip.points.getAll().get(index);

        return value instanceof ValuePosition ? (ValuePosition) value : null;
    }

    public void pickPoint(int index)
    {
        this.points.setIndex(index);
        this.position = this.getPosition(index);

        this.point.fill(this.position.getPoint());
        this.angle.fill(this.position.getAngle());

        if (!Window.isCtrlPressed())
        {
            int offset = this.clip.getTickForPoint(index);

            if (offset == this.clip.duration.get())
            {
                offset -= 1;
            }

            this.editor.setCursor(this.clip.tick.get() + offset);
            this.editor.setFlight(false);
        }
    }

    @Override
    public void editClip(Position position)
    {
        if (this.position != null)
        {
            this.position.set(position);

            super.editClip(position);
        }
    }

    @Override
    public void fillData()
    {
        super.fillData();

        int duration = this.clip.duration.get();
        int offset = MathUtils.clamp(this.editor.getCursor() - this.clip.tick.get(), 0, duration);
        int points = this.clip.size();
        int index = (int) ((offset / (float) duration) * points);

        index = MathUtils.clamp(index, 0, points - 1);

        this.points.fill(this.clip);
        this.position = this.getPosition(index);
        this.points.index = index;

        this.point.fill(this.position.getPoint());
        this.angle.fill(this.position.getAngle());

        this.points.index = index;
    }
}