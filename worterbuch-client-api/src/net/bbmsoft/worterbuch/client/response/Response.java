package net.bbmsoft.worterbuch.client.response;

import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.function.Supplier;

import net.bbmsoft.worterbuch.client.model.Err;

/**
 * Type that represents a server response to a query to a worterbuch server. In
 * the success case, this will be {@link Ok}, if the server responded with an
 * error it will be {@link Error}.
 * <p/>
 * Note that this represents ONLY the actual server response, if the response
 * could not be received, for example due to a network error or a timeout, that
 * case will be handled by the future that produces this result object.
 *
 * @param <T> parametric type of the request's result in case of success
 */
public sealed interface Response<T> permits Ok, Error {

	/**
	 * Checks whether this represents a success response or not.
	 *
	 * @return {@code true} if this is an instance of {@link Ok} or {@code false} if
	 *         this is an instance of {@link Error}
	 */
	default boolean isOk() {
		return this instanceof Ok;
	}

	/**
	 * Returns the request's result if it was a success or throws a
	 * {@link NoSuchElementException} if it wasn't.
	 *
	 * @return the request's result if it was a success
	 * @throws NoSuchElementException if the operation failed
	 */
	T value() throws NoSuchElementException;

	/**
	 * Returns the error produced by the server if the request failed or throws a
	 * {@link NoSuchElementException} if it didn't.
	 *
	 * @return the error produced by the server if the request failed
	 * @throws NoSuchElementException if the request succeeded
	 */
	Err err() throws NoSuchElementException;

	default void ifOk(final Consumer<T> onSuccess) {
		if (this instanceof final Ok<T> ok) {
			onSuccess.accept(ok.value());
		}
	}

	default void ifOkOrElse(final Consumer<T> onSuccess, final Consumer<Err> onError) {
		if (this instanceof final Ok<T> ok) {
			onSuccess.accept(ok.value());
		} else if (this instanceof final Error<T> err) {
			onError.accept(err.err());
		} else {
			throw new IllegalStateException("can only be Ok or Error");
		}
	}

	default T or(final T fallback) {
		if (this.isOk()) {
			return this.value();
		} else {
			return fallback;
		}
	}

	default T orElse(final Supplier<T> fallback) {
		if (this.isOk()) {
			return this.value();
		} else {
			return fallback.get();
		}
	}

}
