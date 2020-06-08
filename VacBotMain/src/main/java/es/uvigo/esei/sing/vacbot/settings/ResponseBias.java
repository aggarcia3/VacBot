// SPDX-License-Identifier: AGPL-3.0-or-later

package es.uvigo.esei.sing.vacbot.settings;

import java.util.concurrent.TimeUnit;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlEnumValue;
import lombok.NonNull;

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
	@XmlEnumValue("positive") POSITIVE {
		private final LoadingCache<String, String> expressionCache = createCache(
			new CacheLoader<>() {
				@Override
				public String load(final String variableName) throws Exception {
					final StringBuilder sb = new StringBuilder();

					sb.append("?")
						.append(variableName)
						.append(" = \"Positive\" || ?")
						.append(variableName)
						.append(" = \"Very positive\"");

					return sb.toString();
				}
			}
		);

		@Override
		public String toSparqlConditionExpression(@NonNull final String variableName) {
			return expressionCache.getUnchecked(variableName);
		}
	},
	/**
	 * The bot will try to select responses that show a negative sentiment towards
	 * vaccines.
	 */
	@XmlEnumValue("negative") NEGATIVE {
		private final LoadingCache<String, String> expressionCache = createCache(
			new CacheLoader<>() {
				@Override
				public String load(final String variableName) throws Exception {
					final StringBuilder sb = new StringBuilder();

					sb.append("?")
						.append(variableName)
						.append(" = \"Negative\" || ?")
						.append(variableName)
						.append(" = \"Very negative\"");

					return sb.toString();
				}
			}
		);

		@Override
		public String toSparqlConditionExpression(@NonNull final String variableName) {
			return expressionCache.getUnchecked(variableName);
		}
	},
	/**
	 * The bot will try to select responses that do not show a positive or negative
	 * sentiment towards vaccines.
	 */
	@XmlEnumValue("neutral") NEUTRAL {
		private final LoadingCache<String, String> expressionCache = createCache(
			new CacheLoader<>() {
				@Override
				public String load(final String variableName) throws Exception {
					final StringBuilder sb = new StringBuilder();

					sb.append("?")
						.append(variableName)
						.append(" = \"Neutral\"");

					return sb.toString();
				}
			}
		);

		@Override
		public String toSparqlConditionExpression(@NonNull final String variableName) {
			return expressionCache.toString();
		}
	},
	/**
	 * The bot will not take into account the sentiment expressed towards vaccines
	 * to select responses.
	 */
	@XmlEnumValue("impartial") IMPARTIAL {
		private final LoadingCache<String, String> expressionCache = createCache(
			new CacheLoader<>() {
				@Override
				public String load(final String variableName) throws Exception {
					final StringBuilder sb = new StringBuilder();

					sb.append("?")
						.append(variableName)
						.append(" = \"Positive\" || ?")
						.append(variableName)
						.append(" = \"Very positive\" || ?")
						.append(variableName)
						.append(" = \"Neutral\" || ?")
						.append(variableName)
						.append(" = \"Negative\" || ?")
						.append(variableName)
						.append(" = \"Very negative\"");

					return sb.toString();
				}
			}
		);

		@Override
		public String toSparqlConditionExpression(@NonNull final String variableName) {
			return expressionCache.getUnchecked(variableName);
		}
	};

	/**
	 * Converts this response bias setting to a SPARQL expression that evaluates to
	 * true when a SPARQL variable value matches the sentiment classes associated to
	 * this setting.
	 *
	 * @param variableName The variable name that will be used in the expression.
	 * @return The expression text.
	 * @throws IllegalArgumentException If {@code variableName} is {@code null}.
	 */
	public abstract String toSparqlConditionExpression(final String variableName);

	/**
	 * Creates a cache for an enum constant.
	 *
	 * @param <T>         The type of the cache key.
	 * @param <U>         The type of the cache value.
	 * @param cacheLoader The cache loader that will provide the values that will be
	 *                    cached.
	 * @return The created {@link LoadingCache}.
	 * @throws IllegalArgumentException If the parameter is {@code null}.
	 */
	private static <T, U> LoadingCache<T, U> createCache(
		@NonNull final CacheLoader<T, U> cacheLoader
	) {
		return CacheBuilder.newBuilder()
			.expireAfterAccess(5, TimeUnit.MINUTES)
			.maximumSize(15)
			.build(cacheLoader);
	}
}
