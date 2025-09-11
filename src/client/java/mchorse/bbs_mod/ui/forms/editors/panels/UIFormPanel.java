package mchorse.bbs_mod.ui.forms.editors.panels;

import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.ui.forms.editors.forms.UIForm;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.UIScrollView;
import mchorse.bbs_mod.ui.framework.elements.utils.UIDraggable;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.utils.MathUtils;

import java.util.HashMap;
import java.util.Map;

public abstract class UIFormPanel <T extends Form> extends UIElement
{
    private static Map<Class, Float> widths = new HashMap<>();

    protected UIForm editor;
    protected T form;

    public UIScrollView options;
    public UIDraggable draggable;

    public UIFormPanel(UIForm editor)
    {
        this.editor = editor;

        this.options = UI.scrollView(5, 10);
        this.options.scroll.cancelScrolling();
        this.options.relative(this).x(1F).w(widths.getOrDefault(this.getClass(), 0F)).minW(140).h(1F).anchorX(1F);

        this.draggable = new UIDraggable((context) ->
        {
            float f = (this.options.area.ex() - context.mouseX) / (float) this.getParent().area.w;
            float w = MathUtils.clamp(f, 0, 0.5F);

            this.options.w(w).resize();
            widths.put(this.getClass(), w);
            this.draggable.resize();
        });

        this.draggable.relative(this.options).x(0F).y(0.5F).w(6).h(40).anchor(0.5F, 0.5F);

        this.add(this.options, this.draggable);
    }

    public void startEdit(T form)
    {
        this.form = form;
    }

    public void finishEdit()
    {}

    public void pickBone(String bone)
    {}
}