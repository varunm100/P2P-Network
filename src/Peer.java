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
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.IntStream;

class Peer {
    private Server server = new Server();
    String Ipv4Local = null;
    private Map<String, Connection> connections = new HashMap<>();

    /**
     * Stores data that's shared between all threads and instances of the Peer class.
     */
    static class Shared {
        static AtomicReference<Map<String, ArrayList<Integer>>> callBackCounter = new AtomicReference<>();
        static AtomicReference<Map<String, Map<String, Boolean>>> pollingResults = new AtomicReference<>();
        static AtomicReference<Map<String, LinkedList<Object>>> listCallbacks = new AtomicReference<>();
        static volatile Map<String, Integer> numTraversalMessage = new HashMap<>();
        static ExecutorService threadManager = Executors.newCachedThreadPool();
        static volatile boolean running;
    }

    /**
     * Peer class.
     */
    Peer() {
    }

    /**
     * Finds the ip address of current device.
     *
     * @return Ip address of current device.
     */
    private String getLocalIpv4() {
        if (Ipv4Local != null) return Ipv4Local;
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
            System.out.println("Could not find a valid config file.");
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
        Consumer<Object> handleSocketInput = this::handleObjData;
        Ipv4Local = getLocalIpv4();

        PeerData peerData = parseConfigFile(configFile);
        Map<String, Integer> adjPeerInfo = new HashMap<>(peerData.adjPeers);
        LinkedList<String> adjIP = new LinkedList<>(adjPeerInfo.keySet());
        LinkedList<Integer> adjPort = new LinkedList<>(adjPeerInfo.values());
        Future queryThread = Shared.threadManager.submit(() -> server.startServer(adjIP, peerData.serverPort, handleSocketInput));

        System.out.println("Press [ENTER] to start client querying. " + "(" + adjPeerInfo.size() + ")");
        Main.scanner.nextLine();

        IntStream.range(0, adjPeerInfo.size()).forEach(i -> {
            connections.put(adjIP.get(i), new Connection());
            connections.get(adjIP.get(i)).establishConnection(adjIP.get(i), adjPort.get(i));
        });
        try {
            queryThread.get();
        } catch (InterruptedException e) {
            System.out.println("Server querying thread was interrupted.");
            e.printStackTrace();
        } catch (ExecutionException e) {
            System.out.println("Server failed to execute.");
            e.printStackTrace();
        }
        System.out.println("|| PEER SUCCESSFULLY INITIALIZED ||");
    }

    /**
     * Stops the peer.
     */
    void stop() {
        Peer.Shared.running = false;
        waitForAllThreads();
        server.closeServer();
        connections.values().forEach(Connection::disconnect);

        this.connections.clear();
        this.server = null;
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
    void sendObject(Object o, String Ipv4) {
        connections.get(Ipv4).sendObject(o);
    }

    /**
     * Sends an object to all immediate adjacent peers.
     *
     * @param o Object to be sent.
     */
    void sendToAdjPeers(Object o) {
        connections.values().forEach(connection -> connection.sendObject(o));
    }

    /**
     * Sends an object to all peers in the network.
     *
     * @param o Object to be sent.
     */
    void sendToAllPeers(Object o, LocalTime time) {
        TraversalObj traversalObj = new TraversalObj();
        traversalObj.data = o;
        traversalObj.visited = new LinkedList<>();
        traversalObj.globalSource = Ipv4Local;
        traversalObj.type = "FORWARD";
        traversalObj.callbackSubject = Ipv4Local;
        traversalObj.timeStamp = time;
        handleObjData(traversalObj);
    }

    /**
     * Used to increase the currentNumberOfCallbacks by 1.
     *  @param reference Reference to the callback Map.
     * @param key       Key of desired callback counter.
     */
    private void updateCallbackCounter(AtomicReference<Map<String, ArrayList<Integer>>> reference, String key) {
        Map<String, ArrayList<Integer>> before, after = new HashMap<>();
        do {
            before = reference.get();
            after.putAll(before);
            after.get(key).set(0, after.get(key).get(0) + 1);
        } while (!reference.compareAndSet(before, after));
    }

    /**
     * Initialize atomic counter to empty Table and ArrayList filled with 2 zeros. First index for Max callbacks and the second for current number of callbacks.
     *
     * @param reference Reference to callback Map.
     * @param key       Key of desired callback counter.
     */
    private void initCounterAtomically(AtomicReference<Map<String, ArrayList<Integer>>> reference, String key) {
        Map<String, ArrayList<Integer>> before, after = new HashMap<>();
        do {
            before = reference.get();
            after.putAll(before);
            after.put(key, new ArrayList<>(Collections.nCopies(2, 0)));
        } while (!reference.compareAndSet(before, after));
    }

    /**
     * Handles received data.
     *
     * @param o Object received.
     */
    private void handleObjData(Object o) {
        if (o instanceof SerializableText) {
            handleTextData((SerializableText) o);
        } else if (o instanceof TraversalObj) {
            recursiveTraversal((TraversalObj) o);
        } else if (o instanceof PollingMessage) {
            handlePollingData((PollingMessage) o);
        } else {
            System.out.println("Received an unrecognizable object.");
        }
    }

    private void handlePollingData(PollingMessage pollingObj) {
        if (pollingObj.pollResults.containsKey(Ipv4Local)) {
            System.out.println("||ERROR OCCURRED|| FOUND KEY TWICE IN POLLING MESSAGE");
            return;
        }
        pollingObj.pollResults.put(Ipv4Local, (new Random()).nextBoolean());

        /* TODO REMOVE THE BLOCK BELOW */
        System.out.println("|| POLLING MAP ||");
        pollingObj.pollResults.forEach((key, value) -> System.out.println(key + " : " + value));
        System.out.println("-----------------");
    }

    Map<String, Boolean> startPollingMessage() {
        LocalTime time = LocalTime.now(Clock.systemUTC());
        sendToAllPeers(new PollingMessage(), time);
        return new HashMap<>(Shared.pollingResults.get().get(time.toString()));
    }

    /**
     * Implementation of a peer traversal algorithm.
     *
     * @param traversalObj An object which contains all required data to continue recursion.
     */
    private void recursiveTraversal(TraversalObj traversalObj) {
        if (checkBaseCase(traversalObj)) return;
        handleObjData(traversalObj.data);
        traversalObj.visited.add(Ipv4Local);
        String timeStamp = traversalObj.timeStamp.toString();

        initCounterAtomically(Peer.Shared.callBackCounter, timeStamp);
        Shared.numTraversalMessage.put(timeStamp, 0);

        TraversalObj sendingData = new TraversalObj();
        sendingData.equals(traversalObj);
        sendingData.callbackSubject = Ipv4Local;

        connections.values().stream().filter(connection -> !traversalObj.visited.contains(connection.ip)).forEach(connection -> {
            updateCallbackCounter(Shared.callBackCounter, timeStamp);
            Shared.numTraversalMessage.put(timeStamp, Shared.numTraversalMessage.get(timeStamp) + 1);
            Shared.threadManager.submit(() -> sendObject(sendingData, connection.ip));
        });

        waitForCallbacks(timeStamp);
        if (traversalObj.globalSource.equals(Ipv4Local)) {
            System.out.println("CONFIRMATION: DATA REACHED ALL NODES");
            System.out.println("NUMBER OF MESSAGES SENT: " + Shared.numTraversalMessage.get(timeStamp));
            if (traversalObj.data instanceof PollingMessage) {
                atomicallyUpdatePollResults(Shared.pollingResults, timeStamp, Shared.listCallbacks.get().get(timeStamp));
            }
            return;
        }
        traversalObj.type = "CALLBACK";
        sendObject(traversalObj, traversalObj.callbackSubject);
        Shared.numTraversalMessage.put(timeStamp, Shared.numTraversalMessage.get(timeStamp) + 1);
    }

    /**
     * Waits to receive all callbacks from adjacent peers.
     *
     * @param timeStamp The time at which the parent node started search. (Used to keep track of which messages was already received)
     */
    private void waitForCallbacks(String timeStamp) {
        System.out.println("Expected Callbacks: " + Peer.Shared.callBackCounter.get().get(timeStamp).get(0));
        Map<String, ArrayList<Integer>> tempCount;
        do {
            tempCount = Peer.Shared.callBackCounter.get();
        } while (tempCount.get(timeStamp).get(1) < tempCount.get(timeStamp).get(0));
        System.out.println("GOT ALL CALLBACKS! " + Peer.Shared.callBackCounter.get().get(timeStamp).get(1));
    }

    /**
     * Used to check base case for recursion.
     *
     * @param data An object which contains all required data to continue recursion.
     * @return Returns whether the Peer should continue recursion.
     */
    private boolean checkBaseCase(TraversalObj data) {
        if (Peer.Shared.callBackCounter.get().containsKey(data.timeStamp.toString())) {
            if (data.type.startsWith("CALLBACK")) {
/*
                if (data.type.equals("CALLBACK")) {
                }
*/
                atomicallyUpdateCallbackList(Shared.listCallbacks, data.timeStamp.toString(), data.data);
                return true;
            }
            data.visited.add(Ipv4Local);
            data.type = "CALLBACKINVALID";
            sendObject(data, data.callbackSubject);
            Shared.numTraversalMessage.put(data.timeStamp.toString(), Shared.numTraversalMessage.get(data.timeStamp.toString()) + 1);
            return true;
        }
        return false;
    }

    /**
     * Handles received text data. (Logs the text message)
     *
     * @param text The text received.
     */
    
    private void handleTextData(SerializableText text) {
        System.out.println(text.text + " (" + text.timeStamp.toString() + ")" + "(" + text.source + ")");
    }

    private void atomicallyUpdateCallbackList(AtomicReference<Map<String, LinkedList<Object>>> reference, String key, Object o) {
        Map<String, LinkedList<Object>> before, after = new HashMap<>();
        do {
            before = reference.get();
            after.putAll(before);
            if (after.containsKey(key)) {
                after.get(key).add(o);
            } else {
                after.put(key, new LinkedList<>(Collections.nCopies(1, o)));
            }
        } while (!reference.compareAndSet(before, after));
    }

    private void atomicallyUpdatePollResults(AtomicReference<Map<String, Map<String, Boolean>>> reference, String key, LinkedList<Object> newValue) {
        Map<String, Map<String, Boolean>> before, after = new HashMap<>();
        if (newValue.isEmpty()) {
            return;
        } else {
            System.out.println("New Value Is Empty");
        }
        if (newValue.get(0) instanceof TraversalObj) {
            do {
                before = reference.get();
                after.putAll(before);
                for (Object newEntry : newValue) {
                    after.put(key, ((PollingMessage) ((TraversalObj) newEntry).data).pollResults);
                }
            } while (!reference.compareAndSet(before, after));
        } else {
            System.out.println("Could not update poll results.");
        }
    }
}
