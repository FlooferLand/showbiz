package com.flooferland.showbiz

import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.fabricmc.loader.api.FabricLoader
import kotlin.jvm.optionals.getOrNull

object UpdateChecker {
    private var json = Json {
        ignoreUnknownKeys = true
        allowTrailingComma = true
    }
    private var client = HttpClient.newBuilder()
        .executor(Executors.newVirtualThreadPerTaskExecutor())
        .connectTimeout(Duration.ofSeconds(5))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()

    var currentVersion: String = "0.0.0"
    var newerVersion: String? = null

    init {
        currentVersion = FabricLoader.getInstance()
            .getModContainer(Showbiz.MOD_ID)
            .map { container -> container.metadata.version.friendlyString }
            .getOrNull() ?: currentVersion
    }

    fun getMessage(): MutableComponent = when {
        newerVersion == null -> Component.literal("Showbiz is up to date")
        else -> Component.literal("Showbiz version '$newerVersion' is available!").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD)
    }

    private suspend fun fetch(): Result<String> = runCatching {
        val req = HttpRequest.newBuilder(URI.create("https://api.modrinth.com/v2/project/wJ32u9Oq/version"))
            .header("User-Agent", "flooferland/showbiz/${currentVersion}")
            .GET().build()
        client.sendAsync(req, HttpResponse.BodyHandlers.ofString()).await().body()!!
    }

    fun check() {
        fun getNums(str: String): Array<Int> {
            val segments = str.substringBefore('-').split('.').mapNotNull { it.toIntOrNull() }
            return Array(3) { segments.getOrNull(it) ?: 0 }
        }

        Showbiz.log.info("Checking for updates..")
        CoroutineScope(Dispatchers.IO).launch {
            val request = fetch()
            request.onSuccess { body ->
                val versionObjects = json.decodeFromString<Array<JsonObject>>(body)
                for (obj in versionObjects) {
                    val version = (obj["version_number"] ?: obj.entries.firstOrNull { it.key.contains("version") }?.value ?: continue)
                        .jsonPrimitive.content
                    val currVer = getNums(currentVersion)
                    val newVer = getNums(version)
                    val isNewer = currVer.zip(newVer)
                        .firstOrNull { it.first != it.second }
                        ?.let { it.first < it.second }
                        ?: false
                    if (isNewer) {
                        newerVersion = version
                        break
                    }
                }

                when {
                    newerVersion != null -> Showbiz.log.warn(getMessage().string)
                    else -> Showbiz.log.info(getMessage().string)
                }
            }
            request.onFailure { Showbiz.log.error("Failed to check for updates", it) }
        }
    }
}