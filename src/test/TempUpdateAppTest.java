package test;

import com.company.TempUpdateApp;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.*;

public class TempUpdateAppTest {

    String[] tester = {"--field", "ambientTemp", "--field", "schedule", "/tmp/ehub_data", "2016-01-01T09:34"};
    TempUpdateApp app;
    CommandLine cmd;


    @Before
    public void setUp() throws Exception {
        app = new TempUpdateApp();
        cmd = new CommandLine(app);
    }

    @Test
    public void call() {
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        int exitCode = cmd.execute(tester);
        assertEquals(0, exitCode);
    }

    @After
    public void tearDown() throws Exception {
        app = null;
        cmd = null;
    }


}