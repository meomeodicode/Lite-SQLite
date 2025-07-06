package lite.sqlite.cli;

/**
 * Interface for command-line interfaces.
 * This interface defines the basic operations of a CLI.
 */
public interface CLI {
    /**
     * Starts the command-line interface.
     */
    void start();
    
    /**
     * Stops the command-line interface.
     */
    void stop();
}