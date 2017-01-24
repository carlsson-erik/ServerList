
package serverList;

import java.io.Serializable;

/**
 *
 * @author Erik
 */
public class UserPackage implements Serializable{
    
    private int players;
    private String name;
    private int port;
    private String ip,key;
    private boolean requestingList;
    
   public UserPackage(int port,String name, int players, String key){
        this.port = port;
        this.name = name;
        this.players = players;
        this.key = key;
    }
    
    public UserPackage(int players){
        this.players = players;
    }
    
    public UserPackage(String key){
        this.key = key;
    }
    public UserPackage(boolean requestList){
        this.requestingList = requestList;
        
    }
    

    /**
     * @return the players
     */
    public int getPlayers() {
        return players;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the port
     */
    public int getPort() {
        return port;
    }

    /**
     * @return the ip
     */
    public String getIp() {
        return ip;
    }

    /**
     * @return the key
     */
    public String getKey() {
        return key;
    }

    /**
     * @return the requestList
     */
    public boolean isRequestingList() {
        return requestingList;
    }
    
    
}
