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
    private Server server;
    String Ipv4Local = null;
    private Map<String, Connection> connections = new HashMap<>();

    /**
     * Stores data that's shared between all threads and instances of the Peer class.
     */
    static class Shared {
        static AtomicReference<Map<String, ArrayList<Integer>>> callBackCounter = new AtomicReference<>(new HashMap<>());
        static AtomicReference<Map<String, LinkedList<Object>>> callBackData = new AtomicReference<>(new HashMap<>());
        static ExecutorService threadManager = Executors.newCachedThreadPool();
        static volatile int numMessagesCount = 0;
        static volatile boolean running;
    }

    /**
     * Peer class.
     */
    Peer() {
        server = new Server();
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
    void sendToAllPeers(Object o) {
        TraversalObj traversalObj = new TraversalObj();
        traversalObj.data = o;
        traversalObj.visited = new LinkedList<>();
        traversalObj.globalSource = Ipv4Local;
        traversalObj.type = "FORWARD";
        traversalObj.callbackSubject = Ipv4Local;
        traversalObj.timeStamp = LocalTime.now(Clock.systemUTC());
        handleObjData(traversalObj);
    }

    /**
     * Used to increase the currentNumberOfCallbacks by 1.
     *
     * @param reference Reference to the callback Map.
     * @param key       Key of desired callback counter.
     * @param index     Index of callback ArrayList. Index 1 - Max Value of Callbacks : Index 2 - Current number of callbacks
     */
    private void updateCallbackCounter(AtomicReference<Map<String, ArrayList<Integer>>> reference, String key, int index) {
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
        } else if (o instanceof NCount) {
            System.out.println("This is the " + ((NCount) o).maxCount + "th adjacent node");
        } else {
            System.out.println("Received an unrecognizable object.");
        }
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

        TraversalObj sendingData = new TraversalObj();
        sendingData.equals(traversalObj);
        sendingData.callbackSubject = Ipv4Local;
        /* UPDATE RECEIVED DATA */
        if (traversalObj.data instanceof NCount) {
            NCount dataRec = (NCount) sendingData.data;
            if (dataRec.count >= dataRec.maxCount) {
                traversalObj.type = "CALLBACK";
                sendObject(traversalObj, traversalObj.callbackSubject);
                Shared.numMessagesCount += 1;
                System.out.println("Sent " + Shared.numMessagesCount + " messages to send data.");
                Shared.numMessagesCount = 0;
                handleObjData(((NCount) traversalObj.data).object);
                return;
            } else {
                sendingData.data = new NCount(dataRec.count + 1, dataRec.maxCount);
            }
        }

        connections.values().stream()
                            .filter(connection -> !traversalObj.visited.contains(connection.ip))
                            .forEach(connection -> {
                                updateCallbackCounter(Shared.callBackCounter, timeStamp, 0);
                                Shared.threadManager.submit(() -> sendObject(sendingData, connection.ip));
                                Shared.numMessagesCount += 1;
                            });

        waitForCallbacks(timeStamp);
        if (traversalObj.globalSource.equals(Ipv4Local)) {
            System.out.println("Sent " + Shared.numMessagesCount + " messages to send data.");
            System.out.println("CONFIRMATION: DATA REACHED ALL NODES");
            return;
        }
        traversalObj.type = "CALLBACK";
        sendObject(traversalObj, traversalObj.callbackSubject);
        Shared.numMessagesCount += 1;
        System.out.println("Sent " + Shared.numMessagesCount + " messages to send data.");
        Shared.numMessagesCount = 0;
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
                updateCallbackCounter(Peer.Shared.callBackCounter, data.timeStamp.toString(), 1);
                updateCallback(Shared.callBackData, data.data, data.timeStamp.toString());
                Shared.numMessagesCount += 1;
                return true;
            }
            data.visited.add(Ipv4Local);
            data.type = "CALLBACKINVALID";
            sendObject(data, data.callbackSubject);
            Shared.numMessagesCount += 1;
            return true;
        }
        return false;
    }

    /**
     * Handles received text data. (Log the message)
     *
     * @param text The text received.
     */
    private void handleTextData(SerializableText text) {
        System.out.println(text.text + " (" + text.timeStamp.toString() + ")" + "(" + text.source + ")");
    }

    /**
     * @param reference AtomicReference to callback list.
     * @param newObj new object to be added in the callback list.
     * @param key time stamp of message.
     */
    private void updateCallback(AtomicReference<Map<String, LinkedList<Object>>> reference, Object newObj, String key) {
        Map<String, LinkedList<Object>> before, after = new HashMap<>();
        do {
            before = reference.get();
            after.putAll(before);
            after.get(key).add(newObj);
        } while (!reference.compareAndSet(before, after));
    }

    void sendToNAdjNode(int n, Object o) {
        NCount temp = new NCount(-1,n);
        temp.object = o;
        sendToAllPeers(temp);
    }
}
