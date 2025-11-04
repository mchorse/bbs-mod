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

    // Ancla mundial y offsets base para traducir posiciones locales de la estructura
    // a coordenadas reales del mundo al consultar iluminación y color.
    private net.minecraft.util.math.BlockPos worldAnchor = net.minecraft.util.math.BlockPos.ORIGIN;
    private int baseDx = 0;
    private int baseDy = 0;
    private int baseDz = 0;

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
     * Establece ancla de mundo y offset base (derivado del centrado/paridad) para
     * mapear las posiciones locales a posiciones absolutas del mundo.
     */
    public VirtualBlockRenderView setWorldAnchor(net.minecraft.util.math.BlockPos anchor, int baseDx, int baseDy, int baseDz)
    {
        this.worldAnchor = anchor == null ? net.minecraft.util.math.BlockPos.ORIGIN : anchor;
        this.baseDx = baseDx;
        this.baseDy = baseDy;
        this.baseDz = baseDz;
        return this;
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
            int wx = this.worldAnchor.getX() + this.baseDx + pos.getX();
            int wz = this.worldAnchor.getZ() + this.baseDz + pos.getZ();
            return colorResolver.getColor(this.biomeOverride, wx, wz);
        }

        if (MinecraftClient.getInstance().world != null)
        {
            BlockPos worldPos = this.worldAnchor.add(this.baseDx + pos.getX(), this.baseDy + pos.getY(), this.baseDz + pos.getZ());
            return MinecraftClient.getInstance().world.getColor(worldPos, colorResolver);
        }

        return 0xFFFFFF;
    }

    @Override
    public int getLightLevel(LightType type, BlockPos pos)
    {
        int worldLevel = 0;
        if (MinecraftClient.getInstance().world != null)
        {
            BlockPos worldPos = this.worldAnchor.add(this.baseDx + pos.getX(), this.baseDy + pos.getY(), this.baseDz + pos.getZ());
            worldLevel = MinecraftClient.getInstance().world.getLightLevel(type, worldPos);
        }

        // Para luz de bloque, combinar con la emitida por bloques luminosos
        // contenidos en esta vista virtual (no presentes en el mundo real).
        if (type == LightType.BLOCK)
        {
            int local = getLocalBlockLight(pos);
            return Math.max(worldLevel, local);
        }

        return worldLevel;
    }

    @Override
    public int getBaseLightLevel(BlockPos pos, int ambientDarkness)
    {
        int worldBase = 0;
        if (MinecraftClient.getInstance().world != null)
        {
            BlockPos worldPos = this.worldAnchor.add(this.baseDx + pos.getX(), this.baseDy + pos.getY(), this.baseDz + pos.getZ());
            worldBase = MinecraftClient.getInstance().world.getBaseLightLevel(worldPos, ambientDarkness);
        }

        // El nivel base es el máximo entre cielo/bloque. Incorporar la contribución
        // local de bloque para que fuentes virtuales iluminen correctamente.
        int localBlock = getLocalBlockLight(pos);
        return Math.max(worldBase, localBlock);
    }

    @Override
    public boolean isSkyVisible(BlockPos pos)
    {
        if (MinecraftClient.getInstance().world != null)
        {
            BlockPos worldPos = this.worldAnchor.add(this.baseDx + pos.getX(), this.baseDy + pos.getY(), this.baseDz + pos.getZ());
            return MinecraftClient.getInstance().world.isSkyVisible(worldPos);
        }

        return false;
    }

    /**
     * Calcula luz de bloque local emitida por estados dentro de esta vista.
     * Aproximación: atenuación por distancia Manhattan como en propagación clásica.
     * Ignora oclusión para mantener costo bajo y evitar rutas complejas.
     */
    private int getLocalBlockLight(BlockPos pos)
    {
        int max = 0;
        for (Map.Entry<BlockPos, BlockState> e : this.states.entrySet())
        {
            BlockState s = e.getValue();
            if (s == null)
            {
                continue;
            }

            int luminance = s.getLuminance();
            if (luminance <= 0)
            {
                continue;
            }

            BlockPos sp = e.getKey();
            int dist = Math.abs(sp.getX() - pos.getX()) + Math.abs(sp.getY() - pos.getY()) + Math.abs(sp.getZ() - pos.getZ());
            int contrib = luminance - dist;
            if (contrib > max)
            {
                max = contrib;
            }
        }

        if (max < 0) max = 0;
        if (max > 15) max = 15;
        return max;
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