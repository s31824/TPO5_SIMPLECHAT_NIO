/**
 *
 *  @author Ogrodnik Piotr S31824
 *
 */

package zad1;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;

public class ChatClient {
    private final String host;
    private final int port;
    private final String id;
    private SocketChannel channel;
    private Selector selector;
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
        selector.close();
    }

    public String getChatView() {
        return chatView.toString();
    }

    private void openChannel() throws IOException {
        channel = SocketChannel.open(new InetSocketAddress(host, port));
        channel.configureBlocking(false);


        selector = Selector.open();
        channel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
    }

    private void sendRaw(String line) throws IOException {
        ByteBuffer bb = ByteBuffer.wrap((line + "\n").getBytes(StandardCharsets.UTF_8));
        while (bb.hasRemaining()) {
            channel.write(bb);
        }
    }

    private void startReader() {
        readerRunning = true;
        new Thread(() -> {
            ByteBuffer buf = ByteBuffer.allocate(4096);
            try {
                while (readerRunning) {
                    if (!selector.isOpen() || !channel.isOpen()) {
                        break;
                    }

                    selector.select();

                    if (!selector.isOpen() || !channel.isOpen()) {
                        break;
                    }

                    for (SelectionKey key : selector.selectedKeys()) {
                        selector.selectedKeys().remove(key);

                        if (key.isReadable()) {
                            buf.clear();
                            int r = channel.read(buf);
                            if (r < 0) break;
                            buf.flip();

                            String resp = StandardCharsets.UTF_8.decode(buf).toString();
                            synchronized (chatView) {
                                for (String line : resp.split("\n")) {
                                    if (!line.isEmpty()) {
                                        chatView.append(line).append("\n");
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (IOException ignored) {
            } finally {
                readerRunning = false;
            }
        }, "Reader-" + id).start();
    }



}







