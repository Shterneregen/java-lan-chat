/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package connector.model;

import connector.resources.ControlLines;
import connector.utils.Encryption;
import connector.utils.ProjectProperties;
import connector.utils.Utils;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Yura
 */
class ServerThread extends Thread {

    private boolean stoped = false;
    private ServerSocket serverSocket = null;
    private Socket socket;
    private int port;
    private List<String> listNames;
    private List<Connection> connections;
    private int userNumber;
    private Encryption serverEncryption;
    private StringBuilder buffChat;
    private String psw;

    ServerThread(int port, String psw) {
        this.port = port;
        this.psw = psw;
        connections = Collections.synchronizedList(new ArrayList<Connection>());
        serverEncryption = new Encryption();
        buffChat = new StringBuilder("");
        listNames = new ArrayList<>();
    }

    //Прекращает пересылку сообщений
    public void setStop() {
        stoped = true;
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(port);
            while (!stoped) {
                socket = serverSocket.accept();
                if (stoped) {
                    break;
                }
                Connection con = new Connection(socket);
                connections.add(con);
                con.start();
            }
        } catch (IOException e) {
            Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, null, e);
        } finally {
            try {
                if (serverSocket != null) {
                    serverSocket.close();
                }
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException ex) {
                Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    protected void closeAll() {
        try {
            if (connections != null && !connections.isEmpty()) {
                synchronized (connections) {
                    for (Connection thisConnection : connections) {
                        thisConnection.getOutputStream().writeObject(new Message(thisConnection.getClientEncryption().encrypt(ControlLines.STR_EXIT_ALL), true));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Потоки не были закрыты! (closeAll)");
            e.printStackTrace();
        }
    }

    private class Connection extends Thread {

        private Socket socket;
        private Boolean flagWrongNic = false;
        private boolean stoped = false;
//        private boolean closed = false;
        private ObjectInputStream inputStream;
        private ObjectOutputStream outputStream;
        private Message message;
        private Encryption clientEncryption;
        private String name = "";
        private Properties stringsFile;

        public Connection(Socket soc) {
            this.socket = soc;
            clientEncryption = new Encryption();
            try {
                inputStream = new ObjectInputStream(this.socket.getInputStream());
                outputStream = new ObjectOutputStream(this.socket.getOutputStream());
            } catch (IOException e) {
                Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, null, e);
            }
            stringsFile = ProjectProperties.getInstance().getStringsFile();
        }

        public void setStop() {
            stoped = true;
        }

        /*Проверяет, есть ли такой же ник в чате*/
        private boolean checkNicname(String nicname, List<String> listNames) throws IOException {
            boolean res = false;
            for (String userName : listNames) {
                if (nicname.equals(userName)) {
                    res = true;
                    break;
                } else {
                    res = false;
                }
            }
            return res;
        }

        @Override
        public void run() {
            try {
                while (!stoped) {
                    message = (Message) inputStream.readObject();
                    String pass = Encryption.decode(message.getPsw(), psw);
                    name = Encryption.decode(message.getName(), psw);

                    clientEncryption.createPair(message.getPublicKey());
                    Connection.this.outputStream.writeObject(new Message(clientEncryption.encrypt(ControlLines.STR_SEND_PUB_KEY), true, serverEncryption.getPublicKeyFromKeypair()));

                    if (stoped) {
                        break;
                    }
                    if (pass.equals(psw)) {
                        // Проверяет, есть ли такой же ник в чате
                        flagWrongNic = stoped = checkNicname(name, listNames);

                        if (!flagWrongNic) {
                            userNumber++;

                            synchronized (listNames) {
                                listNames.add(name);
                            }
                            // Оповещаем всех, что вошел новый участник
                            sendMsgToAllMembers(name + " " + stringsFile.getProperty("server.msg.join"));

                            // В цикле получаем очередное сообщение от данного клиента и рассылаем остальным
                            while (!stoped) {
                                message = (Message) inputStream.readObject();
                                String msgFromClient = Utils.removeTheTrash(serverEncryption.decrypt(message.getMessage()));

                                switch (msgFromClient) {
                                    // Оповещаем всех, что данный клиент вышел
                                    case ControlLines.STR_EXIT:
                                        connections.remove(Connection.this);
                                        sendMsgToAllMembers(name + " " + stringsFile.getProperty("server.msg.left"));
                                        userNumber--;
                                        setStop();
                                        break;
                                    case ControlLines.STR_EXIT_ALL:
                                        setStop();
                                        break;
                                    // Отправляем все сообщения сессии
                                    case ControlLines.STR_GET_ALL_MSG:
                                        String msg = "----- "
                                                + stringsFile.getProperty("server.msg.allMsg")
                                                + " -----"
                                                + new String(buffChat)
                                                + "\n----------------------\n";
                                        writeMsgToStream(Connection.this, msg);
                                        break;
                                    // Отправляем всем клиентам очередное сообщение
                                    default:
                                        sendMsgToAllMembers(name + ": " + msgFromClient);
                                        break;
                                }
                            }
                        } else {
                            writeMsgToStream(Connection.this, ControlLines.STR_SAME_NIC);
                        }
                    } else {
//                        Connection.this.out.println(Encryption.encode(Utils.getSTR_WRONG_PASS(), pfStr)); 
//                        Connection.this.out.println(Encryption.encode("--- Сервер не отвечает --- 3"+"\n", pfStr));
                        this.setStop();
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, null, e);
            } finally {
                close();
            }
        }

        /*Отправляет сообщение всем участникам*/
        private void sendMsgToAllMembers(String broadcastMsg) throws IOException {
            synchronized (connections) {
                for (Connection thisConnection : connections) {
                    String msg = "[" + getTime(false) + "] " + broadcastMsg;
                    writeMsgToStream(thisConnection, msg);
                    buffChat.append(msg).append("\n");
                }
            }
        }

        /*Отправляем сообщение определенному участнику*/
        private void writeMsgToStream(Connection connection, String msg) throws IOException {
            connection.getOutputStream().writeObject(new Message(connection.getClientEncryption().encrypt(msg), false));
        }

        // Возвращает дату (ch == 1) или время (ch == 0)
        private String getTime(boolean ch) {
            //Date calendar = Calendar.getInstance().getTime();
            long curTime = System.currentTimeMillis();
            String curStringDate = ch ? new SimpleDateFormat("dd.MM.yyyy").format(curTime) : new SimpleDateFormat("kk:mm:ss").format(curTime);
            return curStringDate;
        }

        /**
         * Закрывает входной и выходной потоки и сокет
         */
        private void close() {
//            if (!closed) {
//                closed = true;
            try {
                if (this.inputStream != null) {
                    this.inputStream.close();
                }
            } catch (Exception e) {
                Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, null, e);
                System.err.println("Потоки не были закрыты! (close)");
            }
            try {
                if (this.outputStream != null) {
                    this.outputStream.close();
                    this.outputStream.flush();
                }
            } catch (Exception e) {
                Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, null, e);
                System.err.println("Потоки не были закрыты! (close)");
            }
            try {
                if (socket != null) {
                    this.socket.close();
                }
            } catch (Exception e) {
                Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, null, e);
                System.err.println("Потоки не были закрыты! (close)");
            }
            synchronized (connections) {
                connections.remove(Connection.this);
            }
            if (!flagWrongNic && listNames != null && !listNames.isEmpty()) {
                synchronized (listNames) {
                    listNames.remove(name);
                }
            }
//            }
        }

        //<editor-fold defaultstate="collapsed" desc="get-set">
        public ObjectOutputStream getOutputStream() {
            return outputStream;
        }

        public void setOutputStream(ObjectOutputStream outputStream) {
            this.outputStream = outputStream;
        }

        public Encryption getClientEncryption() {
            return clientEncryption;
        }

        public void setClientEncryption(Encryption clientEncryption) {
            this.clientEncryption = clientEncryption;
        }
        //</editor-fold>
    }
}
