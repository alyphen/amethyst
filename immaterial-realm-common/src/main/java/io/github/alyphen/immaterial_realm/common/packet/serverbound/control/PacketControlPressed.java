package io.github.alyphen.immaterial_realm.common.packet.serverbound.control;

import io.github.alyphen.immaterial_realm.common.control.Control;
import io.github.alyphen.immaterial_realm.common.packet.Packet;

public class PacketControlPressed extends Packet {

    private String control;

    public PacketControlPressed(Control control) {
        this.control = control.name();
    }

    public Control getControl() {
        return Control.valueOf(control);
    }

}
