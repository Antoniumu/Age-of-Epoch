package com.robertx22.forge_compat.data;

import java.util.function.Consumer;

public class ConditionalRecipe {
    public static Builder builder() { return new Builder(); }
    public static class Builder {
        public Builder addCondition(Object condition) { return this; }
        public Builder addRecipe(Consumer<Consumer<IFinishedRecipe>> callable) { return this; }
        public Builder addRecipe(IFinishedRecipe recipe) { return this; }
        public void build(Consumer<IFinishedRecipe> consumer, Object id) {}
    }
}
