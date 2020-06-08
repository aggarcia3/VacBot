// SPDX-License-Identifier: AGPL-3.0-or-later

package es.uvigo.esei.sing.vacbot.settings;

import es.uvigo.esei.sing.vacbot.frontend.TextMessage;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElements;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;

/**
 * Holds all the user-provided configuration options for VacBot.
 * <p>
 * This class is meant to be instantiated by client code via
 * {@link SettingsFacade}.
 * </p>
 *
 * @author Alejandro González García
 */
@XmlRootElement(name = "settings")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@ToString
public final class VacBotSettings implements AutoCloseable {
	/**
	 * The number of threads that will be processing responses to incoming text
	 * messages.
	 */
	@Getter
	@XmlElement(name = "workerThreads")
	private final int workerThreads = Runtime.getRuntime().availableProcessors();

	/**
	 * The parameters to pass to NLP algorithms.
	 */
	@Getter
	@XmlElement(name = "nlpSettings")
	private final NaturalLanguageProcessingSettings naturalLanguageProcessingSettings =
		NaturalLanguageProcessingSettings.defaultSettings();

	/**
	 * The relational document database settings.
	 */
	@Getter @NonNull
	@XmlElement(name = "documentDatabaseConnection", required = true)
	private final DocumentDatabaseConnectionSettings documentDatabaseSettings = null;

	/**
	 * The knowledge base connection settings.
	 */
	@Getter @NonNull
	@XmlElement(name = "knowledgeBaseConnection", required = true)
	private final KnowledgeBaseConnectionSettings knowledgeBaseSettings = null;

	/**
	 * The Lucene document index settings.
	 */
	@Getter @NonNull
	@XmlElement(name = "luceneIndex", required = true)
	private final LuceneIndexSettings luceneIndexSettings = null;

	/**
	 * The behavior settings for the bot.
	 */
	@Getter @NonNull
	@XmlElement(name = "behavior", required = true)
	private final BehaviorSettings behaviorSettings = null;

	/**
	 * Parameters for determining the front-end interface, and therefore text
	 * message dispatcher, to use.
	 */
	@Getter @NonNull
	@XmlElements({
		@XmlElement(name = "telegramBotFrontend", type = TelegramBotMessageDispatcherFactory.class),
		@XmlElement(name = "commandLineInterfaceFrontend", type = CommandLineInterfaceDispatcherFactory.class)
	})
	private final MessageDispatcherFactory<? extends TextMessage, ? extends Object> messageDispatcherFactory = null;

	@Override
	public void close() throws Exception {
		Exception thrownException = null;

		if (documentDatabaseSettings != null) {
			try {
				documentDatabaseSettings.close();
			} catch (final Exception exc) {
				thrownException = exc;
			}
		}

		if (knowledgeBaseSettings != null) {
			try {
				knowledgeBaseSettings.close();
			} catch (final Exception exc) {
				if (thrownException != null) {
					exc.addSuppressed(thrownException);
				}
				thrownException = exc;
			}
		}

		if (luceneIndexSettings != null) {
			try {
				luceneIndexSettings.close();
			} catch (final Exception exc) {
				if (thrownException != null) {
					exc.addSuppressed(thrownException);
				}
				thrownException = exc;
			}
		}

		if (thrownException != null) {
			throw thrownException;
		}
	}
}
