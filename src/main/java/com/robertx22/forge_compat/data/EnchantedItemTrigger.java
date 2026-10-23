package com.robertx22.forge_compat.data;

public class EnchantedItemTrigger {

    public static class Instance implements CriterionTriggerInstance {
        public static CriterionTriggerInstance enchantedItem() {
            return new Instance();
        }
    }
}
