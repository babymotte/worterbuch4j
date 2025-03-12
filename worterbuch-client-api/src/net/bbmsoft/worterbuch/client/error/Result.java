package net.bbmsoft.worterbuch.client.error;

import net.bbmsoft.worterbuch.client.model.Err;

public sealed interface Result<T> permits Ok, Error {

	boolean isOk();

	T get();

	Err err();

}
