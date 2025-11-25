package com.ai.vis.domain.model

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.ai.vis.ui.components.BackgroundOption
import com.ai.vis.ui.components.DrawPath
import com.ai.vis.ui.components.FilterType
import com.ai.vis.ui.components.PortraitOption

enum class EditorTool {
    NONE,
    CROP,
    ADJUST,
    FILTER,
    STICKER,
    PORTRAIT,
    AI_STYLE,
    AI_BACKGROUND,
    TEXT,
    DRAW
}

data class TextItem(
    val id: Long = System.currentTimeMillis(),
    val text: String = "",
    val position: Offset = Offset.Zero,
    val fontSize: Int = 24,
    val color: Color = Color.Black,
    val fontWeight: androidx.compose.ui.text.font.FontWeight = androidx.compose.ui.text.font.FontWeight.Normal,
    val fontStyle: androidx.compose.ui.text.font.FontStyle = androidx.compose.ui.text.font.FontStyle.Normal,
    val scale: Float = 1f,
    val rotation: Float = 0f
)

data class StickerItem(
    val id: Long = System.currentTimeMillis(),
    val emoji: String = "",
    val drawableId: Int? = null,
    val position: Offset = Offset.Zero,
    val scale: Float = 1f,
    val rotation: Float = 0f,
    val opacity: Float = 1f
)

data class EditorUiState(
    val isLoading: Boolean = false,
    val loadingMessage: String = "",
    
    val originalBitmap: Bitmap? = null,
    val currentBitmap: Bitmap? = null,
    val displayBitmap: Bitmap? = null,
    
    val selectedTool: EditorTool = EditorTool.NONE,
    
    val adjustmentValues: Map<Int, Float> = mapOf(
        0 to 0f,
        1 to 0f,
        2 to 0f,
        3 to 0f,
        4 to 0f,
        5 to 0f
    ),
    
    val currentFilter: FilterType = FilterType.NONE,
    val filterIntensity: Float = 1f,
    
    val selectedPortraitOption: PortraitOption = PortraitOption.NONE,
    val beautyIntensity: Float = 0.5f,
    val eyeIntensity: Float = 0.5f,
    val blurFaceIntensity: Float = 0.5f,
    
    val selectedAIStyle: AIStyle = AIStyle.NONE,
    val aiStyleIntensity: Float = 1f,
    
    val selectedBackgroundOption: BackgroundOption = BackgroundOption.NONE,
    val backgroundBlurRadius: Int = 25,
    val backgroundColor: Int? = null,
    val backgroundImage: Bitmap? = null,
    
    val textItems: List<TextItem> = emptyList(),
    val selectedTextId: Long? = null,
    
    val stickerItems: List<StickerItem> = emptyList(),
    val selectedStickerId: Long? = null,
    
    val drawPaths: List<DrawPath> = emptyList(),
    val currentDrawColor: Color = Color.Black,
    val currentDrawWidth: Float = 10f,
    val isErasing: Boolean = false,
    
    val cropRatio: String = "Free",
    val rotation: Float = 0f,
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false,
    
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    
    val error: String? = null,
    val showSaveDialog: Boolean = false,
    val showExitDialog: Boolean = false,
    val saveSuccess: Boolean = false,
    val saveMessage: String = ""
)
