package com.robertx22.forge_compat.data;

import java.util.function.Consumer;

public class ConditionalRecipe {
    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private Consumer<Consumer<IFinishedRecipe>> inner;
        private IFinishedRecipe direct;
        public Builder addCondition(Object condition) { return this; }
        public Builder addRecipe(Consumer<Consumer<IFinishedRecipe>> callable) { this.inner = callable; return this; }
        public Builder addRecipe(IFinishedRecipe recipe) { this.direct = recipe; return this; }
        public void build(Consumer<IFinishedRecipe> consumer, Object id) {
            if (inner != null) inner.accept(consumer);
            else if (direct != null) consumer.accept(direct);
        }
    }
}
