// SPDX-License-Identifier: AGPL-3.0-or-later

package es.uvigo.esei.sing.vacbot.frontend;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

/**
 * Represents a text message that can be sent or received by any front-end
 * interface. Concrete front-end interfaces must provide implementations of this
 * class that allow accessing more metadata about the message, if any.
 * <p>
 * It is recommended that subclasses of this class override the
 * {@link #toString()}, {@link #equals(Object)} and {@link #hashCode()} methods.
 *
 * @author Alejandro González García
 */
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@ToString
@EqualsAndHashCode
public abstract class TextMessage {
	@NonNull @Getter
	private final String text;
}
