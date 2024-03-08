package vg.skye.disharmony

import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import net.fabricmc.loader.api.FabricLoader
import java.util.*
import kotlin.io.path.notExists
import kotlin.io.path.reader
import kotlin.io.path.writer

data class LinkData(val accounts: BiMap<UUID, Long>) {
    companion object {
        private val gson = Gson()
        private val mapType = object : TypeToken<Map<UUID, Long>>() {}
        fun load(): LinkData {
            val path = FabricLoader
                .getInstance()
                .gameDir
                .resolve("disharmony_links.json")
            if (path.notExists()) {
                return LinkData(HashBiMap.create())
            }
            return LinkData(HashBiMap.create(gson.fromJson(path.reader(), mapType)))
        }
    }
    fun save() {
        val writer = FabricLoader
            .getInstance()
            .gameDir
            .resolve("disharmony_links.json")
            .writer()
        gson.toJson(accounts, mapType.type, writer)
        writer.close()
    }
}