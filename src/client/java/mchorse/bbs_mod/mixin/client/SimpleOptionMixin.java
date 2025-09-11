package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.client.BBSRendering;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.SimpleOption;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SimpleOption.class)
public class SimpleOptionMixin
{
    @Inject(method = "getValue", at = @At("HEAD"), cancellable = true)
    public void onGetValue(CallbackInfoReturnable info)
    {
        SimpleOption option = (SimpleOption) (Object) this;

        if (MinecraftClient.getInstance().options != null && option == MinecraftClient.getInstance().options.getGamma())
        {
            Double value = BBSRendering.getBrightness();

            if (value != null)
            {
                info.setReturnValue(value);
            }
        }
    }
}