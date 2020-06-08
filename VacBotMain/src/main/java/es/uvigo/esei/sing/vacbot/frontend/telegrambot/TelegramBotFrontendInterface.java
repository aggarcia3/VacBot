// SPDX-License-Identifier: AGPL-3.0-or-later

package es.uvigo.esei.sing.vacbot.frontend.telegrambot;

import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.bots.TelegramWebhookBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.generics.BotSession;

import es.uvigo.esei.sing.vacbot.frontend.FrontendCommunicationException;
import es.uvigo.esei.sing.vacbot.frontend.FrontendInterface;
import es.uvigo.esei.sing.vacbot.settings.TelegramBotMessageDispatcherFactory;
import es.uvigo.esei.sing.vacbot.settings.TelegramBotLongPollingUpdateReceptionMethodFactory;
import es.uvigo.esei.sing.vacbot.settings.TelegramBotWebhookUpdateReceptionMethodFactory;
import es.uvigo.esei.sing.vacbot.util.ExceptionThrowingSupplier;
import lombok.NonNull;

/**
 * A front-end interface that receives and sends messages from and to the user
 * via Telegram chats, where the bot is an actual Telegram bot account.
 *
 * @author Alejandro González García
 */
public final class TelegramBotFrontendInterface implements FrontendInterface<TelegramTextMessage, Chat> {
	private final static Logger LOGGER = LoggerFactory.getLogger(TelegramBotFrontendInterface.class);

	private final BotSession session;
	private final BlockingQueue<TelegramTextMessage> messageQueue = new LinkedBlockingQueue<>(250); // 250 in-flight updates maximum
	private final AbsSender telegramBot;
	private final ExceptionThrowingSupplier<User, FrontendCommunicationException> botUserSupplier;

	{
		// Needed by telegrambots
		ApiContextInitializer.init();

		LOGGER.trace("Telegrambots API context initialized");

		// Thread-safe supplier
		this.botUserSupplier = new ExceptionThrowingSupplier<>() {
			private final Object botUserLock = new Object();
			private User botUser = null;

			@Override
			public User throwingGet() throws FrontendCommunicationException {
				try {
					// AtomicReference may result in the Telegram API being called twice
					// and breaking havoc, so we use plain mutexes
					synchronized (botUserLock) {
						return botUser == null ? telegramBot.getMe() : botUser;
					}
				} catch (final Exception exc) {
					throw new FrontendCommunicationException(exc);
				}
			}
		};
	}

	/**
	 * Creates a new Telegram front-end interface for the specified factories, that
	 * receives updates via long polling. The long polling settings are stored in a
	 * {@link TelegramBotLongPollingUpdateReceptionMethodFactory} object.
	 *
	 * @param longPollingFactory       The long polling update reception method
	 *                                 factory.
	 * @param frontendInterfaceFactory The front-end interface factory for which
	 *                                 this object will be created.
	 * @throws FrontendCommunicationException If an error occurred while registering
	 *                                        the bot with the Telegram servers.
	 */
	public TelegramBotFrontendInterface(
		@NonNull final TelegramBotLongPollingUpdateReceptionMethodFactory longPollingFactory,
		@NonNull final TelegramBotMessageDispatcherFactory frontendInterfaceFactory
	) throws FrontendCommunicationException {
		this.telegramBot = new TelegramLongPollingBot(getBotOptionsFromFactory(frontendInterfaceFactory)) {
			@Override
			public void onUpdateReceived(final Update update) {
				makeUpdateAvailableToClients(update);
			}

			@Override
			public String getBotUsername() {
				return frontendInterfaceFactory.getUserName();
			}

			@Override
			public String getBotToken() {
				return frontendInterfaceFactory.getToken();
			}
		};

		try {
			this.session = new TelegramBotsApi().registerBot((TelegramLongPollingBot) telegramBot);
		} catch (final Exception exc) {
			throw new FrontendCommunicationException(exc);
		}

		LOGGER.trace(
			"Telegram bot frontend with long polling update reception method started"
		);
	}

	/**
	 * Creates a new Telegram front-end interface for the specified factories, that
	 * receives updates via webhooks. The webhook settings are stored in a
	 * {@link TelegramBotWebhookUpdateReceptionMethodFactory} object.
	 *
	 * @param webhookFactory           The webhook update reception method factory.
	 * @param frontendInterfaceFactory The front-end interface factory for which
	 *                                 this object will be created.
	 * @throws FrontendCommunicationException If an error occurred while registering
	 *                                        the bot with the Telegram servers.
	 */
	public TelegramBotFrontendInterface(
		@NonNull final TelegramBotWebhookUpdateReceptionMethodFactory webhookFactory,
		@NonNull final TelegramBotMessageDispatcherFactory frontendInterfaceFactory
	) throws FrontendCommunicationException {
		this.telegramBot = new TelegramWebhookBot(getBotOptionsFromFactory(frontendInterfaceFactory)) {
			@Override
			public BotApiMethod<?> onWebhookUpdateReceived(final Update update) {
				makeUpdateAvailableToClients(update);

				// The Telegram API accepts sending an API call as a response to the
				// POST request they made. This allows for greater efficiency, at the
				// cost of not knowing the result of the API call. For now we don't take
				// advantage of that
				return null;
			}

			@Override
			public String getBotUsername() {
				return frontendInterfaceFactory.getUserName();
			}

			@Override
			public String getBotPath() {
				return webhookFactory.getBotPath();
			}

			@Override
			public String getBotToken() {
				return frontendInterfaceFactory.getToken();
			}
		};

		// Webhook bots have no session: they don't do outgoing connections in their update receive loop
		this.session = null;

		final Path certificateStorePath = webhookFactory.getCertificateStorePath();
		final String certificateStorePassword = webhookFactory.getCertificateStorePassword();
		final Path publicKeyPath = webhookFactory.getPublicKeyPath();

		try {
			if (certificateStorePath == null) {
				// No certificate information, assume that a reverse proxy handles HTTPS
				new TelegramBotsApi(
					webhookFactory.getExternalUrl(),
					webhookFactory.getInternalUrl()
				).registerBot((TelegramWebhookBot) telegramBot);
			} else {
				// We have a certificate store where our private key is stored.
				// There are two options now:
				// - We use a certificate signed by a CA recognized by Telegram
				// - We use a certificate signed by a CA not recognized by Telegram (self-signed or whatever)
				if (publicKeyPath == null) {
					// Signed by a recognized CA
					new TelegramBotsApi(
						certificateStorePath.toAbsolutePath().toString(),
						certificateStorePassword,
						webhookFactory.getExternalUrl(),
						webhookFactory.getInternalUrl()
					).registerBot((TelegramWebhookBot) telegramBot);
				} else {
					// Certificate signed by a unrecognized CA
					new TelegramBotsApi(
						certificateStorePath.toAbsolutePath().toString(),
						certificateStorePassword,
						webhookFactory.getExternalUrl(),
						webhookFactory.getInternalUrl(),
						publicKeyPath.toAbsolutePath().toString()
					).registerBot((TelegramWebhookBot) telegramBot);
				}
			}
		} catch (final Exception exc) {
			throw new FrontendCommunicationException(exc);
		}

		LOGGER.trace(
			"Telegram bot frontend with webhook update reception method started"
		);
	}

	@Override
	public TelegramTextMessage awaitNextMessage() throws FrontendCommunicationException, InterruptedException {
		return messageQueue.take();
	}

	@Override
	public void sendMessage(@NonNull final TelegramTextMessage message) throws FrontendCommunicationException {
		final SendMessage sendMessage = message.toSendMessage();

		try {
			// Sends a POST request to Telegram servers
			telegramBot.execute(sendMessage);

			LOGGER.trace("Outgoing message: " + message);
		} catch (final Exception exc) {
			throw new FrontendCommunicationException(exc);
		}
	}

	@Override
	public boolean isMessageForBot(@NonNull final TelegramTextMessage message) throws FrontendCommunicationException {
		final int maxReplyDepth = 50;
		boolean isForBot = message.getChat().isUserChat();

		// Are further checks needed? We ignore messages from channels
		if (!isForBot || !message.getChat().isChannelChat()) {
			final List<MessageEntity> messageEntities = message.getEntities();

			// First, check if the message explicitly mentions us.
			// That only makes sense if the message contains entities
			if (messageEntities != null && !messageEntities.isEmpty()) {
				final Iterator<MessageEntity> iter = messageEntities.iterator();
				while (!isForBot && iter.hasNext()) {
					final MessageEntity entity = iter.next();

					try {
						final MessageEntityType entityType = MessageEntityType.getType(entity);

						isForBot =
							(MessageEntityType.MENTION == entityType || MessageEntityType.TEXT_MENTION == entityType) &&
							(
								entity.getText().equals("@" + botUserSupplier.throwingGet().getUserName()) ||
								entity.getText().equals("@" + botUserSupplier.throwingGet().getFirstName())
							);
					} catch (final Exception exc) {
						throw new FrontendCommunicationException(exc);
					}
				}
			}

			if (!isForBot) {
				// If the message doesn't mention us, check if it replies to a message from us
				// (either directly or indirectly)
				Message repliedMessage = message.getThisMessage();

				if (repliedMessage == null) {
					throw new FrontendCommunicationException("The Telegram API message object is missing when it shouldn't");
				}

				int replyDepth = 0;
				while (
					!isForBot &&
					(repliedMessage = repliedMessage.getReplyToMessage()) != null &&
					replyDepth++ < maxReplyDepth
				) {
					isForBot = botUserSupplier.throwingGet().getId().equals(repliedMessage.getFrom().getId());
				}
			}
		}

		return isForBot;
	}

	@Override
	public void notifyForthcomingResponse(@NonNull final Chat notificationData) throws FrontendCommunicationException {
		try {
			telegramBot.execute(new SendChatAction(notificationData.getId(), "typing"));
		} catch (final Exception exc) {
			throw new FrontendCommunicationException(exc);
		}
	}

	@Override
	public void close() throws Exception {
		if (session != null) {
			session.stop();

			LOGGER.info("Bot session stopped");
		}
	}

	/**
	 * Puts updates in the blocking update queue that clients will take updates
	 * from, waiting if necessary until the queue has a free spot. This method is
	 * uninterruptible.
	 *
	 * @param update The update to put in the queue.
	 */
	private void makeUpdateAvailableToClients(final Update update) {
		try {
			final TelegramTextMessage textMessage = TelegramTextMessage.ofUpdate(update);
			boolean messageNotPut = true;

			while (messageNotPut) {
				try {
					messageQueue.put(textMessage);

					LOGGER.trace("Update enqueued: " + textMessage);

					messageNotPut = false;
				} catch (InterruptedException exc) {
					// We want to offer the opportunity to handle updates
					// no matter what, as Telegram won't send them again
					Thread.currentThread().interrupt();
				}
			}
		} catch (final IllegalArgumentException ignored) {
			LOGGER.info(
				"Received unwanted or invalid update from Telegram with ID " +
				update.getUpdateId()
			);
		}
	}

	/**
	 * Retrieves the bot options contained in a
	 * {@link TelegramBotMessageDispatcherFactory} factory object, which should be
	 * used when instantiating this class.
	 *
	 * @param frontendInterfaceFactory The factory from which to get the settings.
	 *                                 It is assumed to be not {@code null}.
	 * @return The bot options to use when instantiating the Telegram bot class.
	 */
	private DefaultBotOptions getBotOptionsFromFactory(final TelegramBotMessageDispatcherFactory frontendInterfaceFactory) {
		final DefaultBotOptions botOptions = new DefaultBotOptions();

		botOptions.setMaxThreads(frontendInterfaceFactory.getMaxAsyncThreads());
		botOptions.setMaxWebhookConnections(frontendInterfaceFactory.getMaxWebhookConnections());
		botOptions.setBaseUrl(frontendInterfaceFactory.getBaseUrl());
		// Only receive updates that we know how to handle. See
		// https://core.telegram.org/bots/api#getupdates
		botOptions.setAllowedUpdates(List.of("message"));

		return botOptions;
	}
}
