import java.time.Clock;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

class Peer {
    private Server server;
    static String Ipv4Local;
    static Map<String, Connection> connections;

    Peer() {
        server = new Server();
        connections = new HashMap<>();
    }

    void startPeer(PeerData _data) {
        Ipv4Local = _data.Ipv4;
        Map<String, Integer> adjPeerInfo = _data.adjPeers;
        LinkedList<String> adjIP = new LinkedList<>(adjPeerInfo.keySet());
        LinkedList<Integer> adjPort = new LinkedList<>(adjPeerInfo.values());

        Thread query = new Thread(() -> server.startServer(adjIP, _data.serverPort));
        query.start();
        System.out.println("Press [ENTER] to start client querying.");
        Main.scanner.nextLine();
        for (int i = 0; i < adjPeerInfo.size(); i++) {
            connections.put(adjIP.get(i),new Connection());
            connections.get(adjIP.get(i)).establishConnection(adjIP.get(i), adjPort.get(i));
        }
        try {
            query.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("|| PEER SUCCESSFULLY INITIALIZED ||");
    }

    void cleanUp() {
        server.closeServer();
        for (Connection connection : connections.values())
            connection.disconnect();
    }

    static void sendObject(Object o, String Ipv4) {
        connections.get(Ipv4).sendObject(o);
    }

    void sendToAllPeers(Object o) {
        TraversalObj traversalObj = new TraversalObj();
        traversalObj.data = o;
        traversalObj.visited = new LinkedList<>();
        traversalObj.globalSource = Peer.Ipv4Local;
        traversalObj.type = "FORWARD";
        traversalObj.callbackSubject = Peer.Ipv4Local;
        traversalObj.timeStamp = LocalTime.now(Clock.systemUTC());
        server.handleObjData(traversalObj);
    }
}
