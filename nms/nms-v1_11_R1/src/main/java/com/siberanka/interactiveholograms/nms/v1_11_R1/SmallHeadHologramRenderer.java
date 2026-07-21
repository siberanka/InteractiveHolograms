package com.siberanka.interactiveholograms.nms.v1_11_R1;

import com.siberanka.interactiveholograms.nms.api.renderer.NmsSmallHeadHologramRenderer;

class SmallHeadHologramRenderer extends HeadHologramRenderer implements NmsSmallHeadHologramRenderer {

    SmallHeadHologramRenderer(EntityIdGenerator entityIdGenerator) {
        super(entityIdGenerator, true);
    }

}
