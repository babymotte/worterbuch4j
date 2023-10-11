package net.bbmsoft.worterbuch.client;

import java.io.IOException;

public interface ClientSocket extends AutoCloseable {

	void sendString(String json) throws IOException;

}
