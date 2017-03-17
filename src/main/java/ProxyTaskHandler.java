import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

/**
 * Handler for client/server communications.
 */
public class ProxyTaskHandler {

    private final SocketChannel clientChannel;
    private final SocketChannel serverChannel;
    private final Selector selector;
    private final ProxyBuffer clientBuffer = new ProxyBuffer();
    private final ProxyBuffer serverBuffer = new ProxyBuffer();

    public ProxyTaskHandler(SocketChannel clientChannel, SocketChannel serverChannel, Selector selector) {
        this.clientChannel = clientChannel;
        this.serverChannel = serverChannel;
        this.selector = selector;
    }

    public void handle(SelectionKey selectionKey) {
        try {
            if (selectionKey.channel() == clientChannel) {
                if (selectionKey.isReadable()) readFromClient();
                if (selectionKey.isWritable()) writeToClient();
            } else if (selectionKey.channel() == serverChannel) {
                if (selectionKey.isReadable()) readFromServer();
                if (selectionKey.isWritable()) writeToServer();
            }
        } catch (final IOException exception) {
            exception.printStackTrace();
            ServerSocketThread.closeQuietly(clientChannel);
            ServerSocketThread.closeQuietly(serverChannel);
        }
    }

    private void readFromClient() throws IOException {
        serverBuffer.writeFrom(clientChannel);
        if (serverBuffer.isReadyToRead()) register();
    }

    private void readFromServer() throws IOException {
        clientBuffer.writeFrom(serverChannel);
        if (clientBuffer.isReadyToRead()) register();
    }

    private void writeToClient() throws IOException {
        clientBuffer.writeTo(clientChannel);
        if (clientBuffer.isReadyToWrite()) register();
    }

    private void writeToServer() throws IOException {
        serverBuffer.writeTo(serverChannel);
        if (serverBuffer.isReadyToWrite()) register();
    }

    private void register() throws ClosedChannelException {
        int clientOps = 0;
        if (serverBuffer.isReadyToWrite()) clientOps |= SelectionKey.OP_READ;
        if (clientBuffer.isReadyToRead()) clientOps |= SelectionKey.OP_WRITE;
        clientChannel.register(selector, clientOps, this);

        int serverOps = 0;
        if (clientBuffer.isReadyToWrite()) serverOps |= SelectionKey.OP_READ;
        if (serverBuffer.isReadyToRead()) serverOps |= SelectionKey.OP_WRITE;
        serverChannel.register(selector, serverOps, this);
    }

}
