/*
 * @author Varun on 6/17/2018
 * @project P2P-Network
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.Socket;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

class Peer {
    private Server server;
    private String Ipv4Local = null;
    private Map<String, Connection> connections = new HashMap<>();

    /**
     * Stores data that's shared between all threads.
     */
    static class Shared {
        static AtomicReference<Map<String, ArrayList<Integer>>> callBackCounter = new AtomicReference<>(new HashMap<>());
        static AtomicReference<Map<String, LinkedList<Object>>> callBackData = new AtomicReference<>(new HashMap<>());
        static ExecutorService threadManager = Executors.newCachedThreadPool();
        static volatile int numMessagesCount = 0; // Only for testing (Volatile should be changed to AtomicReference)
        static volatile boolean isRunning;
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
        if (this.Ipv4Local != null) return this.Ipv4Local;
        String output = null;
        try {
            Socket socket = new Socket("192.168.1.1", 80); //could also use port 433
            output = socket.getLocalAddress().getHostAddress();
            socket.close();
        } catch (IOException e) {
            System.out.println("Could not detect ip address of device.");
            e.printStackTrace();
        }
        return output;
    }

    /**
     * Parses the config file.
     *
     * @param file Peer config file.
     * @return Metadata to identify and start the peer.
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
        if (adjPeers.isEmpty() || serverPort == -1) System.out.println("Error while parsing config file. (" + file.getPath() + ")");
        return new PeerData(adjPeers, serverPort);
    }

    /**
     * Stops the peer.
     */
    private void stop() {
        Shared.isRunning = false;
        waitForAllThreads();
        server.closeServer();
        connections.values().forEach(Connection::disconnect);

        this.connections.clear();
        this.server = null;
    }

    /**
     * Starts the peer.
     *
     * @param configFile Peer config file.
     */
    void startPeer(File configFile) {
        Main.scanner = new Scanner(System.in); // Only for debugging.

        Shared.isRunning = true;
        this.Ipv4Local = getLocalIpv4();

        PeerData peerData = parseConfigFile(configFile);
        Map<String, Integer> adjPeerInfo = new HashMap<>(peerData.adjPeers);
        LinkedList<String> adjIP = new LinkedList<>(adjPeerInfo.keySet());
        LinkedList<Integer> adjPort = new LinkedList<>(adjPeerInfo.values());
        Future queryThread = Shared.threadManager.submit(() -> server.startServer(adjIP, peerData.serverPort, this::handleObjData));

        System.out.println("Press [ENTER] to start client querying. (" + adjPeerInfo.size() + ")");
        Main.scanner.nextLine();

        for (int i = 0; i < adjPeerInfo.size(); ++i) {
            connections.put(adjIP.get(i), new Connection());
            connections.get(adjIP.get(i)).establishConnection(adjIP.get(i), adjPort.get(i));
        }
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
     * Waits for all threads to complete.
     */
    private void waitForAllThreads() {
        Shared.threadManager.shutdown();
        try {
            Shared.threadManager.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            System.out.println("Interrupted while waiting for all threads to execute.");
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
    private void sendObject(Object o, String Ipv4) {
        connections.get(Ipv4).sendObject(o);
    }

    /**
     * Sends an object to all immediate adjacent peers.
     *
     * @param o Object to be sent.
     */
    private void sendToAdjPeers(Object o) {
        connections.values().forEach(connection -> connection.sendObject(o));
    }

    /**
     * Sends an object to all peers in the network.
     *
     * @param o Object to be sent.
     */
    private void sendToAllPeers(Object o) {
        TraversalObj traversalObj = new TraversalObj();
        traversalObj.data = o;
        traversalObj.visited = new LinkedList<>();
        traversalObj.globalSource = this.Ipv4Local;
        traversalObj.type = "FORWARD";
        traversalObj.callbackSubject = this.Ipv4Local;
        traversalObj.timeStamp = Instant.now();
        handleObjData(traversalObj);
    }

    /**
     * Incrementing the number of callbacks by 1.
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
        if (o instanceof String) {
            parseStringCommand((String) o);
        } else if (o instanceof SerializableText) {
            handleTextData((SerializableText) o);
        } else if (o instanceof TraversalObj) {
            recursiveTraversal((TraversalObj) o);
        } else if (o instanceof PollingData) {
            printPollingResults((PollingData) o);
        } else {
            System.out.println("Received an unrecognizable object.");
        }
    }

    private void printPollingResults(PollingData o) {
        System.out.println("-----| POLLING RESULTS |-----");
        o.pollingResults.forEach((ip, result) -> System.out.println(ip + ":" + result));
    }

    /**
     * Implementation of a peer traversal algorithm.
     *
     * @param traversalObj An object which contains all required data to continue recursion.
     */
    private void recursiveTraversal(TraversalObj traversalObj) {
        if (checkBaseCase(traversalObj)) return;
        boolean isCountObject = traversalObj.data instanceof NCount;
        boolean isPollingObject = traversalObj.data instanceof PollingData;
        if (!isCountObject && !isPollingObject) handleObjData(traversalObj.data);
        traversalObj.visited.add(Ipv4Local);
        String timeStamp = traversalObj.timeStamp.toString();

        initCounterAtomically(Shared.callBackCounter, timeStamp);

        TraversalObj sendingData = new TraversalObj();
        sendingData.equals(traversalObj);
        sendingData.callbackSubject = Ipv4Local;
        /* UPDATE RECEIVED DATA */
        if (isCountObject) {
            NCount reference = (NCount) traversalObj.data;
            sendingData.data = new NCount(reference.count + 1, reference.maxCount, reference.object);
            NCount dataRec = (NCount) sendingData.data;
            if (dataRec.count >= dataRec.maxCount) {
                traversalObj.type = "CALLBACK";
                sendObject(traversalObj, traversalObj.callbackSubject);
                handleObjData(reference.object);
                Shared.numMessagesCount = 0;
                return;
            }
        } else if (isPollingObject) {
            PollingData pollReference = (PollingData) traversalObj.data;
            pollReference.addEntry(this.Ipv4Local, validateBlock(pollReference.blockData));
        }

        connections.values().stream()
                            .filter(connection -> !traversalObj.visited.contains(connection.ip))
                            .forEach(connection -> {
                                updateCallbackCounter(Shared.callBackCounter, timeStamp, 0);
                                Shared.threadManager.submit(() -> sendObject(sendingData, connection.ip));
                                Shared.numMessagesCount++;
                            });

        waitForCallbacks(timeStamp);
        if (isPollingObject && Shared.callBackData.get().containsKey(timeStamp)) {
            LinkedList<Object> rec = Shared.callBackData.get().get(timeStamp);
            PollingData reference = (PollingData) traversalObj.data;
            rec.forEach(data -> reference.merge((PollingData) data));
        }
        if (traversalObj.globalSource.equals(this.Ipv4Local)) {
            System.out.println("CONFIRMATION: DATA REACHED ALL NODES, sent " + Shared.numMessagesCount + " messages.");
            Shared.numMessagesCount = 0;
            if (isPollingObject) handleObjData(traversalObj.data);
            return;
        }
        traversalObj.type = "CALLBACK";
        sendObject(traversalObj, traversalObj.callbackSubject);
        System.out.println("CONFIRMATION: DATA REACHED ALL NODES, sent " + (Shared.numMessagesCount+1) + " messages.");
        Shared.numMessagesCount = 0;
    }

    private void startPolling(Object blockData) {
        sendToAllPeers(new PollingData(blockData));
    }

    /**
     * Supposed to 'verify' the block.
     * @param blockData Block data.
     * @return Returns validity of block.
     */
    private boolean validateBlock(Object blockData) {
        /* TODO */
        return new Random().nextBoolean();
    }

    /**
     * Waits to receive all callbacks from adjacent peers.
     *
     * @param timeStamp The time at which the parent node started search. (Used to keep track of which messages was already received)
     */
    private void waitForCallbacks(String timeStamp) {
        System.out.println("Expected Callbacks: " + Shared.callBackCounter.get().get(timeStamp).get(0));
        Map<String, ArrayList<Integer>> tempCount;
        do {
            tempCount = Shared.callBackCounter.get();
        } while (tempCount.get(timeStamp).get(1) < tempCount.get(timeStamp).get(0));
        System.out.println("GOT ALL CALLBACKS! " + Shared.callBackCounter.get().get(timeStamp).get(1));
    }

    /**
     * Used to check base case for recursion.
     *
     * @param data An object which contains all required data to continue recursion.
     * @return Returns whether the Peer should continue recursion.
     */
    private boolean checkBaseCase(TraversalObj data) {
        if (Shared.callBackCounter.get().containsKey(data.timeStamp.toString())) {
            if (data.type.startsWith("CALLBACK")) {
                updateCallbackCounter(Shared.callBackCounter, data.timeStamp.toString(), 1);
                updateCallback(data.data, data.timeStamp.toString());
                Shared.numMessagesCount++;
                return true;
            }
            data.visited.add(this.Ipv4Local);
            data.type = "CALLBACKINVALID";
            sendObject(data, data.callbackSubject);
            Shared.numMessagesCount++;
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
     * @param newObj new object to be added in the callback list.
     * @param key time stamp of message.
     */
    private void updateCallback(Object newObj, String key) {
        Map<String, LinkedList<Object>> before, after = new HashMap<>();
        do {
            before = Shared.callBackData.get();
            after.putAll(before);
            if (!after.containsKey(key)) {
                after.put(key, new LinkedList<>());
            }
            after.get(key).add(newObj);
        } while (!Shared.callBackData.compareAndSet(before, after));
    }

    /**
     * Sends data to all Nth adjacent nodes.
     * @param n n is the search depth.
     * @param o o is the data to be sent.
     */
    private void sendToNAdjNode(int n, Object o) {
        sendToAllPeers(new NCount(-1, n, o));
    }

    /**
     * Sends data to a random node.
     * @param min min value.
     * @param max max value.
     * @param o Data to be sent.
     */
    private void sendToRandomNode(int min, int max, Object o) {
        sendToNAdjNode(new Random().nextInt(max-min+1)+min, o);
    }

    void parseStringCommand(final String command) {
        if (command.equals("/exit")) {
            this.stop();
            Main.scanner.close(); // Only for debugging.
        } else if (command.startsWith("/send_to;")) {
            this.sendObject(new SerializableText(command.split(";")[2], this.Ipv4Local), command.split(";")[1]);
        } else if (command.startsWith("/send_to_all;")) {
            this.sendToAllPeers(new SerializableText(command.split(";")[1], this.Ipv4Local));
        } else if (command.startsWith("/send_to_adj;")) {
            this.sendToAdjPeers(new SerializableText(command.split(";")[1], this.Ipv4Local));
        } else if (command.startsWith("/send_to_all_nth;")) {
            this.sendToNAdjNode(Integer.parseInt(command.split(";")[1]), new SerializableText(command.split(";")[2], this.Ipv4Local));
        } else if (command.startsWith("/send_to_random_peer;")) {
            this.sendToRandomNode(Integer.parseInt(command.split(";")[1]), Integer.parseInt(command.split(";")[2]), new SerializableText(command.split(";")[3], this.Ipv4Local));
        } else if (command.startsWith("/start_polling")) {
            this.startPolling("BLOCK_DATA");
        } else if (command.startsWith("/")) {
            System.out.println("'" + command + "' is not recognized as a valid command.");
        }
    }
}
