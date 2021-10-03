package org.danmayr.imagej.updater;

import org.danmayr.imagej.Version;


public interface UpdateListener {
    public enum State {
        NO_NEW_UPDATE,
        NEW_UPDATE_AVAILABLE,
        NO_INTERNET_CONNECTION,
        DOWNLOAD_FAILURE
    };

    abstract public void newUpdateAvailable(Release r, State s);
}
