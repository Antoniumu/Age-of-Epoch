package com.robertx22.library_of_exile.main;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ExileLog {
    public boolean print = true;
    private final Logger logger;

    private ExileLog(Logger logger) {
        this.logger = logger;
    }

    public static ExileLog get() {
        Class<?> caller = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass();
        return new ExileLog(LogManager.getLogger("Exile Log: " + caller.getSimpleName()));
    }

    public void debug(String s) { if (print) logger.debug(s); }
    public void log(String s) { if (print) logger.info(s); }
    public void info(String s) { if (print) logger.info(s); }
    public void warn(String s) { if (print) logger.warn(s); }
    public void onlyInConsole(String s) { if (print) System.out.println(s); }
}
