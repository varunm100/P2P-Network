/*
 * @author Varun on 6/17/2018
 * @project P2P-Network
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.Socket;
import java.time.Clock;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

class Peer {
    private Server server;
    static String Ipv4Local = null;
    static Map<String, Connection> connections;

    /**
     * Stores data that's shared between all threads and instances of the Peer class.
     */
    static class Shared {
        static AtomicReference<Map<String, ArrayList<Integer>>> callBackCounter = new AtomicReference<>(new HashMap<>());
        static ExecutorService threadManager = Executors.newCachedThreadPool();
        static volatile boolean running;
    }

    /**
     * Peer class.
     */
    Peer() {
        server = new Server();
        connections = new HashMap<>();
    }

    /**
     * Finds the ip address of current device.
     *
     * @return Ip address of current device.
     */
    private String getLocalIpv4() {
        if (Ipv4Local != null) return Peer.Ipv4Local;
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

    /**
     * Parses the config file.
     *
     * @param file Peer config file.
     * @return Metadata to identify and start the Peer.
     */
    private PeerData parseConfigFile(File file) {
        Map<String, Integer> adjPeers = new HashMap<>();
        int serverPort = -1;
        try {
            Main.scanner = new Scanner(file);
            String line;
            while (Main.scanner.hasNextLine()) {
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
        Main.scanner = new Scanner(System.in);
        if (adjPeers.isEmpty() || serverPort == -1)
            System.out.println("Error while parsing config file. (" + file.getPath() + ")");
        return new PeerData(adjPeers, serverPort);
    }

    /**
     * Starts the peer.
     *
     * @param configFile Peer config file.
     */
    void startPeer(File configFile) {
        Peer.Shared.running = true;
        PeerData peerData = parseConfigFile(configFile);
        Ipv4Local = getLocalIpv4();
        Map<String, Integer> adjPeerInfo = peerData.adjPeers;
        LinkedList<String> adjIP = new LinkedList<>(adjPeerInfo.keySet());
        LinkedList<Integer> adjPort = new LinkedList<>(adjPeerInfo.values());
        Future queryThread = Shared.threadManager.submit(() -> server.startServer(adjIP, peerData.serverPort));
        System.out.println("Press [ENTER] to start client querying. " + "(" + adjPeerInfo.size() + ")");
        Main.scanner.nextLine();
        for (int i = 0; i < adjPeerInfo.size(); i++) {
            connections.put(adjIP.get(i), new Connection());
            connections.get(adjIP.get(i)).establishConnection(adjIP.get(i), adjPort.get(i));
        }
        try {
            queryThread.wait(Long.MAX_VALUE);
        } catch (InterruptedException e) {
            System.out.println("Server querying thread was interrupted.");
            e.printStackTrace();
        }
        System.out.println("|| PEER SUCCESSFULLY INITIALIZED ||");
    }

    /**
     * Stops the peer.
     */
    void stop() {
        Peer.Shared.running = false;
        Peer.Ipv4Local = null;
        server.closeServer();
        for (Connection connection : connections.values())
            connection.disconnect();
        waitForAllThreads();
    }

    /**
     * Waits for all threads to complete.
     */
    private void waitForAllThreads() {
        Shared.threadManager.shutdown();
        try {
            Shared.threadManager.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            System.out.println("Error while waiting for threads to terminate.");
            e.printStackTrace();
            System.exit(-1);
        }
        System.exit(0);
    }

    /**
     * Sends an object to one of the adjacent peers.
     *
     * @param o    Object to be sent.
     * @param Ipv4 Ip address of destination peer.
     */
    static void sendObject(Object o, String Ipv4) {
        connections.get(Ipv4).sendObject(o);
    }

    /**
     * Sends an object to all immediate adjacent peers.
     *
     * @param o Object to be sent.
     */
    void sendToAdjPeers(Object o) {
        for (Connection connection : connections.values())
            connection.sendObject(o);
    }

    /**
     * Sends an object to all peers in the network.
     *
     * @param o Object to be sent.
     */
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

    /**
     * Used to increase the currentNumberOfCallbacks by 1.
     *
     * @param reference Reference to the callback Map.
     * @param key       Key of desired callback counter.
     * @param index     Index of callback ArrayList. Index 1 - Max Value of Callbacks : Index 2 - Current number of callbacks
     */
    static void updateCallbackCounter(AtomicReference<Map<String, ArrayList<Integer>>> reference, String key, int index) {
        Map<String, ArrayList<Integer>> before, after = new HashMap<>();
        do {
            before = reference.get();
            after.putAll(before);
            after.get(key).set(index, after.get(key).get(index) + 1);
        } while (!reference.compareAndSet(before, after));
    }

    /**
     * Initialize atomic counter to empty Table and ArrayList filled with 2 zeros. First index for Max callbacks and the second for current number of callbacks.
     *
     * @param reference Reference to callback Map.
     * @param key       Key of desired callback counter.
     */
    static void initCounterAtomically(AtomicReference<Map<String, ArrayList<Integer>>> reference, String key) {
        Map<String, ArrayList<Integer>> before, after = new HashMap<>();
        do {
            before = reference.get();
            after.putAll(before);
            after.put(key, new ArrayList<>(Collections.nCopies(2, 0)));
        } while (!reference.compareAndSet(before, after));
    }
}
