import java.io.Serializable;
import java.time.LocalTime;
import java.util.LinkedList;

class TraversalObj implements Serializable {
    private static final long serialVersionUID = 1L;
    LocalTime timeStamp;
    String type;
    String globalSource;
    Object data;
    LinkedList<String> visited = new LinkedList<>();
    String callbackSubject;

    /**
     * Helper class for sending data to all peers in the network recursively.
     */
    TraversalObj() { }

    void equals(TraversalObj o) {
        this.timeStamp = o.timeStamp;
        this.data = o.data;
        this.globalSource = o.globalSource;
        this.type = o.type;
        this.visited = o.visited;
        this.callbackSubject = o.callbackSubject;
    }
}
