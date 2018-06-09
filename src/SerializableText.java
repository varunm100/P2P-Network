import java.io.Serializable;
import java.time.Clock;
import java.time.LocalTime;

public class SerializableText implements Serializable {
    String source;
    LocalTime timeStamp;
    String text;
    public SerializableText(String _text, String _source) {
        text = _text;
        source = _source;
        timeStamp = LocalTime.now(Clock.systemUTC());
    }
}
