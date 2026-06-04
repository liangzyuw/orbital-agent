package Mobile;

import java.io.*;
import java.net.*;
import java.rmi.*;
import java.rmi.server.*;
import java.rmi.registry.*;

// Place represents a station
public class Place extends UnicastRemoteObject implements PlaceInterface {
    private AgentLoader loader = null;
    private int agentSequencer = 0;
    private String placeName = null;
    private StationData station;
    private String rmiUrl;

    /**
     * This constructor instantiates a Mobiel.AgentLoader object that
     * is used to define a new agen class coming from remotely.
     */
    public Place(String stationName, int port) throws RemoteException {
        super( );
        loader = new AgentLoader( );

        // get place name for agent visit history 
        try {
            placeName = java.net.InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            placeName = "unknown";
        }

        rmiUrl = "rmi://" + placeName + ":" + port + "/Place";

        try {
            station =
                StationLoader.loadStation(
                    "stations_formatted.txt",
                    stationName
                );

            System.out.println(
                "Loaded station: " +
                station.name
            );
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * deserialize( ) deserializes a given byte array into a new agent.
     * @param buf a byte array to be deserialized into a new Agent object.
     * @return a deserialized Agent object
     */
    private Agent deserialize( byte[] buf ) 
	throws IOException, ClassNotFoundException {
	    // converts buf into an input stream
        ByteArrayInputStream in = new ByteArrayInputStream( buf );

        // AgentInputStream identify a new agent class and deserialize
        // a ByteArrayInputStream into a new object
        AgentInputStream input = new AgentInputStream( in, loader );
        return ( Agent )input.readObject();
    }

    /**
     * transfer( ) accepts an incoming agent and launches it as an independent
     * thread.
     *
     * @param classname The class name of an agent to be transferred.
     * @param bytecode  The byte code of  an agent to be transferred.
     * @param entity    The serialized object of an agent to be transferred.
     * @return true if an agent was accepted in success, otherwise false.
     */
    public boolean transfer( String classname, byte[] bytecode, byte[] entity )
	    throws RemoteException {
        try {
            // called remotely by Agent.hop()
            // classname - name of incoming agent class
            // bytecode  - actual .class file contents
            // entity    - serialized agent object

            // (1) Register this calling agent’s classname and bytecode into AgentLoader.
            loader.loadClass(classname, bytecode);

            // (2) Deserialize this agent’s entity through deserialize( entity ).
            Agent agent = deserialize(entity);

            // (3) Set this agent’s identifier if it has not yet been set
            if (agent.getId() == -1) {
                agent.setId(agentSequencer++);
            }

            // if (agent instanceof OrbitalAgent) {
            //     ((OrbitalAgent) agent).setStationData(station);
            // }

            // agent.setCurrentPlace(placeName);
            agent.setCurrentPlace(rmiUrl);
            agent.addTrace(placeName); // add place to agent's visit history
            
            // (4) Instantiate a Thread object as passing the deserialized agent to the constructor.
            Thread thread = new Thread(agent); // accepts an agent and launchs it as an independent thread

            // (5) Invoke this thread’s start( ) method.
            thread.start(); // calls agent.run()

            // (6) Return true if everything is done in success, otherwise false.
            return true;
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        
    }

    // retrieves the StationData object of this Place
    public StationData getStationData() throws RemoteException {

        return station;
    }

    /**
     * main( ) starts an RMI registry in local, instantiates a Mobile.Place
     * agent execution platform, and registers it into the registry.
     * @param args receives a port, (i.e., 5001-65535).
     * java Mobile.Place 58090 "ISS"
     */
    public static void main( String args[] ) {

        try {
            // Read args[0] as the port number and checks its validity. 
            int port = Integer.parseInt(args[0]);

            // get station name for this place
            String stationName = args[1];

            // Invoke startRegistry( int port )
            startRegistry(port);

            // Instantiate a Place object
            Place place = new Place(stationName, port);

            // Register it into rmiregistry through Naming,rebind()
            String url = "rmi://localhost:" + port + "/Place";
            Naming.rebind(url, place);

            // for debugging
            System.out.println("Station is ready");
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }
    
    /**
     * startRegistry( ) starts an RMI registry process in local to this Place.
     * 
     * @param port the port to which this RMI should listen.
     */
    private static void startRegistry( int port ) throws RemoteException {
        try {
            Registry registry =
                LocateRegistry.getRegistry( port );
            registry.list( );
        }
        catch ( RemoteException e ) {
            Registry registry =
                LocateRegistry.createRegistry( port );
        }
    }
}