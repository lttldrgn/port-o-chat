package portochat.client.util;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingWorker;
import portochat.common.VersionInfo;

/**
 * Class to handle client version checking
 * @author Brandon
 */
public class VersionChecker {
    public enum VersionResultEnum {
        UP_TO_DATE, OUT_OF_DATE, CONNECTION_ERROR;
    }
    public interface VersionResultCallback {
        public void onResult(VersionResultEnum result);
    }
    private static final Logger logger = Logger.getLogger(VersionChecker.class.getName());
    
    public static void checkVersion(final VersionResultCallback callback) {
        final SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {

            @Override
            protected Boolean doInBackground() throws Exception {
                return VersionInfo.isSoftwareCurrent();
            }

        };
        worker.addPropertyChangeListener(new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent event) {
                if ("state".equals(event.getPropertyName())
                        && SwingWorker.StateValue.DONE == event.getNewValue()) {

                    boolean upToDate = true;
                    try {
                        upToDate = worker.get();
                    } catch (InterruptedException ex) {
                        logger.log(Level.INFO, null, ex);
                    } catch (ExecutionException ex) {
                        logger.log(Level.INFO, "Error checking for update", ex);
                        callback.onResult(VersionResultEnum.CONNECTION_ERROR);
                        return;
                    }

                    if (!upToDate) {
                        callback.onResult(VersionResultEnum.OUT_OF_DATE);
                    } else {
                        callback.onResult(VersionResultEnum.UP_TO_DATE);
                    }
                }
            }

        });
        worker.execute();
    }
}
