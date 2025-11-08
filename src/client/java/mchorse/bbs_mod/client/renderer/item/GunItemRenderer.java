package mchorse.bbs_mod.client.renderer.item;

import com.mojang.blaze3d.systems.RenderSystem;
import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.entities.StubEntity;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.renderers.FormRenderType;
import mchorse.bbs_mod.forms.renderers.FormRenderingContext;
import mchorse.bbs_mod.items.GunProperties;
import mchorse.bbs_mod.ui.framework.UIScreen;
import mchorse.bbs_mod.ui.model_blocks.UIModelBlockEditorMenu;
import mchorse.bbs_mod.utils.MatrixStackUtils;
import mchorse.bbs_mod.utils.pose.Transform;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class GunItemRenderer implements BuiltinItemRendererRegistry.DynamicItemRenderer
{
    private Map<ItemStack, Item> map = new HashMap<>();

    public void update()
    {
        Iterator<Item> it = this.map.values().iterator();

        while (it.hasNext())
        {
            Item item = it.next();

            if (item.expiration <= 0)
            {
                it.remove();
            }

            item.expiration -= 1;
            item.properties.update(item.formEntity);
        }
    }

    @Override
    public void render(ItemStack stack, ModelTransformationMode mode, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay)
    {
        Item item = this.get(stack);

        if (item != null)
        {
            GunProperties properties = item.properties;
            Form form = properties.getForm(mode);
            Transform transform = properties.getTransform(mode);
            boolean zoom = mode.isFirstPerson() && BBSModClient.getGunZoom() != null && properties.zoomForm != null;

            if (zoom)
            {
                form = properties.zoomForm;
                transform = properties.zoomTransform;
            }

            /* Preview zoom form */
            if (UIScreen.getCurrentMenu() instanceof UIModelBlockEditorMenu editorMenu && editorMenu.currentSection == editorMenu.sectionZoom)
            {
                form = editorMenu.getGunProperties().zoomForm;
                transform = editorMenu.getGunProperties().zoomTransform;
            }

            if (form != null)
            {
                item.expiration = 20;

                matrices.push();
                matrices.translate(0.5F, 0F, 0.5F);
                MatrixStackUtils.applyTransform(matrices, transform);

                RenderSystem.enableDepthTest();
                FormUtilsClient.render(form, new FormRenderingContext()
                    .set(FormRenderType.fromModelMode(mode), item.formEntity, matrices, light, overlay, MinecraftClient.getInstance().getTickDelta())
                    .camera(MinecraftClient.getInstance().gameRenderer.getCamera()));
                RenderSystem.disableDepthTest();

                matrices.pop();
            }
        }
    }

    public Item get(ItemStack stack)
    {
        if (stack == null || stack.getItem() != BBSMod.GUN_ITEM)
        {
            return null;
        }

        if (this.map.containsKey(stack))
        {
            return this.map.get(stack);
        }

        Item item = new Item(GunProperties.get(stack));

        this.map.put(stack, item);

        return item;
    }

    public static class Item
    {
        public GunProperties properties;
        public IEntity formEntity;
        public int expiration = 20;

        public Item(GunProperties properties)
        {
            this.properties = properties;
            this.formEntity = new StubEntity(MinecraftClient.getInstance().world);
        }
    }
}