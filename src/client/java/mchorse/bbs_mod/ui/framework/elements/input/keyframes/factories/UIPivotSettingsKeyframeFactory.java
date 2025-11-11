package mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.forms.StructureForm;
import mchorse.bbs_mod.forms.forms.utils.PivotSettings;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtTagSizeTracker;
import net.minecraft.client.MinecraftClient;

/**
 * Editor UI para la pista unificada de pivote: toggle de auto y trackpads X/Y/Z,
 * más un botón para calcular el centro igual que en el panel de estructuras.
 */
public class UIPivotSettingsKeyframeFactory extends UIKeyframeFactory<PivotSettings>
{
    private UIToggle auto;
    private UITrackpad x;
    private UITrackpad y;
    private UITrackpad z;
    private UIButton calcCenter;

    public UIPivotSettingsKeyframeFactory(Keyframe<PivotSettings> keyframe, UIKeyframes editor)
    {
        super(keyframe, editor);

        PivotSettings value = keyframe.getValue();

        this.auto = new UIToggle(UIKeys.FORMS_EDITORS_STRUCTURE_AUTO_PIVOT, value.auto, (t) -> this.setValue(buildValue()));
        this.x = new UITrackpad((v) -> this.setValue(buildValue())).block().onlyNumbers().limit(-1024D, 1024D);
        this.y = new UITrackpad((v) -> this.setValue(buildValue())).block().onlyNumbers().limit(-1024D, 1024D);
        this.z = new UITrackpad((v) -> this.setValue(buildValue())).block().onlyNumbers().limit(-1024D, 1024D);
        this.x.setValue(value.pivot.x);
        this.y.setValue(value.pivot.y);
        this.z.setValue(value.pivot.z);
        this.x.textbox.setColor(Colors.RED);
        this.y.textbox.setColor(Colors.GREEN);
        this.z.textbox.setColor(Colors.BLUE);

        mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon pivotIcon = new mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon(Icons.SPHERE, null);
        pivotIcon.setEnabled(false);

        this.calcCenter = new UIButton(UIKeys.FORMS_EDITORS_STRUCTURE_CALCULATE_CENTER, (b) -> calculateCenterAndApply());

        this.scroll.add(UI.label(UIKeys.FORMS_EDITORS_STRUCTURE_PIVOT_TITLE).marginTop(10));
        this.scroll.add(this.auto);
        this.scroll.add(UI.row(pivotIcon, this.x, this.y, this.z));
        this.scroll.add(this.calcCenter);
    }

    private PivotSettings buildValue()
    {
        PivotSettings s = new PivotSettings();
        s.auto = this.auto.getValue();
        s.pivot.set((float) this.x.getValue(), (float) this.y.getValue(), (float) this.z.getValue(), 0f);
        return s;
    }

    private void calculateCenterAndApply()
    {
        UIKeyframeSheet sheet = this.editor.getGraph().getSheet(this.keyframe);
        if (sheet == null) return;

        if (!(FormUtils.getForm(sheet.property) instanceof StructureForm form))
        {
            return;
        }

        String path = form.structureFile.get();
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

                this.auto.setValue(false);
                this.x.setValue(cx - parityX);
                this.y.setValue(cy - parityY);
                this.z.setValue(cz - parityZ);

                this.setValue(buildValue());
            }
        }
        catch (Throwable ignored) {}
    }
}