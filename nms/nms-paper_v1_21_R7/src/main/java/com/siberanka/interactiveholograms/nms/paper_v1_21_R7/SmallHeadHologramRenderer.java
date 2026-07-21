package com.siberanka.interactiveholograms.nms.paper_v1_21_R7;

import com.siberanka.interactiveholograms.nms.api.renderer.NmsSmallHeadHologramRenderer;

class SmallHeadHologramRenderer extends HeadHologramRenderer implements NmsSmallHeadHologramRenderer {

    SmallHeadHologramRenderer(EntityIdGenerator entityIdGenerator) {
        super(entityIdGenerator, true);
    }

}
