// SPDX-License-Identifier: AGPL-3.0-or-later

/**
 * Classes that implement VacBot and its user interfaces.
 *
 * @author Alejandro González García
 */
module es.uvigo.esei.sing.vacbot.main {
	requires lucene.shaded;

	requires stanford.corenlp;

	requires java.persistence;

	requires org.apache.jena.arq;
	requires org.apache.jena.core;
	requires org.apache.jena.tdb2;
	// This is not needed for compilation with Maven,
	// but Eclipse needs it
	requires org.apache.jena.dboe.base;

	requires telegrambots;
	requires telegrambots.meta;

	requires jakarta.xml.bind;
	requires java.ws.rs;

	requires slf4j.api;
	requires jfiglet;
	requires jcommander;

	requires com.google.common;

	requires lombok;
	requires java.xml;
	requires java.sql;

	// JAXB requires deep reflection access
	opens es.uvigo.esei.sing.vacbot.settings to jakarta.xml.bind;
	// JCommander too
	opens es.uvigo.esei.sing.vacbot to jcommander;
	// Hibernate requires deep reflection access from an unnamed module
	opens es.uvigo.esei.sing.vacbot.entity;
}
