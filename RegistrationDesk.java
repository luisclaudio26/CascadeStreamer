import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Here we wait for someone to request a connection
 * with the server; when request is made, we launch
 * a new thread to treat it.
 */
public class RegistrationDesk extends Thread 
{
	//------------------------------
	//--------- Attributes ---------
	//------------------------------
	private int port;
	private boolean running;
	private IServerData server;
	private ServerSocket listener_socket;
	
	//---------------------------
	//--------- Methods ---------
	//---------------------------
	public RegistrationDesk(IServerData server, int registration_port)
	{
		this.port = registration_port;
		this.server = server;
		this.running = true;
		this.listener_socket = null;
		this.start();
	}

	public void shutdown()
	{
		this.running = false;
		
		try {
			this.listener_socket.close();
		} catch (IOException e) {
			System.err.println("Error while closing RegistrationDesk listener socket.");
			System.err.println( e.getMessage());
		}
	}
	
	//-------------------------------
	//--------- From Thread ---------
	//-------------------------------
	@Override
	public void run() 
	{
		// Holds all processes launched so we 
		// can wait for them to die in peace.
		List<RegistrationHandler> children = new ArrayList<RegistrationHandler>();
					
		try 
		{
			listener_socket = new ServerSocket(this.port);
			
			// accept incoming connections and launch them
			// on separate threads while server is running
			while(running)
			{
				System.out.println("Waiting for connection: ");
				
				Socket incoming_connection = listener_socket.accept();
				RegistrationHandler p = new RegistrationHandler(server, incoming_connection);
				children.add(p);
			}
			
			// Wait for children to die.
			for(RegistrationHandler p : children)
				try {
					p.join();
				} catch (InterruptedException e1) {
					System.err.println("Error joining children thread in WaitRegistration.");
					System.err.println(e1.getMessage());
				}
			
			// shutdown socket
			try {
				listener_socket.close();
			} catch(IOException e) {
				System.err.println("Error while closing socket: ");
				System.err.println(e.getMessage());
			}
		} 
		catch(IOException e) 
		{	
			//If SocketException was thrown and running flag was set to false,
			//this means we closed socket intentionally, so nothing is wrong!
			if(!running) return;
			
			//If we reached this part, socket was closed while server was
			//running; there's a problem!
			System.err.println("Error while opening socket: ");
			System.err.println(e.getMessage());
		}
	}
}
