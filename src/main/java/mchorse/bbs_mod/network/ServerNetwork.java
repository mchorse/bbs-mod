package mchorse.bbs_mod.network;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.actions.ActionManager;
import mchorse.bbs_mod.actions.ActionPlayer;
import mchorse.bbs_mod.actions.ActionRecorder;
import mchorse.bbs_mod.actions.ActionState;
import mchorse.bbs_mod.actions.types.FormTriggerActionClip;
import mchorse.bbs_mod.blocks.entities.ModelBlockEntity;
import mchorse.bbs_mod.data.DataStorageUtils;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.ByteType;
import mchorse.bbs_mod.data.types.ListType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.entity.GunProjectileEntity;
import mchorse.bbs_mod.entity.IEntityFormProvider;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.FilmManager;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.items.GunProperties;
import mchorse.bbs_mod.morphing.Morph;
import mchorse.bbs_mod.resources.ISourcePack;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.resources.packs.ExternalAssetsSourcePack;
import mchorse.bbs_mod.utils.DataPath;
import mchorse.bbs_mod.utils.EnumUtils;
import mchorse.bbs_mod.utils.Pair;
import mchorse.bbs_mod.utils.PermissionUtils;
import mchorse.bbs_mod.utils.StringUtils;
import mchorse.bbs_mod.utils.clips.Clips;
import mchorse.bbs_mod.utils.repos.RepositoryOperation;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ServerNetwork
{
    public static final List<String> EXTENSIONS = Arrays.asList("wav", "json", "vox", "png", "ogg");

    public static final Identifier CLIENT_CLICKED_MODEL_BLOCK_PACKET = new Identifier(BBSMod.MOD_ID, "c1");
    public static final Identifier CLIENT_PLAYER_FORM_PACKET = new Identifier(BBSMod.MOD_ID, "c2");
    public static final Identifier CLIENT_PLAY_FILM_PACKET = new Identifier(BBSMod.MOD_ID, "c3");
    public static final Identifier CLIENT_MANAGER_DATA_PACKET = new Identifier(BBSMod.MOD_ID, "c4");
    public static final Identifier CLIENT_STOP_FILM_PACKET = new Identifier(BBSMod.MOD_ID, "c5");
    public static final Identifier CLIENT_HANDSHAKE = new Identifier(BBSMod.MOD_ID, "c6");
    public static final Identifier CLIENT_RECORDED_ACTIONS = new Identifier(BBSMod.MOD_ID, "c7");
    public static final Identifier CLIENT_FORM_TRIGGER = new Identifier(BBSMod.MOD_ID, "c8");
    public static final Identifier CLIENT_ASSET = new Identifier(BBSMod.MOD_ID, "c9");
    public static final Identifier CLIENT_REQUEST_ASSET = new Identifier(BBSMod.MOD_ID, "c10");
    public static final Identifier CLIENT_CHEATS_PERMISSION = new Identifier(BBSMod.MOD_ID, "c11");
    public static final Identifier CLIENT_SHARED_FORM = new Identifier(BBSMod.MOD_ID, "c12");
    public static final Identifier CLIENT_ENTITY_FORM = new Identifier(BBSMod.MOD_ID, "c13");
    public static final Identifier CLIENT_ACTORS = new Identifier(BBSMod.MOD_ID, "c14");
    public static final Identifier CLIENT_GUN_PROPERTIES = new Identifier(BBSMod.MOD_ID, "c15");
    public static final Identifier CLIENT_PAUSE_FILM = new Identifier(BBSMod.MOD_ID, "c16");

    public static final Identifier SERVER_MODEL_BLOCK_FORM_PACKET = new Identifier(BBSMod.MOD_ID, "s1");
    public static final Identifier SERVER_MODEL_BLOCK_TRANSFORMS_PACKET = new Identifier(BBSMod.MOD_ID, "s2");
    public static final Identifier SERVER_PLAYER_FORM_PACKET = new Identifier(BBSMod.MOD_ID, "s3");
    public static final Identifier SERVER_MANAGER_DATA_PACKET = new Identifier(BBSMod.MOD_ID, "s4");
    public static final Identifier SERVER_ACTION_RECORDING = new Identifier(BBSMod.MOD_ID, "s5");
    public static final Identifier SERVER_TOGGLE_FILM = new Identifier(BBSMod.MOD_ID, "s6");
    public static final Identifier SERVER_ACTION_CONTROL = new Identifier(BBSMod.MOD_ID, "s7");
    public static final Identifier SERVER_FILM_DATA_SYNC = new Identifier(BBSMod.MOD_ID, "s8");
    public static final Identifier SERVER_PLAYER_TP = new Identifier(BBSMod.MOD_ID, "s9");
    public static final Identifier SERVER_FORM_TRIGGER = new Identifier(BBSMod.MOD_ID, "s10");
    public static final Identifier SERVER_REQUEST_ASSET = new Identifier(BBSMod.MOD_ID, "s11");
    public static final Identifier SERVER_ASSET = new Identifier(BBSMod.MOD_ID, "s12");
    public static final Identifier SERVER_SHARED_FORM = new Identifier(BBSMod.MOD_ID, "s13");
    public static final Identifier SERVER_ZOOM = new Identifier(BBSMod.MOD_ID, "s14");
    public static final Identifier SERVER_PAUSE_FILM = new Identifier(BBSMod.MOD_ID, "s15");

    private static ServerPacketCrusher crusher = new ServerPacketCrusher();

    public static void reset()
    {
        crusher.reset();
    }

    public static void setup()
    {
        ServerPlayNetworking.registerGlobalReceiver(SERVER_MODEL_BLOCK_FORM_PACKET, (server, player, handler, buf, responder) -> handleModelBlockFormPacket(server, player, buf));
        ServerPlayNetworking.registerGlobalReceiver(SERVER_MODEL_BLOCK_TRANSFORMS_PACKET, (server, player, handler, buf, responder) -> handleModelBlockTransformsPacket(server, player, buf));
        ServerPlayNetworking.registerGlobalReceiver(SERVER_PLAYER_FORM_PACKET, (server, player, handler, buf, responder) -> handlePlayerFormPacket(server, player, buf));
        ServerPlayNetworking.registerGlobalReceiver(SERVER_MANAGER_DATA_PACKET, (server, player, handler, buf, responder) -> handleManagerDataPacket(server, player, buf));
        ServerPlayNetworking.registerGlobalReceiver(SERVER_ACTION_RECORDING, (server, player, handler, buf, responder) -> handleActionRecording(server, player, buf));
        ServerPlayNetworking.registerGlobalReceiver(SERVER_TOGGLE_FILM, (server, player, handler, buf, responder) -> handleToggleFilm(server, player, buf));
        ServerPlayNetworking.registerGlobalReceiver(SERVER_ACTION_CONTROL, (server, player, handler, buf, responder) -> handleActionControl(server, player, buf));
        ServerPlayNetworking.registerGlobalReceiver(SERVER_FILM_DATA_SYNC, (server, player, handler, buf, responder) -> handleSyncData(server, player, buf));
        ServerPlayNetworking.registerGlobalReceiver(SERVER_PLAYER_TP, (server, player, handler, buf, responder) -> handleTeleportPlayer(server, player, buf));
        ServerPlayNetworking.registerGlobalReceiver(SERVER_FORM_TRIGGER, (server, player, handler, buf, responder) -> handleFormTrigger(server, player, buf));
        ServerPlayNetworking.registerGlobalReceiver(SERVER_REQUEST_ASSET, (server, player, handler, buf, responder) -> handleRequestAssets(server, player, buf));
        ServerPlayNetworking.registerGlobalReceiver(SERVER_ASSET, (server, player, handler, buf, responder) -> handleAssetPacket(server, player, buf));
        ServerPlayNetworking.registerGlobalReceiver(SERVER_SHARED_FORM, (server, player, handler, buf, responder) -> handleSharedFormPacket(server, player, buf));
        ServerPlayNetworking.registerGlobalReceiver(SERVER_ZOOM, (server, player, handler, buf, responder) -> handleZoomPacket(server, player, buf));
        ServerPlayNetworking.registerGlobalReceiver(SERVER_PAUSE_FILM, (server, player, handler, buf, responder) -> handlePauseFilmPacket(server, player, buf));
    }

    /* Handlers */

    private static void handleModelBlockFormPacket(MinecraftServer server, ServerPlayerEntity player, PacketByteBuf buf)
    {
        if (!PermissionUtils.arePanelsAllowed(server, player))
        {
            return;
        }

        crusher.receive(buf, (bytes, packetByteBuf) ->
        {
            BlockPos pos = buf.readBlockPos();

            try
            {
                MapType data = (MapType) DataStorageUtils.readFromBytes(bytes);

                server.execute(() ->
                {
                    World world = player.getWorld();
                    BlockEntity be = world.getBlockEntity(pos);

                    if (be instanceof ModelBlockEntity modelBlock)
                    {
                        modelBlock.updateForm(data, world);
                    }
                });
            }
            catch (Exception e)
            {}
        });
    }

    private static void handleModelBlockTransformsPacket(MinecraftServer server, ServerPlayerEntity player, PacketByteBuf buf)
    {
        if (!PermissionUtils.arePanelsAllowed(server, player))
        {
            return;
        }

        crusher.receive(buf, (bytes, packetByteBuf) ->
        {
            try
            {
                MapType data = (MapType) DataStorageUtils.readFromBytes(bytes);

                server.execute(() ->
                {
                    ItemStack stack = player.getEquippedStack(EquipmentSlot.MAINHAND).copy();

                    if (stack.getItem() == BBSMod.MODEL_BLOCK_ITEM) stack.getNbt().getCompound("BlockEntityTag").put("Properties", DataStorageUtils.toNbt(data));
                    else if (stack.getItem() == BBSMod.GUN_ITEM) stack.getOrCreateNbt().put("GunData", DataStorageUtils.toNbt(data));

                    player.equipStack(EquipmentSlot.MAINHAND, stack);
                });
            }
            catch (Exception e)
            {}
        });
    }

    private static void handlePlayerFormPacket(MinecraftServer server, ServerPlayerEntity player, PacketByteBuf buf)
    {
        if (!PermissionUtils.arePanelsAllowed(server, player))
        {
            return;
        }

        crusher.receive(buf, (bytes, packetByteBuf) ->
        {
            Form form = null;

            try
            {
                if (DataStorageUtils.readFromBytes(bytes) instanceof MapType data)
                {
                    form = BBSMod.getForms().fromData(data);
                }
            }
            catch (Exception e)
            {}

            final Form finalForm = form;

            server.execute(() ->
            {
                Morph.getMorph(player).setForm(FormUtils.copy(finalForm));

                sendMorphToTracked(player, finalForm);
            });
        });
    }

    private static void handleManagerDataPacket(MinecraftServer server, ServerPlayerEntity player, PacketByteBuf buf)
    {
        if (!PermissionUtils.arePanelsAllowed(server, player))
        {
            return;
        }

        crusher.receive(buf, (bytes, packetByteBuf) ->
        {
            MapType data = (MapType) DataStorageUtils.readFromBytes(bytes);
            int callbackId = packetByteBuf.readInt();
            RepositoryOperation op = RepositoryOperation.values()[packetByteBuf.readInt()];
            FilmManager films = BBSMod.getFilms();

            if (op == RepositoryOperation.LOAD)
            {
                String id = data.getString("id");
                Film film = films.load(id);

                sendManagerData(player, callbackId, op, film.toData());
            }
            else if (op == RepositoryOperation.SAVE)
            {
                films.save(data.getString("id"), data.getMap("data"));
            }
            else if (op == RepositoryOperation.RENAME)
            {
                films.rename(data.getString("from"), data.getString("to"));
            }
            else if (op == RepositoryOperation.DELETE)
            {
                films.delete(data.getString("id"));
            }
            else if (op == RepositoryOperation.KEYS)
            {
                ListType list = DataStorageUtils.stringListToData(films.getKeys());

                sendManagerData(player, callbackId, op, list);
            }
            else if (op == RepositoryOperation.ADD_FOLDER)
            {
                sendManagerData(player, callbackId, op, new ByteType(films.addFolder(data.getString("folder"))));
            }
            else if (op == RepositoryOperation.RENAME_FOLDER)
            {
                sendManagerData(player, callbackId, op, new ByteType(films.renameFolder(data.getString("from"), data.getString("to"))));
            }
            else if (op == RepositoryOperation.DELETE_FOLDER)
            {
                sendManagerData(player, callbackId, op, new ByteType(films.deleteFolder(data.getString("folder"))));
            }
        });
    }

    private static void handleActionRecording(MinecraftServer server, ServerPlayerEntity player, PacketByteBuf buf)
    {
        if (!PermissionUtils.arePanelsAllowed(server, player))
        {
            return;
        }

        String filmId = buf.readString();
        int replayId = buf.readInt();
        int tick = buf.readInt();
        int countdown = buf.readInt();
        boolean recording = buf.readBoolean();

        server.execute(() ->
        {
            if (recording)
            {
                Film film = BBSMod.getFilms().load(filmId);

                if (film != null)
                {
                    BBSMod.getActions().startRecording(film, player, 0, countdown, replayId);
                }
            }
            else
            {
                ActionRecorder recorder = BBSMod.getActions().stopRecording(player);
                Clips clips = recorder.composeClips();

                /* Send recorded clips to the client */
                sendRecordedActions(player, filmId, replayId, tick, clips);
            }
        });
    }

    private static void handleToggleFilm(MinecraftServer server, ServerPlayerEntity player, PacketByteBuf buf)
    {
        if (!PermissionUtils.arePanelsAllowed(server, player))
        {
            return;
        }

        String filmId = buf.readString();
        boolean withCamera = buf.readBoolean();

        server.execute(() ->
        {
            ActionPlayer actionPlayer = BBSMod.getActions().getPlayer(filmId);

            if (actionPlayer != null)
            {
                BBSMod.getActions().stop(filmId);

                for (ServerPlayerEntity otherPlayer : server.getPlayerManager().getPlayerList())
                {
                    sendStopFilm(otherPlayer, filmId);
                }
            }
            else
            {
                sendPlayFilm(player, player.getServerWorld(), filmId, withCamera);
            }
        });
    }

    private static void handleActionControl(MinecraftServer server, ServerPlayerEntity player, PacketByteBuf buf)
    {
        if (!PermissionUtils.arePanelsAllowed(server, player))
        {
            return;
        }

        ActionManager actions = BBSMod.getActions();
        String filmId = buf.readString();
        ActionState state = EnumUtils.getValue(buf.readByte(), ActionState.values(), ActionState.STOP);
        int tick = buf.readInt();

        server.execute(() ->
        {
            if (state == ActionState.SEEK)
            {
                ActionPlayer actionPlayer = actions.getPlayer(filmId);

                if (actionPlayer != null)
                {
                    actionPlayer.goTo(tick);
                }
            }
            else if (state == ActionState.PLAY)
            {
                ActionPlayer actionPlayer = actions.getPlayer(filmId);

                if (actionPlayer != null)
                {
                    actionPlayer.goTo(tick);
                    actionPlayer.playing = true;
                }
            }
            else if (state == ActionState.PAUSE)
            {
                ActionPlayer actionPlayer = actions.getPlayer(filmId);

                if (actionPlayer != null)
                {
                    actionPlayer.goTo(tick);
                    actionPlayer.playing = false;
                }
            }
            else if (state == ActionState.RESTART)
            {
                ActionPlayer actionPlayer = actions.getPlayer(filmId);

                if (actionPlayer == null)
                {
                    Film film = BBSMod.getFilms().load(filmId);

                    if (film != null)
                    {
                        actionPlayer = actions.play(player, player.getServerWorld(), film, tick);
                    }
                }
                else
                {
                    actions.stop(filmId);

                    actionPlayer = actions.play(player, player.getServerWorld(), actionPlayer.film, tick);
                }

                if (actionPlayer != null)
                {
                    actionPlayer.syncing = true;
                    actionPlayer.playing = false;

                    if (tick != 0)
                    {
                        actionPlayer.goTo(0, tick);
                    }
                }

                sendStopFilm(player, filmId);
            }
            else if (state == ActionState.STOP)
            {
                actions.stop(filmId);
            }
        });
    }

    private static void handleSyncData(MinecraftServer server, ServerPlayerEntity player, PacketByteBuf buf)
    {
        if (!PermissionUtils.arePanelsAllowed(server, player))
        {
            return;
        }

        crusher.receive(buf, (bytes, packetByteBuf) ->
        {
            String filmId = packetByteBuf.readString();
            List<String> path = new ArrayList<>();

            for (int i = 0, c = buf.readInt(); i < c; i++)
            {
                path.add(buf.readString());
            }

            BaseType data = DataStorageUtils.readFromBytes(bytes);

            server.execute(() ->
            {
                BBSMod.getActions().syncData(filmId, new DataPath(path), data);
            });
        });
    }

    private static void handleTeleportPlayer(MinecraftServer server, ServerPlayerEntity player, PacketByteBuf buf)
    {
        if (!PermissionUtils.arePanelsAllowed(server, player))
        {
            return;
        }

        double x = buf.readDouble();
        double y = buf.readDouble();
        double z = buf.readDouble();
        float yaw = buf.readFloat();
        float bodyYaw = buf.readFloat();
        float pitch = buf.readFloat();

        server.execute(() ->
        {
            player.requestTeleport(x, y, z);

            player.setYaw(yaw);
            player.setHeadYaw(yaw);
            player.setBodyYaw(bodyYaw);
            player.setPitch(pitch);
        });
    }

    private static void handleFormTrigger(MinecraftServer server, ServerPlayerEntity player, PacketByteBuf buf)
    {
        String string = buf.readString();
        PacketByteBuf newBuf = PacketByteBufs.create();

        newBuf.writeInt(player.getId());
        newBuf.writeString(string);

        for (ServerPlayerEntity otherPlayer : PlayerLookup.tracking(player))
        {
            ServerPlayNetworking.send(otherPlayer, CLIENT_FORM_TRIGGER, newBuf);
        }

        server.execute(() ->
        {
            BBSMod.getActions().addAction(player, () ->
            {
                FormTriggerActionClip action = new FormTriggerActionClip();

                action.trigger.set(string);

                return action;
            });
        });
    }

    private static void handleRequestAssets(MinecraftServer server, ServerPlayerEntity player, PacketByteBuf buf)
    {
        String path = buf.readString();
        Link link = Link.assets(path);
        long index = buf.readLong();

        sendAsset(player, link, index);
    }

    private static void handleAssetPacket(MinecraftServer server, ServerPlayerEntity player, PacketByteBuf buf)
    {
        if (!BBSSettings.serverAssetManager.get().equals(player.getUuidAsString()))
        {
            player.sendMessage(Text.literal("You don't have permission to upload files"), true);

            return;
        }

        String path = buf.readString();
        long offset = buf.readLong();

        /* If index is -1, we gotta delete the file */
        if (offset < 0)
        {
            ISourcePack sourcePack = BBSMod.getDynamicSourcePack().getSourcePack();

            if (sourcePack instanceof ExternalAssetsSourcePack pack)
            {
                File file = new File(pack.getFolder(), path);

                if (file.exists())
                {
                    file.delete();
                }
            }

            return;
        }

        int size = buf.readInt();
        boolean last = buf.readBoolean();
        byte[] bytes = new byte[size];

        buf.readBytes(bytes);

        ISourcePack sourcePack = BBSMod.getDynamicSourcePack().getSourcePack();

        if (sourcePack instanceof ExternalAssetsSourcePack pack)
        {
            File file = new File(pack.getFolder(), path);

            file.getParentFile().mkdirs();

            try (OutputStream stream = new FileOutputStream(file, offset != 0))
            {
                stream.write(bytes);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            if (!last)
            {
                sendRequestAsset(player, path, offset);
            }
            else
            {
                System.out.println("[Server] Received completely: " + path);
            }

            BBSMod.getResourceTracker().timer.mark();
        }
    }

    private static void handleSharedFormPacket(MinecraftServer server, ServerPlayerEntity player, PacketByteBuf buf)
    {
        crusher.receive(buf, (bytes, packetByteBuf) ->
        {
            UUID playerUuid = packetByteBuf.readUuid();
            MapType data = (MapType) DataStorageUtils.readFromBytes(bytes);

            server.execute(() ->
            {
                ServerPlayerEntity otherPlayer = server.getPlayerManager().getPlayer(playerUuid);

                if (otherPlayer != null)
                {
                    sendSharedForm(otherPlayer, data);
                }
            });
        });
    }

    private static void handleZoomPacket(MinecraftServer server, ServerPlayerEntity player, PacketByteBuf buf)
    {
        boolean zoom = buf.readBoolean();
        ItemStack main = player.getMainHandStack();

        if (main.getItem() == BBSMod.GUN_ITEM)
        {
            GunProperties properties = GunProperties.get(main);
            String command = zoom ? properties.cmdZoomOn : properties.cmdZoomOff;

            if (!command.isEmpty())
            {
                server.getCommandManager().executeWithPrefix(player.getCommandSource(), command);
            }
        }
    }

    private static void handlePauseFilmPacket(MinecraftServer server, ServerPlayerEntity player, PacketByteBuf buf)
    {
        String filmId = buf.readString();

        ActionPlayer actionPlayer = BBSMod.getActions().getPlayer(filmId);

        if (actionPlayer != null)
        {
            actionPlayer.toggle();
        }

        for (ServerPlayerEntity playerEntity : server.getPlayerManager().getPlayerList())
        {
            sendPauseFilm(playerEntity, filmId);
        }
    }

    /* API */

    public static void sendMorph(ServerPlayerEntity player, int playerId, Form form)
    {
        crusher.send(player, CLIENT_PLAYER_FORM_PACKET, FormUtils.toData(form), (packetByteBuf) ->
        {
            packetByteBuf.writeInt(playerId);
        });
    }

    public static void sendMorphToTracked(ServerPlayerEntity player, Form form)
    {
        sendMorph(player, player.getId(), form);

        for (ServerPlayerEntity otherPlayer : PlayerLookup.tracking(player))
        {
            sendMorph(otherPlayer, player.getId(), form);
        }
    }

    public static void sendClickedModelBlock(ServerPlayerEntity player, BlockPos pos)
    {
        PacketByteBuf buf = PacketByteBufs.create();

        buf.writeBlockPos(pos);

        ServerPlayNetworking.send(player, CLIENT_CLICKED_MODEL_BLOCK_PACKET, buf);
    }

    public static void sendPlayFilm(ServerPlayerEntity player, ServerWorld world, String filmId, boolean withCamera)
    {
        try
        {
            Film film = BBSMod.getFilms().load(filmId);

            if (film != null)
            {
                BBSMod.getActions().play(player, world, film, 0);

                BaseType data = film.toData();

                crusher.send(world.getPlayers().stream().map((p) -> (PlayerEntity) p).toList(), CLIENT_PLAY_FILM_PACKET, data, (packetByteBuf) ->
                {
                    packetByteBuf.writeString(filmId);
                    packetByteBuf.writeBoolean(withCamera);
                });
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public static void sendPlayFilm(ServerPlayerEntity player, String filmId, boolean withCamera)
    {
        try
        {
            Film film = BBSMod.getFilms().load(filmId);

            if (film != null)
            {
                BBSMod.getActions().play(player, player.getServerWorld(), film, 0);

                crusher.send(player, CLIENT_PLAY_FILM_PACKET, film.toData(), (packetByteBuf) ->
                {
                    packetByteBuf.writeString(filmId);
                    packetByteBuf.writeBoolean(withCamera);
                });
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public static void sendStopFilm(ServerPlayerEntity player, String filmId)
    {
        PacketByteBuf buf = PacketByteBufs.create();

        buf.writeString(filmId);

        ServerPlayNetworking.send(player, CLIENT_STOP_FILM_PACKET, buf);
    }

    public static void sendManagerData(ServerPlayerEntity player, int callbackId, RepositoryOperation op, BaseType data)
    {
        crusher.send(player, CLIENT_MANAGER_DATA_PACKET, data, (packetByteBuf) ->
        {
            packetByteBuf.writeInt(callbackId);
            packetByteBuf.writeInt(op.ordinal());
        });
    }

    public static void sendRecordedActions(ServerPlayerEntity player, String filmId, int replayId, int tick, Clips clips)
    {
        crusher.send(player, CLIENT_RECORDED_ACTIONS, clips.toData(), (packetByteBuf) ->
        {
            packetByteBuf.writeString(filmId);
            packetByteBuf.writeInt(replayId);
            packetByteBuf.writeInt(tick);
        });
    }

    public static void sendHandshake(MinecraftServer server, PacketSender packetSender)
    {
        packetSender.sendPacket(ServerNetwork.CLIENT_HANDSHAKE, createHandshakeBuf(server));
    }

    public static void sendHandshake(MinecraftServer server, ServerPlayerEntity player)
    {
        ServerPlayNetworking.send(player, ServerNetwork.CLIENT_HANDSHAKE, createHandshakeBuf(server));
    }

    private static PacketByteBuf createHandshakeBuf(MinecraftServer server)
    {
        PacketByteBuf buf = PacketByteBufs.create();
        String id = BBSSettings.serverId.get().trim();

        /* No need to do that in singleplayer */
        if (server.isSingleplayer())
        {
            id = "";
        }

        buf.writeString(id);

        if (!id.isEmpty())
        {
            Collection<Link> links = BBSMod.getProvider().getLinksFromPath(Link.assets(""));
            List<Pair<String, Long>> assets = new ArrayList<>();

            for (Link link : links)
            {
                String extension = StringUtils.extension(link.path);

                if (!extension.equals(link.path) && EXTENSIONS.contains(extension.toLowerCase()))
                {
                    File file = BBSMod.getProvider().getFile(link);
                    long l = file.lastModified();

                    assets.add(new Pair<>(link.path, l));
                }
            }

            buf.writeInt(assets.size());

            for (Pair<String, Long> asset : assets)
            {
                buf.writeString(asset.a);
                buf.writeLong(asset.b);
            }
        }

        return buf;
    }

    public static void sendAsset(ServerPlayerEntity player, Link link, long offset)
    {
        try
        {
            File file = BBSMod.getDynamicSourcePack().getFile(link);
            int placeholder = 1000;
            int bufferSize = 32767 - placeholder;
            PacketByteBuf buf = PacketByteBufs.create();
            byte[] bytes = new byte[bufferSize];
            int read;

            if (file != null)
            {
                RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r");

                randomAccessFile.seek(offset);
                read = randomAccessFile.read(bytes);
            }
            else
            {
                InputStream stream = BBSMod.getDynamicSourcePack().getAsset(link);

                stream.skip(offset);
                read = stream.read(bytes);
            }

            buf.writeString(link.path);
            buf.writeLong(offset + read);
            buf.writeInt(read);
            buf.writeBoolean(read != bytes.length);
            buf.writeBytes(bytes, 0, read);

            ServerPlayNetworking.send(player, CLIENT_ASSET, buf);
        }
        catch (IOException e)
        {
            System.err.println("Failed to read asset: " + link);
        }
    }

    public static void sendRequestAsset(ServerPlayerEntity player, String asset, long offset)
    {
        PacketByteBuf buf = PacketByteBufs.create();

        buf.writeString(asset);
        buf.writeLong(offset);

        ServerPlayNetworking.send(player, ServerNetwork.CLIENT_REQUEST_ASSET, buf);
    }

    public static void sendCheatsPermission(ServerPlayerEntity player, boolean cheats)
    {
        PacketByteBuf buf = PacketByteBufs.create();

        buf.writeBoolean(cheats);

        ServerPlayNetworking.send(player, ServerNetwork.CLIENT_CHEATS_PERMISSION, buf);
    }

    public static void sendSharedForm(ServerPlayerEntity player, MapType data)
    {
        crusher.send(player, CLIENT_SHARED_FORM, data, (packetByteBuf) ->
        {});
    }

    public static void sendEntityForm(ServerPlayerEntity player, IEntityFormProvider actor)
    {
        crusher.send(player, CLIENT_ENTITY_FORM, FormUtils.toData(actor.getForm()), (packetByteBuf) ->
        {
            packetByteBuf.writeInt(actor.getEntityId());
        });
    }

    public static void sendActors(ServerPlayerEntity player, String filmId, Map<String, LivingEntity> actors)
    {
        PacketByteBuf buf = PacketByteBufs.create();

        buf.writeString(filmId);
        buf.writeInt(actors.size());

        for (Map.Entry<String, LivingEntity> entry : actors.entrySet())
        {
            buf.writeString(entry.getKey());
            buf.writeInt(entry.getValue().getId());
        }

        ServerPlayNetworking.send(player, CLIENT_ACTORS, buf);
    }

    public static void sendGunProperties(ServerPlayerEntity player, GunProjectileEntity projectile)
    {
        PacketByteBuf buf = PacketByteBufs.create();
        GunProperties properties = projectile.getProperties();

        buf.writeInt(projectile.getEntityId());
        properties.toNetwork(buf);

        ServerPlayNetworking.send(player, CLIENT_GUN_PROPERTIES, buf);
    }

    public static void sendPauseFilm(ServerPlayerEntity player, String filmId)
    {
        PacketByteBuf buf = PacketByteBufs.create();

        buf.writeString(filmId);

        ServerPlayNetworking.send(player, CLIENT_PAUSE_FILM, buf);
    }
}