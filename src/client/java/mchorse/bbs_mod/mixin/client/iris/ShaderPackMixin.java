package mchorse.bbs_mod.mixin.client.iris;

import mchorse.bbs_mod.utils.iris.IrisUtils;
import net.irisshaders.iris.shaderpack.ShaderPack;
import net.irisshaders.iris.shaderpack.properties.ShaderProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ShaderPack.class)
public class ShaderPackMixin
{
    @Shadow(remap = false) private ShaderProperties shaderProperties;

    @Inject(
        method = "<init>(Ljava/nio/file/Path;Ljava/util/Map;Lcom/google/common/collect/ImmutableList;)V",
        at = @At(value = "INVOKE", target = "Ljava/util/HashMap;<init>()V", shift = At.Shift.AFTER),
        remap = false
    )
    private void afterActiveFeaturesInit(CallbackInfo ci)
    {
        IrisUtils.setShaderProperties(shaderProperties);
    }
}