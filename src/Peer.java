import java.time.Clock;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class Peer {
    Server server;
    static String Ipv4Local;
    static Map<String, Connection> connections;

    public Peer() {
        server = new Server();
        connections = new HashMap<>();
    }

    public void startPeer(Map<String, Integer> adjPeerInfo, int serverPortId, String _Ipv4Local) {
        Ipv4Local = _Ipv4Local;
        LinkedList<String> adjIP = new LinkedList<>(adjPeerInfo.keySet());
        LinkedList<Integer> adjPort = new LinkedList<>(adjPeerInfo.values());

        Thread query = new Thread(() -> server.startServer(adjIP, serverPortId));
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

    public void cleanUp() {
        server.closeServer();
        for (Connection connection : connections.values())
            connection.disconnect();
    }

    public static void sendObject(Object o, String Ipv4) {
        connections.get(Ipv4).sendObject(o);
    }

    public void sendToAllPeers(Object o) {
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
