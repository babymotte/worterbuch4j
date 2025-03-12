package net.bbmsoft.worterbuch.client.api;

public interface ErrorCode {

	public static final int IllegalWildcard = 0b00000000;
	public static final int IllegalMultiWildcard = 0b00000001;
	public static final int MultiWildcardAtIllegalPosition = 0b00000010;
	public static final int IoError = 0b00000011;
	public static final int SerdeError = 0b00000100;
	public static final int NoSuchValue = 0b00000101;
	public static final int NotSubscribed = 0b00000110;
	public static final int ProtocolNegotiationFailed = 0b00000111;
	public static final int InvalidServerResponse = 0b00001000;
	public static final int ReadOnlyKey = 0b00001001;
	public static final int AuthorizationFailed = 0b00001010;
	public static final int AuthorizationRequired = 0b00001011;
	public static final int AlreadyAuthorized = 0b00001100;
	public static final int MissingValue = 0b00001101;
	public static final int Unauthorized = 0b00001110;
	public static final int NoPubStream = 0b00001111;
	public static final int NotLeader = 0b00010000;
	public static final int Cas = 0b00010001;
	public static final int CasVersionMismatch = 0b00010010;
	public static final int NotImplemented = 0b00010011;
	public static final int KeyIsLocked = 0b00010100;
	public static final int KeyIsNotLocked = 0b00010101;
	public static final int Other = 0b11111111;
}
