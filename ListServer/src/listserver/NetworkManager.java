
package listserver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Erik
 */
public class NetworkManager implements Runnable{

    private Thread t;
    private ServerSocket socket;
    private ArrayList<Socket> waitingSockets;
    
    private boolean running;
    
    
    NetworkManager(int port, int backlog){
        t = new Thread(this,"MainSocket");
        running = true;
        waitingSockets = new ArrayList();
        
        //Start a new ServerSocket
        try {
            socket = new ServerSocket(port,backlog);
        } catch (IOException ex) {
            System.out.println("NetworkManager could not setup ServerSocket");
        }
        
        
        
        
        t.start();
        
    }
    
    @Override
    public void run() {
        //ServerSocket waits for a user to connect to it and adds it to waitingSockets arraylist, then loops.
        while(running){
            try {
                
                waitingSockets.add(socket.accept());
                
                
                
            } catch (IOException ex) {
                System.out.println("NetworkManager, ServerSocket failed at accepting new connection");
            }
            
        }
        
    }
    
    synchronized public void stop(){
        
        try {
            socket.close();
            waitingSockets.clear();
            running = false;
            
        } catch (IOException ex) {
            System.out.println("NetworkManager, failed at closing ServerSocket");
        }
    }

    /**
     * @return the waitingSockets
     */
   synchronized public ArrayList<Socket> getWaitingSockets() {
        return waitingSockets;
    }
    
   
   
   synchronized public void remove(Socket s){
        waitingSockets.remove(s);
    }
    
    
        
    
    
    
}
