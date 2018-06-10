import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.Socket;
import java.time.Clock;
import java.time.LocalTime;
import java.util.*;

class Peer {
    private Server server;
    static String Ipv4Local;
    static Map<String, Connection> connections;
    static class Shared {
        static volatile Map<String, ArrayList<Integer>> callBackCounter = new HashMap<>();
        static volatile boolean running;
    }

    Peer() {
        server = new Server();
        connections = new HashMap<>();
    }

    private String getLocalIpv4() {
        String output = null;
        try {
            Socket socket = new Socket("192.168.1.1", 80); //could also use port 433
            output = socket.getLocalAddress().getHostAddress();
            socket.close();
        } catch (IOException e) {
            System.out.println("Could not detect ip of device.");
            e.printStackTrace();
        }
        return output;
    }

    private PeerData parseConfigFile(File _file) {
        Map<String, Integer> adjPeers = new HashMap<>();
        int serverPort = -1;
        try {
            Main.scanner = new Scanner(_file);
            String line;
            while(Main.scanner.hasNextLine()) {
                line = Main.scanner.nextLine();
                if (line.startsWith("adjPeer:")) {
                    adjPeers.put(line.split(":")[1], Integer.valueOf(line.split(":")[2]));
                } else if (line.startsWith("server:")) {
                    serverPort = Integer.valueOf(line.split(":")[1]);
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("Could not find inputted config file.");
            e.printStackTrace();
        }
        if (adjPeers.isEmpty() || serverPort == -1)
            System.exit(0);
        Main.scanner = new Scanner(System.in);
        return new PeerData(adjPeers, serverPort);
    }

    void startPeer(File _configFile) {
        Peer.Shared.running = true;
        PeerData peerData = parseConfigFile(_configFile);
        Ipv4Local = getLocalIpv4();
        Map<String, Integer> adjPeerInfo = peerData.adjPeers;
        LinkedList<String> adjIP = new LinkedList<>(adjPeerInfo.keySet());
        LinkedList<Integer> adjPort = new LinkedList<>(adjPeerInfo.values());
        Thread query = new Thread(() -> server.startServer(adjIP, peerData.serverPort));
        query.start();
        System.out.println("Press [ENTER] to start client querying. " + "(" + adjPeerInfo.size() + ")");
        Main.scanner.nextLine();
        for (int i = 0; i < adjPeerInfo.size(); i++) {
            connections.put(adjIP.get(i),new Connection());
            connections.get(adjIP.get(i)).establishConnection(adjIP.get(i), adjPort.get(i));
        }
        try {
            query.join();
        } catch (InterruptedException e) {
            System.out.println("Client querying thread was interrupted.");
            e.printStackTrace();
        }
        System.out.println("|| PEER SUCCESSFULLY INITIALIZED ||");
    }

    void stop() {
        Peer.Shared.running = false;
        server.closeServer();
        for (Connection connection : connections.values())
            connection.disconnect();
    }

    static void sendObject(Object _o, String _Ipv4) {
        connections.get(_Ipv4).sendObject(_o);
    }

    void sendToAllPeers(Object _o) {
        TraversalObj traversalObj = new TraversalObj();
        traversalObj.data = _o;
        traversalObj.visited = new LinkedList<>();
        traversalObj.globalSource = Peer.Ipv4Local;
        traversalObj.type = "FORWARD";
        traversalObj.callbackSubject = Peer.Ipv4Local;
        traversalObj.timeStamp = LocalTime.now(Clock.systemUTC());
        server.handleObjData(traversalObj);
    }
}
