package com.robertx22.forge_compat.fml.network;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.entity.Entity;

import java.util.function.Supplier;

import net.minecraft.world.level.chunk.LevelChunk;
public class PacketDistributor<T> {

    public enum Target { TRACKING_CHUNK, TRACKING_ENTITY, TRACKING_AND_SELF, ALL, PLAYER, SERVER }

    public static final PacketDistributor<net.minecraft.world.level.chunk.LevelChunk> TRACKING_CHUNK =
        new PacketDistributor<>(Target.TRACKING_CHUNK);
    public static final PacketDistributor<Entity> TRACKING_ENTITY =
        new PacketDistributor<>(Target.TRACKING_ENTITY);
    public static final PacketDistributor<Object> ALL = new PacketDistributor<>(Target.ALL);
    public static final PacketDistributor<Object> SERVER = new PacketDistributor<>(Target.SERVER);

    private final Target target;

    PacketDistributor(Target target) { this.target = target; }

    public Target getTarget() { return target; }

    public static class Directed<T> {
        final PacketDistributor<T> distributor;
        final Supplier<T> supplier;

        Directed(PacketDistributor<T> distributor, Supplier<T> supplier) {
            this.distributor = distributor;
            this.supplier = supplier;
        }

        public T getObject() { return supplier.get(); }

        public PacketDistributor<T> getDistributor() { return distributor; }
    }

    public Directed<T> with(Supplier<T> supplier) {
        return new Directed<>(this, supplier);
    }
}
