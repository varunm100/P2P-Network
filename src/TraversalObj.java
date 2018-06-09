import java.io.Serializable;
import java.time.LocalTime;
import java.util.LinkedList;

public class TraversalObj implements Serializable {
    LocalTime timeStamp;
    String type;
    String globalSource;
    Object data;
    LinkedList<String> visited = new LinkedList<>();
    String callbackSubject;
    public TraversalObj() {  }
}
