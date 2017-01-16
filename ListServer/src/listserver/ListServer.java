package listserver;

import serverList.ServerData;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
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

    private long waitForKeyTime, requestListDelay;

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
    private final int TPS = 5;

    private Thread t;

    public static void main(String[] args) {
        try {
            // Set System L&F
            //UIManager.setLookAndFeel(
            //       UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        new ListServer();
    }

    public ListServer() {
        t = new Thread(this, "Messenger");

        users = new ArrayList();
        players = new ArrayList();
        gameServers = new ArrayList();
        removeUsers = new ArrayList();

        removedCount = 0;
        connectedCount = 0;
        serverCount = 0;
        playerCount = 0;
        lastTick = System.currentTimeMillis();
        waitForKeyTime = 5000;
        requestListDelay = 5000;

        createAndShowGUI();

        if (portText.getText() != null) {

            mainSocket = new NetworkManager(Integer.parseInt(portText.getText()), 10);
            portText.setEnabled(false);
            keyText.setEnabled(false);
            startStopButton.setText("Stop");
        }

        

        t.start();

    }

    public void createAndShowGUI() {

        this.setVisible(true);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setSize(650, 600);
        this.setResizable(false);
        this.setLayout(new FlowLayout(1));

        this.setTitle("List Server");

        sendButton = new JButton("Send");
        sendButton.setPreferredSize(new Dimension(100, 100));
        sendButton.setVisible(true);

        startStopButton = new JButton("Start");
        startStopButton.setPreferredSize(new Dimension(100, 100));
        startStopButton.setVisible(true);

        textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setVisible(true);
        DefaultCaret caret = (DefaultCaret) textArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(200, 400));
        scrollPane.setVisible(true);

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
        try {
            yourIpLabel.setText(InetAddress.getLocalHost().getHostAddress());
        } catch (UnknownHostException ex) {
            Logger.getLogger(ListServer.class.getName()).log(Level.SEVERE, null, ex);
        }

        this.add(yourIpLabel);
        this.add(portLabel);
        this.add(portText);
        this.add(keyLabel);
        this.add(keyText);
        this.add(startStopButton);
        this.add(sendButton, BorderLayout.WEST);
        this.add(scrollPane, BorderLayout.EAST);
        this.add(text, BorderLayout.SOUTH);

        sendButton.addActionListener(this);
        startStopButton.addActionListener(this);

        this.validate();
        this.repaint();

    }

    @Override
    public void run() {
        while (true) {
            if (System.currentTimeMillis() > lastTick + 1000/TPS) {
                
                if (mainSocket != null) {

                    //add new servers to the server list
                    ArrayList<Socket> newSockets = mainSocket.getWaitingSockets();

                    for (int i = 0; i < newSockets.size(); i++) {
                        users.add(new ListUser(newSockets.get(i)));
                        showMessage("Added new user");
                        mainSocket.remove(users.get(users.size() - 1).getSocket());
                        connectedCount++;
                    }

                    for (ListUser s : gameServers) {
                        if (!s.isBound()) {
                            gameServers.remove(s);
                            break;
                        }
                    }
                    for (ListUser p : players) {
                        if (!p.isBound()) {
                            players.remove(p);
                            break;
                        }
                    }

                    //Adds users to removeUsers who didn't send Key in time
                    for (ListUser lU : users) {

                        if (lU.getKey() == keyText.getText() || keyText.getText().isEmpty()) {

                            if (lU.getPort() != 0 && lU.getName() != null) {
                                gameServers.add(lU);
                                removeUsers.add(lU);
                                serverCount++;
                            } else {
                                players.add(lU);
                                removeUsers.add(lU);
                                playerCount++;
                            }

                        } else if (lU.getTimeWhenConnected() + waitForKeyTime < System.currentTimeMillis()) {
                            showMessage("Removed User: " + lU.getIp() + "  | No key | " + "Key Delay = " + waitForKeyTime / 1000 + "sec");
                            removeUsers.add(lU);

                        }

                    }

                    //Removes users from users arrayList
                    for (ListUser r : removeUsers) {
                        ListUser temp = r;
                        removedCount++;
                        users.remove(temp);
                        removeUsers.remove(temp);
                        break;
                    }

                    for (ListUser p : players) {
                        if (p.getName() != null && p.getPort() != 0) {
                            ListUser temp = p;
                            gameServers.add(temp);
                            players.remove(temp);
                            break;
                        }

                        if (p.isRequestingList()) {

                            if (System.currentTimeMillis() > requestListDelay + p.getLastListRequest()) {

                                for (ListUser s : gameServers) {

                                    p.output(new ServerData(s.getIp(), s.getPort(), s.getName(), s.getPlayers()));

                                }

                                p.setLastListRequest(System.currentTimeMillis());
                                p.setRequestingList(false);
                            }

                        }

                    }

                }
                lastTick = System.currentTimeMillis();
            }
        }
    }

    public void showMessage(String message) {
        textArea.append("\n" + message);

    }

    public void doServerCmd(String message) {

        String cmd;
        cmd = message;
        String[] split;
        split = cmd.split(" ");

        switch (split[0]) {
            case "/help":
                showMessage("ListServer 1.0");
                showMessage("-----Help------");
                showMessage("list - lists the connected servers");
                showMessage("remove [ip] - Removes the specific server");
                break;
            case "/list":

                for (ListUser s : gameServers) {

                    showMessage("Name: " + s.getName() + " : Ip:" + s.getIp() + " Players:" + s.getPlayers() + " Port:" + s.getPort());
                }

                showMessage(users.size() + " Users");
                showMessage(gameServers.size() + " Servers");
                showMessage(players.size() + " Players");

                break;
            case "/remove":

                if (split.length > 1) {
                    removeUser(split[1]);
                }

                break;
            case "/hist":

                showMessage("Removed users: " + removedCount);
                showMessage("Connected users: " + connectedCount);
                showMessage("Servers: " + serverCount);
                showMessage("Players: " + playerCount);

                break;

        }

    }

    public void removeUser(String ip) {
        System.out.println("removing user: " + ip);
        for (int i = 0; i < users.size() - 1; i++) {
            if (users.get(i).getIp() == ip) {
                users.remove(i);
                System.out.println("User Removed");
                break;
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        String cmd = e.getActionCommand().toString();

        switch (cmd) {
            case "Send":

                doServerCmd(text.getText());
                break;
            case "Start":
                if (portText.getText() != null) {

                    mainSocket = new NetworkManager(Integer.parseInt(portText.getText()), 10);
                    portText.setEnabled(false);
                    keyText.setEnabled(false);
                    startStopButton.setText("Stop");
                }

                break;
            case "Stop":

                mainSocket.stop();
                portText.setEnabled(true);
                keyText.setEnabled(true);

                startStopButton.setText("Start");
                break;

        }

    }
}
