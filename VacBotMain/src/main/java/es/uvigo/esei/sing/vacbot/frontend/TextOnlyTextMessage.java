// SPDX-License-Identifier: AGPL-3.0-or-later

package es.uvigo.esei.sing.vacbot.frontend;

import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Represents a text message without any additional metadata.
 *
 * @author Alejandro González García
 * @implNote This class is immutable and, therefore, thread-safe.
 */
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public final class TextOnlyTextMessage extends TextMessage {
	/**
	 * Creates a new command line text interface.
	 *
	 * @param text The text of the message.
	 * @throws IllegalArgumentException If {@code text} is {@code null}.
	 */
	public TextOnlyTextMessage(final String text) {
		super(text);
	}
}
