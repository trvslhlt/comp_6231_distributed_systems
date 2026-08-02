package vehiclerental.common.discovery;

import java.net.InetAddress;
import java.net.UnknownHostException;

/** Resolves the host this instance should advertise to etcd for others to call it back on. */
public final class HostResolver {

    private HostResolver() {}

    public static String resolveHost() throws UnknownHostException {
        String override = System.getenv("INSTANCE_HOST");
        return override != null ? override : InetAddress.getLocalHost().getHostAddress();
    }
}
