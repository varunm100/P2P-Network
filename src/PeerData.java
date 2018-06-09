import java.util.Map;

class PeerData {
    Map<String, Integer> adjPeers;
    String Ipv4;
    int serverPort;
    PeerData(Map<String, Integer> _adjPeers, String _Ipv4, int _serverPort) {
        adjPeers = _adjPeers;
        Ipv4 = _Ipv4;
        serverPort = _serverPort;
    }
}
