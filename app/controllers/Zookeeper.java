package controllers;

import java.net.InetAddress;
import java.net.Inet4Address;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.io.IOException;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.nio.charset.Charset;

import javax.inject.*;

import play.Logger;
import play.Configuration;
import play.libs.Json;
import play.inject.ApplicationLifecycle;
import play.libs.F;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Singleton
public class Zookeeper {
	private ZooKeeper zooKeeper;
	private AtomicBoolean zooKeeperRegistered = new AtomicBoolean (false);

	@Inject
	public Zookeeper(ApplicationLifecycle lifecycle, Configuration conf) {
		final String zooKeeperHosts = conf.getString ("zooKeeper.hosts", null);
		if (zooKeeperHosts == null) {
			Logger.warn ("ZooKeeper not configured");
		} else {
			final int zooKeeperTimeout = conf.getInt ("zooKeeper.timeoutInMillis", 10000);

			final String applicationDomain = conf.getString ("application.domain", "localhost");
			final int httpPort = conf.getInt ("http.port", 9000);
			final String httpAddress = conf.getString ("http.address", "0.0.0.0");
			final String destinationIp;

			Logger.debug ("Http address: " + httpAddress);

			if ("0.0.0.0".equals (httpAddress)) {
				destinationIp = getPublicIp ();
			} else {
				destinationIp = httpAddress;
			}

			if (destinationIp == null) {
				Logger.error ("Failed to determine the public IP of this server, skipping Zookeeper registration.");
			} else {
				final ObjectNode configuration = Json.newObject ();
				final ArrayNode proxy = configuration.putArray ("proxy");
				final ObjectNode proxyLine = proxy.addObject ();

				final String context = conf.getString ("play.http.context", "/");

				proxyLine.put ("type", "http");
				proxyLine.put ("path", context);
				proxyLine.put ("domain", applicationDomain);
				proxyLine.put ("destination", "http://" + destinationIp + ":" + httpPort + context);

				final String zooKeeperConfiguration = Json.stringify (configuration);

				Logger.info ("Connecting to ZooKeeper cluster: " + zooKeeperHosts);
				Logger.info ("Sending ZooKeeper configuration: " + configuration);

				try {
					zooKeeper = new ZooKeeper (zooKeeperHosts, zooKeeperTimeout, (event) -> handleZooKeeperEvent (event, applicationDomain, zooKeeperConfiguration), false);
					lifecycle.addStopHook(() -> {
						zooKeeper.close();
						return F.Promise.pure(null);
			        });
				} catch (IOException e) {
					zooKeeper = null;
					Logger.error ("Failed to connect to a ZooKeeper instance", e);
					throw new RuntimeException (e);
				}
			}
		}
	}
	
	private String getPublicIp () {
		try {
			final Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces ();

			while (networkInterfaces.hasMoreElements ()) {
				final NetworkInterface networkInterface = networkInterfaces.nextElement ();

				// Skip loopback point-to-point interfaces and interfaces that are not up:
				if (networkInterface.isLoopback () || networkInterface.isPointToPoint () || !networkInterface.isUp ()) {
					continue;
				}

				final Enumeration<InetAddress> addresses = networkInterface.getInetAddresses ();
				while (addresses.hasMoreElements ()) {
					final InetAddress address = addresses.nextElement ();

					if (address.isLoopbackAddress () || address.isMulticastAddress ()) {
						continue;
					}

					if (address instanceof Inet4Address) {
						return address.getHostAddress ();
					}
				}
			}

			return null;
		} catch (SocketException e) {
			throw new RuntimeException ("Failed to determine public IP of this server.");
		}
	}

	private void handleZooKeeperEvent (final WatchedEvent event, final String applicationDomain, final String configuration) {
		switch (event.getState ()) {
		case AuthFailed:
			break;
		case ConnectedReadOnly:
			break;
		case Disconnected:
			break;
		case Expired:
			break;
		case SaslAuthenticated:
			break;
		case SyncConnected:
			registerApplication (applicationDomain, configuration);
			break;
		default:
			break;
		}

	}

	private void registerApplication (final String applicationDomain, final String configuration) {

		if (!zooKeeperRegistered.compareAndSet (false, true)) {
			return;
		}

		try {
			createPublicPath ("/services");
			createPublicPath ("/services/web");
			createPublicPath ("/services/web/domains");
			createPublicPath ("/services/web/domains/" + applicationDomain);

			final String zooKeeperPath = zooKeeper.create (String.format ("/services/web/domains/%s/", applicationDomain), configuration.getBytes (Charset.forName ("UTF-8")), Ids.READ_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);

			Logger.info ("Registered at path: " + zooKeeperPath);
		} catch (KeeperException | InterruptedException e) {
			Logger.error ("Failed to register this application with ZooKeeper", e);
			throw new RuntimeException (e);
		}
	}

	private void createPublicPath (final String path) throws InterruptedException, KeeperException {
		try {
			if (zooKeeper.exists (path, null) == null) {
				zooKeeper.create (path, null, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
			}
		} catch (KeeperException e) {
			if (!Code.NODEEXISTS.equals (e.code ())) {
				throw e;
			}
		}
	}
}