package message;

public class Constant {
    // constant values used in ActualMessage.java
    public static final int SHORT_INTERVAL = 100;
    public static final int MINIMUM_SIZE = 5;

    // constant values used in HandshakeMessage.java
    //public static final int SHORT_INTERVAL = 100;
    public static final String HANDSHAKER_HEADER = "P2PFILESHARINGPROJ";
    public static final byte[] ZERO_BITS = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};    //10-byte zero bits
    public static final int HANDSHAKER_HEADER_LENGTH = 18;
    public static final int HANDSHAKER_LENGTH = 32;

    // constant values used in BitField.java
    public static final int BYTE_SIZE = 8;
    public static final byte MAX_BYTE = -1;
    public static final byte MIN_BYTE = 0;
}
