package mchorse.bbs_mod.blocks.entities;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.data.DataStorageUtils;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.events.ModelBlockEntityUpdateCallback;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.entities.StubEntity;
import mchorse.bbs_mod.forms.forms.Form;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class ModelBlockEntity extends BlockEntity
{
    private ModelProperties properties = new ModelProperties();
    private IEntity entity = new StubEntity();

    public ModelBlockEntity(BlockPos pos, BlockState state)
    {
        super(BBSMod.MODEL_BLOCK_ENTITY, pos, state);
    }

    public String getName()
    {
        BlockPos pos = this.getPos();
        Form form = this.getProperties().getForm();
        String s = "(" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ")";

        if (form != null)
        {
            s += " " + form.getDisplayName();
        }

        return s;
    }

    public ModelProperties getProperties()
    {
        return this.properties;
    }

    public IEntity getEntity()
    {
        return this.entity;
    }

    public void tick(World world, BlockPos pos, BlockState state)
    {
        ModelBlockEntityUpdateCallback.EVENT.invoker().update(this);
        /* Asegura que el StubEntity tenga posición y mundo correctos para cálculos de luz/bioma.
         * Sin esto, el entity se queda en (0,0,0) y los renders toman luz de esa zona,
         * provocando oscurecimiento en editor, miniatura y bloque de modelo. */
        this.entity.setWorld(world);
        this.entity.setPosition(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        this.entity.update();
        this.properties.update(this.entity);
    }

    @Nullable
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket()
    {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt()
    {
        return createNbt();
    }

    @Override
    protected void writeNbt(NbtCompound nbt)
    {
        super.writeNbt(nbt);

        MapType data = this.properties.toData();

        DataStorageUtils.writeToNbtCompound(nbt, "Properties", data);
    }

    @Override
    public void readNbt(NbtCompound nbt)
    {
        super.readNbt(nbt);

        BaseType baseType = DataStorageUtils.readFromNbtCompound(nbt, "Properties");

        if (baseType instanceof MapType mapType)
        {
            this.properties.fromData(mapType);
        }
    }

    public void updateForm(MapType data, World world)
    {
        this.properties.fromData(data);

        BlockPos pos = this.getPos();
        BlockState blockState = world.getBlockState(pos);

        world.updateListeners(pos, blockState, blockState, Block.NOTIFY_LISTENERS);
        world.markDirty(pos);
    }
}