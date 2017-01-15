
package listserver;

import java.io.Serializable;

/**
 *
 * @author Erik
 */
public class ServerData implements Serializable{
    
    private int players,port;
    private String name,ip;
    
    
    ServerData(String ip, int port, String name, int players){
        this.ip = ip;
        this.port = port;
        this.name = name;
        this.players = players;
    }

    /**
     * @return the players
     */
    public int getPlayers() {
        return players;
    }

    /**
     * @return the port
     */
    public int getPort() {
        return port;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the ip
     */
    public String getIp() {
        return ip;
    }
    
}
