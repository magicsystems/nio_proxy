import java.io.IOException;
import java.util.Collection;

/**
 * Proxy server - loads configuration and starting {@link ServerSocketThread} for each specified local port
 */
public class Server {

    public static void startServer() throws IOException {
        final Collection<ProxySettings> proxyListSettings = Configurer.readConfiguration();
        for (ProxySettings proxySettings : proxyListSettings) {
            new ServerSocketThread(proxySettings).start();
        }
    }

    public static void main(String[] args) throws IOException {
        System.out.println("*** Start server ***");
        Server.startServer();
    }
}
