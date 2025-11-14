package com.ai.vis.domain.model

enum class AIStyle(val displayNameResId: Int, val styleImagePath: String?) {
    NONE(com.ai.vis.R.string.style_none, null),
    OIL_PAINTING(com.ai.vis.R.string.style_oil_painting, "styles/oil_painting.jpg"),
    WATERCOLOR(com.ai.vis.R.string.style_watercolor, "styles/watercolor.jpg"),
    CARTOON(com.ai.vis.R.string.style_cartoon, "styles/cartoon.jpg"),
    PENCIL_SKETCH(com.ai.vis.R.string.style_pencil_sketch, "styles/pencil_sketch.jpg"),
    VAN_GOGH(com.ai.vis.R.string.style_van_gogh, "styles/vangogh.jpg"),
    POP_ART(com.ai.vis.R.string.style_pop_art, "styles/pop_art.jpg"),
    IMPRESSIONISM(com.ai.vis.R.string.style_impressionism, "styles/impressionism.jpg")
}
