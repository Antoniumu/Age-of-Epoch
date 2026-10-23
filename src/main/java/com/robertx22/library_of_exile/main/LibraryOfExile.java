package com.robertx22.library_of_exile.main;

import com.robertx22.library_of_exile.events.base.ExileEvents;

public class LibraryOfExile {

    public static boolean runDevTools() {
        ExileEvents.OnCheckIsDevToolsRunning event = ExileEvents.CHECK_IF_DEV_TOOLS_SHOULD_RUN.callEvents(new ExileEvents.OnCheckIsDevToolsRunning());
        return event.run;
    }

}
