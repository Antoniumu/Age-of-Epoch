package com.robertx22.mine_and_slash.gui.screens.skill_tree;

import com.google.common.collect.HashMultimap;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class VertexContainer {
    public HashMultimap<Identifier, BufferInfo> map = HashMultimap.create(100, 500);

    public void refresh() {
        this.map = HashMultimap.create(100, 500);
    }

    public void draw(GuiGraphicsExtractor gui) {
        List<Map.Entry<Identifier, BufferInfo>> all = new ArrayList<>(this.map.entries());
        all.sort(Comparator.comparingDouble(e -> e.getValue().pBlitOffset()));
        for (Map.Entry<Identifier, BufferInfo> entry : all) {
            entry.getValue().draw(gui, entry.getKey());
        }
    }
}
