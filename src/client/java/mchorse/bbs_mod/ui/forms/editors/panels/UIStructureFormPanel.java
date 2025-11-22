package mchorse.bbs_mod.ui.forms.editors.panels;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.forms.forms.StructureForm;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.forms.editors.forms.UIForm;
import mchorse.bbs_mod.ui.forms.editors.utils.UIStructureOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.input.UIColor;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.text.UITextbox;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIListOverlayPanel;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.colors.Colors;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtTagSizeTracker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;

public class UIStructureFormPanel extends UIFormPanel<StructureForm>
{
    public UIButton pickStructure;
    public UIButton pickBiome;
    public UITextbox structureFile;
    public UIColor color;
    public UIToggle toggleLight;
    public UITrackpad lightIntensity;
    /* Pivot controls removed per request; structure pivots automatically */

    public UIStructureFormPanel(UIForm editor)
    {
        super(editor);

        this.pickStructure = new UIButton(UIKeys.FORMS_EDITORS_STRUCTURE_PICK_STRUCTURE, (b) -> this.pickStructure());
        this.structureFile = new UITextbox(100, (s) -> this.form.structureFile.set(s)).path().border();
        this.color = new UIColor((c) -> this.form.color.set(Color.rgba(c))).withAlpha();
        this.pickBiome = new UIButton(UIKeys.FORMS_EDITORS_STRUCTURE_PICK_BIOME, (b) -> this.pickBiome());
        // Inicializar con valor por defecto; se sincroniza en startEdit
        this.toggleLight = new UIToggle(UIKeys.FORMS_EDITORS_STRUCTURE_LIGHT, false, (t) -> this.toggleLight(t));
        this.lightIntensity = new UITrackpad((v) -> this.setLightIntensity(v.intValue()))
                .integer()
                .limit(1D, 15D);

        // Pivot UI removed; calculate center moved to Transform panel

        /* Quitar etiquetas; mostrar solo los controles */
        this.options.add(this.color);
        this.options.add(this.pickStructure);
        this.options.add(this.pickBiome);
        this.options.add(this.toggleLight);
        this.options.add(UI.label(UIKeys.FORMS_EDITORS_STRUCTURE_LIGHT_INTENSITY_LABEL).marginTop(6), this.lightIntensity);

        // Pivot controls removed
    }

    private void pickStructure()
    {
        UIStructureOverlayPanel overlay = new UIStructureOverlayPanel(
                UIKeys.FORMS_EDITORS_STRUCTURE_PICK_STRUCTURE,
                (link) -> this.setStructure(link)
        );

        String current = this.form.structureFile.get();
        if (current == null || current.isEmpty())
        {
            overlay.set("");
        }
        else
        {
            try
            {
                overlay.set(Link.assets(current));
            }
            catch (Exception e)
            {
                overlay.set("");
            }
        }
        /* Igualar tamaño al overlay usado en el panel de keyframes */
        UIOverlay.addOverlay(this.getContext(), overlay, 280, 0.5F);
    }

    private void pickBiome()
    {
        UIListOverlayPanel overlay = new UIListOverlayPanel(UIKeys.FORMS_EDITORS_STRUCTURE_PICK_BIOME, (value) ->
        {
            String id = value == null ? "" : value;
            this.form.biomeId.set(id);
        });

        // Construir lista de biomas de forma segura
        java.util.List<String> ids = new java.util.ArrayList<>();
        try
        {
            if (MinecraftClient.getInstance().world != null)
            {
                Registry<Biome> reg = MinecraftClient.getInstance().world.getRegistryManager().get(RegistryKeys.BIOME);
                for (Identifier id : reg.getIds())
                {
                    ids.add(id.toString());
                }
            }
        }
        catch (Throwable ignored) {}

        overlay.addValues(ids);
        overlay.setValue(this.form.biomeId.get());
        UIOverlay.addOverlay(this.getContext(), overlay, 280, 0.5F);
    }

    private void toggleLight(UIToggle t)
    {
        mchorse.bbs_mod.forms.forms.utils.StructureLightSettings s = this.form.structureLight.get();
        if (s == null) s = new mchorse.bbs_mod.forms.forms.utils.StructureLightSettings(false, 15);
        s.enabled = t.getValue();
        this.form.structureLight.set(s);
        // Mantener sincronizados los valores legados usados como fallback cuando
        // no hay pista activa: emit_light y light_intensity
        this.form.emitLight.set(s.enabled);
    }

    private void setLightIntensity(int v)
    {
        mchorse.bbs_mod.forms.forms.utils.StructureLightSettings s = this.form.structureLight.get();
        if (s == null) s = new mchorse.bbs_mod.forms.forms.utils.StructureLightSettings(false, 15);
        s.intensity = Math.max(1, Math.min(15, v));
        this.form.structureLight.set(s);
        // Mantener sincronizado el valor legado de intensidad
        this.form.lightIntensity.set(s.intensity);
    }


    /* calculate center moved to Transform panel */


    private void setStructure(Link link)
    {
        String path = link == null ? "" : link.path;

        this.form.structureFile.set(path);
        this.structureFile.setText(path);
    }

    @Override
    public void startEdit(StructureForm form)
    {
        super.startEdit(form);

        this.structureFile.setText(form.structureFile.get());
        this.color.setColor(form.color.get().getARGBColor());
        mchorse.bbs_mod.forms.forms.utils.StructureLightSettings s = form.structureLight.get();
        boolean enabled = (s != null) ? s.enabled : form.emitLight.get();
        int intensity = (s != null) ? s.intensity : form.lightIntensity.get();

        this.toggleLight.setValue(enabled);
        this.lightIntensity.setValue((double) intensity);
        // Pivot controls removed
    }
}