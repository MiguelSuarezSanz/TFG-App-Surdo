package gestiones_tunnel;

public final class Protocol {

    // =========================
    // CONEXIÓN / USUARIO
    // =========================

    public static final byte MSG_SET_NAME = 0x01;
    public static final byte MSG_NAME_ERROR = 0x02;
    public static final byte MSG_NAME_OK = 0x03;

    // =========================
    // MINIJUEGOS
    // =========================

    // Cliente -> servidor
    public static final byte MSG_MINIGAME_COUNT = 0x04;

    // Servidor -> clientes
    public static final byte MSG_PLAY_MINIGAME = 0x05;

    // Cliente -> servidor
    public static final byte MSG_REQUEST_MINIGAME_STATEMENT = 0x06;

 
    public static final byte MSG_PREP_MINIGAME = 0x07;
    
    public static final byte MSG_MINIGAME_RESULT = 0x08;
    public static final byte MSG_SCORE_UPDATE = 0x09;

    private Protocol() {}
}