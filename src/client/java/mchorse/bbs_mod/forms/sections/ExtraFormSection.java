package mchorse.bbs_mod.forms.sections;

import mchorse.bbs_mod.forms.FormCategories;
import mchorse.bbs_mod.forms.categories.FormCategory;
import mchorse.bbs_mod.forms.forms.AnchorForm;
import mchorse.bbs_mod.forms.forms.BillboardForm;
import mchorse.bbs_mod.forms.forms.BlockForm;
import mchorse.bbs_mod.forms.forms.ExtrudedForm;
import mchorse.bbs_mod.forms.forms.FramebufferForm;
import mchorse.bbs_mod.forms.forms.ItemForm;
import mchorse.bbs_mod.forms.forms.LabelForm;
import mchorse.bbs_mod.forms.forms.MobForm;
import mchorse.bbs_mod.forms.forms.StructureForm;
import mchorse.bbs_mod.forms.forms.TrailForm;
import mchorse.bbs_mod.forms.forms.VanillaParticleForm;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.UIKeys;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.Arrays;
import java.util.List;

public class ExtraFormSection extends FormSection
{
    private static final List<String> mobAnimalsIds = Arrays.asList("minecraft:axolotl", "minecraft:bat", "minecraft:bee", "minecraft:camel", "minecraft:cat", "minecraft:chicken", "minecraft:cod", "minecraft:cow", "minecraft:dolphin", "minecraft:donkey", "minecraft:fox", "minecraft:frog", "minecraft:glow_squid", "minecraft:goat", "minecraft:horse", "minecraft:llama", "minecraft:mooshroom", "minecraft:mule", "minecraft:ocelot", "minecraft:panda", "minecraft:parrot", "minecraft:pig", "minecraft:polar_bear", "minecraft:pufferfish", "minecraft:rabbit", "minecraft:salmon", "minecraft:sheep", "minecraft:skeleton_horse", "minecraft:sniffer", "minecraft:squid", "minecraft:tropical_fish", "minecraft:turtle", "minecraft:wolf", "minecraft:zombie_horse");
    private static final List<String> mobNeutralIds = Arrays.asList("minecraft:allay", "minecraft:enderman", "minecraft:iron_golem", "minecraft:piglin", "minecraft:piglin_brute", "minecraft:snow_golem", "minecraft:strider", "minecraft:villager", "minecraft:wandering_trader");
    private static final List<String> mobHostileIds = Arrays.asList("minecraft:blaze", "minecraft:cave_spider", "minecraft:creeper", "minecraft:drowned", "minecraft:elder_guardian", "minecraft:ender_dragon", "minecraft:endermite", "minecraft:evoker", "minecraft:ghast", "minecraft:guardian", "minecraft:hoglin", "minecraft:husk", "minecraft:illusioner", "minecraft:magma_cube", "minecraft:phantom", "minecraft:pillager", "minecraft:ravager", "minecraft:silverfish", "minecraft:skeleton", "minecraft:slime", "minecraft:spider", "minecraft:stray", "minecraft:vex", "minecraft:vindicator", "minecraft:warden", "minecraft:witch", "minecraft:wither", "minecraft:wither_skeleton", "minecraft:zoglin", "minecraft:zombie", "minecraft:zombie_villager", "minecraft:zombified_piglin");
    private static final List<String> mobMiscIds = Arrays.asList("minecraft:armor_stand", "minecraft:arrow", "minecraft:boat", "minecraft:end_crystal", "minecraft:lightning_bolt", "minecraft:minecart", "minecraft:shulker_bullet", "minecraft:spectral_arrow", "minecraft:trident");

    private FormCategory mobsAnimals;
    private FormCategory mobsNeutral;
    private FormCategory mobsHostile;
    private FormCategory mobsMisc;
    private FormCategory extra;
    private List<FormCategory> categories;

    public ExtraFormSection(FormCategories parent)
    {
        super(parent);
    }

    @Override
    public void initiate()
    {
        FormCategory extra = new FormCategory(UIKeys.FORMS_CATEGORIES_EXTRA, this.parent.visibility.get("extra"));
        AnchorForm anchor = new AnchorForm();
        BillboardForm billboard = new BillboardForm();
        LabelForm label = new LabelForm();
        ExtrudedForm extruded = new ExtrudedForm();
        BlockForm block = new BlockForm();
        ItemForm item = new ItemForm();
        VanillaParticleForm vanillaParticle = new VanillaParticleForm();
        TrailForm trail = new TrailForm();
        StructureForm structure = new StructureForm();

        billboard.texture.set(Link.assets("textures/error.png"));
        extruded.texture.set(Link.assets("textures/error.png"));
        block.blockState.set(Blocks.GRASS_BLOCK.getDefaultState());
        item.stack.set(new ItemStack(Items.STICK));

        extra.addForm(anchor);
        extra.addForm(billboard);
        extra.addForm(label);
        extra.addForm(extruded);
        extra.addForm(block);
        extra.addForm(item);
        extra.addForm(vanillaParticle);
        extra.addForm(trail);
        extra.addForm(structure);

        this.mobsAnimals = new FormCategory(UIKeys.FORMS_CATEGORIES_MOBS_ANIMALS, this.parent.visibility.get("mobs_animals"));
        this.mobsNeutral = new FormCategory(UIKeys.FORMS_CATEGORIES_MOBS_NEUTRAL, this.parent.visibility.get("mobs_neutral"));
        this.mobsHostile = new FormCategory(UIKeys.FORMS_CATEGORIES_MOBS_HOSTILE, this.parent.visibility.get("mobs_hostile"));
        this.mobsMisc = new FormCategory(UIKeys.FORMS_CATEGORIES_MOBS_MISC, this.parent.visibility.get("mobs_misc"));
        this.extra = extra;

        this.fillMobs(this.mobsAnimals, mobAnimalsIds);
        this.fillMobs(this.mobsNeutral, mobNeutralIds);
        this.fillMobs(this.mobsHostile, mobHostileIds);
        this.fillMobs(this.mobsMisc, mobMiscIds);

        this.categories = Arrays.asList(this.extra, this.mobsAnimals, this.mobsNeutral, this.mobsHostile, this.mobsMisc);
    }

    private void fillMobs(FormCategory category, List<String> ids)
    {
        for (String mobId : ids)
        {
            MobForm form = new MobForm();

            form.mobID.set(mobId);
            category.addForm(form);
        }
    }

    @Override
    public List<FormCategory> getCategories()
    {
        return this.categories;
    }
}