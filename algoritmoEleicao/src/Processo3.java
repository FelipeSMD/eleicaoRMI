import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.rmi.AccessException;
import java.rmi.AlreadyBoundException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;



public class Processo3 implements P, Serializable {

	ArrayList<String> processos = new ArrayList<>();
	boolean foundgreater = false;
	static boolean electionInProgress = false;
	private static String thisProcess = "3";
	private static String coordinator;
	static P stub2;

	public Processo3() {

	}

	public static void main(String[] args) throws InterruptedException, AccessException, RemoteException {
		Processo3 obj = new Processo3();
		
		Registry reg=null;
		// Criando registro
	    try {
	        System.out.println("Criando registro...");
	        reg = LocateRegistry.createRegistry(9999);
	     } catch(Exception e){       
	    	 try {	     
	           reg = LocateRegistry.getRegistry(9999);
	     } catch(Exception ee){  System.exit(0); }
	    	 
	     }

		try {

			stub2 = (P) UnicastRemoteObject.exportObject(obj, 0);
			// Colocando o processo no registro
			reg = LocateRegistry.getRegistry(9999);
			reg.bind(thisProcess, stub2);

			System.err.println("Processo " + thisProcess + " está pronto!");
			//stub2.startElection(thisProcess);
		} catch (RemoteException e) {
			System.out.println("Não foi possivel registrar\n");
			e.printStackTrace();
		} catch (AlreadyBoundException e) {
			System.out.println("Processo já resgistrado\n");
			e.printStackTrace();
		}
		Runtime.getRuntime().addShutdownHook(new ShutDown());
		
		Thread.sleep(60000);
		
		
		Random rand = new Random();
		int randomNum = rand.nextInt(6) + 1;
		Thread.sleep(randomNum);
		if(reg.list().length > 1) {
			stub2.startElection(thisProcess);
		}
		else {
			while(reg.list().length <= 1) {
				Thread.sleep(100);
			}
			stub2.startElection(thisProcess);
		}
		
		repeat();

	}
	//timer para repetir a checagem do coordenador
	public static void repeat() {
		Random rand = new Random();
		int randomNum = rand.nextInt((6 - 1) + 1) + 1;
		Timer timer = new Timer();
		timer.schedule(new TimerCheck(), randomNum * 1000);
	}

	
	
	//Começando a eleição
	@Override
	public String startElection(String nodeId) throws RemoteException {
		electionInProgress = true;
		foundgreater = false;

		if (nodeId.equals(thisProcess)) {
			System.out.println("Você começou a eleição");

			Registry reg = LocateRegistry.getRegistry(9999);
			for (String nodeName : reg.list()) {
				if (!nodeName.equals(thisProcess) && Integer.parseInt(nodeName) > Integer.parseInt(thisProcess)) {
					System.out.println("Numero de processos no registro " + reg.list().length);
					P stub;
					try {
						stub = (P) reg.lookup(nodeName);
						System.out.println("Enviando candidatura para o processo " + nodeName);
						stub.startElection(nodeId);
						foundgreater = true;

					} catch (NotBoundException e) {
						// TODO Auto-generated catch block
						//e.printStackTrace()
						try {
							LocateRegistry.getRegistry(9999).unbind(nodeName);
						} catch (NotBoundException e1) {
							// TODO Auto-generated catch block
							//e1.printStackTrace();
							System.out.println("O processo não respondeu");
						}
					}catch (RemoteException e) {
						try {
							foundgreater = false;
							LocateRegistry.getRegistry(9999).unbind(nodeName);
						} catch (NotBoundException e1) {
							// TODO Auto-generated catch block
							//e1.printStackTrace();
							System.out.println("O processo não respondeu");
						}
					}
				}
			}
			if (!foundgreater) {
				iWon(thisProcess);
			}

			return null;
		} else {
			System.out.println("Pedido de eleição recebido de processo " + nodeId);
			sendOk(thisProcess, nodeId);
			return null;
		}
	}

	@Override
	public String sendOk(String where, String to) throws RemoteException {
		if (!thisProcess.equals(to)) {
			try {
				Registry reg = LocateRegistry.getRegistry(9999);
				P stub = (P) reg.lookup(to);
				System.out.println("Mandando te cala ai para processo" + to);
				stub.sendOk(where, to);

				// Continuando eleição depois de responder
				startElection(thisProcess);
			} catch (NotBoundException e) {
				//e.printStackTrace();
				System.out.println("O processo não respondeu");
			}
		} else {
			// ok recebido
			System.out.println(where + " respondendo com Ok..");
		}
		return null;
	}

	@Override
	public String iWon(String node) throws RemoteException {
		coordinator = node;
		electionInProgress = false;
		if (node.equals(thisProcess)) {
			// informando a vitoria
			System.out.println("Você ganhou a eleição.");
			System.out.println("Informando sua vitoria aos outros processos.....");
			Registry reg = LocateRegistry.getRegistry(9999);
			for (String nodeName : reg.list()) {
				if (!nodeName.equals(thisProcess)) {
					P stub;
					try {
						stub = (P) reg.lookup(nodeName);
						stub.iWon(node);

					} catch (NotBoundException e) {
						// TODO Auto-generated catch block
						//e.printStackTrace();
						System.out.println("O processo não respondeu");
					}
				}
			}

			System.out.println("Processo " + node + " é o novo coordenador\n");
		} else {
			// recebendo resultado
			System.out.println("Processo " + node + " ganhou a eleição.");
			System.out.println("Processo " + node + " é o novo coordenador\n");
		}
		return null;
	}

	@Override
	public String register(String id) throws RemoteException {
		processos.add(id);
		return null;
	}

	static class TimerCheck extends TimerTask {

		@Override
		public void run() {
			if (!thisProcess.equals(coordinator) && !electionInProgress) {
				try {
					Registry reg = LocateRegistry.getRegistry(9999);
					P stub;
					stub = (P) reg.lookup(coordinator);
					stub.isalive();

				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					// e.printStackTrace();
					 coordinatorCrashed();
				} catch (NotBoundException e) {
					// TODO Auto-generated catch block
					// e.printStackTrace();
					coordinatorCrashed();
				}
			}
			repeat();

		}
	}

	private static void coordinatorCrashed() {
		System.out.println("Coordenator morreu. Iniciar nova elição.");
		try {
			stub2.startElection(thisProcess);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Override
	public boolean isalive() throws RemoteException {
		// TODO Auto-generated method stub
		return true;
	}

	static class ShutDown extends Thread {
		@Override
		public void run() {
			// TODO Auto-generated method stub
			super.run();
			try {
				System.out.println("Encerrando processo...");
				LocateRegistry.getRegistry(9999).unbind(thisProcess);
			} catch (AccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NotBoundException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				System.out.println("O processo não respondeu");
			}
		}

	}
}
