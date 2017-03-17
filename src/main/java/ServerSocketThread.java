import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Set;

/**
 * Thread implementation for listening local port. For each new socket-accept operation {@link ProxyTaskHandler}
 * instance created and attached to client and remote server channels.
 */
public class ServerSocketThread extends Thread {
    private final static long SELECTOR_TIMEOUT = 100;

    private final ProxySettings proxySettings;
    private Selector selector;

    public ServerSocketThread(final ProxySettings proxySettings) throws IOException {
        this.proxySettings = proxySettings;
        this.selector = initSelector();
    }

    public void run() {
        System.out.println(String.format("*** Listen local port %s for redirecting to host %s and port %s ***", proxySettings.getLocalPort(), proxySettings.getRemoteHost(), proxySettings.getRemotePort()));
        try {
            while (true) {
                selector.select(SELECTOR_TIMEOUT);
                final Set<SelectionKey> keys = selector.selectedKeys();
                for (final SelectionKey key : keys) {
                    if (!key.isValid()) {
                        continue;
                    }
                    if (key.isAcceptable()) {
                        acceptConnection(key);
                    } else {
                        final ProxyTaskHandler handler = (ProxyTaskHandler) key.attachment();
                        if (handler != null) {
                            handler.handle(key);
                        }
                    }
                }
                keys.clear();
            }
        } catch (IOException e) {
            throw new RuntimeException("Couldn't open server socket on port " + proxySettings.getLocalPort(), e);
        } finally {
            closeQuietly(selector);
        }
    }

    private Selector initSelector() throws IOException {
        final Selector socketSelector = Selector.open();
        final ServerSocketChannel serverChannel = ServerSocketChannel.open();
        final InetSocketAddress isa = new InetSocketAddress(this.proxySettings.getLocalPort());
        serverChannel.socket().bind(isa);
        serverChannel.configureBlocking(false);
        serverChannel.register(socketSelector, SelectionKey.OP_ACCEPT);
        return socketSelector;
    }

    private void acceptConnection(SelectionKey key) throws IOException {
        final ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
        final SocketChannel socketChannel = serverSocketChannel.accept();
        socketChannel.configureBlocking(false);
        SocketChannel remoteSocketChannel = initRemoteServerChannel();
        final ProxyTaskHandler proxyTaskHandler = new ProxyTaskHandler(socketChannel, remoteSocketChannel, selector);
        socketChannel.register(this.selector, SelectionKey.OP_READ, proxyTaskHandler);
        remoteSocketChannel.register(this.selector, SelectionKey.OP_READ, proxyTaskHandler);
    }

    private SocketChannel initRemoteServerChannel() throws IOException {
        final InetSocketAddress socketAddress = new InetSocketAddress(
                proxySettings.getRemoteHost(), proxySettings.getRemotePort());
        final SocketChannel remoteServerChannel = SocketChannel.open();
        remoteServerChannel.connect(socketAddress);
        remoteServerChannel.configureBlocking(false);
        return remoteServerChannel;
    }

    public static void closeQuietly(final Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (IOException e) {
        }
    }
}
