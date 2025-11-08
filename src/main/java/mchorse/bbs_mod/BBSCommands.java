package mchorse.bbs_mod;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import mchorse.bbs_mod.blocks.entities.ModelBlockEntity;
import mchorse.bbs_mod.data.DataToString;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.states.AnimationState;
import mchorse.bbs_mod.mixin.LevelPropertiesAccessor;
import mchorse.bbs_mod.morphing.Morph;
import mchorse.bbs_mod.network.ServerNetwork;
import mchorse.bbs_mod.settings.Settings;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.settings.values.core.ValueGroup;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.EntitySelector;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.PosArgument;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.SaveProperties;
import net.minecraft.world.World;
import net.minecraft.world.level.LevelInfo;

import java.util.Collection;
import java.util.function.Predicate;

public class BBSCommands
{
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment)
    {
        Predicate<ServerCommandSource> hasPermissions = (source) -> source.hasPermissionLevel(2);
        LiteralArgumentBuilder<ServerCommandSource> bbs = CommandManager.literal("bbs").requires((source) -> true);

        registerMorphCommand(bbs, environment, hasPermissions);
        registerModelBlockCommand(bbs, environment, hasPermissions);
        registerMorphEntityCommand(bbs, environment, hasPermissions);
        registerFilmsCommand(bbs, environment, hasPermissions);
        registerDCCommand(bbs, environment, hasPermissions);
        registerOnHeadCommand(bbs, environment, hasPermissions);
        registerConfigCommand(bbs, environment, hasPermissions);
        registerCheatsCommand(bbs, environment);
        registerBoomCommand(bbs, environment, hasPermissions);
        registerStructureSaveCommand(bbs, environment, hasPermissions);

        dispatcher.register(bbs);
    }

    private static void registerStructureSaveCommand(LiteralArgumentBuilder<ServerCommandSource> bbs, CommandManager.RegistrationEnvironment environment, Predicate<ServerCommandSource> hasPermissions)
    {
        LiteralArgumentBuilder<ServerCommandSource> structures = CommandManager.literal("structures");
        LiteralArgumentBuilder<ServerCommandSource> save = CommandManager.literal("save");
        RequiredArgumentBuilder<ServerCommandSource, String> name = CommandManager.argument("name", StringArgumentType.word());
        RequiredArgumentBuilder<ServerCommandSource, PosArgument> from = CommandManager.argument("from", BlockPosArgumentType.blockPos());
        RequiredArgumentBuilder<ServerCommandSource, PosArgument> to = CommandManager.argument("to", BlockPosArgumentType.blockPos());

        bbs.then(structures
            .then(save.then(name.then(from.then(to
                .executes(BBSCommands::saveStructure))))
        ));
    }

    private static void registerMorphCommand(LiteralArgumentBuilder<ServerCommandSource> bbs, CommandManager.RegistrationEnvironment environment, Predicate<ServerCommandSource> hasPermissions)
    {
        LiteralArgumentBuilder<ServerCommandSource> morph = CommandManager.literal("morph");
        RequiredArgumentBuilder<ServerCommandSource, EntitySelector> target = CommandManager.argument("target", EntityArgumentType.players());
        RequiredArgumentBuilder<ServerCommandSource, String> form = CommandManager.argument("form", StringArgumentType.greedyString());

        morph.then(target
            .executes(BBSCommands::morphCommandDemorph)
            .then(form.executes(BBSCommands::morphCommandMorph)));

        bbs.then(morph.requires(hasPermissions));
    }

    private static void registerModelBlockCommand(LiteralArgumentBuilder<ServerCommandSource> bbs, CommandManager.RegistrationEnvironment environment, Predicate<ServerCommandSource> hasPermissions)
    {
        LiteralArgumentBuilder<ServerCommandSource> modelBlock = CommandManager.literal("model_block");
        LiteralArgumentBuilder<ServerCommandSource> playState = CommandManager.literal("play_state");
        RequiredArgumentBuilder<ServerCommandSource, PosArgument> coords = CommandManager.argument("coords", BlockPosArgumentType.blockPos());
        RequiredArgumentBuilder<ServerCommandSource, String> state = CommandManager.argument("state", StringArgumentType.string());

        LiteralArgumentBuilder<ServerCommandSource> refresh = CommandManager.literal("refresh");
        RequiredArgumentBuilder<ServerCommandSource, Integer> randomRange = CommandManager.argument("random_range", IntegerArgumentType.integer());

        state.suggests((ctx, builder) ->
        {
            BlockPos pos = BlockPosArgumentType.getBlockPos(ctx, "coords");
            BlockEntity blockEntity = ctx.getSource().getWorld().getBlockEntity(pos);

            if (blockEntity instanceof ModelBlockEntity block)
            {
                Form form = block.getProperties().getForm();

                if (form != null)
                {
                    for (AnimationState animationState : form.states.getAllTyped())
                    {
                        String customId = animationState.customId.get();

                        builder.suggest(customId.trim().isEmpty() ? animationState.id.get() : customId);
                    }
                }
            }

            return builder.buildFuture();
        });

        modelBlock.then(
            playState.then(
                coords.then(
                    state.executes((ctx) ->
                    {
                        BlockPos pos = BlockPosArgumentType.getBlockPos(ctx, "coords");
                        String animationState = StringArgumentType.getString(ctx, "state");
                        BlockEntity blockEntity = ctx.getSource().getWorld().getBlockEntity(pos);

                        if (blockEntity instanceof ModelBlockEntity)
                        {
                            for (ServerPlayerEntity player : ctx.getSource().getWorld().getPlayers())
                            {
                                if (player.getBlockPos().getSquaredDistance(pos) <= 64F)
                                {
                                    ServerNetwork.sendModelBlockState(player, pos, animationState);
                                }
                            }

                            return 1;
                        }

                        return 0;
                    })
                )
            )
        );

        modelBlock.then(
            refresh.then(
                randomRange.executes((ctx) ->
                {
                    int range = IntegerArgumentType.getInteger(ctx, "random_range");

                    for (ServerPlayerEntity player : ctx.getSource().getServer().getPlayerManager().getPlayerList())
                    {
                        ServerNetwork.sendReloadModelBlocks(player, range);
                    }

                    return 1;
                })
            )
        );

        bbs.then(modelBlock.requires(hasPermissions));
    }

    private static void registerMorphEntityCommand(LiteralArgumentBuilder<ServerCommandSource> bbs, CommandManager.RegistrationEnvironment environment, Predicate<ServerCommandSource> hasPermissions)
    {
        LiteralArgumentBuilder<ServerCommandSource> morph = CommandManager.literal("morph_entity");

        morph.executes((source) ->
        {
            Entity entity = source.getSource().getEntity();

            if (entity instanceof ServerPlayerEntity player)
            {
                Form form = Morph.getMobForm(player);

                if (form != null)
                {
                    ServerNetwork.sendMorphToTracked(player, form);
                    Morph.getMorph(entity).setForm(FormUtils.copy(form));
                }
            }

            return 1;
        });

        bbs.then(morph.requires(hasPermissions));
    }

    private static void registerFilmsCommand(LiteralArgumentBuilder<ServerCommandSource> bbs, CommandManager.RegistrationEnvironment environment, Predicate<ServerCommandSource> hasPermissions)
    {
        LiteralArgumentBuilder<ServerCommandSource> scene = CommandManager.literal("films");
        LiteralArgumentBuilder<ServerCommandSource> play = CommandManager.literal("play");
        LiteralArgumentBuilder<ServerCommandSource> stop = CommandManager.literal("stop");
        RequiredArgumentBuilder<ServerCommandSource, EntitySelector> target = CommandManager.argument("target", EntityArgumentType.players());
        RequiredArgumentBuilder<ServerCommandSource, String> playFilm = CommandManager.argument("film", StringArgumentType.string());
        RequiredArgumentBuilder<ServerCommandSource, String> stopFilm = CommandManager.argument("film", StringArgumentType.string());
        RequiredArgumentBuilder<ServerCommandSource, Boolean> camera = CommandManager.argument("camera", BoolArgumentType.bool());

        playFilm.suggests((ctx, builder) ->
        {
            for (String key : BBSMod.getFilms().getKeys())
            {
                builder.suggest(key);
            }

            return builder.buildFuture();
        });

        stopFilm.suggests((ctx, builder) ->
        {
            for (String key : BBSMod.getFilms().getKeys())
            {
                builder.suggest(key);
            }

            return builder.buildFuture();
        });

        scene.then(
            target.then(
                play.then(
                    playFilm.executes((source) -> sceneCommandPlay(source, true))
                        .then(
                            camera.executes((source) -> sceneCommandPlay(source, BoolArgumentType.getBool(source, "camera")))
                        )
                )
            )
            .then(
                stop.then(
                    stopFilm.executes(BBSCommands::sceneCommandStop)
                )
            )
        );

        bbs.then(scene.requires(hasPermissions));
    }

    private static void registerDCCommand(LiteralArgumentBuilder<ServerCommandSource> bbs, CommandManager.RegistrationEnvironment environment, Predicate<ServerCommandSource> hasPermissions)
    {
        LiteralArgumentBuilder<ServerCommandSource> dc = CommandManager.literal("dc");
        LiteralArgumentBuilder<ServerCommandSource> shutdown = CommandManager.literal("shutdown");
        LiteralArgumentBuilder<ServerCommandSource> start = CommandManager.literal("start");
        LiteralArgumentBuilder<ServerCommandSource> stop = CommandManager.literal("stop");

        bbs.then(
            dc.requires(hasPermissions).then(start.executes(BBSCommands::DCCommandStart))
                .then(stop.executes(BBSCommands::DCCommandStop))
                .then(shutdown.executes(BBSCommands::DCCommandShutdown))
        );
    }

    private static void registerOnHeadCommand(LiteralArgumentBuilder<ServerCommandSource> bbs, CommandManager.RegistrationEnvironment environment, Predicate<ServerCommandSource> hasPermissions)
    {
        LiteralArgumentBuilder<ServerCommandSource> onHead = CommandManager.literal("on_head");

        bbs.then(onHead.requires(hasPermissions).executes(BBSCommands::onHead));
    }

    private static void registerConfigCommand(LiteralArgumentBuilder<ServerCommandSource> bbs, CommandManager.RegistrationEnvironment environment, Predicate<ServerCommandSource> hasPermissions)
    {
        LiteralArgumentBuilder<ServerCommandSource> config = CommandManager.literal("config");

        config.requires((ctx) -> ctx.hasPermissionLevel(4)).then(
            CommandManager.literal("set").then(
                CommandManager.argument("option", StringArgumentType.word())
                    .suggests((ctx, builder) ->
                    {
                        Settings settings = BBSMod.getSettings().modules.get("bbs");

                        if (settings != null)
                        {
                            for (ValueGroup value : settings.categories.values())
                            {
                                for (BaseValue baseValue : value.getAll())
                                {
                                    builder.suggest(value.getId() + "." + baseValue.getId());
                                }
                            }
                        }

                        return builder.buildFuture();
                    })
                    .then(
                        CommandManager.argument("value", StringArgumentType.greedyString()).executes((ctx) ->
                        {
                            Settings settings = BBSMod.getSettings().modules.get("bbs");

                            if (settings != null)
                            {
                                String option = StringArgumentType.getString(ctx, "option");
                                String value = StringArgumentType.getString(ctx, "value");
                                BaseType valueType = DataToString.fromString(value);
                                String[] split = option.split("\\.");

                                if (valueType != null && split.length >= 2)
                                {
                                    BaseValue baseValue = settings.get(split[0], split[1]);

                                    if (baseValue != null)
                                    {
                                        baseValue.fromData(valueType);
                                        settings.saveLater();
                                    }
                                }
                            }

                            return 1;
                        })
                    )
            )
        );

        bbs.then(config.requires(hasPermissions));
    }

    private static void registerCheatsCommand(LiteralArgumentBuilder<ServerCommandSource> bbs, CommandManager.RegistrationEnvironment environment)
    {
        if (environment.dedicated)
        {
            return;
        }

        bbs.then(
            CommandManager.literal("cheats").then(
                CommandManager.argument("enabled", BoolArgumentType.bool()).executes((ctx) ->
                {
                    MinecraftServer server = ctx.getSource().getServer();
                    boolean enabled = BoolArgumentType.getBool(ctx, "enabled");
                    SaveProperties saveProperties = server.getSaveProperties();

                    if (saveProperties instanceof LevelPropertiesAccessor accessor)
                    {
                        LevelInfo levelInfo = saveProperties.getLevelInfo();

                        accessor.bbs$setLevelInfo(new LevelInfo(levelInfo.getLevelName(),
                            levelInfo.getGameMode(),
                            levelInfo.isHardcore(),
                            levelInfo.getDifficulty(),
                            enabled,
                            levelInfo.getGameRules(),
                            levelInfo.getDataConfiguration()
                        ));

                        for (ServerPlayerEntity serverPlayerEntity : server.getPlayerManager().getPlayerList())
                        {
                            server.getCommandManager().sendCommandTree(serverPlayerEntity);
                            ServerNetwork.sendCheatsPermission(serverPlayerEntity, enabled);
                        }
                    }

                    return 1;
                })
            )
        );
    }

    private static void registerBoomCommand(LiteralArgumentBuilder<ServerCommandSource> bbs, CommandManager.RegistrationEnvironment environment, Predicate<ServerCommandSource> hasPermissions)
    {
        bbs.then(
            CommandManager.literal("boom").requires(hasPermissions).then(
                CommandManager.argument("pos", Vec3ArgumentType.vec3()).then(
                    CommandManager.argument("radius", FloatArgumentType.floatArg(1)).then(
                        CommandManager.argument("fire", BoolArgumentType.bool()).executes((ctx) ->
                        {
                            ServerCommandSource source = ctx.getSource();
                            Vec3d pos = Vec3ArgumentType.getVec3(ctx, "pos");
                            float radius = FloatArgumentType.getFloat(ctx, "radius");
                            boolean fire = BoolArgumentType.getBool(ctx, "fire");

                            source.getWorld().createExplosion(null, pos.x, pos.y, pos.z, radius, fire, World.ExplosionSourceType.BLOCK);

                            return 1;
                        })
                    )
                )
            )
        );
    }

    /**
     * /bbs morph McHorseYT - demorph (remove morph) player McHorseYT
     */
    private static int morphCommandDemorph(CommandContext<ServerCommandSource> source) throws CommandSyntaxException
    {
        ServerPlayerEntity entity = EntityArgumentType.getPlayer(source, "target");

        ServerNetwork.sendMorphToTracked(entity, null);
        Morph.getMorph(entity).setForm(null);

        return 1;
    }

    /**
     * /bbs morph McHorse {id:"bbs:model",model:"butterfly",texture:"assets:models/butterfly/yellow.png"}
     *
     * Morphs player McHorseYT into a butterfly model with yellow skin
     */
    private static int morphCommandMorph(CommandContext<ServerCommandSource> source) throws CommandSyntaxException
    {
        Collection<ServerPlayerEntity> players = EntityArgumentType.getPlayers(source, "target");
        String formData = StringArgumentType.getString(source, "form");

        try
        {
            Form form = FormUtils.fromData(DataToString.mapFromString(formData));

            for (ServerPlayerEntity player : players)
            {
                ServerNetwork.sendMorphToTracked(player, form);
                Morph.getMorph(player).setForm(FormUtils.copy(form));
            }

            return 1;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return -1;
    }

    /**
     * /bbs film McHorseYT play test - Plays a film (with camera) to McHorseYT
     * /bbs film @a play test false - Plays a film (without camera) to all players
     */
    private static int sceneCommandPlay(CommandContext<ServerCommandSource> source, boolean withCamera) throws CommandSyntaxException
    {
        Collection<ServerPlayerEntity> players = EntityArgumentType.getPlayers(source, "target");
        String filmId = StringArgumentType.getString(source, "film");

        for (ServerPlayerEntity player : players)
        {
            ServerNetwork.sendPlayFilm(player, filmId, withCamera);
        }

        return 1;
    }

    /**
     * /bbs film McHorseYT stop test - Stops film playback
     */
    private static int sceneCommandStop(CommandContext<ServerCommandSource> source) throws CommandSyntaxException
    {
        Collection<ServerPlayerEntity> players = EntityArgumentType.getPlayers(source, "target");
        String filmId = StringArgumentType.getString(source, "film");

        for (ServerPlayerEntity player : players)
        {
            ServerNetwork.sendStopFilm(player, filmId);
        }

        return 1;
    }

    private static int DCCommandShutdown(CommandContext<ServerCommandSource> source)
    {
        BBSMod.getActions().resetDamage(source.getSource().getWorld());

        return 1;
    }

    private static int DCCommandStart(CommandContext<ServerCommandSource> source)
    {
        BBSMod.getActions().trackDamage(source.getSource().getWorld());

        return 1;
    }

    private static int DCCommandStop(CommandContext<ServerCommandSource> source)
    {
        BBSMod.getActions().stopDamage(source.getSource().getWorld());

        return 1;
    }

    private static int onHead(CommandContext<ServerCommandSource> source)
    {
        if (source.getSource().getEntity() instanceof LivingEntity livingEntity)
        {
            ItemStack stack = livingEntity.getEquippedStack(EquipmentSlot.MAINHAND);

            if (!stack.isEmpty())
            {
                livingEntity.equipStack(EquipmentSlot.HEAD, stack.copy());
            }
        }

        return 1;
    }

    private static int saveStructure(CommandContext<ServerCommandSource> source)
    {
        String name = StringArgumentType.getString(source, "name");
        BlockPos from = BlockPosArgumentType.getBlockPos(source, "from");
        BlockPos to = BlockPosArgumentType.getBlockPos(source, "to");

        ServerWorld world = source.getSource().getWorld();
        StructureTemplateManager structureTemplateManager = world.getStructureTemplateManager();
        StructureTemplate structureTemplate;

        try
        {
            structureTemplate = structureTemplateManager.getTemplateOrBlank(new Identifier(name));
        }
        catch (InvalidIdentifierException e)
        {
            return 0;
        }

        BlockPos min = new BlockPos(Math.min(from.getX(), to.getX()), Math.min(from.getY(), to.getY()), Math.min(from.getZ(), to.getZ()));
        BlockPos max = new BlockPos(Math.max(from.getX(), to.getX()), Math.max(from.getY(), to.getY()), Math.max(from.getZ(), to.getZ()));
        BlockPos size = max.subtract(min).add(1, 1, 1);

        structureTemplate.saveFromWorld(world, min, size, true, Blocks.STRUCTURE_VOID);

        try
        {
            if (structureTemplateManager.saveTemplate(new Identifier(name)))
            {
                return 1;
            }
        }
        catch (InvalidIdentifierException var7)
        {}

        return 0;
    }
}