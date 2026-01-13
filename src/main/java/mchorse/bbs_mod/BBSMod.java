package mchorse.bbs_mod;

import mchorse.bbs_mod.actions.ActionHandler;
import mchorse.bbs_mod.actions.ActionManager;
import mchorse.bbs_mod.actions.types.AttackActionClip;
import mchorse.bbs_mod.actions.types.DamageActionClip;
import mchorse.bbs_mod.actions.types.SwipeActionClip;
import mchorse.bbs_mod.actions.types.blocks.BreakBlockActionClip;
import mchorse.bbs_mod.actions.types.blocks.InteractBlockActionClip;
import mchorse.bbs_mod.actions.types.blocks.PlaceBlockActionClip;
import mchorse.bbs_mod.actions.types.chat.ChatActionClip;
import mchorse.bbs_mod.actions.types.chat.CommandActionClip;
import mchorse.bbs_mod.actions.types.item.ItemDropActionClip;
import mchorse.bbs_mod.actions.types.item.UseBlockItemActionClip;
import mchorse.bbs_mod.actions.types.item.UseItemActionClip;
import mchorse.bbs_mod.blocks.ModelBlock;
import mchorse.bbs_mod.blocks.entities.ModelBlockEntity;
import mchorse.bbs_mod.blocks.entities.ModelProperties;
import mchorse.bbs_mod.camera.clips.ClipFactoryData;
import mchorse.bbs_mod.camera.clips.converters.DollyToKeyframeConverter;
import mchorse.bbs_mod.camera.clips.converters.DollyToPathConverter;
import mchorse.bbs_mod.camera.clips.converters.IdleConverter;
import mchorse.bbs_mod.camera.clips.converters.IdleToDollyConverter;
import mchorse.bbs_mod.camera.clips.converters.IdleToKeyframeConverter;
import mchorse.bbs_mod.camera.clips.converters.IdleToPathConverter;
import mchorse.bbs_mod.camera.clips.converters.PathToDollyConverter;
import mchorse.bbs_mod.camera.clips.converters.PathToKeyframeConverter;
import mchorse.bbs_mod.camera.clips.misc.AudioClip;
import mchorse.bbs_mod.camera.clips.misc.CurveClip;
import mchorse.bbs_mod.camera.clips.misc.SubtitleClip;
import mchorse.bbs_mod.camera.clips.modifiers.AngleClip;
import mchorse.bbs_mod.camera.clips.modifiers.DollyZoomClip;
import mchorse.bbs_mod.camera.clips.modifiers.DragClip;
import mchorse.bbs_mod.camera.clips.modifiers.LookClip;
import mchorse.bbs_mod.camera.clips.modifiers.MathClip;
import mchorse.bbs_mod.camera.clips.modifiers.OrbitClip;
import mchorse.bbs_mod.camera.clips.modifiers.RemapperClip;
import mchorse.bbs_mod.camera.clips.modifiers.ShakeClip;
import mchorse.bbs_mod.camera.clips.modifiers.TrackerClip;
import mchorse.bbs_mod.camera.clips.modifiers.TranslateClip;
import mchorse.bbs_mod.camera.clips.overwrite.DollyClip;
import mchorse.bbs_mod.camera.clips.overwrite.IdleClip;
import mchorse.bbs_mod.camera.clips.overwrite.KeyframeClip;
import mchorse.bbs_mod.camera.clips.overwrite.PathClip;
import mchorse.bbs_mod.entity.ActorEntity;
import mchorse.bbs_mod.entity.GunProjectileEntity;
import mchorse.bbs_mod.events.BBSAddonMod;
import mchorse.bbs_mod.events.EventBus;
import mchorse.bbs_mod.events.register.RegisterSettingsEvent;
import mchorse.bbs_mod.events.register.RegisterSourcePacksEvent;
import mchorse.bbs_mod.film.FilmManager;
import mchorse.bbs_mod.forms.FormArchitect;
import mchorse.bbs_mod.forms.forms.AnchorForm;
import mchorse.bbs_mod.forms.forms.BillboardForm;
import mchorse.bbs_mod.forms.forms.BlockForm;
import mchorse.bbs_mod.forms.forms.ExtrudedForm;
import mchorse.bbs_mod.forms.forms.FramebufferForm;
import mchorse.bbs_mod.forms.forms.ItemForm;
import mchorse.bbs_mod.forms.forms.LabelForm;
import mchorse.bbs_mod.forms.forms.MobForm;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.forms.forms.ParticleForm;
import mchorse.bbs_mod.forms.forms.TrailForm;
import mchorse.bbs_mod.forms.forms.VanillaParticleForm;
import mchorse.bbs_mod.items.GunItem;
import mchorse.bbs_mod.morphing.Morph;
import mchorse.bbs_mod.network.ServerNetwork;
import mchorse.bbs_mod.resources.AssetProvider;
import mchorse.bbs_mod.resources.ISourcePack;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.resources.packs.DynamicSourcePack;
import mchorse.bbs_mod.resources.packs.ExternalAssetsSourcePack;
import mchorse.bbs_mod.resources.packs.InternalAssetsSourcePack;
import mchorse.bbs_mod.settings.Settings;
import mchorse.bbs_mod.settings.SettingsBuilder;
import mchorse.bbs_mod.settings.SettingsManager;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.factory.MapFactory;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameRules;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class BBSMod implements ModInitializer
{
    public static final String MOD_ID = "bbs";

    public static final EventBus events = new EventBus();

    private static ActionManager actions;

    /* Important folders */
    private static File gameFolder;
    private static File assetsFolder;
    private static File settingsFolder;

    /* Core services */
    private static AssetProvider provider;
    private static DynamicSourcePack dynamicSourcePack;
    private static ExternalAssetsSourcePack originalSourcePack;

    /* Foundation services */
    private static SettingsManager settings;
    private static FormArchitect forms;

    /* Data */
    private static FilmManager films;

    private static List<Runnable> runnables = new ArrayList<>();

    private static MapFactory<Clip, ClipFactoryData> factoryCameraClips;
    private static MapFactory<Clip, ClipFactoryData> factoryActionClips;

    public static final EntityType<ActorEntity> ACTOR_ENTITY = Registry.register(
        Registries.ENTITY_TYPE,
        new Identifier(MOD_ID, "actor"),
        FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, ActorEntity::new)
            .dimensions(EntityDimensions.fixed(0.6F, 1.8F))
            .trackRangeBlocks(256)
            .trackedUpdateRate(1)
            .build());

    public static final EntityType<GunProjectileEntity> GUN_PROJECTILE_ENTITY = Registry.register(
        Registries.ENTITY_TYPE,
        new Identifier(MOD_ID, "gun_projectile"),
        FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, GunProjectileEntity::new)
            .dimensions(EntityDimensions.fixed(0.25F, 0.25F))
            .trackRangeChunks(24)
            .trackedUpdateRate(1)
            .build());

    public static final Block MODEL_BLOCK = new ModelBlock(FabricBlockSettings.create()
        .noBlockBreakParticles()
        .dropsNothing()
        .noCollision()
        .nonOpaque()
        .notSolid()
        .strength(0F));
    public static final Block CHROMA_RED_BLOCK = createChromaBlock();
    public static final Block CHROMA_GREEN_BLOCK = createChromaBlock();
    public static final Block CHROMA_BLUE_BLOCK = createChromaBlock();
    public static final Block CHROMA_CYAN_BLOCK = createChromaBlock();
    public static final Block CHROMA_MAGENTA_BLOCK = createChromaBlock();
    public static final Block CHROMA_YELLOW_BLOCK = createChromaBlock();
    public static final Block CHROMA_BLACK_BLOCK = createChromaBlock();
    public static final Block CHROMA_WHITE_BLOCK = createChromaBlock();

    public static final BlockItem MODEL_BLOCK_ITEM = new BlockItem(MODEL_BLOCK, new Item.Settings());
    public static final GunItem GUN_ITEM = new GunItem(new Item.Settings().maxCount(1));
    public static final BlockItem CHROMA_RED_BLOCK_ITEM = new BlockItem(CHROMA_RED_BLOCK, new Item.Settings());
    public static final BlockItem CHROMA_GREEN_BLOCK_ITEM = new BlockItem(CHROMA_GREEN_BLOCK, new Item.Settings());
    public static final BlockItem CHROMA_BLUE_BLOCK_ITEM = new BlockItem(CHROMA_BLUE_BLOCK, new Item.Settings());
    public static final BlockItem CHROMA_CYAN_BLOCK_ITEM = new BlockItem(CHROMA_CYAN_BLOCK, new Item.Settings());
    public static final BlockItem CHROMA_MAGENTA_BLOCK_ITEM = new BlockItem(CHROMA_MAGENTA_BLOCK, new Item.Settings());
    public static final BlockItem CHROMA_YELLOW_BLOCK_ITEM = new BlockItem(CHROMA_YELLOW_BLOCK, new Item.Settings());
    public static final BlockItem CHROMA_BLACK_BLOCK_ITEM = new BlockItem(CHROMA_BLACK_BLOCK, new Item.Settings());
    public static final BlockItem CHROMA_WHITE_BLOCK_ITEM = new BlockItem(CHROMA_WHITE_BLOCK, new Item.Settings());

    public static final GameRules.Key<GameRules.BooleanRule> BBS_EDITING_RULE = GameRuleRegistry.register("bbsEditing", GameRules.Category.MISC, GameRuleFactory.createBooleanRule(true));

    public static final BlockEntityType<ModelBlockEntity> MODEL_BLOCK_ENTITY = Registry.register(
        Registries.BLOCK_ENTITY_TYPE,
        new Identifier(MOD_ID, "model_block_entity"),
        FabricBlockEntityTypeBuilder.create(ModelBlockEntity::new, MODEL_BLOCK).build()
    );

    public static final ItemGroup ITEM_GROUP = FabricItemGroup.builder()
        .icon(() -> createModelBlockStack(Link.assets("textures/icon.png")))
        .displayName(Text.translatable("itemGroup.bbs.main"))
        .entries((context, entries) ->
        {
            entries.add(createModelBlockStack(Link.assets("textures/model_block.png")));
            entries.add(CHROMA_RED_BLOCK_ITEM);
            entries.add(CHROMA_GREEN_BLOCK_ITEM);
            entries.add(CHROMA_BLUE_BLOCK_ITEM);
            entries.add(CHROMA_CYAN_BLOCK_ITEM);
            entries.add(CHROMA_MAGENTA_BLOCK_ITEM);
            entries.add(CHROMA_YELLOW_BLOCK_ITEM);
            entries.add(CHROMA_BLACK_BLOCK_ITEM);
            entries.add(CHROMA_WHITE_BLOCK_ITEM);
            entries.add(new ItemStack(GUN_ITEM));
        })
        .build();

    public static final SoundEvent CLICK = registerSound("click");

    private static SoundEvent registerSound(String path)
    {
        Identifier id = new Identifier(MOD_ID, path);

        return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }

    private static File worldFolder;

    private static Block createChromaBlock()
    {
        return new Block(FabricBlockSettings.create()
            .noBlockBreakParticles()
            .dropsNothing()
            .requiresTool()
            .strength(-1F, 3600000F));
    }

    private static ItemStack createModelBlockStack(Link texture)
    {
        ItemStack stack = new ItemStack(MODEL_BLOCK_ITEM);
        ModelBlockEntity entity = new ModelBlockEntity(BlockPos.ORIGIN, MODEL_BLOCK.getDefaultState());
        NbtCompound nbt = new NbtCompound();
        BillboardForm form = new BillboardForm();
        ModelProperties properties = entity.getProperties();

        form.transform.get().translate.set(0F, 0.5F, 0F);
        form.texture.set(texture);
        properties.setForm(form);
        properties.getTransformFirstPerson().translate.set(0F, 0F, -0.25F);

        NbtCompound compound = entity.createNbtWithId();

        nbt.put("BlockEntityTag", compound);
        stack.setNbt(nbt);

        return stack;
    }

    /**
     * Main folder, where all the other folders are located.
     */
    public static File getGameFolder()
    {
        return gameFolder;
    }

    public static File getGamePath(String path)
    {
        return new File(gameFolder, path);
    }

    /**
     * Assets folder within game's folder. It's used to store any assets that can
     * be loaded by {@link #provider}.
     */
    public static File getAssetsFolder()
    {
        ISourcePack sourcePack = getDynamicSourcePack().getSourcePack();

        if (sourcePack instanceof ExternalAssetsSourcePack pack)
        {
            return pack.getFolder();
        }

        return assetsFolder;
    }

    public static File getAudioFolder()
    {
        return getAssetsPath("audio");
    }

    public static File getAssetsPath(String path)
    {
        return new File(getAssetsFolder(), path);
    }

    public static File getAudioCacheFolder()
    {
        return getSettingsPath("audio_cache");
    }

    /**
     * Config folder within game's folder. It's used to store any configuration
     * files.
     */
    public static File getSettingsFolder()
    {
        return settingsFolder;
    }

    public static File getSettingsPath(String path)
    {
        return new File(settingsFolder, path);
    }

    public static File getExportFolder()
    {
        return getGamePath("export");
    }

    public static ActionManager getActions()
    {
        return actions;
    }

    public static AssetProvider getProvider()
    {
        return provider;
    }

    public static DynamicSourcePack getDynamicSourcePack()
    {
        return dynamicSourcePack;
    }

    public static ExternalAssetsSourcePack getOriginalSourcePack()
    {
        return originalSourcePack;
    }

    public static SettingsManager getSettings()
    {
        return settings;
    }

    public static FormArchitect getForms()
    {
        return forms;
    }

    public static FilmManager getFilms()
    {
        return films;
    }

    public static MapFactory<Clip, ClipFactoryData> getFactoryCameraClips()
    {
        return factoryCameraClips;
    }

    public static MapFactory<Clip, ClipFactoryData> getFactoryActionClips()
    {
        return factoryActionClips;
    }

    @Override
    public void onInitialize()
    {
        /* Core */
        gameFolder = FabricLoader.getInstance().getGameDir().toFile();
        assetsFolder = new File(gameFolder, "config/bbs/assets");
        settingsFolder = new File(gameFolder, "config/bbs/settings");

        assetsFolder.mkdirs();

        FabricLoader.getInstance()
            .getEntrypointContainers("bbs-addon", BBSAddonMod.class)
            .forEach((container) ->
            {
                events.register(container.getEntrypoint());
            });

        actions = new ActionManager();

        originalSourcePack = new ExternalAssetsSourcePack(Link.ASSETS, assetsFolder).providesFiles();
        dynamicSourcePack = new DynamicSourcePack(originalSourcePack);
        provider = new AssetProvider();
        provider.register(dynamicSourcePack);
        provider.register(new InternalAssetsSourcePack());

        events.post(new RegisterSourcePacksEvent(provider));

        settings = new SettingsManager();
        forms = new FormArchitect();
        forms
            .register(Link.bbs("billboard"), BillboardForm.class, null)
            .register(Link.bbs("label"), LabelForm.class, null)
            .register(Link.bbs("model"), ModelForm.class, null)
            .register(Link.bbs("particle"), ParticleForm.class, null)
            .register(Link.bbs("extruded"), ExtrudedForm.class, null)
            .register(Link.bbs("block"), BlockForm.class, null)
            .register(Link.bbs("item"), ItemForm.class, null)
            .register(Link.bbs("anchor"), AnchorForm.class, null)
            .register(Link.bbs("mob"), MobForm.class, null)
            .register(Link.bbs("vanilla_particles"), VanillaParticleForm.class, null)
            .register(Link.bbs("trail"), TrailForm.class, null)
            .register(Link.bbs("framebuffer"), FramebufferForm.class, null);

        films = new FilmManager(() -> new File(worldFolder, "bbs/films"));

        /* Register camera clips */
        factoryCameraClips = new MapFactory<Clip, ClipFactoryData>()
            .register(Link.bbs("idle"), IdleClip.class, new ClipFactoryData(Icons.FRUSTUM, 0x159e64)
                .withConverter(Link.bbs("dolly"), new IdleToDollyConverter())
                .withConverter(Link.bbs("path"), new IdleToPathConverter())
                .withConverter(Link.bbs("keyframe"), new IdleToKeyframeConverter()))
            .register(Link.bbs("dolly"), DollyClip.class, new ClipFactoryData(Icons.CAMERA, 0xffa500)
                .withConverter(Link.bbs("idle"), IdleConverter.CONVERTER)
                .withConverter(Link.bbs("path"), new DollyToPathConverter())
                .withConverter(Link.bbs("keyframe"), new DollyToKeyframeConverter()))
            .register(Link.bbs("path"), PathClip.class, new ClipFactoryData(Icons.GALLERY, 0x6820ad)
                .withConverter(Link.bbs("idle"), IdleConverter.CONVERTER)
                .withConverter(Link.bbs("dolly"), new PathToDollyConverter())
                .withConverter(Link.bbs("keyframe"), new PathToKeyframeConverter()))
            .register(Link.bbs("keyframe"), KeyframeClip.class, new ClipFactoryData(Icons.CURVES, 0xde2e9f)
                .withConverter(Link.bbs("idle"), IdleConverter.CONVERTER))
            .register(Link.bbs("translate"), TranslateClip.class, new ClipFactoryData(Icons.UPLOAD, 0x4ba03e))
            .register(Link.bbs("angle"), AngleClip.class, new ClipFactoryData(Icons.ARC, 0xd77a0a))
            .register(Link.bbs("drag"), DragClip.class, new ClipFactoryData(Icons.FADING, 0x4baff7))
            .register(Link.bbs("shake"), ShakeClip.class, new ClipFactoryData(Icons.EXCHANGE, 0x159e64))
            .register(Link.bbs("math"), MathClip.class, new ClipFactoryData(Icons.GRAPH, 0x6820ad))
            .register(Link.bbs("look"), LookClip.class, new ClipFactoryData(Icons.VISIBLE, 0x197fff))
            .register(Link.bbs("orbit"), OrbitClip.class, new ClipFactoryData(Icons.GLOBE, 0xd82253))
            .register(Link.bbs("remapper"), RemapperClip.class, new ClipFactoryData(Icons.TIME, 0x222222))
            .register(Link.bbs("audio"), AudioClip.class, new ClipFactoryData(Icons.SOUND, 0xffc825))
            .register(Link.bbs("subtitle"), SubtitleClip.class, new ClipFactoryData(Icons.FONT, 0x888899))
            .register(Link.bbs("curve"), CurveClip.class, new ClipFactoryData(Icons.ARC, 0xff1493))
            .register(Link.bbs("tracker"), TrackerClip.class, new ClipFactoryData(Icons.USER, 0xffffff))
            .register(Link.bbs("dolly_zoom"), DollyZoomClip.class, new ClipFactoryData(Icons.FILTER, 0x7d56c9));

        factoryActionClips = new MapFactory<Clip, ClipFactoryData>()
            .register(Link.bbs("chat"), ChatActionClip.class, new ClipFactoryData(Icons.BUBBLE, Colors.YELLOW))
            .register(Link.bbs("command"), CommandActionClip.class, new ClipFactoryData(Icons.PROPERTIES, Colors.ACTIVE))
            .register(Link.bbs("place_block"), PlaceBlockActionClip.class, new ClipFactoryData(Icons.BLOCK, Colors.INACTIVE))
            .register(Link.bbs("interact_block"), InteractBlockActionClip.class, new ClipFactoryData(Icons.FULLSCREEN, Colors.MAGENTA))
            .register(Link.bbs("break_block"), BreakBlockActionClip.class, new ClipFactoryData(Icons.BULLET, Colors.GREEN))
            .register(Link.bbs("use_item"), UseItemActionClip.class, new ClipFactoryData(Icons.POINTER, Colors.BLUE))
            .register(Link.bbs("use_block_item"), UseBlockItemActionClip.class, new ClipFactoryData(Icons.BUCKET, Colors.CYAN))
            .register(Link.bbs("drop_item"), ItemDropActionClip.class, new ClipFactoryData(Icons.ARROW_DOWN, Colors.DEEP_PINK))
            .register(Link.bbs("attack"), AttackActionClip.class, new ClipFactoryData(Icons.DROP, Colors.RED))
            .register(Link.bbs("damage"), DamageActionClip.class, new ClipFactoryData(Icons.SKULL, Colors.CURSOR))
            .register(Link.bbs("swipe"), SwipeActionClip.class, new ClipFactoryData(Icons.LIMB, Colors.ORANGE));

        setupConfig(Icons.PROCESSOR, "bbs", new File(settingsFolder, "bbs.json"), BBSSettings::register);

        events.post(new RegisterSettingsEvent());

        /* Networking */
        ServerNetwork.setup();

        /* Commands */
        CommandRegistrationCallback.EVENT.register(BBSCommands::register);

        /* Event listener */
        registerEvents();

        /* Entities */
        FabricDefaultAttributeRegistry.register(ACTOR_ENTITY, ActorEntity.createActorAttributes());

        /* Blocks */
        Registry.register(Registries.BLOCK, new Identifier(MOD_ID, "model"), MODEL_BLOCK);
        Registry.register(Registries.BLOCK, new Identifier(MOD_ID, "chroma_red"), CHROMA_RED_BLOCK);
        Registry.register(Registries.BLOCK, new Identifier(MOD_ID, "chroma_green"), CHROMA_GREEN_BLOCK);
        Registry.register(Registries.BLOCK, new Identifier(MOD_ID, "chroma_blue"), CHROMA_BLUE_BLOCK);
        Registry.register(Registries.BLOCK, new Identifier(MOD_ID, "chroma_cyan"), CHROMA_CYAN_BLOCK);
        Registry.register(Registries.BLOCK, new Identifier(MOD_ID, "chroma_magenta"), CHROMA_MAGENTA_BLOCK);
        Registry.register(Registries.BLOCK, new Identifier(MOD_ID, "chroma_yellow"), CHROMA_YELLOW_BLOCK);
        Registry.register(Registries.BLOCK, new Identifier(MOD_ID, "chroma_black"), CHROMA_BLACK_BLOCK);
        Registry.register(Registries.BLOCK, new Identifier(MOD_ID, "chroma_white"), CHROMA_WHITE_BLOCK);

        Registry.register(Registries.ITEM, new Identifier(MOD_ID, "model"), MODEL_BLOCK_ITEM);
        Registry.register(Registries.ITEM, new Identifier(MOD_ID, "gun"), GUN_ITEM);
        Registry.register(Registries.ITEM, new Identifier(MOD_ID, "chroma_red"), CHROMA_RED_BLOCK_ITEM);
        Registry.register(Registries.ITEM, new Identifier(MOD_ID, "chroma_green"), CHROMA_GREEN_BLOCK_ITEM);
        Registry.register(Registries.ITEM, new Identifier(MOD_ID, "chroma_blue"), CHROMA_BLUE_BLOCK_ITEM);
        Registry.register(Registries.ITEM, new Identifier(MOD_ID, "chroma_cyan"), CHROMA_CYAN_BLOCK_ITEM);
        Registry.register(Registries.ITEM, new Identifier(MOD_ID, "chroma_magenta"), CHROMA_MAGENTA_BLOCK_ITEM);
        Registry.register(Registries.ITEM, new Identifier(MOD_ID, "chroma_yellow"), CHROMA_YELLOW_BLOCK_ITEM);
        Registry.register(Registries.ITEM, new Identifier(MOD_ID, "chroma_black"), CHROMA_BLACK_BLOCK_ITEM);
        Registry.register(Registries.ITEM, new Identifier(MOD_ID, "chroma_white"), CHROMA_WHITE_BLOCK_ITEM);

        Registry.register(Registries.ITEM_GROUP, new Identifier(MOD_ID, "main"), ITEM_GROUP);
    }

    private void registerEvents()
    {
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) ->
        {
            if (entity instanceof ServerPlayerEntity player)
            {
                Morph morph = Morph.getMorph(player);

                ServerNetwork.sendMorphToTracked(player, morph.getForm());
            }
        });

        ServerLifecycleEvents.SERVER_STARTED.register((event) -> worldFolder = event.getSavePath(WorldSavePath.ROOT).toFile());
        ServerPlayConnectionEvents.JOIN.register((a, b, c) -> ServerNetwork.sendHandshake(c, b));

        ActionHandler.registerHandlers(actions);

        ServerTickEvents.START_SERVER_TICK.register((server) ->
        {
            actions.tick();
        });

        ServerTickEvents.END_SERVER_TICK.register((server) ->
        {
            for (Runnable runnable : runnables)
            {
                runnable.run();
            }

            runnables.clear();
        });

        ServerLifecycleEvents.SERVER_STOPPED.register((server) ->
        {
            actions.reset();
            ServerNetwork.reset();
        });

        EntityTrackingEvents.START_TRACKING.register((trackedEntity, player) ->
        {
            runnables.add(() ->
            {
                if (trackedEntity instanceof ServerPlayerEntity playerTwo)
                {
                    Morph morph = Morph.getMorph(trackedEntity);

                    if (morph != null)
                    {
                        ServerNetwork.sendMorph(player, playerTwo.getId(), morph.getForm());
                    }
                }
            });
        });
    }

    public static Settings setupConfig(Icon icon, String id, File destination, Consumer<SettingsBuilder> registerer)
    {
        SettingsBuilder builder = new SettingsBuilder(icon, id, destination);
        Settings settings = builder.getConfig();

        registerer.accept(builder);

        BBSMod.settings.modules.put(settings.getId(), settings);
        BBSMod.settings.load(settings, settings.file);

        return settings;
    }
}