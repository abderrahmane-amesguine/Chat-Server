import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

public class Server {
    private static final ArrayList<Clients> clients = new ArrayList<>();
    private static final LinkedList<String> ChatHistory = new LinkedList<>();
    public static void main(String[] args) {
        try {
            Selector selector = Selector.open();
            ServerSocketChannel server = ServerSocketChannel.open();
            server.bind(new InetSocketAddress(4666));
            server.configureBlocking (false);
            server.register(selector, SelectionKey.OP_ACCEPT);

            while (true) {
                int readyChannels = selector.select();

                if (readyChannels > 0){
                    Set<SelectionKey> selectedKeys = selector.selectedKeys();
                    Iterator<SelectionKey> keyIterator = selectedKeys.iterator();
                    while (keyIterator.hasNext ()){
                        SelectionKey key = keyIterator.next();
                        
                        if (key.isAcceptable()) {
                            // connecte un client 
                            connect(key, selector);
                        } 
                        else if (key.isReadable()) {
                            
                            SocketChannel clientChannel = (SocketChannel) key.channel();
                            ByteBuffer buffer = ByteBuffer.allocate(1024);
                            int bytesRead = clientChannel.read(buffer);

                            if (bytesRead > 0) {

                                buffer.flip();
                                byte[] data = new byte[buffer.remaining()];
                                buffer.get(data);

                                // lire le message envoyer par le client
                                read(clientChannel, data);

                                String message = new String(data, Charset.forName("UTF-8"));
                                if(message.startsWith("JOIN")){
                                    // le client join le chat server avec un nom
                                    join(clientChannel, message);
                                }
                                else if ("exit".equals(new String(data).toLowerCase())) {
                                    // si le client écrit exit
                                    exit(clientChannel, key);
                                }
                                else{
                                    // Envoyer le message à tous les autres clients
                                    broadcastMessage(clientChannel, Nom_client(clientChannel)+ " : " + new String(data));
                                    addToChatHistory(Nom_client(clientChannel) + " : " + new String(data) + "\n");
                                }
                            }
                            else if (bytesRead == -1) {
                                exit(clientChannel, key);
                            }
                        }
                        keyIterator.remove();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void connect( SelectionKey key, Selector selector) throws IOException{
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = serverChannel.accept();
        clientChannel.configureBlocking(false);
        clientChannel.register(selector, SelectionKey.OP_READ);
    }

    private static void join(SocketChannel clientChannel, String message) throws IOException{
        String[] parts = message.split(" ");
        if (parts.length == 2) {
            String nom = parts[1];

            afficher_liste_clients(clientChannel);
            sendChatHistory(clientChannel);
            // Associer le nom au client
            clients.add(new Clients(clientChannel, nom));
            message = "Client " + nom + " joined the server";
            System.out.println(message);
            broadcastMessage(clientChannel, message);
            addToChatHistory(message + "\n");
        }
    }

    public static void afficher_liste_clients(SocketChannel clientChannel) throws IOException {
        String clientListMessage = "\nClients connecte : ";
        if (clients.size() > 0) {
            for (Clients existingClient : clients) {
                clientListMessage += existingClient.getNom() + "    ";
            }
            clientChannel.write(ByteBuffer.wrap((clientListMessage + "\n\n").getBytes(Charset.forName("UTF-8"))));
        }
    }

    private static void read(SocketChannel clientChannel, byte[] data) throws IOException{
        if (clients.size() > 0 && !Nom_client(clientChannel).isEmpty()) {
            System.out.println("Recieved from " + Nom_client(clientChannel) + " : " + new String(data));
        }
    }

    private static void exit(SocketChannel clientChannel, SelectionKey key) throws IOException{
        String message = "Client disconnected: " + Nom_client(clientChannel);
        System.out.println(message);
        broadcastMessage(clientChannel, message);
        addToChatHistory(message + "\n");
        for (Iterator<Clients> iterator = clients.iterator(); iterator.hasNext();) {
            Clients client = iterator.next();
            if (client.getC().equals(clientChannel)) {
                iterator.remove();
            }
        }
        key.cancel();
        clientChannel.close();
    }

    private static void broadcastMessage(SocketChannel sender, String message) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(message.getBytes(Charset.forName("UTF-8")));
        for (Clients client : clients) {
            if (client.getC() != sender) {
                client.getC().write(buffer);
                buffer.rewind();
            }
        }
    }
    
    private static void sendChatHistory(SocketChannel clientChannel) throws IOException {
        if (!ChatHistory.isEmpty()) {
            for (String message : ChatHistory) {
                ByteBuffer buffer = ByteBuffer.wrap( message.getBytes(Charset.forName("UTF-8")));
                clientChannel.write(buffer);
            }
        }
    }

    private static void addToChatHistory(String message) {
        ChatHistory.add(message);
        if (ChatHistory.size() > 30) {
            ChatHistory.removeFirst();
        }
    }

    private static String Nom_client(SocketChannel clientChannel){
        for (Clients client : clients) {
            if (client.getC().equals(clientChannel)) {
                return client.getNom();
            }
        }
        return "";
    }
}

class Clients {
    private SocketChannel C;
    private String nom;

    public Clients(SocketChannel C, String nom){
        this.C = C;
        this.nom = nom;
    }

    public SocketChannel getC() {
        return C;
    }

    public String getNom() {
        return nom;
    }
}