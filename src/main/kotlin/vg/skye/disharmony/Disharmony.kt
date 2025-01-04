package vg.skye.disharmony

import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.events.CoroutineEventListener
import dev.minn.jda.ktx.jdabuilder.light
import eu.pb4.placeholders.api.ParserContext
import eu.pb4.placeholders.api.Placeholders
import eu.pb4.placeholders.api.TextParserUtils
import eu.pb4.placeholders.api.parsers.MarkdownLiteParserV1
import eu.pb4.placeholders.api.parsers.PatternPlaceholderParser
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.Webhook
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.session.ReadyEvent
import net.dv8tion.jda.api.requests.GatewayIntent
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.server.MinecraftServer
import net.minecraft.text.Style
import net.minecraft.text.Text
import org.slf4j.LoggerFactory
import java.util.*


object Disharmony : ModInitializer, CoroutineEventListener {
    private val logger = LoggerFactory.getLogger("disharmony")
	private val client = light(Config.INSTANCE.token, builder = {
		enableIntents(GatewayIntent.GUILD_MEMBERS)
		enableIntents(GatewayIntent.MESSAGE_CONTENT)
		enableIntents(GatewayIntent.GUILD_WEBHOOKS)
		addEventListeners(Disharmony)
	})
	private lateinit var channel: TextChannel
	lateinit var webhook: Webhook

	private fun toMarkdown(text: Text): String {
		var result = ""
		text.visit<Void>({ style, contents ->
			var affix = ""
			if (style.isObfuscated) {
				affix += "||"
			}
			if (style.isBold) {
				affix += "**"
			}
			if (style.isItalic) {
				affix += "*"
			}
			if (style.isUnderlined) {
				affix += "__"
			}
			if (style.isStrikethrough) {
				affix += "~~"
			}
			result += affix + contents + affix.reversed()
			Optional.empty()
		}, Style.EMPTY)
		return result
	}

	fun onPlayerCountChange(count: Int) {
		val message = Config
			.INSTANCE
			.statusMessageTemplate
			.replace("%count%", count.toString())
		client.presence.activity = Activity.customStatus(message)
	}

	override fun onInitialize() {
		ServerMessageEvents.GAME_MESSAGE.register { _, msg, overlay ->
			if (overlay) {
				return@register
			}
			channel.sendMessage(toMarkdown(msg)).queue()
		}

		ServerMessageEvents.CHAT_MESSAGE.register { msg, player, params ->
			val sender = params.name.string
			val pfp = Config.INSTANCE.pfpLinkTemplate.replace("%UUID%", player.uuidAsString)
			webhook.sendMessage(toMarkdown(msg.content)).apply {
				setUsername(sender)
				setAvatarUrl(pfp)
				queue()
			}
		}

		ServerLifecycleEvents.SERVER_STOPPING.register {
			client.shutdown()
		}

		ServerLifecycleEvents.SERVER_STOPPED.register {
			client.shutdownNow()
		}
	}

	private suspend fun onReady() {
		val maybe = client.getTextChannelById(Config.INSTANCE.channel.toLong())
		if (maybe == null) {
			logger.error("Could not find channel specified by config!")
		} else {
			channel = maybe
		}
		val webhooks = channel.retrieveWebhooks().await()
		webhook = webhooks.find { webhook ->
			webhook.name == "MC_DC_INTEGRATION"
		} ?: channel.createWebhook("MC_DC_INTEGRATION").await()

		val server = FabricLoader.getInstance().gameInstance as MinecraftServer
		val message = Config
			.INSTANCE
			.statusMessageTemplate
			.replace("%count%", server.currentPlayerCount.toString())
		client.presence.activity = Activity.customStatus(message)
	}

	private fun formatDiscordMessage(message: Message): Text {
		val replyToName = message.referencedMessage?.member?.effectiveName ?: message.referencedMessage?.author?.effectiveName
		val replyToColorRaw = (message.referencedMessage?.member?.colorRaw ?: Role.DEFAULT_COLOR_RAW) and 0xFFFFFF
		val replyToColorHex = "#%06X".format(replyToColorRaw)
		val name = message.member?.effectiveName ?: message.author.effectiveName
		val content = MarkdownLiteParserV1.ALL.parseText(
			message.contentDisplay,
			ParserContext.of()
		)
		val template = if (replyToName == null) {
			Config.INSTANCE.discordMessageTemplate
		} else {
			Config.INSTANCE.discordReplyMessageTemplate
		}
		val colorRaw = (message.member?.colorRaw ?: Role.DEFAULT_COLOR_RAW) and 0xFFFFFF
		val colorHex = "#%06X".format(colorRaw)
		val processed = template
			.replace("%roleColor%", colorHex)
			.replace("%replyToRoleColor%", replyToColorHex)
		val placeholders = mapOf(
			"content" to content,
			"name" to Text.literal(name),
			"replyToName" to Text.literal(replyToName),
		)
		val substituted = TextParserUtils.formatText(processed)
		return Placeholders.parseText(
			substituted,
			PatternPlaceholderParser.PREDEFINED_PLACEHOLDER_PATTERN,
			placeholders
		)
	}

	private fun onMessageReceived(event: MessageReceivedEvent) {
		if (event.channel.idLong != channel.idLong
			|| event.isWebhookMessage
			|| event.author.idLong == client.selfUser.idLong) {
			return
		}
		val server = FabricLoader.getInstance().gameInstance as MinecraftServer
		val message = formatDiscordMessage(event.message)
		server.sendMessage(message)
		server.playerManager.playerList.forEach {
			it.sendMessageToClient(message, false)
		}
	}

	override suspend fun onEvent(event: GenericEvent) {
		when (event) {
			is ReadyEvent -> onReady()
			is MessageReceivedEvent -> onMessageReceived(event)
		}
	}
}