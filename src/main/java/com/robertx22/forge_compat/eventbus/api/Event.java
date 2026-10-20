package com.robertx22.forge_compat.eventbus.api;

/** Minimal Forge-shaped event base class. */
public class Event {
    private Result result = Result.DEFAULT;
    private boolean canceled = false;

    public enum Result { DENY, DEFAULT, ALLOW }

    public boolean isCancelable() { return false; }

    public void setCanceled(boolean cancel) {
        if (!isCancelable() && cancel) throw new IllegalArgumentException("Event is not cancelable");
        this.canceled = cancel;
    }

    public boolean isCanceled() { return canceled; }

    public boolean hasResult() { return false; }

    public Result getResult() { return result; }

    public void setResult(Result result) { this.result = result; }
}
