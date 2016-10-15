import java.net.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Scanner;
import java.util.concurrent.locks.ReentrantLock;

public class Server implements IServerData {

	//------------------------------------
	//----------- Attributes -------------
	//------------------------------------
	private int registration_port;
	
	//TODO: Maybe this should become a class itself?
	private int max_peers; 
	private ArrayList<InetAddress> peers;
	private final ReentrantLock peers_locker = new ReentrantLock();
	
	private RegistrationDesk waiter;
	
	//----------------------------------
	//----------- Messages -------------
	//----------------------------------	
	private String msg_accept_registration()
	{
		return "ACCEPT_REGISTRATION";
	}
	
	private String msg_deny_and_suggest()
	{
		return "DENY_AND_SUGGEST";
	}
	
	//---------------------------------------------
	//------------ From IServerData ---------------
	//---------------------------------------------
	@Override
	public boolean push_new_peer(InetAddress peer_ip) 
	{
		peers_locker.lock();
		
		if( peers.size() < max_peers)
		{
			peers.add(peer_ip);
			peers_locker.unlock();
			
			return true;
		}
		else
		{
			peers_locker.unlock();
			return false;
		}
	}

	@Override
	public int peer_count() { return peers.size(); }

	@Override //TODO: Return unmodifiable iterator
	public Iterator<InetAddress> peers() { return peers.iterator(); }

	//------------------------------------------------
	//------------ External operations ---------------
	//------------------------------------------------
	public void launch() 
	{
		// Launch thread that waits for incoming registration requests
		this.waiter = new RegistrationDesk(this, this.registration_port);
		
		// TODO: launch thread that streams data using UDP protocol
	}
	
	public void shutdown()
	{		
		try {
			//Server can close only after RegistrationDesk 
			//object is done with its work
			this.waiter.shutdown();
			this.waiter.join();
		} catch (InterruptedException e) {
			System.err.println("RegistrationDesk thread was interrupted and was not correctly closed.");
			System.err.println(e.getMessage());
		}
	}
	
	//------------------------------------
	//---------- Constructors ------------
	//------------------------------------
	public Server()
	{
		this.registration_port = Parameters.DEFAULT_PORT.toInt();
		this.max_peers = 2;
		this.peers = new ArrayList<InetAddress>();
	}
	
	//------------------------------------
	public static void main(String[] args) throws InterruptedException 
	{
		Server s = new Server();
		s.launch();
		
		//TODO: Fix this. It's horrible.
		Scanner keystroke = new Scanner(System.in);
		keystroke.next();
		
		s.shutdown();
		
		return;
	}
}
