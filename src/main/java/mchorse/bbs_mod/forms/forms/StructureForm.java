package mchorse.bbs_mod.forms.forms;

import mchorse.bbs_mod.settings.values.core.ValueColor;
import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.utils.StringUtils;
import mchorse.bbs_mod.utils.colors.Color;

public class StructureForm extends Form
{
    public final ValueString structureFile = new ValueString("structure", "structures/tree.nbt");
    public final ValueColor color = new ValueColor("color", Color.white());
    public final ValueString biomeId = new ValueString("biome", "");
    public final ValueBoolean emitLight = new ValueBoolean("emit_light", false);
    public final ValueInt lightIntensity = new ValueInt("light_intensity", 15);
    public final ValueBoolean tintBlockEntities = new ValueBoolean("tint_block_entities", false);
    public final ValueFloat pivotX = new ValueFloat("pivot_x", 0f);
    public final ValueFloat pivotY = new ValueFloat("pivot_y", 0f);
    public final ValueFloat pivotZ = new ValueFloat("pivot_z", 0f);
    public final ValueBoolean autoPivot = new ValueBoolean("auto_pivot", true);

    public StructureForm()
    {
        super();

        this.emitLight.invisible();
        this.lightIntensity.invisible();
        this.tintBlockEntities.invisible();

        this.pivotX.invisible();
        this.pivotY.invisible();
        this.pivotZ.invisible();

        this.autoPivot.invisible();

        this.add(this.structureFile);
        this.add(this.color);
        this.add(this.biomeId);
        this.add(this.emitLight);
        this.add(this.lightIntensity);
        this.add(this.tintBlockEntities);

        this.add(this.pivotX);
        this.add(this.pivotY);
        this.add(this.pivotZ);

        this.add(this.autoPivot);
    }

    @Override
    protected String getDefaultDisplayName()
    {
        String path = this.structureFile.get();

        if (path == null || path.isEmpty())
        {
            return super.getDefaultDisplayName();
        }

        return StringUtils.removeExtension(path);
    }
}