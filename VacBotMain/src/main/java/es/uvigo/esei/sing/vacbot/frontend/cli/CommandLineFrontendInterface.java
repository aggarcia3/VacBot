// SPDX-License-Identifier: AGPL-3.0-or-later

package es.uvigo.esei.sing.vacbot.frontend.cli;

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStreamReader;

import es.uvigo.esei.sing.vacbot.frontend.TextOnlyTextMessage;
import es.uvigo.esei.sing.vacbot.frontend.FrontendCommunicationException;
import es.uvigo.esei.sing.vacbot.frontend.FrontendInterface;
import lombok.NonNull;

/**
 * A front-end interface that receives and sends messages from and to the user
 * via the standard input and output streams associated to the process.
 *
 * @author Alejandro González García
 */
public final class CommandLineFrontendInterface implements FrontendInterface<TextOnlyTextMessage> {
	private final Console tty = System.console();
	private final BufferedReader stdin = tty == null ? new BufferedReader(new InputStreamReader(System.in)) : null;

	@Override
	public TextOnlyTextMessage awaitNextMessage() throws FrontendCommunicationException, InterruptedException {
		try {
			final String messageText;
			if (tty == null) {
				messageText = stdin.readLine();
			} else {
				System.out.print("> ");
				messageText = tty.readLine();
			}

			return new TextOnlyTextMessage(messageText);
		} catch (final IOError | IOException exc) {
			throw new FrontendCommunicationException(exc);
		}
	}

	@Override
	public boolean hasPendingMessages() throws FrontendCommunicationException {
		try {
			return tty  == null ? stdin.ready() : tty.reader().ready();
		} catch (final IOException exc) {
			throw new FrontendCommunicationException(exc);
		}
	}

	@Override
	public void sendMessage(@NonNull final TextOnlyTextMessage message) throws FrontendCommunicationException {
		System.out.print("- ");
		System.out.println(message.getText());
	}

	@Override
	public Class<TextOnlyTextMessage> getTextMessageType() {
		return TextOnlyTextMessage.class;
	}

	@Override
	public void close() throws IOException {
		// No resources to close
	}
}
