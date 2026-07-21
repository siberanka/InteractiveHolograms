package com.siberanka.interactiveholograms.nms.v1_13_R1;

import com.siberanka.interactiveholograms.nms.api.renderer.NmsClickableHologramRenderer;
import com.siberanka.interactiveholograms.nms.api.renderer.NmsEntityHologramRenderer;
import com.siberanka.interactiveholograms.nms.api.renderer.NmsHeadHologramRenderer;
import com.siberanka.interactiveholograms.nms.api.renderer.NmsHologramRendererFactory;
import com.siberanka.interactiveholograms.nms.api.renderer.NmsIconHologramRenderer;
import com.siberanka.interactiveholograms.nms.api.renderer.NmsSmallHeadHologramRenderer;
import com.siberanka.interactiveholograms.nms.api.renderer.NmsTextHologramRenderer;

class HologramRendererFactory implements NmsHologramRendererFactory {

    private final EntityIdGenerator entityIdGenerator;

    HologramRendererFactory(EntityIdGenerator entityIdGenerator) {
        this.entityIdGenerator = entityIdGenerator;
    }

    @Override
    public NmsTextHologramRenderer createTextRenderer() {
        return new TextHologramRenderer(entityIdGenerator);
    }

    @Override
    public NmsIconHologramRenderer createIconRenderer() {
        return new IconHologramRenderer(entityIdGenerator);
    }

    @Override
    public NmsHeadHologramRenderer createHeadRenderer() {
        return new HeadHologramRenderer(entityIdGenerator);
    }

    @Override
    public NmsSmallHeadHologramRenderer createSmallHeadRenderer() {
        return new SmallHeadHologramRenderer(entityIdGenerator);
    }

    @Override
    public NmsEntityHologramRenderer createEntityRenderer() {
        return new EntityHologramRenderer(entityIdGenerator);
    }

    @Override
    public NmsClickableHologramRenderer createClickableRenderer() {
        return new ClickableHologramRenderer(entityIdGenerator);
    }

}
