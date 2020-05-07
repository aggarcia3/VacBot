// SPDX-License-Identifier: AGPL-3.0-or-later

package es.uvigo.esei.sing.vacbot.frontend.telegrambot;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

import es.uvigo.esei.sing.vacbot.frontend.TextMessage;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

/**
 * Represents a text message to be received or sent from or to a Telegram bot
 * front-end interface. These messages have the Telegram specific metadata
 * provided by its API.
 *
 * @author Alejandro González García
 * @see <a href="https://core.telegram.org/bots/api#message">Telegram API
 *      documentation</a>
 * @implNote This class is immutable and, therefore, thread-safe.
 */
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public final class TelegramTextMessage extends TextMessage {
	@Getter
	private final int messageId;
	@Getter
	private final User user;
	@Getter @NonNull
	private final Instant date;
	@Getter @NonNull
	private final Chat chat;
	@Getter
	private final User forwardedFromUser;
	@Getter
	private final Chat forwardedFromChat;
	@Getter
	private final Integer forwardedFromMessageId;
	@Getter
	private final String forwardedSenderName;
	@Getter
	private final Instant forwardedDate;
	@Getter
	private final Message replyToMessage;
	@Getter
	private final Instant editDate;
	@Getter
	private final List<MessageEntity> entities;
	@Getter
	private final Message thisMessage;

	/**
	 * Creates a new Telegram text message.
	 *
	 * @param text                   The text of the message.
	 * @param messageId              The numeric ID of the message. It may be set to
	 *                               a dummy value when unknown (i.e. sending
	 *                               messages).
	 * @param user                   The user that sent the message. It may be
	 *                               {@code null} if the message is to be sent by
	 *                               the bot.
	 * @param date                   The date at which the message was sent. If
	 *                               {@code null}, this value will be initialized to
	 *                               the current date.
	 * @param chat                   The chat the message belongs to.
	 * @param forwardedFromUser      The original sender user of the message. It may
	 *                               be {@code null} if not applicable.
	 * @param forwardedFromChat      The original chat where the message was sent.
	 *                               It may be {@code null} if not applicable.
	 * @param forwardedFromMessageId The original message ID that was forwarded. It
	 *                               may be {@code null} if not applicable.
	 * @param forwardedSenderName    The original sender username of the message. It
	 *                               may be {@code null} if not applicable.
	 * @param forwardedDate          The original send date of the forwarded
	 *                               message. It may be {@code null} if not
	 *                               applicable.
	 * @param replyToMessage         The message that this message replies to. It
	 *                               may be {@code null} if this message doesn't
	 *                               explicitly reply other message.
	 * @param editDate               The last edit date of the message. It may be
	 *                               {@code null} if not applicable.
	 * @param entities               The entities present in a received message. It
	 *                               may be {@code null} if not applicable.
	 * @param thisMessage            A message object that represents this text
	 *                               message at the Telegram API level. It may be
	 *                               {@code null} if the message is to be sent.
	 * @throws IllegalArgumentException If any parameter is set to {@code null} when
	 *                                  it wasn't explicitly stated how it handles
	 *                                  {@code null} values.
	 */
	public TelegramTextMessage(
		@NonNull final String text, final int messageId, final User user, final Instant date, @NonNull final Chat chat,
		final User forwardedFromUser, final Chat forwardedFromChat, final Integer forwardedFromMessageId, final String forwardedSenderName,
		final Instant forwardedDate, final Message replyToMessage, final Instant editDate, final List<MessageEntity> entities,
		final Message thisMessage
	) {
		super(text);

		this.messageId = messageId;
		this.user = user;
		this.date = date != null ? date : Instant.now();
		this.chat = chat;
		this.forwardedFromUser = forwardedFromUser;
		this.forwardedFromChat = forwardedFromChat;
		this.forwardedFromMessageId = forwardedFromMessageId;
		this.forwardedSenderName = forwardedSenderName;
		this.forwardedDate = forwardedDate;
		this.replyToMessage = replyToMessage;
		this.editDate = editDate;
		this.entities = entities != null ? Collections.unmodifiableList(entities) : null;
		this.thisMessage = thisMessage;
	}

	/**
	 * Factory method that converts the provided update to a
	 * {@link TelegramTextMessage}.
	 *
	 * @param update The update to convert to a message.
	 * @return The message contained in the update.
	 * @throws IllegalArgumentException If {@code update} is {@code null} or not a
	 *                                  text message update.
	 */
	static TelegramTextMessage ofUpdate(@NonNull final Update update) {
		if (!update.hasMessage() || !update.getMessage().hasText()) {
			throw new IllegalArgumentException("The update must be a text message");
		}

		final Message message = update.getMessage();

		return new TelegramTextMessage(
			message.getText(), message.getMessageId(),
			message.getFrom(),
			Instant.ofEpochSecond(message.getDate()),
			message.getChat(), message.getForwardFrom(),
			message.getForwardFromChat(),
			message.getForwardFromMessageId(),
			message.getForwardSenderName(),
			message.getForwardDate() != null ? Instant.ofEpochSecond(message.getForwardDate()) : null,
			message.getReplyToMessage(),
			message.getEditDate() != null ? Instant.ofEpochSecond(message.getEditDate()) : null,
			message.getEntities(),
			update.getMessage()
		);
	}

	/**
	 * Converts this Telegram bot text message to a {@link SendMessage} object to be
	 * used for the corresponding Telegram API call.
	 *
	 * @return The described object.
	 */
	SendMessage toSendMessage() {
		final SendMessage message = new SendMessage()
			.setChatId(chat.getId())
			.setText(getText());

		// To avoid visual noise, only send reply information
		// for chats where there is several people
		if (replyToMessage != null && (chat.isGroupChat() || chat.isSuperGroupChat() || chat.isChannelChat())) {
			message.setReplyToMessageId(replyToMessage.getMessageId());
		}

		return message;
	}
}
