package se.leap.bitmaskclient.pluggableTransports;

import client.EventLogger;

public interface PtClientInterface extends EventLogger {
    int start();
    void stop();
    boolean isStarted();
}
