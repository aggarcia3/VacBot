// SPDX-License-Identifier: AGPL-3.0-or-later

package es.uvigo.esei.sing.vacbot.settings;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlEnumValue;

/**
 * Represents the bias shown by the bot in its responses towards vaccination
 * topics.
 *
 * @author Alejandro González García
 */
@XmlEnum(String.class)
public enum ResponseBias {
	/**
	 * The bot will try to select responses that show a positive sentiment towards
	 * vaccines.
	 */
	@XmlEnumValue("positive") POSITIVE,
	/**
	 * The bot will try to select responses that show a negative sentiment towards
	 * vaccines.
	 */
	@XmlEnumValue("negative") NEGATIVE,
	/**
	 * The bot will try to select responses that do not show a positive or negative
	 * sentiment towards vaccines.
	 */
	@XmlEnumValue("neutral") NEUTRAL,
	/**
	 * The bot will not take into account the sentiment expressed towards vaccines
	 * to select responses.
	 */
	@XmlEnumValue("impartial") IMPARTIAL;
}
