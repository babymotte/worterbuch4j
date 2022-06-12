package net.bbmsoft.worterbuch.tcp.client.error;

public class UndefinedMessageType extends DecodeException {

	private static final long serialVersionUID = -1201831148561288053L;

	public final int typeByte;

	public UndefinedMessageType(final int typeByte) {
		this.typeByte = typeByte;
	}

}
