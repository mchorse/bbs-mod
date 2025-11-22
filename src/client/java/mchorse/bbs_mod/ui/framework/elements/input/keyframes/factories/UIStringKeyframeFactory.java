package mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories;

import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.forms.editors.utils.UIStructureOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIListOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.input.text.UITextbox;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;

import java.util.ArrayList;
import java.util.List;

public class UIStringKeyframeFactory extends UIKeyframeFactory<String>
{
    private UITextbox string;

    public UIStringKeyframeFactory(Keyframe<String> keyframe, UIKeyframes editor)
    {
        super(keyframe, editor);

        /* Campo de texto por defecto */
        this.string = new UITextbox(1000, this::setValue);
        this.string.setText(keyframe.getValue());

        /* Si la pista es structure_file, añadir botón de selección de estructura */
        UIKeyframeSheet sheet = editor.getGraph().getSheet(keyframe);
        boolean isStructureFile = sheet != null && ("structure_file".equals(sheet.id) || sheet.id.endsWith("/structure_file"));
        boolean isBiomeId = sheet != null && ("biome_id".equals(sheet.id) || sheet.id.endsWith("/biome_id"));

        if (isStructureFile)
        {
            UIButton pickStructure = new UIButton(UIKeys.FORMS_EDITORS_STRUCTURE_PICK_STRUCTURE, (b) ->
            {
                UIStructureOverlayPanel panel = new UIStructureOverlayPanel(UIKeys.FORMS_EDITORS_STRUCTURE_PICK_STRUCTURE, (link) ->
                {
                    String value = link == null ? "" : link.path;
                    this.editor.getGraph().setValue(value, true);
                });

                String current = this.keyframe.getValue();
                if (current != null && !current.isEmpty())
                {
                    panel.set(Link.assets(current));
                }

                UIOverlay.addOverlay(this.getContext(), panel, 280, 0.5F);
            });

            this.scroll.add(pickStructure);
        }
        else if (isBiomeId)
        {
            UIButton pickBiome = new UIButton(UIKeys.FORMS_EDITORS_STRUCTURE_PICK_BIOME, (b) ->
            {
                UIListOverlayPanel overlay = new UIListOverlayPanel(UIKeys.FORMS_EDITORS_STRUCTURE_PICK_BIOME, (value) ->
                {
                    String id = value == null ? "" : value;
                    this.editor.getGraph().setValue(id, true);
                });

                List<String> ids = new ArrayList<>();
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
                overlay.setValue(this.keyframe.getValue());
                UIOverlay.addOverlay(this.getContext(), overlay, 280, 0.5F);
            });

            this.scroll.add(pickBiome);
        }

        this.scroll.add(this.string);
    }
}