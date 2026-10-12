package dev.architectury.event;

/** Forge stand-in (see dev.architectury.impl.ForgeShim): a listener's answer, as in Architectury. */
public final class EventResult {
    private static final EventResult PASS = new EventResult(false, null);
    private static final EventResult TRUE = new EventResult(true, Boolean.TRUE);
    private static final EventResult FALSE = new EventResult(true, Boolean.FALSE);
    private static final EventResult DEFAULT = new EventResult(true, null);

    private final boolean interrupts;
    private final Boolean value;

    private EventResult(boolean interrupts, Boolean value) {
        this.interrupts = interrupts;
        this.value = value;
    }

    public static EventResult pass() {
        return PASS;
    }

    public static EventResult interruptTrue() {
        return TRUE;
    }

    public static EventResult interruptFalse() {
        return FALSE;
    }

    public static EventResult interruptDefault() {
        return DEFAULT;
    }

    public boolean interruptsFurtherEvaluation() {
        return this.interrupts;
    }

    public Boolean value() {
        return this.value;
    }

    public boolean isEmpty() {
        return this.value == null;
    }
}
