import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author varun on 7/14/2018
 * @project P2P-Network
 */
public class PollingData implements Serializable {
    private static final long serialVersionUID = -439304458575695265L;
    Map<String, Boolean> pollingResults = new HashMap<>();
    public PollingData() {
    }
    public PollingData(Map<String, Boolean> results) {
        pollingResults = results;
    }
    void addEntry(String ip, Boolean verdict) {
        pollingResults.put(ip,verdict);
    }
    void merge(PollingData other) {
        pollingResults.putAll(other.pollingResults);
    }
}
