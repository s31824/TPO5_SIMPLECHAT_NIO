/**
 *
 *  @author Ogrodnik Piotr S31824
 *
 */

package zad1;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

public class ChatClient {
    private final String host;
    private final int port;
    private final String id;
    private SocketChannel channel;
    private final StringBuilder chatView;
    private volatile boolean readerRunning = false;

    public ChatClient(String host, int port, String id) {
        this.host = host;
        this.port = port;
        this.id = id;
        this.chatView = new StringBuilder("=== " + id + " chat view ===\n");
    }

    public void login() throws IOException {
        openChannel();
        startReader();
        sendRaw("LOGIN " + id);
    }

    public void send(String msg) throws IOException {
        sendRaw("MSG " + msg);
    }

    public void logout() throws IOException {
        sendRaw("LOGOUT");
        try {
            Thread.sleep(50);
        } catch (InterruptedException ignored) {
        }
        readerRunning = false;
        channel.close();
    }

    public String getChatView() {
        return chatView.toString();
    }

    private void openChannel() throws IOException {
        channel = SocketChannel.open(new InetSocketAddress(host, port));
        channel.configureBlocking(true);
    }

    private void sendRaw(String line) throws IOException {
        ByteBuffer bb = ByteBuffer.wrap((line + "\n").getBytes(StandardCharsets.UTF_8));
        channel.write(bb);
    }

    private void startReader() {
        readerRunning = true;
        new Thread(() -> {
            ByteBuffer buf = ByteBuffer.allocate(4096);
            try {
                while (readerRunning) {
                    buf.clear();
                    int r = channel.read(buf);
                    if (r < 0) break;
                    if (r == 0) {
                        Thread.sleep(10);
                        continue;
                    }
                    buf.flip();
                    String resp = StandardCharsets.UTF_8.decode(buf).toString();
                    synchronized (chatView) {
                        for (String l : resp.split("\n")) {
                            if (!l.isEmpty()) chatView.append(l).append("\n");
                        }
                    }
                }
            } catch (IOException | InterruptedException ignored) {
            }
        }, "Reader-" + id).start();
    }
}




