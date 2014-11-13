package io.github.lucariatias.amethyst.common.packet.tile;

import io.github.lucariatias.amethyst.common.packet.Packet;
import io.github.lucariatias.amethyst.common.util.ImageUtils;

import java.awt.image.BufferedImage;
import java.io.IOException;

public class PacketSendTileSheet extends Packet {

    private String name;
    private byte[] image;
    private int tileWidth;
    private int tileHeight;

    public PacketSendTileSheet(String name, BufferedImage image, int tileWidth, int tileHeight) {
        this.name = name;
        try {
            this.image = ImageUtils.toByteArray(image);
        } catch (IOException exception) {
            exception.printStackTrace();
        }
        this.tileWidth = tileWidth;
        this.tileHeight = tileHeight;
    }

    public String getName() {
        return name;
    }

    public BufferedImage getImage() {
        try {
            return ImageUtils.fromByteArray(image);
        } catch (IOException exception) {
            exception.printStackTrace();
        }
        return null;
    }

    public int getTileWidth() {
        return tileWidth;
    }

    public int getTileHeight() {
        return tileHeight;
    }

}
