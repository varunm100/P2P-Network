/*
 * @author Varun on 6/17/2018
 * @project P2P-Network
 */

import java.io.Serializable;
import java.time.Instant;
import java.util.LinkedList;

class TraversalObj implements Serializable {
    private static final long serialVersionUID = -4942040386018202272L;

    LinkedList<String> visited = new LinkedList<>();
    String globalSource;
    String callbackSubject;
    String type;
    Instant timeStamp;
    Object data;

    /**
     * Helper class for sending data to all peers in the network recursively.
     */
    TraversalObj() {
    }

    /**
     * Copies contents from one obj to another obj.
     *
     * @param o Object to equal.
     */
    void equals(TraversalObj o) {
        this.timeStamp = o.timeStamp;
        this.data = o.data;
        this.globalSource = o.globalSource;
        this.type = o.type;
        this.visited = o.visited;
        this.callbackSubject = o.callbackSubject;
    }
}
