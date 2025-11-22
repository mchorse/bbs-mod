package mchorse.bbs_mod.forms;

import com.mojang.blaze3d.systems.RenderSystem;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.forms.renderers.utils.RecolorVertexConsumer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import org.lwjgl.opengl.GL11;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

public class CustomVertexConsumerProvider extends VertexConsumerProvider.Immediate
{
    private static Consumer<RenderLayer> runnables;

    private Function<VertexConsumer, VertexConsumer> substitute;
    private boolean ui;

    public static void drawLayer(RenderLayer layer)
    {
        if (runnables != null)
        {
            runnables.accept(layer);
        }
    }

    public static void hijackVertexFormat(Consumer<RenderLayer> runnable)
    {
        runnables = runnable;
    }

    public static void clearRunnables()
    {
        runnables = null;
    }

    public CustomVertexConsumerProvider(BufferBuilder fallback, Map<RenderLayer, BufferBuilder> layers)
    {
        super(fallback, layers);
    }

    public void setSubstitute(Function<VertexConsumer, VertexConsumer> substitute)
    {
        this.substitute = substitute;

        if (this.substitute == null)
        {
            RecolorVertexConsumer.newColor = null;
        }
    }

    public void setUI(boolean ui)
    {
        this.ui = ui;
    }

    @Override
    public VertexConsumer getBuffer(RenderLayer renderLayer)
    {
        // Si hay recolor activo con alpha < 1, forzar capas cutout a translúcidas
        // para que el fade se aplique también a entidades/banners con shaders.
        RenderLayer selectedLayer = renderLayer;
        if (RecolorVertexConsumer.newColor != null && RecolorVertexConsumer.newColor.a < 0.999f)
        {
            boolean shadersEnabled = BBSRendering.isIrisShadersEnabled() && BBSRendering.isRenderingWorld();
            if (renderLayer == RenderLayer.getCutout()
                || renderLayer == RenderLayer.getCutoutMipped()
                || renderLayer == TexturedRenderLayers.getEntityCutout()
                || renderLayer == TexturedRenderLayers.getBannerPatterns())
            {
                selectedLayer = shadersEnabled ? TexturedRenderLayers.getEntityTranslucentCull() : RenderLayer.getTranslucent();
            }
        }

        VertexConsumer buffer = super.getBuffer(selectedLayer);

        if (this.substitute != null)
        {
            VertexConsumer apply = this.substitute.apply(buffer);

            if (apply != null)
            {
                return apply;
            }
        }

        return buffer;
    }

    public void draw()
    {
        super.draw();

        if (this.ui)
        {
            /* Restore the default depth function used by 3D UI rendering.
             * Some vertex consumer paths may change it (e.g., to GL_LESS),
             * so bring it back to GL_LEQUAL to avoid leaking incorrect state
             * into subsequent renders. */
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
        }
    }
}