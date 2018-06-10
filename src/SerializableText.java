import java.io.Serializable;
import java.time.Clock;
import java.time.LocalTime;

class SerializableText implements Serializable {
    private static final long serialVersionUID = 1L;
    String source;
    LocalTime timeStamp;
    String text;

    /**
     * Object used to send String data.
     * @param _text String data.
     * @param _source IP address of source peer.
     */
    SerializableText(String _text, String _source) {
        text = _text;
        source = _source;
        timeStamp = LocalTime.now(Clock.systemUTC());
    }
}
