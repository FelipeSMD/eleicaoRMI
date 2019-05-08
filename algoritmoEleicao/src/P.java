import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface P extends Remote {

	

	public String startElection(String nodeId) throws RemoteException;

	public String sendOk(String where, String to) throws RemoteException;

	public String iWon(String node) throws RemoteException;

	public String register(String id) throws RemoteException;

	public boolean isalive() throws RemoteException;
}
