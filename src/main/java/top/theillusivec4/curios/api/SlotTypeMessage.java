package top.theillusivec4.curios.api;

import net.minecraft.resources.Identifier;

public class SlotTypeMessage {
    public static final String REGISTER_TYPE = "register_type";

    public static class Builder {
        public Builder(String id) {}
        public Builder size(int size) { return this; }
        public Builder icon(Identifier icon) { return this; }
        public Builder priority(int priority) { return this; }
        public SlotTypeMessage build() { return new SlotTypeMessage(); }
    }
}
