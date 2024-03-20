import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.Scanner;

public class Client {
    private static final CharsetDecoder decoder = Charset.forName("UTF-8").newDecoder();
    public static void main(String[] args) {
        try {
            SocketChannel socketChannel = SocketChannel.open();
            socketChannel.connect(new InetSocketAddress("localhost", 4666));
            socketChannel.configureBlocking(false);

            while (!socketChannel.finishConnect()) {}
            System.out.println("Connecter au serveur. Entrer 'exit' pour ce deconnecter.");

            try (Scanner scanner = new Scanner(System.in)) {
                System.out.print("Entrer ton nom: ");
                String nom = scanner.nextLine();

                EnvoiyerMessages(socketChannel, "JOIN "+nom);

                new Thread(() -> {
                    try {
                        LireMessages(socketChannel);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }).start();

                while (true) {
                    String message = scanner.nextLine();
                    EnvoiyerMessages(socketChannel, message);
                    if("exit".equals(message.toLowerCase())) break;
                }

                System.out.println("Vous etes deconnecte du server.");
                socketChannel.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }        
    }

    private static void LireMessages(SocketChannel socketChannel) throws IOException{
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        while (socketChannel.isOpen()) {
            int bytesRead = socketChannel.read(buffer);
            if (bytesRead > 0) {
                buffer.flip();
                CharBuffer charBuffer = decoder.decode(buffer);
                System.out.println(charBuffer.toString());
                buffer.clear();
            }else if(bytesRead == -1){
                break;
            }
        }
    }

    private static void EnvoiyerMessages(SocketChannel socketChannel, String message) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(message.getBytes(Charset.forName("UTF-8")));
        socketChannel.write(buffer);
    }

}
