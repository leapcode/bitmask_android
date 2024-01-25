package se.leap.bitmaskclient.tor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.content.Context;
import android.content.pm.PackageManager;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

@RunWith(MockitoJUnitRunner.class)
public class TorStatusObservableTest {


    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mockContext;



    @Test
    public void testPropertyChange() throws PackageManager.NameNotFoundException {
        TorStatusObservable statusObservable = TorStatusObservable.getInstance();
        int i = 10;
        PropertyChangeListener propertyChangeListener = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                assertEquals(12, TorStatusObservable.getBootstrapProgress());
                assertEquals("a logkey", TorStatusObservable.getLastLogs().lastElement());
                assertNull(evt.getOldValue());
                TorStatusObservable.getInstance().deleteObserver(this);
            }
        };

        TorStatusObservable.updateState(mockContext, "STARTING", 10, "a logkey");
        TorStatusObservable.updateState(mockContext, "STARTING", 11, "a log 2");
        TorStatusObservable.getInstance().addObserver(propertyChangeListener);
        TorStatusObservable.updateState(mockContext, "STARTING", 12, "a log 3");

    }

}