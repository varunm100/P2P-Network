import java.io.Serializable;
import java.time.LocalTime;
import java.util.LinkedList;

public class TraversalObj implements Serializable {
    private static final long serialVersionUID = 1L;
    LocalTime timeStamp;
    String type;
    String globalSource;
    Object data;
    LinkedList<String> visited = new LinkedList<>();
    String callbackSubject;
    public TraversalObj() {  }
}
