package pwp.client;

import lombok.Getter;
import pwp.client.ui.MainFrame;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.CompletionException;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Main {
    private static volatile boolean verbose;
    @Getter
    private static final Logger logger;

    static {
        logger = Logger.getLogger("BookClub");
        logger.setUseParentHandlers(false);
        logger.addHandler(new ConsoleHandler());
        logger.setLevel(Level.FINEST);
        logger.getHandlers()[0].setLevel(Level.FINEST);
    }

    public static void main(String[] args) {
        logger.info("Starting application");
        var list = Arrays.asList(args);
        verbose = list.contains("--verbose");
        if (!verbose) {
            logger.setLevel(Level.INFO);
            logger.getHandlers()[0].setLevel(Level.INFO);
        } else {
            logger.fine("Verbose output");
        }
        MainFrame.run(list);
    }

    public static Void handleException(Throwable t) {
        if (verbose) {
            if (t instanceof CompletionException && Objects.nonNull(t.getCause())) {
                t = t.getCause();
            }
            if (t instanceof Exception) {
                logger.log(
                        Level.WARNING,
                        t,
                        () -> "Caught an exception"
                );
            } else {
                logger.log(
                        Level.SEVERE,
                        t,
                        () -> "A Severe exception has occurred"
                );
            }
        } else {
            logger.warning("An exception occurred: " + t.getMessage());
        }
        return null;
    }
}
