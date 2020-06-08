// SPDX-License-Identifier: AGPL-3.0-or-later

package es.uvigo.esei.sing.vacbot.entity;

import javax.persistence.Entity;
import javax.persistence.Table;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * The text-only original text document, unprocessed.
 *
 * @author Alejandro González García
 */
@Entity
@Table(name = "text_document")
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public final class OriginalDocument extends Document {}
