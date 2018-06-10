import java.util.Map;

class PeerData {
    Map<String, Integer> adjPeers;
    int serverPort;

    /**
     * Holds all the required metadata to start a peer.
     * @param _adjPeers List of adjacent peers to connect to. Map IP address, Port Number
     * @param _serverPort The port number at which the serverSocket will start at.
     */
    PeerData(Map<String, Integer> _adjPeers, int _serverPort) {
        adjPeers = _adjPeers;
        serverPort = _serverPort;
    }
}
