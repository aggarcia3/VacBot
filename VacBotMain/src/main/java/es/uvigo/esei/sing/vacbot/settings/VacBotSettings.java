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
 * The no-arguments constructor is meant to be used by JAXB only, as it
 * initializes fields to {@code null} values, which without further action will
 * remain {@code null} and break the contract that the fields of this class are
 * initialized to non-null values.
 * </p>
 *
 * @author Alejandro González García
 */
@XmlRootElement(name = "settings")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@ToString
public final class VacBotSettings {
	/**
	 * The number of threads that will be processing responses to incoming text
	 * messages.
	 */
	@Getter
	@XmlElement(name = "workerThreads")
	private final int workerThreads = Runtime.getRuntime().availableProcessors();

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
	 * Parameters for determining the front-end interface to use.
	 */
	@Getter @NonNull
	@XmlElements({
		@XmlElement(name = "telegramBotFrontend", type = TelegramBotFrontendInterfaceFactory.class),
		@XmlElement(name = "commandLineInterfaceFrontend", type = CommandLineInterfaceFrontendInterfaceFactory.class)
	})
	private final FrontendInterfaceFactory<? extends TextMessage> frontendInterfaceFactory = null;
}
