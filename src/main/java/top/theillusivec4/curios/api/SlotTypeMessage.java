package top.theillusivec4.curios.api;

public class SlotTypeMessage {

    public static final String REGISTER_TYPE = "register_type";

    public static class Builder {
        private final String id;
        private int size = 1;

        public Builder(String id) {
            this.id = id;
        }

        public Builder size(int size) {
            this.size = size;
            return this;
        }

        public SlotTypeMessage build() {
            return new SlotTypeMessage();
        }
    }
}
