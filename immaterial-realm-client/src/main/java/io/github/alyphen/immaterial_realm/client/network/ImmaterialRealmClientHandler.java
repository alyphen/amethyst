package io.github.alyphen.immaterial_realm.client.network;

import io.github.alyphen.immaterial_realm.client.ImmaterialRealmClient;
import io.github.alyphen.immaterial_realm.common.chat.ChatChannel;
import io.github.alyphen.immaterial_realm.common.character.Character;
import io.github.alyphen.immaterial_realm.common.entity.Entity;
import io.github.alyphen.immaterial_realm.common.entity.EntityCharacter;
import io.github.alyphen.immaterial_realm.common.entity.EntityFactory;
import io.github.alyphen.immaterial_realm.common.object.WorldObject;
import io.github.alyphen.immaterial_realm.common.object.WorldObjectFactory;
import io.github.alyphen.immaterial_realm.common.object.WorldObjectInitializer;
import io.github.alyphen.immaterial_realm.common.packet.clientbound.PacketPong;
import io.github.alyphen.immaterial_realm.common.packet.clientbound.chat.PacketClientboundGlobalChatMessage;
import io.github.alyphen.immaterial_realm.common.packet.clientbound.chat.PacketClientboundLocalChatMessage;
import io.github.alyphen.immaterial_realm.common.packet.clientbound.chat.PacketSetChannel;
import io.github.alyphen.immaterial_realm.common.packet.clientbound.login.PacketClientboundPublicKey;
import io.github.alyphen.immaterial_realm.common.packet.clientbound.player.PacketPlayerJoin;
import io.github.alyphen.immaterial_realm.common.packet.clientbound.player.PacketPlayerLeave;
import io.github.alyphen.immaterial_realm.common.packet.serverbound.PacketPing;
import io.github.alyphen.immaterial_realm.common.packet.clientbound.character.PacketCharacterSpawn;
import io.github.alyphen.immaterial_realm.common.packet.serverbound.chat.PacketRequestChannels;
import io.github.alyphen.immaterial_realm.common.packet.clientbound.chat.PacketSendChannel;
import io.github.alyphen.immaterial_realm.common.packet.clientbound.entity.PacketEntityMove;
import io.github.alyphen.immaterial_realm.common.packet.clientbound.entity.PacketEntitySpawn;
import io.github.alyphen.immaterial_realm.common.packet.clientbound.login.PacketLoginStatus;
import io.github.alyphen.immaterial_realm.common.packet.serverbound.login.PacketServerboundPublicKey;
import io.github.alyphen.immaterial_realm.common.packet.clientbound.login.PacketVersion;
import io.github.alyphen.immaterial_realm.common.packet.clientbound.object.PacketCreateObject;
import io.github.alyphen.immaterial_realm.common.packet.serverbound.object.PacketRequestObjectTypes;
import io.github.alyphen.immaterial_realm.common.packet.clientbound.object.PacketSendObjectType;
import io.github.alyphen.immaterial_realm.common.packet.serverbound.player.PacketRequestPlayers;
import io.github.alyphen.immaterial_realm.common.packet.clientbound.player.PacketSendPlayers;
import io.github.alyphen.immaterial_realm.common.packet.serverbound.tile.PacketRequestTileSheets;
import io.github.alyphen.immaterial_realm.common.packet.clientbound.tile.PacketSendTileSheet;
import io.github.alyphen.immaterial_realm.common.packet.world.*;
import io.github.alyphen.immaterial_realm.common.player.Player;
import io.github.alyphen.immaterial_realm.common.sprite.Sprite;
import io.github.alyphen.immaterial_realm.common.tile.TileSheet;
import io.github.alyphen.immaterial_realm.common.world.World;
import io.github.alyphen.immaterial_realm.common.world.WorldArea;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;

import java.awt.*;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.sql.SQLException;

import static io.github.alyphen.immaterial_realm.common.object.WorldObjectFactory.registerObjectInitializer;
import static java.util.concurrent.TimeUnit.SECONDS;

public class ImmaterialRealmClientHandler extends ChannelHandlerAdapter {

    private ImmaterialRealmClient client;
    private Timer timer;

    public ImmaterialRealmClientHandler(ImmaterialRealmClient client) {
        this.client = client;
        timer = new HashedWheelTimer();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws GeneralSecurityException, IOException, SQLException {
        System.out.println(msg.toString());
        if (msg instanceof PacketVersion) {
            ctx.writeAndFlush(new PacketServerboundPublicKey(client.getEncryptionManager().getKeyPair().getPublic().getEncoded()));
        } else if (msg instanceof PacketClientboundPublicKey) {
            PacketClientboundPublicKey packet = (PacketClientboundPublicKey) msg;
            client.getNetworkManager().setServerPublicKey(packet.getEncodedPublicKey());
            client.showPanel("login");
        } else if (msg instanceof PacketLoginStatus) {
            PacketLoginStatus packet = (PacketLoginStatus) msg;
            if (packet.isSuccessful()) {
                client.showPanel("world");
                client.getWorldPanel().setActive(true);
                ctx.writeAndFlush(new PacketRequestPlayers());
            } else {
                client.getLoginPanel().setStatusMessage("Login unsuccessful.");
                client.getLoginPanel().reEnableLoginButtons();
            }
        } else if (msg instanceof PacketPong) {
            timer.newTimeout(timeout -> ctx.writeAndFlush(new PacketPing()), 20, SECONDS);
        } else if (msg instanceof PacketSendPlayers) {
            PacketSendPlayers packet = (PacketSendPlayers) msg;
            for (Player player : packet.getPlayers()) {
                if (client.getPlayerManager().getPlayer(player.getId()) == null) {
                    client.getPlayerManager().addPlayer(player);
                }
            }
            ctx.writeAndFlush(new PacketRequestTileSheets());
            ctx.writeAndFlush(new PacketRequestObjectTypes());
            ctx.writeAndFlush(new PacketRequestWorlds());
            ctx.writeAndFlush(new PacketRequestChannels());
        } else if (msg instanceof PacketSendTileSheet) {
            TileSheet.load((PacketSendTileSheet) msg);
        } else if (msg instanceof PacketSendObjectType) {
            PacketSendObjectType packet = (PacketSendObjectType) msg;
            registerObjectInitializer(packet.getName(), new WorldObjectInitializer() {

                @Override
                public String getObjectName() {
                    return packet.getName();
                }

                @Override
                public Sprite getObjectSprite() {
                    return packet.getSprite();
                }

                @Override
                public Rectangle getObjectBounds() {
                    return packet.getBounds();
                }

                @Override
                public WorldObject initialize(long id) {
                    return new WorldObject(id, getObjectName(), getObjectSprite(), getObjectBounds());
                }

            });
        } else if (msg instanceof PacketSendWorld) {
            PacketSendWorld packet = (PacketSendWorld) msg;
            client.getWorldPanel().setWorld(World.create(packet.getName()));
            ctx.writeAndFlush(new PacketRequestCurrentWorldArea());
        } else if (msg instanceof PacketSendArea) {
            PacketSendArea packet = (PacketSendArea) msg;
            client.getWorldPanel().getWorld().addArea(WorldArea.load(packet));
            ctx.writeAndFlush(new PacketRequestObjects(packet.getWorld(), packet.getArea()));
        } else if (msg instanceof PacketShowArea) {
            PacketShowArea packet = (PacketShowArea) msg;
            client.getWorldPanel().setArea(client.getWorldPanel().getWorld().getArea(packet.getArea()));
        } else if (msg instanceof PacketCreateObject) {
            PacketCreateObject packet = (PacketCreateObject) msg;
            if (client.getWorldPanel().getWorld().getName().equals(packet.getWorld()) && client.getWorldPanel().getArea().getName().equals(packet.getArea())) {
                WorldObject object = WorldObjectFactory.createObject(packet.getType());
                object.setX(packet.getX());
                object.setY(packet.getY());
                client.getWorldPanel().getArea().addObject(object);
            }
        } else if (msg instanceof PacketEntitySpawn) {
            PacketEntitySpawn packet = (PacketEntitySpawn) msg;
            if (packet.getAreaName().equals(client.getWorldPanel().getArea().getName())) {
                Entity entity = EntityFactory.spawn(packet, client.getWorldPanel().getWorld());
                entity.setX(packet.getX());
                entity.setY(packet.getY());
            }
        } else if (msg instanceof PacketCharacterSpawn) {
            PacketCharacterSpawn packet = (PacketCharacterSpawn) msg;
            if (client.getWorldPanel().getArea() != null) {
                if (packet.getAreaName().equals(client.getWorldPanel().getArea().getName())) {
                    Character character = client.getCharacterManager().getCharacter(packet.getId());
                    if (character == null) {
                        character = new Character(packet.getPlayerId(), packet.getId(), packet.getName(), packet.getGender(), packet.getRace(), packet.getDescription(), packet.isDead(), packet.isActive(), packet.getAreaName(), packet.getX(), packet.getY());
                        client.getCharacterManager().addCharacter(character);
                    } else {
                        character.setPlayerId(packet.getPlayerId());
                        character.setName(packet.getName());
                        character.setGender(packet.getGender());
                        character.setRace(packet.getRace());
                        character.setDescription(packet.getDescription());
                        character.setDead(packet.isDead());
                        character.setActive(packet.isActive());
                        character.setAreaName(packet.getAreaName());
                        character.setX(packet.getX());
                        character.setY(packet.getY());
                        client.getCharacterManager().updateCharacter(character);
                    }
                    character.setWalkUpSprite(packet.getWalkUpSprite());
                    character.setWalkDownSprite(packet.getWalkDownSprite());
                    character.setWalkLeftSprite(packet.getWalkLeftSprite());
                    character.setWalkRightSprite(packet.getWalkRightSprite());
                    EntityCharacter entity = EntityFactory.spawn(EntityCharacter.class, client.getWorldPanel().getArea(), packet.getX(), packet.getY());
                    entity.setCharacter(character);
                    if (character.getPlayerId() == client.getPlayerManager().getPlayer(client.getPlayerName()).getId()) {
                        client.getWorldPanel().setPlayerCharacter(entity);
                    }
                }
            }
        } else if (msg instanceof PacketEntityMove) {
            PacketEntityMove packet = (PacketEntityMove) msg;
            if (packet.getAreaName().equals(client.getWorldPanel().getArea().getName())) {
                Entity entity = null;
                for (Entity entity1 : client.getWorldPanel().getArea().getEntities()) {
                    if (packet.getEntityId() == entity1.getId()) {
                        entity = entity1;
                        break;
                    }
                }
                if (entity != null) {
                    entity.setDirectionFacing(packet.getDirectionFacing());
                    entity.setX(packet.getX());
                    entity.setY(packet.getY());
                    entity.setHorizontalSpeed(packet.getHorizontalSpeed());
                    entity.setVerticalSpeed(packet.getVerticalSpeed());
                }
            }
        } else if (msg instanceof PacketSendChannel) {
            PacketSendChannel packet = (PacketSendChannel) msg;
            client.getChatManager().addChannel(new ChatChannel(packet.getName(), packet.getColour(), packet.getRadius()));
        } else if (msg instanceof PacketClientboundLocalChatMessage) {
            PacketClientboundLocalChatMessage packet = (PacketClientboundLocalChatMessage) msg;
            client.getWorldPanel().getArea().getEntities().stream().filter(entity -> (entity instanceof EntityCharacter && ((EntityCharacter) entity).getCharacter().getId() == packet.getCharacterId())).forEach(entity -> {
                EntityCharacter character = (EntityCharacter) entity;
                character.setLastChatMessage(packet.getMessage());
            });
        } else if (msg instanceof PacketSetChannel) {
            PacketSetChannel packet = (PacketSetChannel) msg;
            client.getChatManager().setChannel(client.getChatManager().getChannel(packet.getChannel()));
        } else if (msg instanceof PacketClientboundGlobalChatMessage) {
            PacketClientboundGlobalChatMessage packet = (PacketClientboundGlobalChatMessage) msg;
            client.getWorldPanel().getChatBox().onGlobalMessage(packet.getPlayerId(), packet.getChannel(), packet.getMessage());
        } else if (msg instanceof PacketPlayerJoin) {
            PacketPlayerJoin packet = (PacketPlayerJoin) msg;
            if (client.getPlayerManager().getPlayer(packet.getPlayerId()) == null) {
                client.getPlayerManager().addPlayer(new Player(packet.getPlayerId(), packet.getPlayerName()));
            } else {
                client.getPlayerManager().updatePlayer(new Player(packet.getPlayerId(), packet.getPlayerName()));
            }
        } else if (msg instanceof PacketPlayerLeave) {
            PacketPlayerLeave packet = (PacketPlayerLeave) msg;
            client.getWorldPanel().getArea().getEntities().stream().filter(entity -> entity instanceof EntityCharacter).forEach(entity -> {
                EntityCharacter characterEntity  = (EntityCharacter) entity;
                if (characterEntity.getCharacter().getPlayerId() == packet.getPlayerId()) {
                    client.getWorldPanel().getArea().removeEntity(characterEntity);
                }
            });
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext context, Throwable cause) throws Exception {
        cause.printStackTrace();
    }

}
