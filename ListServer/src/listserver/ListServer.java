package listserver;

import serverList.ServerData;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.text.DefaultCaret;

/**
 *
 * @author carls
 */
public class ListServer extends JFrame implements ActionListener, Runnable {

    //GUI
    private JButton sendButton, startStopButton;

    private JTextArea textArea;
    private JTextField text, keyText, portText;
    private JScrollPane scrollPane;

    private JLabel yourIpLabel, portLabel, keyLabel;

    private final long WAIT_FOR_KEY_TIME = 5000; // ms - Time the user has to send the key
    private final long REQUEST_LIST_DELAY = 1000; // ms - Time between requests of the list
    private final int TPS = 5; // amount of ticks per second
    private final String VERSION = "1.1";

    private NetworkManager mainSocket;
    private ArrayList<ListUser> users;
    private ArrayList<ListUser> players;
    private ArrayList<ListUser> removeUsers;
    private ArrayList<ListUser> gameServers;

    private int removedCount;
    private int connectedCount;
    private int playerCount;
    private int serverCount;
    private long lastTick;

    private Thread t;

    public static void main(String[] args) {

        new ListServer();
    }

    public ListServer() {
        t = new Thread(this, "Messenger");

        users = new ArrayList();
        players = new ArrayList();
        gameServers = new ArrayList();
        removeUsers = new ArrayList();

        //reset the history
        resetHist();

        //set the time
        lastTick = System.currentTimeMillis();

        createAndShowGUI();

        //Start the networkmanager if portText is defined.
        if (portText.getText() != null) {

            mainSocket = new NetworkManager(Integer.parseInt(portText.getText()), 10);
            //disable the settings while the networkManager is running
            portText.setEnabled(false);
            keyText.setEnabled(false);
            startStopButton.setText("Stop");
        }

        t.start();

    }

    public void createAndShowGUI() {

        //Setup JFrame
        this.setVisible(true);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setSize(650, 600);
        this.setResizable(false);
        this.setLayout(new FlowLayout(1));
        this.setTitle("List Server v." + VERSION);

        //Setup buttons
        sendButton = new JButton("Send");
        sendButton.setPreferredSize(new Dimension(100, 100));
        sendButton.setVisible(true);

        startStopButton = new JButton("Start");
        startStopButton.setPreferredSize(new Dimension(100, 100));
        startStopButton.setVisible(true);

        //Setup the text area and the scroll pane
        textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setVisible(true);
        DefaultCaret caret = (DefaultCaret) textArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(200, 400));
        scrollPane.setVisible(true);

        //setup labels and textfields
        text = new JTextField("Text");
        text.setPreferredSize(new Dimension(200, 50));
        text.setVisible(true);

        portText = new JTextField("33678");
        portText.setPreferredSize(new Dimension(200, 50));
        portText.setVisible(true);

        keyText = new JTextField("");
        keyText.setPreferredSize(new Dimension(200, 50));
        keyText.setVisible(true);

        portLabel = new JLabel("Port:");
        portLabel.setVisible(true);
        portLabel.setPreferredSize(new Dimension(50, 50));

        keyLabel = new JLabel("Key:");
        keyLabel.setVisible(true);
        keyLabel.setPreferredSize(new Dimension(50, 50));

        yourIpLabel = new JLabel();
        //Get your local ip
        try {
            yourIpLabel.setText(InetAddress.getLocalHost().getHostAddress());
        } catch (UnknownHostException ex) {
            System.out.println("Could not get your local ip");
        }

        //add components to jpanel
        this.add(yourIpLabel);
        this.add(portLabel);
        this.add(portText);
        this.add(keyLabel);
        this.add(keyText);
        this.add(startStopButton);
        this.add(sendButton, BorderLayout.WEST);
        this.add(scrollPane, BorderLayout.EAST);
        this.add(text, BorderLayout.SOUTH);

        //add actionlisteners to the buttons
        sendButton.addActionListener(this);
        startStopButton.addActionListener(this);

        //Refresh the jframe
        this.validate();
        this.repaint();

    }

    @Override
    public void run() {
        while (true) {
            //sets the maximum amount of Ticks
            if (System.currentTimeMillis() > lastTick + 1000 / TPS) {

                //only do this if mainsocket is ready
                if (mainSocket != null) {

                    //add new sockets from the mainSocket to the newscokets temporary arrayList
                    ArrayList<Socket> newSockets = mainSocket.getWaitingSockets();

                    //adds the newSockets to the users arrayList and removes them from the mainsockets socket list
                    for (int i = 0; i < newSockets.size(); i++) {
                        users.add(new ListUser(newSockets.get(i)));
                        showMessage("Added new user");
                        mainSocket.remove(users.get(users.size() - 1).getSocket());
                        connectedCount++;
                    }
                    //if a server isn't bound (closed it's streams), remove them
                    for (ListUser s : gameServers) {
                        if (!s.isBound()) {
                            gameServers.remove(s);
                            break;
                        }
                    }

                    //if a player isn't bound (closed it's streams), remove them
                    for (ListUser p : players) {
                        if (!p.isBound()) {
                            players.remove(p);
                            break;
                        }
                    }

                    //Adds users to removeUsers arraylist if they haven't sent the key in the time frame of WAIT_FOR_KEY_TIME
                    for (ListUser lU : users) {
                        //Checks if key match or if there is no key else the user is placed in the temporary removeUsers arrayList
                        if (lU.getKey() != null && lU.getKey().equals(keyText.getText()) || keyText.getText().isEmpty()) {

                            //if the user gives a port and name it becoms a server else it becoms a player
                            if (lU.getPort() != 0 && lU.getName() != null) {
                                gameServers.add(lU);
                                removeUsers.add(lU);
                                serverCount++;
                            } else {
                                players.add(lU);
                                removeUsers.add(lU);
                                playerCount++;
                            }

                        } else if (lU.getTimeWhenConnected() + WAIT_FOR_KEY_TIME < System.currentTimeMillis()) {
                            showMessage("Removed User: " + lU.getIp() + "  | No key | " + "Key Delay = " + WAIT_FOR_KEY_TIME / 1000 + "sec");
                            removeUsers.add(lU);

                        }

                    }

                    //Removes users from users arrayList
                    for (ListUser r : removeUsers) {
                        removedCount++;
                        users.remove(r);

                    }
                    //clears the removeUsers arraylist when it has removed the users
                    removeUsers.clear();

                    //checks if a player in the players arraylist has becomen a server and if so it is moved to the gameServer arraylist
                    for (ListUser p : players) {
                        if (p.getName() != null && p.getPort() != 0) {
                            ListUser temp = p;
                            gameServers.add(temp);
                            players.remove(temp);
                            break;
                        }

                        //checks if player is requesting to get the server list
                        if (p.isRequestingList()) {

                            //checks if the player can request a new server list and if the player is requesting to often
                            if (System.currentTimeMillis() > REQUEST_LIST_DELAY + p.getLastListRequest()) {
                                //sends the servers one by one to minimize the lag if there is to many servers
                                for (ListUser s : gameServers) {

                                    p.output(new ServerData(s.getIp(), s.getPort(), s.getName(), s.getPlayers()));

                                }
                                //sets the players last list request
                                p.setLastListRequest(System.currentTimeMillis());
                                //the player is no longer request list
                                p.setRequestingList(false);
                            }

                        }

                    }

                }

                lastTick = System.currentTimeMillis();
            }
        }
    }

    //displays a massage in the textArea
    public void showMessage(String message) {
        textArea.append("\n" + message);

    }

    //Resets the server,players,removed users and connected history
    public void resetHist() {

        removedCount = 0;
        connectedCount = 0;
        serverCount = 0;
        playerCount = 0;

    }

    //does a command in the form of a string
    public void doServerCmd(String message) {

        //Seperated the different elements in the command
        String cmd;
        cmd = message;
        String[] arg;
        arg = cmd.split(" ");

        switch (arg[0]) {
            case "/help":
                showMessage("ListServer " + VERSION);
                showMessage("-----Help------");
                showMessage("/list - lists the connected servers");
                showMessage("/remove [player/server] [ip] - Removes the specific server or player");
                showMessage("/list - Shows the servers,players and the users");
                showMessage("/hist - Shows the history of connected users,servers,players and removed users");
                showMessage("/reset history - Resets the List servers history");

                break;
            case "/list":
                //lists the server or the players. If the command doesn't have a second argument, show the users,servers and the players
                if (arg.length > 1) {

                    if (arg[1].contains("servers")) {
                        for (ListUser s : gameServers) {

                            showMessage("Name: " + s.getName() + " : Ip:" + s.getIp() + " Players:" + s.getPlayers() + " Port:" + s.getPort());
                        }

                    }

                    if (arg[1].contains("players")) {
                        for (ListUser s : players) {

                            showMessage("Ip: " + s.getIp());
                        }

                    }
                } else {

                    showMessage(users.size() + " Users");
                    showMessage(gameServers.size() + " Servers");
                    showMessage(players.size() + " Players");
                }
                break;
            case "/remove":
                //removes a player or a server with the right ip
                if (arg.length > 2) {
                    if (arg[1].contains("server")) {
                        if(removeServer(arg[2], Integer.parseInt(arg[3]))){
                            showMessage("Server: " + arg[2] + " Port: " + arg[3] + " removed");
                        }
                        
                    }

                }
                if (arg.length > 1) {
                    if (arg[1].contains("player")) {
                        if (removePlayer(arg[2])) {
                            showMessage("Player: " + arg[2] + " removed");
                        }
                    }
                }
                break;
            case "/hist":
                //displays history
                showMessage("Removed users: " + removedCount);
                showMessage("Connected users: " + connectedCount);
                showMessage("Servers: " + serverCount);
                showMessage("Players: " + playerCount);

                break;
            case "/reset History":
                //resets history
                resetHist();
                showMessage("Reset the history");

                break;

        }

    }

    //removes a player with that ip from the players arraylist
    public boolean removePlayer(String ip) {

        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).getIp().contains(ip)) {
                players.remove(i);

                return true;

            }
        }
        return false;
    }

    //removes a server with that ip and port from the servers arraylist
    public boolean removeServer(String ip, int port) {

        for (int i = 0; i < gameServers.size(); i++) {
            if (gameServers.get(i).getIp().contains(ip) && gameServers.get(i).getPort() == port) {
                gameServers.remove(i);

                return true;
            }
        }
        return false;
    }

    //Handles all the key inputs
    @Override
    public void actionPerformed(ActionEvent e) {

        String cmd = e.getActionCommand().toString();

        switch (cmd) {
            case "Send":
                //executes a server command
                doServerCmd(text.getText());
                break;
            case "Start":
                //when starting the ListServer
                if (portText.getText() != null) {

                    mainSocket = new NetworkManager(Integer.parseInt(portText.getText()), 10);
                    portText.setEnabled(false);
                    keyText.setEnabled(false);
                    startStopButton.setText("Stop");
                }

                break;
            case "Stop":
                //when stopping the ListServer
                mainSocket.stop();
                portText.setEnabled(true);
                keyText.setEnabled(true);

                startStopButton.setText("Start");
                break;

        }

    }
}
