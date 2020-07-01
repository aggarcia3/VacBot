// SPDX-License-Identifier: AGPL-3.0-or-later

package es.uvigo.esei.sing.vacbot.settings;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Contains all the natural language processing algorithm settings, which affect
 * NLP operations carried on by the bot.
 *
 * @author Alejandro González García
 * @see VacBotSettings
 */
@XmlRootElement(name = "nlpSettings")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@ToString
public final class NaturalLanguageProcessingSettings {
	/**
	 * The natural language identifier.
	 */
	@Getter
	@XmlElement(name = "language")
	private String language = "en";

	/**
	 * The options to pass to the CoreNLP tokenizer annotator.
	 */
	@Getter
	@XmlElement(name = "tokenizerOptions")
	private String tokenizerOptions = "quotes=ascii";

	/**
	 * The model to use for part of speech tagging.
	 */
	@Getter
	@XmlElement(name = "posModel")
	// Mirror of a model included with the latest CoreNLP models (as of 03-26-2020)
	private String posModel = "https://dl.dropboxusercontent.com/s/bmqus6uon0cr9ek/english-caseless-left3words-distsim.tagger";

	/**
	 * The model to use for constituency parsing.
	 */
	@Getter
	@XmlElement(name = "parserModel")
	// Mirror of a model included with the latest CoreNLP models (as of 03-26-2020).
	// It is a shift-reduce parser, more efficient than the default, based on PCFG.
	// See: https://nlp.stanford.edu/software/srparser.html
	private String parserModel = "https://dl.dropboxusercontent.com/s/ltq27ez8lr4nwb4/englishSR.beam.ser.gz";

	/**
	 * The model to use for dependency parsing.
	 */
	@Getter
	@XmlElement(name = "dependencyParseModel")
	// Mirror of a model included with the latest CoreNLP models (as of 03-26-2020)
	private String dependencyParseModel = "https://dl.dropboxusercontent.com/s/jrrgezz7ng29i2i/english_wsj_UD.gz";

	/**
	 * Returns a {@link NaturalLanguageProcessingSettings} instance with the default
	 * settings, which need not be different from instances returned previously.
	 *
	 * @return The {@link NaturalLanguageProcessingSettings} instance with the
	 *         default settings.
	 */
	public static NaturalLanguageProcessingSettings defaultSettings() {
		return NaturalLanguageProcessingSettingsInstanceHolder.INSTANCE;
	}

	/**
	 * Holds a single {@link NaturalLanguageProcessingSettings} instance. Used for
	 * the initialization on demand holder singleton idiom.
	 *
	 * @author Alejandro González García
	 */
	private static final class NaturalLanguageProcessingSettingsInstanceHolder {
		private static final NaturalLanguageProcessingSettings INSTANCE = new NaturalLanguageProcessingSettings();
	}
}
