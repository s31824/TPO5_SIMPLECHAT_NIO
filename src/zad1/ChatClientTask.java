/**
 *
 *  @author Ogrodnik Piotr S31824
 *
 */

package zad1;

import java.util.List;
import java.util.concurrent.FutureTask;

public class ChatClientTask extends FutureTask<Void> {
    private final ChatClient client;
    private final List<String> msgs;
    private final int wait;

    private ChatClientTask(ChatClient client, List<String> msgs, int wait) {
        super(() -> {
            if (wait > 0) Thread.sleep(wait);
            client.login();
            if (wait > 0) Thread.sleep(wait);
            for (String msg : msgs) {
                client.send(msg);
                if (wait > 0) Thread.sleep(wait);
            }
            client.logout();
            if (wait > 0) Thread.sleep(wait);
            return null;
        });
        this.client = client;
        this.msgs = msgs;
        this.wait = wait;
    }

    public static ChatClientTask create(ChatClient client, List<String> msgs, int wait) {
        return new ChatClientTask(client, msgs, wait);
    }

    @Override
    public Void get() throws InterruptedException, java.util.concurrent.ExecutionException {
        return super.get();
    }

    public ChatClient getClient() {
        return client;
    }
}








