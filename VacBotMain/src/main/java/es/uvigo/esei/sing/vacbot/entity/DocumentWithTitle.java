// SPDX-License-Identifier: AGPL-3.0-or-later

package es.uvigo.esei.sing.vacbot.entity;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * Represents a text document which has a title.
 *
 * @author Alejandro González García
 * @implNote The implementation of this class is not thread-safe.
 */
@MappedSuperclass
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class DocumentWithTitle extends Document {
	@Getter @NonNull @Column(nullable = false)
	private String title;

	@Override
	public String toString() {
		final StringBuilder stringBuilder = new StringBuilder(super.toString());

		stringBuilder
			.append('\n').append("Title: ").append(title);

		return stringBuilder.toString();
	}
}
