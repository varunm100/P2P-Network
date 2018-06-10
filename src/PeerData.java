import java.util.Map;

class PeerData {
    Map<String, Integer> adjPeers;
    int serverPort;
    PeerData(Map<String, Integer> _adjPeers, int _serverPort) {
        adjPeers = _adjPeers;
        serverPort = _serverPort;
    }
}
