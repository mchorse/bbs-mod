package mchorse.bbs_mod.selectors;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.entities.MCEntity;
import mchorse.bbs_mod.forms.forms.Form;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.world.World;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class SelectorOwner
{
    public IEntity entity;

    private Form form;
    private long check;
    private int nbtCheck;
    private NbtCompound lastNbt;

    private LivingEntity mcEntity;

    public SelectorOwner(LivingEntity mcEntity)
    {
        this.mcEntity = mcEntity;
        this.entity = new MCEntity(mcEntity);
    }

    public Form getForm()
    {
        return form;
    }

    public void update()
    {
        World world = this.entity.getWorld();

        if (!world.isClient)
        {
            return;
        }

        this.check();
        this.entity.update();

        if (this.form != null)
        {
            this.form.update(this.entity);
        }
    }

    public void check()
    {
        EntitySelectors selectors = BBSModClient.getSelectors();

        if (this.nbtCheck <= 0)
        {
            this.nbtCheck = 10;

            Set<String> keys = createWhitelist();
            NbtCompound compound = this.mcEntity.writeNbt(new NbtCompound());
            NbtCompound newCompound = new NbtCompound();

            for (String key : keys)
            {
                NbtElement element = compound.get(key);

                if (element != null)
                {
                    newCompound.put(key, element);
                }
            }

            if (!Objects.equals(newCompound, this.lastNbt))
            {
                this.check = 0;
            }

            this.lastNbt = newCompound;
        }

        if (this.check < selectors.getLastUpdate())
        {
            this.check = selectors.getLastUpdate();

            EntitySelector selectorFor = selectors.getSelectorFor(this.mcEntity);

            if (selectorFor != null)
            {
                this.form = FormUtils.copy(selectorFor.form);

                if (this.form != null)
                {
                    this.form.playMain();
                }
            }
            else
            {
                this.form = null;
            }
        }

        this.nbtCheck -= 1;
    }

    private Set<String> createWhitelist()
    {
        HashSet<String> strings = new HashSet<>();
        String s = BBSSettings.entitySelectorsPropertyWhitelist.get();
        String[] split = s.split(",");

        for (String string : split)
        {
            strings.add(string.trim());
        }

        return strings;
    }
}