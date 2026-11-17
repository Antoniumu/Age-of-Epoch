package com.robertx22.age_of_exile.wiki;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Development-only bridge for the CI wiki screenshot crawler. A request file
 * is polled on the client thread so the normal screen, resource loading and
 * rendering paths are exercised for every captured page.
 */
public final class WikiScreenshotCrawler {

    private static final Logger LOGGER = LoggerFactory.getLogger("AgeOfEpoch-Wiki-Crawler");
    private static final String REQUEST_FILE = "aoe-wiki-page.txt";
    private static String lastRequest = "";

    private WikiScreenshotCrawler() {
    }

    public static void tick(Minecraft client) {
        if (!FabricLoader.getInstance().isDevelopmentEnvironment()) {
            return;
        }

        Path requestFile = FabricLoader.getInstance().getGameDir().resolve(REQUEST_FILE);
        if (!Files.isRegularFile(requestFile)) {
            lastRequest = "";
            return;
        }

        try {
            String request = Files.readString(requestFile).trim();
            if (request.isEmpty() || request.equals(lastRequest)) {
                return;
            }
            lastRequest = request;

            int separator = request.indexOf('\t');
            String pageId = (separator < 0 ? request : request.substring(0, separator)).trim();
            if (pageId.isEmpty()) {
                return;
            }
            if (!WikiCatalog.exists(pageId)) {
                LOGGER.warn("AOEWIKI requested unknown page={}", pageId);
                return;
            }

            ModWikiScreen.openAt(pageId);
        } catch (IOException error) {
            LOGGER.warn("AOEWIKI request file could not be read: {}", error.toString());
        }
    }
}
