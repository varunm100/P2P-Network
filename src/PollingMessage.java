import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author varun on 6/22/2018
 * @project P2P-Network
 */
public class PollingMessage implements Serializable {
    private static final long serialVersionUID = -4223473385165167047L;

    Map<String, Boolean> pollResults = new HashMap<>();

    public PollingMessage() { }
}
