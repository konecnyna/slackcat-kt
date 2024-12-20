import kotlinx.serialization.Serializable

@Serializable
data class PokemonData(
    val name: String,
    val stats: List<StatEntry>,
    val types: List<TypeSlot>,
    val weight: Int,
    val height: Int,
    val id: Int,
    val species: Species,
    val sprites: Sprites,
)

@Serializable
data class StatEntry(
    val base_stat: Int,
    val effort: Int,
    val stat: Resource,
)

@Serializable
data class TypeSlot(
    val slot: Int,
    val type: Resource,
)

@Serializable
data class Resource(
    val name: String,
)

@Serializable
data class Species(
    val name: String,
)

@Serializable
data class Sprites(
    val front_default: String? = null,
    val other: Other,
)

@Serializable
data class Other(
    val `official-artwork`: OfficialArtwork? = null,
)

@Serializable
data class OfficialArtwork(
    val front_default: String? = null,
    val front_shiny: String? = null,
)
