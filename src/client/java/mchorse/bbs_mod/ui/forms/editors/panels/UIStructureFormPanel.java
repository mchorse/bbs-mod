package mchorse.bbs_mod.ui.forms.editors.panels;

import mchorse.bbs_mod.forms.forms.StructureForm;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.forms.editors.forms.UIForm;
import mchorse.bbs_mod.ui.forms.editors.utils.UIStructureOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UIColor;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.text.UITextbox;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIListOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.colors.Color;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;

import java.util.ArrayList;
import java.util.List;

public class UIStructureFormPanel extends UIFormPanel<StructureForm>
{
    public UIButton pickStructure;
    public UIButton pickBiome;
    public UITextbox structureFile;
    public UIColor color;
    public UIToggle toggleLight;
    public UITrackpad lightIntensity;

    public UIStructureFormPanel(UIForm editor)
    {
        super(editor);

        this.pickStructure = new UIButton(UIKeys.FORMS_EDITORS_STRUCTURE_PICK_STRUCTURE, (b) -> this.pickStructure());
        this.structureFile = new UITextbox(100, (s) -> this.form.structureFile.set(s)).path().border();
        this.color = new UIColor((c) -> this.form.color.set(Color.rgba(c))).withAlpha();
        this.pickBiome = new UIButton(UIKeys.FORMS_EDITORS_STRUCTURE_PICK_BIOME, (b) -> this.pickBiome());
        this.toggleLight = new UIToggle(UIKeys.FORMS_EDITORS_STRUCTURE_LIGHT, false, (t) -> this.form.emitLight.set(t.getValue()));
        this.lightIntensity = new UITrackpad((v) -> this.form.lightIntensity.set(MathUtils.clamp(v.intValue(), 1, 15)));
        this.lightIntensity.integer().limit(1D, 15D);

        this.options.add(this.color);
        this.options.add(this.pickStructure);
        this.options.add(this.pickBiome);
        this.options.add(this.toggleLight);
        this.options.add(UI.label(UIKeys.FORMS_EDITORS_STRUCTURE_LIGHT_INTENSITY).marginTop(6), this.lightIntensity);
    }

    private void pickStructure()
    {
        UIStructureOverlayPanel overlay = new UIStructureOverlayPanel(UIKeys.FORMS_EDITORS_STRUCTURE_PICK_STRUCTURE, this::setStructure);
        String current = this.form.structureFile.get();

        if (current == null || current.isEmpty())
        {
            overlay.set("");
        }
        else
        {
            overlay.set(Link.assets(current));
        }

        UIOverlay.addOverlay(this.getContext(), overlay, 280, 0.5F);
    }

    private void pickBiome()
    {
        UIListOverlayPanel overlay = new UIListOverlayPanel(UIKeys.FORMS_EDITORS_STRUCTURE_PICK_BIOME, (value) ->
        {
            String id = value == null ? "" : value;
            this.form.biomeId.set(id);
        });

        List<String> ids = new ArrayList<>();

        try
        {
            Registry<Biome> reg = MinecraftClient.getInstance().world.getRegistryManager().get(RegistryKeys.BIOME);

            for (Identifier id : reg.getIds())
            {
                ids.add(id.toString());
            }
        }
        catch (Throwable ignored)
        {}

        overlay.addValues(ids);
        overlay.setValue(this.form.biomeId.get());
        UIOverlay.addOverlay(this.getContext(), overlay, 280, 0.5F);
    }

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
        this.toggleLight.setValue(form.emitLight.get());
        this.lightIntensity.setValue(form.lightIntensity.get());
    }
}