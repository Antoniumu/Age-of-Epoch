package com.robertx22.forge_compat.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Minecraft 26.3 made {@code MenuScreens.register(...)} private (together with the
 * {@code MenuScreens.ScreenConstructor} argument type), so mods cannot call it directly anymore.
 * This helper registers a screen factory through reflection, which keeps the vanilla lookup table
 * working for Fabric mods without an access widener.
 */
public class MenuScreensCompat {

    @FunctionalInterface
    public interface Factory<M extends AbstractContainerMenu, U extends Screen & MenuAccess<M>> {
        U create(M menu, Inventory inventory, Component title);
    }

    private static Method registerMethod;
    private static Class<?> constructorType;

    private static void init() {
        if (registerMethod != null) {
            return;
        }
        try {
            constructorType = Class.forName("net.minecraft.client.gui.screens.MenuScreens$ScreenConstructor");
            registerMethod = MenuScreens.class.getDeclaredMethod("register", MenuType.class, constructorType);
            registerMethod.setAccessible(true);
        } catch (Exception e) {
            throw new RuntimeException("Failed to access MenuScreens.register", e);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <M extends AbstractContainerMenu, U extends Screen & MenuAccess<M>> void register(MenuType<M> type, Factory<M, U> factory) {
        try {
            init();

            Object constructor = Proxy.newProxyInstance(MenuScreens.class.getClassLoader(), new Class<?>[]{constructorType}, (proxy, method, args) -> {
                switch (method.getName()) {
                    case "create":
                        return factory.create((M) args[0], (Inventory) args[1], (Component) args[2]);
                    case "fromPacket": {
                        Component title = (Component) args[0];
                        MenuType<?> menuType = (MenuType<?>) args[1];
                        Minecraft minecraft = (Minecraft) args[2];
                        int windowId = (Integer) args[3];
                        if (minecraft.player == null) {
                            return null;
                        }
                        Inventory inventory = minecraft.player.getInventory();
                        M menu = (M) menuType.create(windowId, inventory);
                        U screen = factory.create(menu, inventory, title);
                        minecraft.player.containerMenu = menu;
                        minecraft.setScreenAndShow(screen);
                        return null;
                    }
                    case "toString":
                        return "mmorpg_screen_constructor";
                    case "hashCode":
                        return System.identityHashCode(proxy);
                    case "equals":
                        return proxy == args[0];
                    default:
                        return null;
                }
            });

            registerMethod.invoke(null, type, constructor);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
