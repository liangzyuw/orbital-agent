package Mobile;
import java.io.*;
import java.rmi.*;
import java.lang.reflect.*;

import java.util.Vector;

/**
 * Mobile.Agent is the base class of all user-define mobile agents. It carries
 * an agent identifier, the next host IP and port, the name of the function to
 * invoke at the next host, arguments passed to this function, its class name,
 * and its byte code. It runs as an independent thread that invokes a given
 * function upon migrating the next host.
 *
 * @author  Munehiro Fukuda
 * @version %I% %G%
 * @since   1.0
 */
public class Agent implements Serializable, Runnable {
    // live data to carry with the agent upon a migration
    protected int agentId        = -1;    // this agent's identifier
    private String _hostname     = null;  // the next host name to migrate
    private String _function     = null;  // the function to invoke upon a move
    private int _port            = 0;     // the next host port to migrate
    private String[] _arguments  = null;  // arguments pass to _function
    private String _classname    = null;  // this agent's class name
    private byte[] _bytecode     = null;  // this agent's byte code

    // additional feature - agent history to keep track of places been and what it has called 
    // protected Vector<Pair<String, String>> visitHistory = new Vector<Pair<String, String>>();
    protected Vector<String> executionTrace = new Vector<String>();
    // current place name where the agent resides (used by Place)
    public String currentPlace = null;

    public void setCurrentPlace(String place) {
        // System.out.println("SETCURRENTPLACE() CALLED with place: " + place);
        currentPlace = place;
    }

    public String getCurrentPlace() {
        return currentPlace;
    }

    public void addTrace(String placeName) {
        String argsText = "";

        if (_arguments == null) {
            argsText = "no arguments";
        } 
        else {
            // keep track of what method and args were called by the agent at a place
            for (int i = 0; i < _arguments.length; i++) {
                argsText += _arguments[i];

                if (i < _arguments.length - 1) {
                    argsText += ", ";
                }
            }
        }

        String entry = "place=" + placeName + ", function=" + _function + ", args=[" + argsText + "]";
        executionTrace.add(entry);
    }

    // getter method for execution trace
    public String getExecutionTrace() {
        String result = "";

        for (int i = 0; i < executionTrace.size(); i++) {

            result += "[" + i + "] " + executionTrace.get(i) + "\n";
        }

        return result;
    }

    /**
     * setPort( ) sets a port that is used to contact a remote Mobile.Place.
     * 
     * @param port a port to be set.
     */
    public void setPort( int port ) {
	this._port = port;
    }

    /**
     * setId( ) sets this agent identifier: agentId.
     *
     * @param id an idnetifier to set to this agent.
     */
    public void setId( int id ) {
	this.agentId = id;
    }

    /**
     * getId( ) returns this agent identifier: agentId.
     *
     * @param this agent's identifier.
     */
    public int getId( ) {
	return agentId;
    }

    /**
     * getByteCode( ) reads a byte code from the file whosename is given in
     * "classname.class".
     *
     * @param classname the name of a class to read from local disk.
     * @return a byte code of a given class.
     */
    public static byte[] getByteCode( String classname ) {
	// create the file name
	String filename = classname + ".class";

	// allocate the buffer to read this agent's bytecode in
	File file = new File( filename );
	byte[] bytecode = new byte[( int )file.length( )];

	// read this agent's bytecode from the file.
	try {
	    BufferedInputStream bis =
		new BufferedInputStream( new FileInputStream( filename ) );
	    bis.read( bytecode, 0, bytecode.length );
	    bis.close( );
	} catch ( Exception e ) {
	    e.printStackTrace( );
	    return null;
	}

	// now you got a byte code from the file. just return it.
	return bytecode;	
    }

    /**
     * getByteCode( ) reads this agent's byte code from the corresponding file.
     *
     * @return a byte code of this agent.
     */
    public byte[] getByteCode( ) {
	if ( _bytecode != null ) // bytecode has been already read from a file
	    return _bytecode; 
	
	// obtain this agent's class name and file name
	_classname = this.getClass( ).getName( );
	_bytecode = getByteCode( _classname );

	return _bytecode;
    }

    /**
     * run( ) is the body of Mobile.Agent that is executed upon an injection
     * or a migration as an independent thread. run( ) identifies the method 
     * with a given function name and arguments and invokes it. The invoked
     * method may include hop( ) that transfers this agent to a remote host or
     * simply returns back to run( ) that termiantes the agent.
     */
    public void run( ) {

        // Implement by yourself.
        // Agent.run( ) must perform the following tasks:

        try {

            // the class of the agent
            Class c = this.getClass();

            // (1) Find the method to invoke, through this.getClass( ).getMethod( ). 
            Method m;
            
            // check if there is a method with a specific function name and arguments 
            // (2) Invoke this method through Method.invoke( ).
            if (_arguments == null) {
                m = c.getMethod(_function);
                //Since the method belongs to this agent object
                m.invoke(this); // wraps hop inside InvocationTargetException
            }
            else {
                // Find a public method whose name is stored in _function and whose parameter type is String[]
                m = c.getMethod(_function, String[].class); //ex. if _function = "step"; then java look for public void step(String[] args)
                m.invoke(this, (Object) _arguments); // call that method on this agent object and pass _arguments into it
            }
        } catch (InvocationTargetException e) { // so it doesnt show ThreadDeath error
            Throwable cause = e.getCause();
            if (cause instanceof ThreadDeath) {
                throw (ThreadDeath) cause; // if the cause is ThreadDeath rethrow it so the thread is actually allowed to die
                // for weak migration
            }
            cause.printStackTrace();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * hop( ) transfers this agent to a given host, and invoeks a given
     * function of this agent.
     *
     * @param hostname the IP name of the next host machine to migrate
     * @param function the name of a function to invoke upon a migration
     */    
    public void hop( String hostname, String function ) {
	hop( hostname, function, null );
    }

    /**
     * hop( ) transfers this agent to a given host, and invoeks a given
     * function of this agent as passing given arguments to it.
     *
     * @param hostname the IP name of the next host machine to migrate
     * @param function the name of a function to invoke upon a migration
     * @param args     the arguments passed to a function called upon a 
     *                 migration.
     */
    @SuppressWarnings( "deprecation" )
    public void hop( String hostname, String function, String[] args ) {

        try {
            // save the next hostname, function name, and arguments
            _hostname = hostname;
            _function = function;
            _arguments = args;

            // (1) Load this agent’s byte code into the memory
            getByteCode();
    
            // (2) Serialize this agent into a byte array.
            byte[] byteArray = serialize(); // turns the whole agent object into bytes.

            // (3) Find a remote place through Naming.lookup( ).
            String url = "rmi://" + _hostname + ":" + _port + "/Place";
            
            // since naming.lookup returnsa a generic remote object,
            // need to call the RMI method in PlaceInterface
            PlaceInterface place = (PlaceInterface) Naming.lookup(url);

            // (4) Invoke an RMI call.
            place.transfer(_classname, _bytecode, byteArray);

            // (5) Kill this agent with Thread.currentThread( ).stop( ) 
            Thread.currentThread().stop(); // throws ThreadDeath
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * serialize( ) serializes this agent into a byte array.
     *
     * @return a byte array to contain this serialized agent.
     */
    private byte[] serialize( ) {
	try {
	    // instantiate an object output stream.
	    ByteArrayOutputStream out = new ByteArrayOutputStream( );
	    ObjectOutputStream os = new ObjectOutputStream( out );
	    
	    // write myself to this object output stream
	    os.writeObject( this );

	    return out.toByteArray( ); // conver the stream to a byte array
	} catch ( IOException e ) {
	    e.printStackTrace( );
	    return null;
	}
    }
}