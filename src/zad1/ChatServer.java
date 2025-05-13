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
import java.text.SimpleDateFormat;
import java.util.*;

public class ChatServer {
    private final String host;
    private final int port;
    private Selector selector;
    private ServerSocketChannel serverChannel;
    private final StringBuilder serverLog = new StringBuilder();
    private final Map<SocketChannel, String> clientIds = new HashMap<>();
    private volatile boolean running = false;
    private final SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss.SSS");

    public ChatServer(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void startServer() throws IOException {
        selector = Selector.open();
        serverChannel = ServerSocketChannel.open();
        serverChannel.bind(new InetSocketAddress(host, port));
        serverChannel.configureBlocking(false);
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        running = true;
        new Thread(this::runLoop, "ChatServer-Selector").start();
        System.out.println("Server started\n");
    }

    private void runLoop() {
        try {
            while (running) {
                if (!selector.isOpen()) {
                    break;
                }
                try {
                    selector.select();
                } catch (ClosedSelectorException e) {
                    break;
                }
                if (!running || !selector.isOpen()) break;

                Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                while (it.hasNext()) {
                    SelectionKey key = it.next();
                    it.remove();
                    if (!key.isValid()) continue;
                    if (key.isAcceptable()) acceptClient();
                    else if (key.isReadable()) handleRead(key);
                }
            }
        } catch (IOException ignored) {
        }
    }


    private void acceptClient() throws IOException {
        SocketChannel sc = serverChannel.accept();
        sc.configureBlocking(false);
        sc.register(selector, SelectionKey.OP_READ, ByteBuffer.allocate(4096));
    }

    private void handleRead(SelectionKey key) {
        SocketChannel sc = (SocketChannel) key.channel();
        ByteBuffer buf = (ByteBuffer) key.attachment();
        try {
            int r = sc.read(buf);
            if (r <= 0) {
                disconnect(sc);
                return;
            }

            buf.flip();
            String data = StandardCharsets.UTF_8.decode(buf).toString();
            buf.clear();

            String[] lines = data.split("\n");
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty()) continue;

                if (line.startsWith("LOGIN ")) {
                    String id = line.substring(6);
                    clientIds.put(sc, id);
                    log(id + " logged in");
                    broadcast(id + " logged in");
                } else if (line.equals("LOGOUT")) {
                    String id = clientIds.get(sc);
                    if (id != null) {
                        log(id + " logged out");
                        broadcast(id + " logged out");
                        clientIds.remove(sc);
                    }
                    disconnect(sc);
                } else if (line.startsWith("MSG ")) {
                    String id = clientIds.get(sc);
                    if (id == null) continue;
                    String msg = line.substring(4);
                    log(id + ": " + msg);
                    broadcast(id + ": " + msg);
                }
            }
        } catch (IOException e) {
            disconnect(sc);
        }
    }

    private void disconnect(SocketChannel sc) {
        try {
            sc.close();
        } catch (IOException ignored) {}
        clientIds.remove(sc);
    }

    private void log(String entry) {
        String ts = df.format(new Date());
        serverLog.append(ts).append(" ").append(entry).append("\n");
    }

    private void broadcast(String msg) {
        ByteBuffer bb = ByteBuffer.wrap((msg + "\n").getBytes(StandardCharsets.UTF_8));
        for (SocketChannel sc : clientIds.keySet()) {
            try {
                if (sc.isOpen()) {
                    sc.write(bb.duplicate());
                }
            } catch (IOException ignored) {}
        }
    }


    public void stopServer() throws IOException {
        running = false;
        selector.wakeup();

        try {
            if (selector.isOpen()) {
                selector.select(200);
                Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                while (it.hasNext()) {
                    SelectionKey key = it.next();
                    it.remove();
                    if (key.isValid() && key.isReadable()) handleRead(key);
                }
            }
        } catch (IOException | ClosedSelectorException ignored) {}

        serverChannel.close();
        selector.close();
        System.out.println("Server stopped");
    }


    public String getServerLog() {
        return serverLog.toString();
    }
}





