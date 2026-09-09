package net.bbmsoft.worterbuch.client.api;

/**
 * WB protocol error codes sent by the server
 */
public interface ErrorCode {

	public static final int IllegalWildcard = 0;
	public static final int IllegalMultiWildcard = 1;
	public static final int MultiWildcardAtIllegalPosition = 2;
	public static final int IoError = 3;
	public static final int SerdeError = 4;
	public static final int NoSuchValue = 5;
	public static final int NotSubscribed = 6;
	public static final int ProtocolNegotiationFailed = 7;
	public static final int InvalidServerResponse = 8;
	public static final int ReadOnlyKey = 9;
	public static final int AuthorizationFailed = 10;
	public static final int AuthorizationRequired = 11;
	public static final int AlreadyAuthorized = 12;
	public static final int MissingValue = 13;
	public static final int Unauthorized = 14;
	public static final int NoPubStream = 15;
	public static final int NotLeader = 16;
	public static final int Cas = 17;
	public static final int CasVersionMismatch = 18;
	public static final int NotImplemented = 19;
	public static final int KeyIsLocked = 20;
	public static final int KeyIsNotLocked = 21;
	public static final int LockAcquisitionCancelled = 22;
	public static final int FeatureDisabled = 23;
	public static final int ClientIDCollision = 24;
	public static final int EmptyKey = 25;
	public static final int LockLost = 26;
	public static final int Other = 255;
}
