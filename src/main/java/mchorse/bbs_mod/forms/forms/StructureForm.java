package mchorse.bbs_mod.forms.forms;

import mchorse.bbs_mod.settings.values.core.ValueColor;
import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.utils.colors.Color;

/**
 * StructureForm
 *
 * Un formulario ligero para renderizar estructuras guardadas en archivos NBT.
 * Minimiza archivos al encapsular únicamente las propiedades necesarias.
 */
public class StructureForm extends Form
{
    /** Ruta relativa dentro de assets al archivo de estructura (.nbt), por ejemplo: "structures/casa.nbt" */
    public final ValueString structureFile = new ValueString("structure_file", "");
    /** Color de tinte aplicado al renderizado (multiplicado) */
    public final ValueColor color = new ValueColor("color", Color.white());

    public StructureForm()
    {
        super();

        this.add(this.structureFile);
        this.add(this.color);
    }

    @Override
    protected String getDefaultDisplayName()
    {
        return this.structureFile.get();
    }
}