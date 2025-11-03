package mchorse.bbs_mod.forms.renderers.utils;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.light.LightingProvider;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.Blocks;
import net.minecraft.world.biome.ColorResolver;
import net.minecraft.world.biome.Biome;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Vista de mundo mínima para permitir render de bloques con culling.
 *
 * Provee estados de bloque y métodos básicos requeridos por BlockRenderView.
 * Iluminación y color se delegan al ClientWorld si existe; en ausencia de mundo
 * se retornan valores seguros (brillo máximo y luz base cero) para evitar NPE.
 */
public class VirtualBlockRenderView implements BlockRenderView
{
    private final Map<BlockPos, BlockState> states = new HashMap<>();
    private int bottomY = 0;
    private int topY = 256;

    // Biome override, if provided by the UI
    private Identifier biomeOverrideId = null;
    private Biome biomeOverride = null;

    public static class Entry
    {
        public final BlockState state;
        public final BlockPos pos;

        public Entry(BlockState state, BlockPos pos)
        {
            this.state = state;
            this.pos = pos;
        }
    }

    public VirtualBlockRenderView(List<Entry> entries)
    {
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;

        for (Entry e : entries)
        {
            this.states.put(e.pos, e.state == null ? Blocks.AIR.getDefaultState() : e.state);

            if (e.pos.getY() < minY) minY = e.pos.getY();
            if (e.pos.getY() > maxY) maxY = e.pos.getY();
        }

        if (minY != Integer.MAX_VALUE && maxY != Integer.MIN_VALUE)
        {
            this.bottomY = minY;
            this.topY = maxY;
        }
    }

    /**
     * Establece un bioma a usar para consultas de color. Pasa null o "" para limpiar.
     */
    public VirtualBlockRenderView setBiomeOverride(String biomeId)
    {
        if (biomeId == null || biomeId.isEmpty())
        {
            this.biomeOverrideId = null;
            this.biomeOverride = null;
            return this;
        }

        try
        {
            this.biomeOverrideId = new Identifier(biomeId);
            // Resolver preferentemente desde el mundo del cliente
            if (MinecraftClient.getInstance().world != null)
            {
                Registry<Biome> reg = MinecraftClient.getInstance().world.getRegistryManager().get(RegistryKeys.BIOME);
                this.biomeOverride = reg.get(this.biomeOverrideId);
            }
            else
            {
                this.biomeOverride = null;
            }
        }
        catch (Throwable t)
        {
            this.biomeOverrideId = null;
            this.biomeOverride = null;
        }

        return this;
    }

    // BlockView
    @Override
    public BlockEntity getBlockEntity(BlockPos pos)
    {
        return null;
    }

    @Override
    public BlockState getBlockState(BlockPos pos)
    {
        BlockState state = this.states.get(pos);
        return state != null ? state : Blocks.AIR.getDefaultState();
    }

    @Override
    public FluidState getFluidState(BlockPos pos)
    {
        return Fluids.EMPTY.getDefaultState();
    }

    @Override
    public int getLuminance(BlockPos pos)
    {
        BlockState s = getBlockState(pos);
        return s == null ? 0 : s.getLuminance();
    }

    @Override
    public int getMaxLightLevel()
    {
        return 15;
    }

    // BlockRenderView
    @Override
    public float getBrightness(Direction direction, boolean shaded)
    {
        if (MinecraftClient.getInstance().world != null)
        {
            return MinecraftClient.getInstance().world.getBrightness(direction, shaded);
        }

        return 1.0F;
    }

    @Override
    public LightingProvider getLightingProvider()
    {
        if (MinecraftClient.getInstance().world != null)
        {
            return MinecraftClient.getInstance().world.getLightingProvider();
        }

        // Sin mundo: devolver null no es ideal, pero la ruta de UI mantiene render as entity.
        // Esta clase se usa únicamente en render 3D donde hay mundo.
        return null;
    }

    @Override
    public int getColor(BlockPos pos, ColorResolver colorResolver)
    {
        // Si hay bioma forzado, usarlo para resolver el color
        if (this.biomeOverride != null)
        {
            return colorResolver.getColor(this.biomeOverride, pos.getX(), pos.getZ());
        }

        if (MinecraftClient.getInstance().world != null)
        {
            return MinecraftClient.getInstance().world.getColor(pos, colorResolver);
        }

        return 0xFFFFFF;
    }

    @Override
    public int getLightLevel(LightType type, BlockPos pos)
    {
        if (MinecraftClient.getInstance().world != null)
        {
            return MinecraftClient.getInstance().world.getLightLevel(type, pos);
        }

        return 0;
    }

    @Override
    public int getBaseLightLevel(BlockPos pos, int ambientDarkness)
    {
        if (MinecraftClient.getInstance().world != null)
        {
            return MinecraftClient.getInstance().world.getBaseLightLevel(pos, ambientDarkness);
        }

        return 0;
    }

    @Override
    public boolean isSkyVisible(BlockPos pos)
    {
        if (MinecraftClient.getInstance().world != null)
        {
            return MinecraftClient.getInstance().world.isSkyVisible(pos);
        }

        return false;
    }

    // HeightLimitView
    @Override
    public int getBottomY()
    {
        return this.bottomY;
    }

    @Override
    public int getTopY()
    {
        return this.topY;
    }

    @Override
    public int getHeight()
    {
        return this.topY - this.bottomY + 1;
    }
}