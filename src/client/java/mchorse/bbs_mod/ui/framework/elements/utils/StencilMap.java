package mchorse.bbs_mod.ui.framework.elements.utils;

import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.ui.utils.Gizmo;
import mchorse.bbs_mod.utils.Pair;

import java.util.HashMap;
import java.util.Map;

public class StencilMap
{
    public int objectIndex;
    public Map<Integer, Pair<Form, String>> indexMap = new HashMap<>();
    public boolean increment = true;

    public void setIncrement(boolean increment)
    {
        this.increment = increment;
    }

    public void setup()
    {
        this.objectIndex = 7;

        /* Reset map and setup pairs for Gizmo's individual axes
         * and perpendicular planes */
        this.indexMap.clear();
        this.indexMap.put(Gizmo.STENCIL_X, new Pair<>(null, "x"));
        this.indexMap.put(Gizmo.STENCIL_Y, new Pair<>(null, "y"));
        this.indexMap.put(Gizmo.STENCIL_Z, new Pair<>(null, "z"));
        this.indexMap.put(Gizmo.STENCIL_XZ, new Pair<>(null, "xz"));
        this.indexMap.put(Gizmo.STENCIL_XY, new Pair<>(null, "xy"));
        this.indexMap.put(Gizmo.STENCIL_ZY, new Pair<>(null, "zy"));
    }

    public void addPicking(Form form)
    {
        this.addPicking(form, "");
    }

    public void addPicking(Form form, String bone)
    {
        if (this.increment)
        {
            this.indexMap.put(this.objectIndex, new Pair<>(form, bone));

            this.objectIndex += 1;
        }
        else
        {
            this.indexMap.put(this.objectIndex, new Pair<>(form, ""));
        }
    }
}