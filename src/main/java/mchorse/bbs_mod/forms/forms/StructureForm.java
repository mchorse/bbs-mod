package mchorse.bbs_mod.forms.forms;

import mchorse.bbs_mod.settings.values.core.ValueColor;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
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
    /** Bioma seleccionado para coloreo (override). Vacío para usar el del mundo */
    public final ValueString biomeId = new ValueString("biome_id", "");
    /** Alterna si los bloques de iluminación de la estructura emiten luz */
    public final ValueBoolean emitLight = new ValueBoolean("emit_light", false);
    /** Intensidad de luz emitida por los bloques de la estructura (1-15) */
    public final ValueInt lightIntensity = new ValueInt("light_intensity", 15);

    public StructureForm()
    {
        super();

        this.add(this.structureFile);
        this.add(this.color);
        this.add(this.biomeId);
        this.add(this.emitLight);
        this.add(this.lightIntensity);
    }

    @Override
    protected String getDefaultDisplayName()
    {
        return this.structureFile.get();
    }
}