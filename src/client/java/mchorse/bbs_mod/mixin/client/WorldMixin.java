package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.client.BBSRendering;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(World.class)
public class WorldMixin
{
    @Inject(method = "getRainGradient", at = @At("HEAD"), cancellable = true)
    public void onGetRainGradient(CallbackInfoReturnable<Float> info)
    {
        Double rainFactor = BBSRendering.getWeather();

        if (rainFactor != null)
        {
            info.setReturnValue(rainFactor.floatValue());
        }
    }
}