package listserver;

import serverList.ServerData;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import serverList.UserPackage;

/**
 *
 * @author Erik
 */
public class ListUser implements Runnable {

    private Thread t;
    private Socket socket;
    private ObjectOutputStream output;
    private ObjectInputStream input;
    private long timeWhenConnected, lastListRequest;

    private String ip, name;
    private int port;
    private int players;
    private String key;
    private boolean requestingList,bound;

    private UserPackage currentPackage;

    public ListUser(Socket socket) {

        t = new Thread(this, "Server");
        timeWhenConnected = System.currentTimeMillis();

        requestingList = false;
        bound = true;

        this.socket = socket;

        t.start();

    }

    public ListUser(String ip, int port, String name, int players) {
        this.ip = ip;
        this.port = port;
        this.name = name;
        this.players = players;
    }

    @Override
    public void run() {
        while (true) {
            if (socket != null) {
                if (socket.isBound()) {

                    ip= "155.4.235.146";
                    //ip = socket.getInetAddress().getHostAddress();
                    break;
                }
            }

        }
        System.out.println("setting up streams");
        setupStreams();
        System.out.println("Connected");
        whileConnected();
        System.out.println("Cloing streams");
        closeStreams();
        System.out.println("Streams closed");

    }

    public void setupStreams() {
        try {
            output = new ObjectOutputStream(socket.getOutputStream());
            output.flush();
            input = new ObjectInputStream(socket.getInputStream());
        } catch (IOException ex) {
            Logger.getLogger(ListUser.class.getName()).log(Level.SEVERE, null, ex);
        }
        bound = true;
    }

    public void whileConnected() {
        do {
            try {
                currentPackage = (UserPackage) input.readObject();
            } catch (IOException ex) {
                closeStreams();
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(ListUser.class.getName()).log(Level.SEVERE, null, ex);
            }
            if (currentPackage != null) {
                if (currentPackage.getPort() != 0) {
                    
                    port = currentPackage.getPort();
                }
                if (currentPackage.getPlayers() != 0) {
                    players = currentPackage.getPlayers();
                }
                if (currentPackage.getName() != null) {
                    name = currentPackage.getName();
                }
                if (currentPackage.getKey() != null) {
                    key = currentPackage.getKey();
                }
                if (currentPackage.isRequestingList()) {
                    requestingList = true;
                }
            }
        } while (true);
    }

    synchronized public void closeStreams() {
        bound = false;
        try {
            output.close();
            input.close();
        } catch (IOException ex) {
            Logger.getLogger(ListUser.class.getName()).log(Level.SEVERE, null, ex);
        }
        

    }

    synchronized public void output(ServerData data) {
        try {
            output.writeObject(data);
        } catch (IOException ex) {
            Logger.getLogger(ListUser.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * @return the ip
     */
    synchronized public String getIp() {
        return ip;
    }

    /**
     * @return the name
     */
    synchronized public String getName() {
        return name;
    }

    /**
     * @return the port
     */
    synchronized public int getPort() {
        return port;
    }

    /**
     * @return the players
     */
    synchronized public int getPlayers() {
        return players;
    }

    /**
     * @return the timeWhenConnected
     */
    synchronized public long getTimeWhenConnected() {
        return timeWhenConnected;
    }

    /**
     * @return the key
     */
    synchronized public String getKey() {
        return key;
    }

    

    /**
     * @return the requestList
     */
    public boolean isRequestingList() {
        return requestingList;
    }

    /**
     * @return the lastListRequest
     */
    public long getLastListRequest() {
        return lastListRequest;
    }

    /**
     * @param lastListRequest the lastListRequest to set
     */
    public void setLastListRequest(long lastListRequest) {
        this.lastListRequest = lastListRequest;
    }

    /**
     * @param requestingList the requestingList to set
     */
    public void setRequestingList(boolean requestingList) {
        this.requestingList = requestingList;
    }

    /**
     * @return the bound
     */
    public boolean isBound() {
        return bound;
    }

    /**
     * @return the socket
     */
    public Socket getSocket() {
        return socket;
    }

}
