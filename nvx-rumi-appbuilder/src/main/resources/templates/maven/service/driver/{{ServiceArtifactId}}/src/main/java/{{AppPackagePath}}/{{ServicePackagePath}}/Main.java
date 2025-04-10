package {{AppPackageName}}.{{ServicePackageName}};

import com.neeve.aep.AepEngine;
import com.neeve.aep.AepMessageSender;
import com.neeve.server.app.annotations.AppInjectionPoint;
import com.neeve.server.app.annotations.AppMain;

import {{AppPackageName}}.roe.*;

public class Main {
    private AepEngine _engine;
    private AepMessageSender _messageSender;

    @AppInjectionPoint
    final public void setEngine(AepEngine engine) {
        _engine = engine;
    }

    @AppInjectionPoint
    public void setMessageSender(AepMessageSender messageSender) {
        _messageSender = messageSender;
    }

    @AppMain
    public void main(String[] args) throws Exception {
        // wait for engine to connect to the messaging bus
        _engine.waitForMessagingToStart();

        // put code here to send messages
    }
}
