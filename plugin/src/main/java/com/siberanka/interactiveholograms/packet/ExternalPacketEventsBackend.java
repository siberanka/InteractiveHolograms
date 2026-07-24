package com.siberanka.interactiveholograms.packet;

public class ExternalPacketEventsBackend extends AbstractPacketEventsBackend {

    @Override
    public void onLoad() {
        // External mode: lifecycle is owned by external PacketEvents plugin.
    }

    @Override
    public void onEnable() {
        // External mode: lifecycle is owned by external PacketEvents plugin.
    }

    @Override
    public void onDisable() {
        // External mode: lifecycle is owned by external PacketEvents plugin.
    }

    @Override
    public boolean isExternal() {
        return true;
    }
}
