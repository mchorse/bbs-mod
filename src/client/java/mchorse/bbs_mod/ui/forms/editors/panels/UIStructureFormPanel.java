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
    public UIToggle autoPivot;
    public UITrackpad pivotX;
    public UITrackpad pivotY;
    public UITrackpad pivotZ;
    public UIButton calcCenter;

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

        // Pivote (estilo similar al panel de Transform)
        this.autoPivot = new UIToggle(UIKeys.FORMS_EDITORS_STRUCTURE_AUTO_PIVOT, true, (t) -> this.form.autoPivot.set(t.getValue()));
        this.pivotX = new UITrackpad((v) -> this.form.pivotX.set(v.floatValue())).block().onlyNumbers().limit(-1024D, 1024D);
        this.pivotY = new UITrackpad((v) -> this.form.pivotY.set(v.floatValue())).block().onlyNumbers().limit(-1024D, 1024D);
        this.pivotZ = new UITrackpad((v) -> this.form.pivotZ.set(v.floatValue())).block().onlyNumbers().limit(-1024D, 1024D);
        // Colores X/Y/Z
        this.pivotX.textbox.setColor(Colors.RED);
        this.pivotY.textbox.setColor(Colors.GREEN);
        this.pivotZ.textbox.setColor(Colors.BLUE);
        this.calcCenter = new UIButton(UIKeys.FORMS_EDITORS_STRUCTURE_CALCULATE_CENTER, (b) -> this.calculateCenter());

        /* Quitar etiquetas; mostrar solo los controles */
        this.options.add(this.color);
        this.options.add(this.pickStructure);
        this.options.add(this.pickBiome);
        this.options.add(this.toggleLight);
        this.options.add(UI.label(UIKeys.FORMS_EDITORS_STRUCTURE_LIGHT_INTENSITY_LABEL).marginTop(6), this.lightIntensity);

        // Sección de pivote con fila X/Y/Z y un ícono
        this.options.add(UI.label(UIKeys.FORMS_EDITORS_STRUCTURE_PIVOT_TITLE).marginTop(10));
        this.options.add(this.autoPivot);
        UIIcon pivotIcon = new UIIcon(Icons.SPHERE, null);
        pivotIcon.setEnabled(false);
        this.options.add(UI.row(pivotIcon, this.pivotX, this.pivotY, this.pivotZ));
        this.options.add(this.calcCenter);
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
    }

    private void setLightIntensity(int v)
    {
        mchorse.bbs_mod.forms.forms.utils.StructureLightSettings s = this.form.structureLight.get();
        if (s == null) s = new mchorse.bbs_mod.forms.forms.utils.StructureLightSettings(false, 15);
        s.intensity = Math.max(1, Math.min(15, v));
        this.form.structureLight.set(s);
    }


    private void calculateCenter()
    {
        String path = this.form.structureFile.get();
        if (path == null || path.isEmpty())
        {
            return;
        }

        try (java.io.InputStream is = BBSMod.getProvider().getAsset(Link.assets(path)))
        {
            NbtCompound root = NbtIo.readCompressed(is, NbtTagSizeTracker.ofUnlimitedBytes());

            int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

            if (root.contains("blocks", NbtElement.LIST_TYPE))
            {
                NbtList list = root.getList("blocks", NbtElement.COMPOUND_TYPE);
                for (int i = 0; i < list.size(); i++)
                {
                    NbtCompound be = list.getCompound(i);
                    NbtList pos = be.getList("pos", NbtElement.INT_TYPE);
                    if (pos != null && pos.size() >= 3)
                    {
                        int x = pos.getInt(0);
                        int y = pos.getInt(1);
                        int z = pos.getInt(2);
                        if (x < minX) minX = x;
                        if (y < minY) minY = y;
                        if (z < minZ) minZ = z;
                        if (x > maxX) maxX = x;
                        if (y > maxY) maxY = y;
                        if (z > maxZ) maxZ = z;
                    }
                }
            }

            if (minX != Integer.MAX_VALUE)
            {
                float cx = (minX + maxX) / 2f;
                float cy = (minY + maxY) / 2f;
                float cz = (minZ + maxZ) / 2f;

                int widthX = (maxX - minX + 1);
                int widthY = (maxY - minY + 1);
                int widthZ = (maxZ - minZ + 1);
                float parityX = (widthX % 2 == 1) ? -0.5f : 0f;
                float parityY = (widthY % 2 == 1) ? -0.5f : 0f;
                float parityZ = (widthZ % 2 == 1) ? -0.5f : 0f;

                this.form.pivotX.set(cx - parityX);
                this.form.pivotY.set(cy - parityY);
                this.form.pivotZ.set(cz - parityZ);
                this.form.autoPivot.set(false);

                this.pivotX.setValue(this.form.pivotX.get());
                this.pivotY.setValue(this.form.pivotY.get());
                this.pivotZ.setValue(this.form.pivotZ.get());
                this.autoPivot.setValue(false);
            }
        }
        catch (Throwable ignored) {}
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
        mchorse.bbs_mod.forms.forms.utils.StructureLightSettings s = form.structureLight.get();
        boolean enabled = (s != null) ? s.enabled : form.emitLight.get();
        int intensity = (s != null) ? s.intensity : form.lightIntensity.get();

        this.toggleLight.setValue(enabled);
        this.lightIntensity.setValue((double) intensity);
        this.autoPivot.setValue(form.autoPivot.get());
        this.pivotX.setValue((double)form.pivotX.get());
        this.pivotY.setValue((double)form.pivotY.get());
        this.pivotZ.setValue((double)form.pivotZ.get());
    }
}