import kotlinx.serialization.Serializable

@Serializable
data class BitMapping(
    val flow: Double,
    val name: String,

    val rotate: RotateCommand? = null,
    val anim: AnimCommand? = null
)

@Serializable
data class AnimCommand(
    val id: String? = null,
    val on: String? = null,
    val off: String? = null
)

@Serializable
data class RotateCommand(
    val bone: String,
    val target: Angle3
)

@Serializable
data class Angle3(
    val x: Int = 0,
    val y: Int = 0,
    val z: Int = 0
)
