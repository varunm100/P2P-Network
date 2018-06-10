import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedList;

class Server {
    private ServerSocket serverSocket;
    private LinkedList<Socket> socketList = new LinkedList<>();
    private LinkedList<ObjectInputStream> objInputStreams = new LinkedList<>();
    private ArrayList<Integer> zerozeroPair = new ArrayList<>();

    Server() { zerozeroPair.add(0); zerozeroPair.add(0); }

    private String formatIP(Socket _socket) {
        if (_socket != null)
            return _socket.getInetAddress().toString().replace("/", "");
        return "";
    }

    private Socket findValidClient(LinkedList<String> _whiteListIP) {
        Socket tempSocket;
        while(true) {
            try {
                tempSocket = serverSocket.accept();
                if (_whiteListIP.contains(formatIP(tempSocket))) {
                    System.out.println("Accepted Client with ip " + formatIP(tempSocket));
                    return tempSocket;
                } else {
                    tempSocket.close();
                    System.out.println("Rejected Client with ip " + formatIP(tempSocket));
                }
            } catch (IOException e) {
                System.out.println("Error occurred while trying to accept client connection.");
                e.printStackTrace();
            }
        }
    }

    private void checkUpdate(ObjectInputStream _inStream) {
        while (Peer.Shared.running) {
            try {
                Object o = _inStream.readObject();
                handleObjData(o);
            } catch (IOException e) {
                System.out.println("Error occurred while receiving data.");
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                System.out.println("Received data that does not contain a recognizable object.");
                e.printStackTrace();
            }
        }
    }

    void startServer(LinkedList<String> _adjPeerIP, int _port) {
        try {
            serverSocket = new ServerSocket(_port);
            serverSocket.setSoTimeout(0);
        } catch (IOException e) {
            System.out.println("Could not create ServerSocket.");
            e.printStackTrace();
        }

        for (String peer : _adjPeerIP) {
            try {
                socketList.add(findValidClient(_adjPeerIP));
                objInputStreams.add(new ObjectInputStream(socketList.getLast().getInputStream()));
                new Thread(() -> checkUpdate(objInputStreams.getLast())).start();
                System.out.println("Server connected to " + formatIP(socketList.getLast()) + ":" + socketList.getLast().getPort() + " (" + (_adjPeerIP.indexOf(peer)+1) + "/" + _adjPeerIP.size() + ")");
            } catch (IOException e) {
                System.out.println("Error occurred while finding a suitable client.");
                e.printStackTrace();
            }
        }
        System.out.println("Setup connections to ALL clients successfully!");
    }

    void closeServer() {
        for (int i = 0; i < socketList.size(); i++) {
            try {
                socketList.get(i).close();
                objInputStreams.get(i).close();
            } catch (IOException e) {
                System.out.println("Error while closing a socket.");
                e.printStackTrace();
            }
        }
        try {
            serverSocket.close();
        } catch (IOException e) {
            System.out.println("Error while closing server socket.");
            e.printStackTrace();
        }
    }

    void handleObjData(Object _o) {
        if (_o instanceof SerializableText) {
            SerializableText text = (SerializableText) _o;
            System.out.println(text.text + " (" + text.source + ")" + "(" + text.timeStamp.toString() + ")");
        } else if (_o instanceof  TraversalObj) {
            zerozeroPair = new ArrayList<>();
            zerozeroPair.add(0); //first index is the number of callbacks expected.
            zerozeroPair.add(0); //second index is the current number of callbacks.
            TraversalObj data = (TraversalObj) _o;
            if (Peer.Shared.callBackCounter.containsKey(data.timeStamp.toString())) {
                if (data.type.contains("CALLBACK")) {
                    Peer.Shared.callBackCounter.get(data.timeStamp.toString()).set(1, Peer.Shared.callBackCounter.get(data.timeStamp.toString()).get(1)+1);
                    return;
                }
                data.visited.add(Peer.Ipv4Local);
                data.type = "CALLBACKINVALID";
                Peer.sendObject(data, data.callbackSubject);
                return;
            }

            handleObjData(data.data);

            data.visited.add(Peer.Ipv4Local);
            Peer.Shared.callBackCounter.put(data.timeStamp.toString(), zerozeroPair);
            TraversalObj sendingData = new TraversalObj();
            sendingData.timeStamp = data.timeStamp;
            sendingData.data = data.data;
            sendingData.globalSource = data.globalSource;
            sendingData.type = data.type;
            sendingData.visited = data.visited;
            sendingData.callbackSubject = Peer.Ipv4Local;
            for (Connection connection : Peer.connections.values()) {
                if (!data.visited.contains(connection.ip)) {
                    Peer.Shared.callBackCounter.get(data.timeStamp.toString()).set(0, Peer.Shared.callBackCounter.get(data.timeStamp.toString()).get(0)+1);
                    new Thread(() -> Peer.sendObject(sendingData, connection.ip)).start();
                }
            }
            System.out.println("Expected Callbacks: " + Peer.Shared.callBackCounter.get(data.timeStamp.toString()).get(0));
            while(Peer.Shared.callBackCounter.get(data.timeStamp.toString()).get(1) < Peer.Shared.callBackCounter.get(data.timeStamp.toString()).get(0)) {

            }
            System.out.println("GOT ALL CALLBACKS! " + Peer.Shared.callBackCounter.get(data.timeStamp.toString()).get(1));
            if (data.globalSource.equals(Peer.Ipv4Local)) { System.out.println("CONFIRMATION: DATA REACHED ALL NODES"); return; }
            data.type = "CALLBACK";
            Peer.sendObject(data, data.callbackSubject);
        }
    }
}
