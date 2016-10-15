import java.net.InetAddress;
import java.util.Iterator;

/**
 * We'll use this to interact with the server while
 * begin able to low coupling between RegistrationHandler
 * and the Server class itself
 * @author luis
 */
public interface IServerData {
	
	/**
	 * Try to push a new peer to server
	 * @param peer_ip Peer address
	 * @return True if max number of peers wasn't reach, false otherwise
	 */
	public boolean push_new_peer(InetAddress peer_ip);
	
	public int peer_count();
	
	public Iterator<InetAddress> peers();
}
