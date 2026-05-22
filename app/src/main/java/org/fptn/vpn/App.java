package org.fptn.vpn;

import android.app.Application;
import com.elvishew.xlog.LogConfiguration;
import com.elvishew.xlog.LogLevel;
import com.elvishew.xlog.XLog;
import com.elvishew.xlog.flattener.PatternFlattener;
import com.elvishew.xlog.printer.AndroidPrinter;
import com.elvishew.xlog.printer.file.FilePrinter;
import com.elvishew.xlog.printer.file.backup.FileSizeBackupStrategy2;
import com.elvishew.xlog.printer.file.clean.FileLastModifiedCleanStrategy;
import com.elvishew.xlog.printer.file.naming.DateFileNameGenerator;

import java.io.File;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        initXLog();
    }

    private void initXLog() {
        File logDir = new File(getFilesDir(), "logs2");
        if (!logDir.exists()) {
            logDir.mkdirs();
        }
        String logPath = logDir.getAbsolutePath() + File.separator;

        PatternFlattener flattener = new PatternFlattener("{d yyyy-MM-dd HH:mm:ss}|{l}|{t}|{m}");

        LogConfiguration config = new LogConfiguration.Builder()
                .logLevel(LogLevel.INFO)
                .tag("FPTN")
                .build();

        FilePrinter filePrinter = new FilePrinter.Builder(logPath)
                .fileNameGenerator(new DateFileNameGenerator())
                .backupStrategy(new FileSizeBackupStrategy2(4 * 1024, 3))
                .cleanStrategy(new FileLastModifiedCleanStrategy(2 * 24 * 60 * 60 * 1000L))
                .flattener(flattener)
                .build();
        XLog.init(config, filePrinter, new AndroidPrinter());

        XLog.i("XLog initialized successfully");
    }
}
