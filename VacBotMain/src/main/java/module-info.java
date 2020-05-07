// SPDX-License-Identifier: AGPL-3.0-or-later

/**
 * Classes that implement VacBot and its user interfaces.
 *
 * @author Alejandro González García
 */
module es.uvigo.esei.sing.vacbot.main {
	requires telegrambots;
	requires telegrambots.meta;

	requires jakarta.xml.bind;
	requires lombok;
	requires java.xml;
	requires java.sql;
	requires slf4j.api;

	// JAXB requires deep reflection access
	opens es.uvigo.esei.sing.vacbot.settings to jakarta.xml.bind;
}
