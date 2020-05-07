// SPDX-License-Identifier: AGPL-3.0-or-later

package es.uvigo.esei.sing.vacbot.settings;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;

/**
 * Contains all the settings related to the behavior of VacBot, which affects
 * response generation.
 *
 * @author Alejandro González García
 * @see VacBotSettings
 */
@XmlRootElement(name = "behavior")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@ToString
public final class BehaviorSettings {
	/**
	 * The bias towards vaccination topics expressed by the bot, achieved by trying
	 * to select only responses that manifest that sentiment category towards the
	 * topic.
	 */
	@Getter @NonNull
	@XmlElement(required = true)
	private final ResponseBias responseBias = null;
}
