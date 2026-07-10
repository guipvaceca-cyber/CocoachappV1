package com.example.coachapp.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChangeHistory
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class BoardLine(
    val points: List<@Serializable(with = OffsetSerializer::class) Offset>,
    @Serializable(with = ColorSerializer::class) val color: Color
)

@Serializable
data class BoardElement(
    val id: String,
    val type: ElementType,
    @Serializable(with = OffsetSerializer::class) val position: Offset,
    val label: String = "",
    val rotation: Float = 0f
)

@Serializable
enum class ElementType {
    PLAYER, CONE, BALL_CART, HURDLE;
    
    val icon: ImageVector get() = when(this) {
        PLAYER -> Icons.Default.Person
        CONE -> Icons.Default.ChangeHistory
        BALL_CART -> Icons.Default.ShoppingBasket
        HURDLE -> Icons.Default.Remove
    }
    
    val color: Color get() = when(this) {
        PLAYER -> Color.Blue
        CONE -> Color.Red
        BALL_CART -> Color.DarkGray
        HURDLE -> Color.Black
    }
}

@Serializable
data class SavedBoard(
    val name: String,
    val date: Long,
    val lines: List<BoardLine>,
    val elements: List<BoardElement>
)
