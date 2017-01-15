
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
        
        try {
            socket = new ServerSocket(port,backlog);
        } catch (IOException ex) {
            Logger.getLogger(NetworkManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
        
        
        t.start();
        
    }
    
    @Override
    public void run() {
        
        while(running){
            try {
                
                waitingSockets.add(socket.accept());
                System.out.println("New Connection in MainSocket");
                
                
            } catch (IOException ex) {
            
            }
            
        }
        
    }
    
    synchronized public void stop(){
        System.out.println("Closing MainSocket");
        try {
            socket.close();
            waitingSockets.clear();
            running = false;
            
        } catch (IOException ex) {
            System.out.println("MainSocket failed at closing");
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
