package vg.skye.disharmony

import kotlinx.serialization.Serializable
import net.fabricmc.loader.api.FabricLoader
import net.peanuuutz.tomlkt.Toml
import net.peanuuutz.tomlkt.decodeFromNativeReader
import kotlin.io.path.*

@Serializable
data class Config(
    val token: String,
    val channel: ULong,
    val linkedRoleId: ULong,
    val pfpLinkTemplate: String,
    val discordMessageTemplate: String,
    val discordReplyMessageTemplate: String,
    val badCodeMessage: String,
    val linkedMessage: String,
    val alreadyLinkedMessage: String,
    val unlinkedMessage: String,
    val notLinkedMessage: String,
    val kickMessageTemplate: String,
    val statusMessageTemplate: String,
) {
    companion object {
        private var maybeInstance: Config? = null
        val INSTANCE: Config
            get() {
                if (maybeInstance == null) {
                    val path = FabricLoader
                        .getInstance()
                        .configDir
                        .resolve("disharmony.toml")
                    if (path.notExists()) {
                        val default = FabricLoader
                            .getInstance()
                            .getModContainer("disharmony")
                            .get()
                            .findPath("assets/disharmony/default_config.toml")
                            .get()
                        default.copyTo(path)
                    }
                    maybeInstance = Toml.decodeFromNativeReader(serializer(), path.reader())
                }
                return maybeInstance!!
            }
    }
}