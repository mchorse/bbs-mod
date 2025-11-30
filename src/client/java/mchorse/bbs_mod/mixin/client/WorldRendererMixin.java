package mchorse.bbs_mod.mixin.client;

import com.mojang.blaze3d.systems.RenderSystem;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.utils.colors.Color;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin
{
    @Shadow
    public Framebuffer entityOutlinesFramebuffer;

    @Inject(method = "renderSky(Lnet/minecraft/client/util/math/MatrixStack;Lorg/joml/Matrix4f;FLnet/minecraft/client/render/Camera;ZLjava/lang/Runnable;)V", at = @At("HEAD"), cancellable = true)
    public void onRenderSky(CallbackInfo info)
    {
        if (BBSSettings.chromaSkyEnabled.get())
        {
            Color color = Color.rgb(BBSSettings.chromaSkyColor.get());

            GL11.glClearColor(color.r, color.g, color.b, 1F);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
            RenderSystem.setShaderFogColor(color.r, color.g, color.b, 1F);

            info.cancel();
        }
    }

    @Inject(method = "renderLayer", at = @At("HEAD"), cancellable = true)
    public void onRenderLayer(RenderLayer renderLayer, MatrixStack matrices, double cameraX, double cameraY, double cameraZ, Matrix4f positionMatrix, CallbackInfo info)
    {
        if (BBSSettings.chromaSkyEnabled.get() && !BBSSettings.chromaSkyTerrain.get())
        {
            BBSRendering.onRenderChunkLayer(matrices);

            info.cancel();
        }
    }

    @Inject(method = "renderLayer", at = @At("TAIL"))
    public void onRenderChunkLayer(RenderLayer layer, MatrixStack stack, double x, double y, double z, Matrix4f positionMatrix, CallbackInfo info)
    {
        if (layer == RenderLayer.getSolid())
        {
            BBSRendering.onRenderChunkLayer(stack);
        }
    }

    @Inject(at = @At("RETURN"), method = "loadEntityOutlinePostProcessor")
    private void onLoadEntityOutlineShader(CallbackInfo info)
    {
        BBSRendering.resizeExtraFramebuffers();
    }

    @Inject(at = @At("RETURN"), method = "onResized")
    private void onResized(CallbackInfo info)
    {
        if (this.entityOutlinesFramebuffer == null)
        {
            return;
        }

        BBSRendering.resizeExtraFramebuffers();
    }
}