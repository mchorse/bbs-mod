package mchorse.bbs_mod.ui.framework.elements.input;

import mchorse.bbs_mod.data.types.ListType;
import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.Keys;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.Axis;
import mchorse.bbs_mod.utils.colors.Colors;
import org.joml.Vector3d;
import org.lwjgl.glfw.GLFW;

/**
 * Transformation editor GUI
 * 
 * Must be exactly 190 by 70 (with extra 12 on top for labels)
 */
public abstract class UITransform extends UIElement
{
    public UITrackpad tx;
    public UITrackpad ty;
    public UITrackpad tz;
    public UITrackpad sx;
    public UITrackpad sy;
    public UITrackpad sz;
    public UITrackpad rx;
    public UITrackpad ry;
    public UITrackpad rz;
    public UITrackpad r2x;
    public UITrackpad r2y;
    public UITrackpad r2z;

    protected UIIcon iconT;
    protected UIIcon iconS;
    protected UIIcon iconR;
    protected UIIcon iconR2;

    protected UIElement scaleRow;

    private boolean uniformDrag;
    private boolean uniformScale;

    public UITransform()
    {
        super();

        IKey raw = IKey.constant("%s (%s)");

        this.tx = new UITrackpad((value) -> this.internalSetT(value, Axis.X)).block().onlyNumbers();
        this.tx.tooltip(raw.format(UIKeys.TRANSFORMS_TRANSLATE, UIKeys.GENERAL_X));
        this.tx.textbox.setColor(Colors.RED);
        this.ty = new UITrackpad((value) -> this.internalSetT(value, Axis.Y)).block().onlyNumbers();
        this.ty.tooltip(raw.format(UIKeys.TRANSFORMS_TRANSLATE, UIKeys.GENERAL_Y));
        this.ty.textbox.setColor(Colors.GREEN);
        this.tz = new UITrackpad((value) -> this.internalSetT(value, Axis.Z)).block().onlyNumbers();
        this.tz.tooltip(raw.format(UIKeys.TRANSFORMS_TRANSLATE, UIKeys.GENERAL_Z));
        this.tz.textbox.setColor(Colors.BLUE);

        this.sx = new UITrackpad((value) ->
        {
            this.internalSetS(value, Axis.X);
            this.syncScale(value);
        }).disableCanceling();
        this.sx.onlyNumbers().tooltip(raw.format(UIKeys.TRANSFORMS_SCALE, UIKeys.GENERAL_X));
        this.sx.textbox.setColor(Colors.RED);
        this.sy = new UITrackpad((value) ->
        {
            this.internalSetS(value, Axis.Y);
            this.syncScale(value);
        }).disableCanceling();
        this.sy.onlyNumbers().tooltip(raw.format(UIKeys.TRANSFORMS_SCALE, UIKeys.GENERAL_Y));
        this.sy.textbox.setColor(Colors.GREEN);
        this.sz = new UITrackpad((value) ->
        {
            this.internalSetS(value, Axis.Z);
            this.syncScale(value);
        }).disableCanceling();
        this.sz.onlyNumbers().tooltip(raw.format(UIKeys.TRANSFORMS_SCALE, UIKeys.GENERAL_Z));
        this.sz.textbox.setColor(Colors.BLUE);

        this.rx = new UITrackpad((value) -> this.internalSetR(value, Axis.X)).degrees().onlyNumbers();
        this.rx.tooltip(raw.format(UIKeys.TRANSFORMS_ROTATE, UIKeys.GENERAL_X));
        this.rx.textbox.setColor(Colors.RED);
        this.ry = new UITrackpad((value) -> this.internalSetR(value, Axis.Y)).degrees().onlyNumbers();
        this.ry.tooltip(raw.format(UIKeys.TRANSFORMS_ROTATE, UIKeys.GENERAL_Y));
        this.ry.textbox.setColor(Colors.GREEN);
        this.rz = new UITrackpad((value) -> this.internalSetR(value, Axis.Z)).degrees().onlyNumbers();
        this.rz.tooltip(raw.format(UIKeys.TRANSFORMS_ROTATE, UIKeys.GENERAL_Z));
        this.rz.textbox.setColor(Colors.BLUE);

        this.r2x = new UITrackpad((value) -> this.internalSetR2(value, Axis.X)).degrees().onlyNumbers();
        this.r2x.tooltip(raw.format(UIKeys.TRANSFORMS_ROTATE2, UIKeys.GENERAL_X));
        this.r2x.textbox.setColor(Colors.RED);
        this.r2y = new UITrackpad((value) -> this.internalSetR2(value, Axis.Y)).degrees().onlyNumbers();
        this.r2y.tooltip(raw.format(UIKeys.TRANSFORMS_ROTATE2, UIKeys.GENERAL_Y));
        this.r2y.textbox.setColor(Colors.GREEN);
        this.r2z = new UITrackpad((value) -> this.internalSetR2(value, Axis.Z)).degrees().onlyNumbers();
        this.r2z.tooltip(raw.format(UIKeys.TRANSFORMS_ROTATE2, UIKeys.GENERAL_Z));
        this.r2z.textbox.setColor(Colors.BLUE);

        this.w(1F).column().stretch().vertical();

        this.iconT = new UIIcon(Icons.ALL_DIRECTIONS, null);
        this.iconS = new UIIcon(Icons.SCALE, (b) -> this.toggleUniformScale());
        this.iconS.tooltip(UIKeys.TRANSFORMS_UNIFORM_SCALE);
        this.iconR = new UIIcon(Icons.REFRESH, null);
        this.iconR2 = new UIIcon(Icons.REFRESH, null);

        this.iconT.disabledColor = this.iconS.disabledColor = this.iconR.disabledColor = this.iconR2.disabledColor = Colors.WHITE;
        this.iconT.hoverColor = this.iconS.hoverColor = this.iconR.hoverColor = this.iconR2.hoverColor = Colors.WHITE;

        this.iconT.setEnabled(false);
        this.iconR.setEnabled(false);
        this.iconR2.setEnabled(false);

        this.add(UI.row(this.iconT, this.tx, this.ty, this.tz));
        this.add(this.scaleRow = UI.row(this.iconS, this.sx, this.sy, this.sz));
        this.add(UI.row(this.iconR, this.rx, this.ry, this.rz));
        this.add(UI.row(this.iconR2, this.r2x, this.r2y, this.r2z));

        this.context((menu) ->
        {
            ListType transforms = Window.getClipboardList();

            if (transforms != null && transforms.size() < 9)
            {
                transforms = null;
            }

            menu.autoKeys().action(Icons.COPY, UIKeys.TRANSFORMS_CONTEXT_COPY, this::copyTransformations);

            if (transforms != null)
            {
                final ListType innerList = transforms;

                menu.action(Icons.PASTE, UIKeys.TRANSFORMS_CONTEXT_PASTE, () -> this.pasteAll(innerList));
                menu.action(Icons.ALL_DIRECTIONS, UIKeys.TRANSFORMS_CONTEXT_PASTE_TRANSLATION, () -> this.pasteTranslation(this.getVector(innerList, 0)));
                menu.action(Icons.MAXIMIZE, UIKeys.TRANSFORMS_CONTEXT_PASTE_SCALE, () -> this.pasteScale(this.getVector(innerList, 3)));
                menu.action(Icons.REFRESH, UIKeys.TRANSFORMS_CONTEXT_PASTE_ROTATION, () -> this.pasteRotation(this.getVector(innerList, 6)));
            }

            menu.action(Icons.CLOSE, UIKeys.TRANSFORMS_CONTEXT_RESET, this::reset);
        });

        this.wh(190, 70);

        this.keys().register(Keys.COPY, this::copyTransformations).inside().label(UIKeys.TRANSFORMS_CONTEXT_COPY);
        this.keys().register(Keys.PASTE, () ->
        {
            ListType transforms = Window.getClipboardList();

            if (transforms != null && transforms.size() < 9)
            {
                transforms = null;
            }

            if (transforms != null)
            {
                this.pasteAll(transforms);
            }
        }).inside().label(UIKeys.TRANSFORMS_CONTEXT_PASTE);
    }

    protected void toggleUniformScale()
    {
        this.uniformScale = !this.uniformScale;

        this.scaleRow.removeAll();

        if (this.uniformScale)
        {
            this.scaleRow.add(this.iconS, this.sx);
        }
        else
        {
            this.scaleRow.add(this.iconS, this.sx, this.sy, this.sz);
        }

        UIElement parentContainer = this.getParentContainer();

        if (parentContainer != null)
        {
            parentContainer.resize();
        }
    }

    protected boolean isUniformScale()
    {
        return this.uniformDrag || Window.isKeyPressed(GLFW.GLFW_KEY_SPACE);
    }

    private void syncScale(double value)
    {
        if (this.isUniformScale())
        {
            this.fillS(value, value, value);
            this.setS(null, value, value, value);
        }
    }

    public void fillSetT(double x, double y, double z)
    {
        this.fillT(x, y, z);
        this.setT(null, x, y, z);
    }

    public void fillSetS(double x, double y, double z)
    {
        this.fillS(x, y, z);
        this.setS(null, x, y, z);
    }

    public void fillSetR(double x, double y, double z)
    {
        this.fillR(x, y, z);
        this.setR(null, x, y, z);
    }

    public void fillSetR2(double x, double y, double z)
    {
        this.fillR2(x, y, z);
        this.setR2(null, x, y, z);
    }

    public void fillT(double x, double y, double z)
    {
        this.tx.setValue(x);
        this.ty.setValue(y);
        this.tz.setValue(z);
    }

    public void fillS(double x, double y, double z)
    {
        this.sx.setValue(x);
        this.sy.setValue(y);
        this.sz.setValue(z);
    }

    public void fillR(double x, double y, double z)
    {
        this.rx.setValue(x);
        this.ry.setValue(y);
        this.rz.setValue(z);
    }

    public void fillR2(double x, double y, double z)
    {
        this.r2x.setValue(x);
        this.r2y.setValue(y);
        this.r2z.setValue(z);
    }
    
    protected void internalSetT(double x, Axis axis)
    {
        try
        {
            this.setT(axis,
                axis == Axis.X ? x : this.tx.value,
                axis == Axis.Y ? x : this.ty.value,
                axis == Axis.Z ? x : this.tz.value
            );
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    protected void internalSetS(double x, Axis axis)
    {
        try
        {
            if (this.uniformScale && axis == Axis.X)
            {
                this.setS(axis, x, x, x);
                this.sy.setValue(x);
                this.sz.setValue(x);

                return;
            }

            this.setS(axis,
                axis == Axis.X ? x : this.sx.value,
                axis == Axis.Y ? x : this.sy.value,
                axis == Axis.Z ? x : this.sz.value
            );
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    protected void internalSetR(double x, Axis axis)
    {
        try
        {
            this.setR(axis,
                axis == Axis.X ? x : this.rx.value,
                axis == Axis.Y ? x : this.ry.value,
                axis == Axis.Z ? x : this.rz.value
            );
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    protected void internalSetR2(double x, Axis axis)
    {
        try
        {
            this.setR2(axis,
                axis == Axis.X ? x : this.r2x.value,
                axis == Axis.Y ? x : this.r2y.value,
                axis == Axis.Z ? x : this.r2z.value
            );
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public abstract void setT(Axis axis, double x, double y, double z);

    public abstract void setS(Axis axis, double x, double y, double z);

    public abstract void setR(Axis axis, double x, double y, double z);

    public abstract void setR2(Axis axis, double x, double y, double z);

    private void copyTransformations()
    {
        ListType list = new ListType();

        list.addDouble(this.tx.value);
        list.addDouble(this.ty.value);
        list.addDouble(this.tz.value);
        list.addDouble(this.sx.value);
        list.addDouble(this.sy.value);
        list.addDouble(this.sz.value);
        list.addDouble(this.rx.value);
        list.addDouble(this.ry.value);
        list.addDouble(this.rz.value);
        list.addDouble(this.r2x.value);
        list.addDouble(this.r2y.value);
        list.addDouble(this.r2z.value);

        Window.setClipboard(list);
    }

    public void pasteAll(ListType list)
    {
        this.pasteTranslation(this.getVector(list, 0));
        this.pasteScale(this.getVector(list, 3));
        this.pasteRotation(this.getVector(list, 6));
        this.pasteRotation2(this.getVector(list, 9));
    }

    public void pasteTranslation(Vector3d translation)
    {
        this.setT(null, translation.x, translation.y, translation.z);
    }

    public void pasteScale(Vector3d scale)
    {
        this.setS(null, scale.x, scale.y, scale.z);
    }

    public void pasteRotation(Vector3d rotation)
    {
        this.setR(null, rotation.x, rotation.y, rotation.z);
    }

    public void pasteRotation2(Vector3d rotation)
    {
        this.setR2(null, rotation.x, rotation.y, rotation.z);
    }

    private Vector3d getVector(ListType list, int offset)
    {
        Vector3d result = new Vector3d();

        if (list.get(offset).isNumeric() && list.get(offset + 1).isNumeric() && list.get(offset + 2).isNumeric())
        {
            result.x = list.get(offset).asNumeric().doubleValue();
            result.y = list.get(offset + 1).asNumeric().doubleValue();
            result.z = list.get(offset + 2).asNumeric().doubleValue();
        }

        if (offset == 0)
        {
            result.x *= Window.isShiftPressed() ? -1 : 1;
        }

        if (offset >= 6)
        {
            result.y *= Window.isShiftPressed() ? -1 : 1;
            result.z *= Window.isShiftPressed() ? -1 : 1;
        }

        return result;
    }

    protected void reset()
    {
        this.fillSetT(0, 0, 0);
        this.fillSetS(1, 1, 1);
        this.fillSetR(0, 0, 0);
        this.fillSetR2(0, 0, 0);
    }

    @Override
    protected boolean subMouseClicked(UIContext context)
    {
        if (this.sx.area.isInside(context) || this.sy.area.isInside(context) || this.sz.area.isInside(context))
        {
            if (context.mouseButton == 1 && (this.sx.isDragging() || this.sy.isDragging() || this.sz.isDragging()))
            {
                this.uniformDrag = true;

                return true;
            }
        }

        return super.subMouseClicked(context);
    }

    @Override
    protected boolean subMouseReleased(UIContext context)
    {
        if (context.mouseButton == 1)
        {
            this.uniformDrag = false;
        }

        return super.subMouseReleased(context);
    }

    @Override
    protected boolean subKeyPressed(UIContext context)
    {
        if (this.sx.isDragging() || this.sy.isDragging() || this.sz.isDragging())
        {
            if (context.isHeld(GLFW.GLFW_KEY_SPACE))
            {
                return true;
            }
        }

        return super.subKeyPressed(context);
    }
}