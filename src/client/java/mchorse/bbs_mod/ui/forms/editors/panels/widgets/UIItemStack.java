package mchorse.bbs_mod.ui.forms.editors.panels.widgets;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.forms.CustomVertexConsumerProvider;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.ui.utils.UIUtils;
import mchorse.bbs_mod.ui.utils.context.ItemStackContextAction;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.colors.Colors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;

import java.util.function.Consumer;

public class UIItemStack extends UIElement
{
    private Consumer<ItemStack> callback;
    private ItemStack stack;
    private boolean opened;

    public UIItemStack(Consumer<ItemStack> callback)
    {
        this.stack = ItemStack.EMPTY;
        this.callback = callback;

        this.context((menu) ->
        {
            /* Abrir panel de inventario desde el menú contextual */
            menu.action(Icons.SPHERE, UIKeys.ITEM_STACK_CONTEXT_INVENTORY, () ->
            {
                this.opened = true;

                UIPlayerInventoryPanel panel = new UIPlayerInventoryPanel((i) ->
                {
                    if (this.callback != null)
                    {
                        this.callback.accept(i);
                    }

                    this.setStack(i);
                });

                panel.onClose((a) -> this.opened = false);
                /* Revertir altura y reducir ancho del panel de inventario */
                UIOverlay.addOverlay(this.getContext(), panel, 0.23F, 0.5F);
                UIUtils.playClick();
            });

            menu.action(Icons.PASTE, UIKeys.ITEM_STACK_CONTEXT_PASTE, () ->
            {
                ItemStack stack = MinecraftClient.getInstance().player.getMainHandStack().copy();

                if (this.callback != null)
                {
                    this.callback.accept(stack);
                }

                this.setStack(stack);
            });

            menu.action(Icons.CLOSE, UIKeys.ITEM_STACK_CONTEXT_RESET, () ->
            {
                if (this.callback != null)
                {
                    this.callback.accept(ItemStack.EMPTY);
                }

                this.setStack(ItemStack.EMPTY);
            });

            menu.action(Icons.POSE, UIKeys.ITEM_STACK_CONTEXT_HOTBAR, () ->
            {
                this.getContext().replaceContextMenu((newMenu) ->
                {
                    PlayerInventory inventory = MinecraftClient.getInstance().player.getInventory();

                    /* First nine slots are hotbar items */
                    for (int i = 0; i < 9; i++)
                    {
                        ItemStack s = inventory.getStack(i);

                        newMenu.action(new ItemStackContextAction(s, IKey.constant(s.getName().getString()), () ->
                        {
                            if (this.callback != null)
                            {
                                this.callback.accept(s);
                            }

                            this.setStack(s);
                        }));
                    }
                });
            });
        });

        this.h(20);
    }

    public void setStack(ItemStack stack)
    {
        this.stack = stack == null ? ItemStack.EMPTY : stack.copy();
    }

    protected boolean subMouseClicked(UIContext context)
    {
        if (this.area.isInside(context) && context.mouseButton == 0)
        {
            this.opened = true;

            /* Restaurado: clic izquierdo abre el buscador de items */
            UIItemStackOverlayPanel panel = new UIItemStackOverlayPanel((i) ->
            {
                if (this.callback != null)
                {
                    this.callback.accept(i);
                }

                this.setStack(i);
            }, this.stack);

            panel.onClose((a) -> this.opened = false);
            UIOverlay.addOverlay(this.getContext(), panel, 0.9F, 0.5F);
            UIUtils.playClick();

            return true;
        }

        return super.subMouseClicked(context);
    }

    public void render(UIContext context)
    {
        int border = this.opened ? Colors.A100 | BBSSettings.primaryColor.get() : Colors.WHITE;

        context.batcher.box((float)this.area.x, (float)this.area.y, (float)this.area.ex(), (float)this.area.ey(), border);
        context.batcher.box((float)(this.area.x + 1), (float)(this.area.y + 1), (float)(this.area.ex() - 1), (float)(this.area.ey() - 1), -3750202);

        if (this.stack != null && !this.stack.isEmpty())
        {
            MatrixStack matrices = context.batcher.getContext().getMatrices();
            CustomVertexConsumerProvider consumers = FormUtilsClient.getProvider();

            matrices.push();
            consumers.setUI(true);
            context.batcher.getContext().drawItem(this.stack, this.area.mx() - 8, this.area.my() - 8);
            context.batcher.getContext().drawItemInSlot(context.batcher.getFont().getRenderer(), this.stack, this.area.mx() - 8, this.area.my() - 8);
            consumers.setUI(false);
            matrices.pop();
        }

        super.render(context);
    }
}