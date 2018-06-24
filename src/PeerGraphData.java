import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * @author varun on 6/23/2018
 * @project P2P-Network
 */
public class PeerGraphData implements Serializable {
    private static final long serialVersionUID = -5395214196729013092L;

    Map<String, LinkedList<String>> localGraphData = new HashMap<>();

    void Combine(LinkedList<PeerGraphData> structureData) {
        structureData.forEach(data -> localGraphData.putAll(data.localGraphData));
    }

    PeerGraphData() { }
}
