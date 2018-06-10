import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;

class Connection {
    private Socket socket;
    String ip = null;
    private int port = -1;
    private ObjectOutputStream outStream;

    Connection() {}

    void establishConnection(String _ip, int _port) {
        ip = _ip;
        port = _port;
        try {
            socket = new Socket(ip, port);
            outStream = new ObjectOutputStream(socket.getOutputStream());
            System.out.println("Connection Established with " + ip + ":" + port);
        } catch (IOException e) {
            System.out.println("Error while trying to establish a connection with " + ip + ":" + port);
            e.printStackTrace();
        }
    }

    void disconnect() {
        if (socket.isConnected()) {
            try {
                outStream.close();
                socket.close();
                ip = null;
                port = -1;
            } catch (IOException e) {
                System.out.println("Error while disconnecting socket.");
                e.printStackTrace();
            }
        } else {
            System.out.println("Socket is connected, therefore could not disconnect.");
        }
    }

    void sendObject(Object _o) {
        try {
            outStream.writeObject(_o);
        } catch (IOException e) {
            System.out.println("Error while sending object data.");
            e.printStackTrace();
        }
    }
}
