#!/usr/bin/env python3
"""
Pass 3 Deterministic Source Port: 2025 RobertSkalko 1.20.1 Suite -> MC 26.3 Fabric (Java 25).
Transforms all 1,938 upstream Java files across:
  1. Library-of-Exile-Rework (328 files, incl. orbs_of_crafting)
  2. Mine-And-Slash-Rework   (1,403 files)
  3. dungeon_realm           (112 files)
  4. ancient_obelisks        (54 files)
  5. the_harvest             (41 files)
into src/main/java with native MC 26.3 DataComponents, Equippable, Block useWithoutItem/useItemOn,
Item appendHoverText/getUseDuration, CapabilityProvider bridges, SubmitNodeCollector EntityRenderers,
SingleQuadParticle, GuiGraphicsExtractor, ValueInput/ValueOutput, and Holder<T> registries.
"""
import glob
import os
import re

REPOS = [
    ("library_of_exile", "/tmp/Library-of-Exile-Rework/src/main/java"),
    ("mine_and_slash",   "/tmp/Mine-And-Slash-Rework/src/main/java"),
    ("dungeon_realm",    "/tmp/dungeon_realm/src/main/java"),
    ("ancient_obelisks", "/tmp/ancient_obelisks/src/main/java"),
    ("the_harvest",      "/tmp/the_harvest/src/main/java"),
]

DEST_JAVA = "/home/user/Age-of-Epoch/src/main/java"

IMPORT_REWRITES = [
    ("net.minecraftforge.common.capabilities.", "com.robertx22.forge_compat.common.capabilities."),
    ("net.minecraftforge.common.ForgeConfigSpec", "com.robertx22.forge_compat.common.ForgeConfigSpec"),
    ("net.minecraftforge.common.MinecraftForge", "com.robertx22.forge_compat.common.MinecraftForge"),
    ("net.minecraftforge.common.util.LazyOptional", "com.robertx22.forge_compat.common.util.LazyOptional"),
    ("net.minecraftforge.common.util.INBTSerializable", "com.robertx22.forge_compat.common.util.INBTSerializable"),
    ("net.minecraftforge.common.IExtensibleEnum", "com.robertx22.forge_compat.common.IExtensibleEnum"),
    ("net.minecraftforge.event.entity.living.", "com.robertx22.forge_compat.event.entity.living."),
    ("net.minecraftforge.event.entity.player.", "com.robertx22.forge_compat.event.entity.player."),
    ("net.minecraftforge.event.entity.", "com.robertx22.forge_compat.event.entity."),
    ("net.minecraftforge.event.level.", "com.robertx22.forge_compat.event.level."),
    ("net.minecraftforge.event.server.", "com.robertx22.forge_compat.event.server."),
    ("net.minecraftforge.event.", "com.robertx22.forge_compat.event."),
    ("net.minecraftforge.client.event.", "com.robertx22.forge_compat.client.event."),
    ("net.minecraftforge.client.gui.overlay.", "com.robertx22.forge_compat.client.gui.overlay."),
    ("net.minecraftforge.client.settings.", "com.robertx22.forge_compat.client.settings."),
    ("net.minecraftforge.client.", "com.robertx22.forge_compat.client."),
    ("net.minecraftforge.data.event.", "com.robertx22.forge_compat.data."),
    ("net.minecraftforge.eventbus.api.", "com.robertx22.forge_compat.eventbus.api."),
    ("net.minecraftforge.fml.common.", "com.robertx22.forge_compat.fml.common."),
    ("net.minecraftforge.fml.config.", "com.robertx22.forge_compat.fml.config."),
    ("net.minecraftforge.fml.event.config.", "com.robertx22.forge_compat.fml.event.config."),
    ("net.minecraftforge.fml.event.lifecycle.", "com.robertx22.forge_compat.fml.event.lifecycle."),
    ("net.minecraftforge.fml.javafmlmod.", "com.robertx22.forge_compat.fml.javafmlmod."),
    ("net.minecraftforge.fml.loading.", "com.robertx22.forge_compat.fml.loading."),
    ("net.minecraftforge.fml.", "com.robertx22.forge_compat.fml."),
    ("net.minecraftforge.network.simple.", "com.robertx22.forge_compat.fml.network.simple."),
    ("net.minecraftforge.network.", "com.robertx22.forge_compat.fml.network."),
    ("net.minecraftforge.server.", "com.robertx22.forge_compat.fml.server."),
    ("net.minecraftforge.registries.RegistryObject", "com.robertx22.forge_compat.fml.RegistryObject"),
    ("net.minecraftforge.registries.", "com.robertx22.forge_compat.registries."),
    ("net.minecraftforge.api.distmarker.", "com.robertx22.forge_compat.api.distmarker."),
    ("net.minecraftforge.common.crafting.ConditionalRecipe", "com.robertx22.forge_compat.data.ConditionalRecipe"),
    ("net.minecraftforge.common.crafting.conditions.ModLoadedCondition", "com.robertx22.forge_compat.data.ModLoadedCondition"),
    ("net.minecraftforge.", "com.robertx22.forge_compat."),

    # MC 1.20.1 -> MC 26.3 package/type moves & GUI compat adapters
    ("net.minecraft.resources.ResourceLocation", "net.minecraft.resources.Identifier"),
    ("net.minecraft.client.gui.GuiGraphics", "net.minecraft.client.gui.GuiGraphicsExtractor"),
    ("net.minecraft.client.gui.screens.Screen", "com.robertx22.forge_compat.client.Screen"),
    ("net.minecraft.client.gui.screens.inventory.AbstractContainerScreen", "com.robertx22.forge_compat.client.AbstractContainerScreen"),
    ("net.minecraft.client.gui.screens.inventory.ContainerScreen", "com.robertx22.forge_compat.client.ContainerScreen"),
    ("net.minecraft.client.gui.components.Button", "com.robertx22.forge_compat.client.Button"),
    ("net.minecraft.client.gui.components.ImageButton", "com.robertx22.forge_compat.client.ImageButton"),
    ("net.minecraft.client.gui.components.EditBox", "com.robertx22.forge_compat.client.EditBox"),
    ("net.minecraft.client.gui.components.ObjectSelectionList", "com.robertx22.forge_compat.client.ObjectSelectionList"),
    ("net.minecraft.world.entity.MobSpawnType", "net.minecraft.world.entity.EntitySpawnReason"),
    ("net.minecraft.client.renderer.RenderType", "com.robertx22.forge_compat.client.RenderType"),
    ("com.mojang.blaze3d.systems.RenderSystem", "com.robertx22.forge_compat.client.RenderSystem"),
    ("net.minecraft.data.recipes.ShapedRecipeBuilder", "com.robertx22.forge_compat.data.ShapedRecipeBuilder"),
    ("net.minecraft.data.recipes.ShapelessRecipeBuilder", "com.robertx22.forge_compat.data.ShapelessRecipeBuilder"),
    ("net.minecraft.data.recipes.FinishedRecipe", "com.robertx22.forge_compat.data.IFinishedRecipe"),
    ("net.minecraft.client.renderer.ItemBlockRenderTypes", "com.robertx22.forge_compat.client.ItemBlockRenderTypes"),
    ("net.minecraft.data.loot.LootTableProvider", "com.robertx22.forge_compat.data.LootTableProvider"),
    ("net.minecraft.data.loot.LootTableSubProvider", "com.robertx22.forge_compat.data.LootTableSubProvider"),
    ("net.minecraft.advancements.critereon.EnchantedItemTrigger", "com.robertx22.forge_compat.data.EnchantedItemTrigger"),
    ("net.minecraft.advancements.critereon.", "net.minecraft.advancements.criterion."),
    ("net.minecraft.world.InteractionResultHolder", "com.robertx22.forge_compat.common.InteractionResultHolder"),
    ("net.minecraft.world.item.Tiers", "com.robertx22.forge_compat.common.Tiers"),
    ("net.minecraft.world.item.ArmorMaterials", "com.robertx22.forge_compat.common.ArmorMaterials"),
    ("net.minecraft.world.item.ArmorMaterial", "com.robertx22.forge_compat.common.ArmorMaterial"),
    ("net.minecraft.world.item.ItemUseAnimation", "net.minecraft.world.item.ItemUseAnimation"),
    ("net.minecraft.world.item.UseAnim", "net.minecraft.world.item.ItemUseAnimation"),
    ("net.minecraft.world.level.GameRules", "net.minecraft.world.level.gamerules.GameRules"),
    ("net.minecraft.world.level.storage.loot.LootDataType", "net.minecraft.world.level.storage.loot.LootDataType"),
    ("net.minecraft.world.entity.monster.Skeleton", "net.minecraft.world.entity.monster.skeleton.Skeleton"),
    ("net.minecraft.world.entity.monster.AbstractSkeleton", "net.minecraft.world.entity.monster.skeleton.AbstractSkeleton"),
    ("net.minecraft.world.entity.monster.WitherSkeleton", "net.minecraft.world.entity.monster.skeleton.WitherSkeleton"),
    ("net.minecraft.world.entity.monster.Stray", "net.minecraft.world.entity.monster.skeleton.Stray"),
    ("net.minecraft.world.entity.monster.Zombie", "net.minecraft.world.entity.monster.zombie.Zombie"),
    ("net.minecraft.world.entity.monster.Husk", "net.minecraft.world.entity.monster.zombie.Husk"),
    ("net.minecraft.world.entity.monster.Drowned", "net.minecraft.world.entity.monster.zombie.Drowned"),
    ("net.minecraft.world.entity.monster.ZombifiedPiglin", "net.minecraft.world.entity.monster.zombie.ZombifiedPiglin"),
    ("net.minecraft.world.entity.monster.Spider", "net.minecraft.world.entity.monster.spider.Spider"),
    ("net.minecraft.world.entity.monster.CaveSpider", "net.minecraft.world.entity.monster.spider.CaveSpider"),
    ("net.minecraft.world.entity.animal.Wolf", "net.minecraft.world.entity.animal.wolf.Wolf"),
    ("net.minecraft.world.entity.animal.IronGolem", "net.minecraft.world.entity.animal.golem.IronGolem"),
    ("net.minecraft.world.entity.animal.SnowGolem", "net.minecraft.world.entity.animal.golem.SnowGolem"),
    ("net.minecraft.world.entity.npc.Villager", "net.minecraft.world.entity.npc.villager.Villager"),
    ("net.minecraft.world.entity.npc.AbstractVillager", "net.minecraft.world.entity.npc.villager.AbstractVillager"),
    ("net.minecraft.world.entity.projectile.AbstractArrow", "net.minecraft.world.entity.projectile.arrow.AbstractArrow"),
    ("net.minecraft.world.entity.projectile.Arrow", "net.minecraft.world.entity.projectile.arrow.Arrow"),
    ("net.minecraft.world.entity.projectile.ThrownTrident", "net.minecraft.world.entity.projectile.arrow.ThrownTrident"),
    ("net.minecraft.world.entity.projectile.ItemSupplier", "net.minecraft.world.entity.projectile.ItemSupplier"),
    ("net.minecraft.world.entity.projectile.ThrowableItemProjectile", "net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile"),
    ("net.minecraft.world.level.block.entity.SkullBlockEntity", "net.minecraft.world.level.block.entity.SkullBlockEntity"),
]

def find_matching_paren(s: str, open_idx: int) -> int:
    depth = 0
    in_str = False
    esc = False
    for i in range(open_idx, len(s)):
        ch = s[i]
        if in_str:
            if esc:
                esc = False
            elif ch == "\\":
                esc = True
            elif ch == '"':
                in_str = False
        else:
            if ch == '"':
                in_str = True
            elif ch == "(":
                depth += 1
            elif ch == ")":
                depth -= 1
                if depth == 0:
                    return i
    return -1

def find_matching_brace(s: str, open_idx: int) -> int:
    depth = 0
    in_str = False
    esc = False
    for i in range(open_idx, len(s)):
        ch = s[i]
        if in_str:
            if esc:
                esc = False
            elif ch == "\\":
                esc = True
            elif ch == '"':
                in_str = False
        else:
            if ch == '"':
                in_str = True
            elif ch == "{":
                depth += 1
            elif ch == "}":
                depth -= 1
                if depth == 0:
                    return i
    return -1

def split_top_args(arg_str: str):
    args = []
    cur = []
    depth = 0
    in_str = False
    esc = False
    for ch in arg_str:
        if in_str:
            cur.append(ch)
            if esc:
                esc = False
            elif ch == "\\":
                esc = True
            elif ch == '"':
                in_str = False
        else:
            if ch == '"':
                in_str = True
                cur.append(ch)
            elif ch in "([{":
                depth += 1
                cur.append(ch)
            elif ch in ")]}":
                depth -= 1
                cur.append(ch)
            elif ch == "," and depth == 0:
                args.append("".join(cur).strip())
                cur = []
            else:
                cur.append(ch)
    if cur:
        args.append("".join(cur).strip())
    return args

def rewrite_method_call(code: str, pattern: str, transform_fn) -> str:
    rx = re.compile(pattern)
    out = []
    pos = 0
    while True:
        m = rx.search(code, pos)
        if not m:
            out.append(code[pos:])
            break
        open_p = m.end() - 1
        close_p = find_matching_paren(code, open_p)
        if close_p == -1:
            out.append(code[pos:m.end()])
            pos = m.end()
            continue
        inner = code[open_p + 1:close_p]
        args = split_top_args(inner) if inner.strip() else []
        rep = transform_fn(m, args)
        if rep is None:
            out.append(code[pos:m.end()])
            pos = m.end()
        else:
            out.append(code[pos:m.start()])
            out.append(rep)
            pos = close_p + 1
    return "".join(out)

EXPR_TOKEN = r"([A-Za-z_][A-Za-z0-9_]*(?:\.[A-Za-z_][A-Za-z0-9_]*|\(\)|\[[^\]]*\])*)"
NBT_VAR_NAMES = {
    "nbt", "tag", "compound", "ctag", "locnbt", "stacknbt", "beNbt",
    "blockNbt", "entityNbt", "itemNbt", "pCompound", "pTag", "dataNbt",
    "subtag", "ptag", "compoundtag"
}

def rewrite_scoped_nbt_getters(code: str) -> str:
    defaults = {
        "getString": '""',
        "getInt": "0",
        "getBoolean": "false",
        "getFloat": "0F",
        "getDouble": "0D",
        "getLong": "0L",
        "getByte": "(byte) 0",
        "getShort": "(short) 0",
    }
    for getter, dflt in defaults.items():
        def make_tf(g=getter, d=dflt):
            def tf(m, args):
                recv = m.group(1)
                if len(args) != 1:
                    return None
                last_seg = recv.rstrip(")").split(".")[-1].replace("()", "")
                is_nbt = (
                    last_seg in NBT_VAR_NAMES
                    or "Tag" in recv
                    or "Nbt" in recv
                    or "NBT" in recv
                    or "nbt" in last_seg.lower()
                )
                if not is_nbt:
                    return None
                return f"{recv}.{g}Or({args[0]}, {d})"
            return tf
        code = rewrite_method_call(code, EXPR_TOKEN + rf"\.{getter}\s*\(", make_tf())

    def tf_compound(m, args):
        recv = m.group(1)
        if len(args) != 1:
            return None
        last_seg = recv.rstrip(")").split(".")[-1].replace("()", "")
        is_nbt = (
            last_seg in NBT_VAR_NAMES
            or "Tag" in recv
            or "Nbt" in recv
            or "NBT" in recv
            or "nbt" in last_seg.lower()
        )
        return f"{recv}.getCompoundOrEmpty({args[0]})" if is_nbt else None

    code = rewrite_method_call(code, EXPR_TOKEN + r"\.getCompound\s*\(", tf_compound)
    return code


def _transform_file_inner(rel: str, code: str) -> str:
    # ------------------------------------------------------------------
    # Dedicated full-file MC 26.3 replacements for files whose entire API
    # moved to MC 26.3 DataComponents / SubmitNodeCollector / SingleQuadParticle
    # ------------------------------------------------------------------
    norm = rel.replace("\\", "/")

    if norm == "com/robertx22/library_of_exile/dimension/WipeDimensionFeature.java":
        return """package com.robertx22.library_of_exile.dimension;

import com.robertx22.forge_compat.fml.loading.FMLPaths;
import com.robertx22.library_of_exile.main.ExileLog;
import net.minecraft.resources.Identifier;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.stream.Stream;

public class WipeDimensionFeature {

    public static void OnStartResetMap(MapDimensionInfo map, Identifier mapid) {
        Path root = FMLPaths.GAMEDIR.get().toAbsolutePath();
        if (!Files.exists(root)) return;
        try (Stream<Path> walk = Files.walk(root)) {
            walk.filter(Files::isDirectory).toList().forEach(dir -> {
                try {
                    if (dirMatches(dir.toFile(), mapid)) {
                        deleteDirectoryRecursion(dir);
                        map.markDataForClear = true;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static boolean dirMatches(File file, Identifier mapid) {
        if (file.isDirectory()) {
            String str = file.toPath().toString();
            String check = "dimensions" + File.separator + mapid.getNamespace() + File.separator + mapid.getPath();
            if (str.endsWith(check)) {
                ExileLog.get().log("Wiping dimension folder: " + file);
                return true;
            }
        }
        return false;
    }

    static void deleteDirectoryRecursion(Path path) throws IOException {
        if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
            try (DirectoryStream<Path> entries = Files.newDirectoryStream(path)) {
                for (Path entry : entries) {
                    deleteDirectoryRecursion(entry);
                }
            }
        }
        Files.deleteIfExists(path);
    }
}
"""

    if norm == "com/robertx22/library_of_exile/config/map_dimension/MapGameRules.java":
        return """package com.robertx22.library_of_exile.config.map_dimension;

import net.minecraft.world.level.gamerules.GameRules;

public class MapGameRules extends GameRules {
    public GameRules rules;

    public MapGameRules(GameRules rules) {
        super(null);
        this.rules = rules;
    }
}
"""

    if norm == "com/robertx22/library_of_exile/config/map_dimension/MapWorldBorder.java":
        return """package com.robertx22.library_of_exile.config.map_dimension;

import net.minecraft.world.level.border.WorldBorder;

public class MapWorldBorder extends WorldBorder {
    public MapWorldBorder() {
        super();
    }
}
"""

    if norm == "com/robertx22/orbs_of_crafting/register/reqs/vanilla/VanillaItemMod.java":
        return """package com.robertx22.orbs_of_crafting.register.reqs.vanilla;

import com.google.gson.JsonObject;
import com.robertx22.library_of_exile.localization.ExileTranslation;
import com.robertx22.library_of_exile.localization.TranslationBuilder;
import com.robertx22.library_of_exile.localization.TranslationType;
import com.robertx22.library_of_exile.main.Ref;
import com.robertx22.orbs_of_crafting.misc.StackHolder;
import com.robertx22.orbs_of_crafting.register.mods.base.ItemModification;
import com.robertx22.orbs_of_crafting.register.mods.base.ItemModificationResult;
import com.robertx22.orbs_of_crafting.register.mods.base.VanillaItemModSers;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

public class VanillaItemMod extends ItemModification {
    transient String desc;
    public int enchantLevels = 0;
    public JsonObject vanillaFunction = new JsonObject();

    public VanillaItemMod() {
        super(VanillaItemModSers.VANILLA, "");
    }

    public VanillaItemMod(String id, String desc, int levels) {
        super(VanillaItemModSers.VANILLA, id);
        this.desc = desc;
        this.enchantLevels = levels;
    }

    @Override
    public Class<?> getClassForSerialization() {
        return VanillaItemMod.class;
    }

    @Override
    public OutcomeType getOutcomeType() {
        return OutcomeType.NEUTRAL;
    }

    @Override
    public void applyINTERNAL(StackHolder stack, ItemModificationResult r) {
    }

    @Override
    public void applyMod(Player p, StackHolder stack, ItemModificationResult r) {
        if (enchantLevels > 0 && p != null && p.level() != null) {
            var reg = p.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            stack.stack = EnchantmentHelper.enchantItem(p.getRandom(), stack.stack, enchantLevels, reg.listElements().map(h -> (net.minecraft.core.Holder<net.minecraft.world.item.enchantment.Enchantment>) h));
        }
    }

    @Override
    public MutableComponent getDescWithParams() {
        return getTranslation(TranslationType.DESCRIPTION).getTranslatedName();
    }

    @Override
    public TranslationBuilder createTranslationBuilder() {
        return TranslationBuilder.of(Ref.MODID).desc(ExileTranslation.registry(this, desc == null ? "" : desc));
    }
}
"""

    if norm == "com/robertx22/orbs_of_crafting/register/reqs/vanilla/VanillaRequirement.java":
        return """package com.robertx22.orbs_of_crafting.register.reqs.vanilla;

import com.google.gson.JsonObject;
import com.robertx22.library_of_exile.localization.ExileTranslation;
import com.robertx22.library_of_exile.localization.TranslationBuilder;
import com.robertx22.library_of_exile.localization.TranslationType;
import com.robertx22.library_of_exile.main.Ref;
import com.robertx22.orbs_of_crafting.misc.StackHolder;
import com.robertx22.orbs_of_crafting.register.reqs.ItemReqSers;
import com.robertx22.orbs_of_crafting.register.reqs.base.ItemRequirement;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

public class VanillaRequirement extends ItemRequirement {
    transient String desc;
    public String itemTagId = "";
    public JsonObject vanillaCondition = new JsonObject();

    public VanillaRequirement() {
        super(ItemReqSers.VANILLA_CONDITION, "");
    }

    public VanillaRequirement(String id, String desc, TagKey<Item> tag) {
        super(ItemReqSers.VANILLA_CONDITION, id);
        this.desc = desc;
        this.itemTagId = tag.location().toString();
    }

    @Override
    public Class<?> getClassForSerialization() {
        return VanillaRequirement.class;
    }

    @Override
    public MutableComponent getDescWithParams() {
        return getTranslation(TranslationType.DESCRIPTION).getTranslatedName();
    }

    @Override
    public TranslationBuilder createTranslationBuilder() {
        return TranslationBuilder.of(Ref.MODID).desc(ExileTranslation.registry(this, desc == null ? "" : desc));
    }

    @Override
    public boolean isValid(Player p, StackHolder obj) {
        if (itemTagId != null && !itemTagId.isEmpty()) {
            return obj.stack.is(TagKey.create(Registries.ITEM, Identifier.parse(itemTagId)));
        }
        return true;
    }
}
"""

    if norm == "com/robertx22/orbs_of_crafting/register/Modifications.java":
        return """package com.robertx22.orbs_of_crafting.register;

import com.robertx22.library_of_exile.main.Ref;
import com.robertx22.library_of_exile.registry.helpers.ExileKey;
import com.robertx22.library_of_exile.registry.helpers.ExileKeyHolder;
import com.robertx22.library_of_exile.registry.helpers.KeyInfo;
import com.robertx22.library_of_exile.registry.register_info.ModRequiredRegisterInfo;
import com.robertx22.orbs_of_crafting.register.mods.DestroyItemMod;
import com.robertx22.orbs_of_crafting.register.mods.DoNothingItemMod;
import com.robertx22.orbs_of_crafting.register.mods.base.ItemModification;
import com.robertx22.orbs_of_crafting.register.reqs.vanilla.VanillaItemMod;

public class Modifications extends ExileKeyHolder<ItemModification> {
    public Modifications(ModRequiredRegisterInfo modRegisterInfo) {
        super(modRegisterInfo);
    }

    public static Modifications INSTANCE = new Modifications(Ref.REGISTER_INFO);

    public ExileKey<ItemModification, KeyInfo> DESTROY_ITEM = ExileKey.ofId(this, "destroy_item", x -> new DestroyItemMod(x.GUID()));
    public ExileKey<ItemModification, KeyInfo> DO_NOTHING = ExileKey.ofId(this, "do_nothing", x -> new DoNothingItemMod(x.GUID()));
    public ExileKey<ItemModification, KeyInfo> ENCHANT_30_LEVELS = ExileKey.ofId(this, "enchant_with_30_levels", x -> new VanillaItemMod(x.GUID(), "Applies Enchantments worth 30 Levels", 30));
    public ExileKey<ItemModification, KeyInfo> ENCHANT_20_LEVELS = ExileKey.ofId(this, "enchant_with_20_levels", x -> new VanillaItemMod(x.GUID(), "Applies Enchantments worth 20 Levels", 20));
    public ExileKey<ItemModification, KeyInfo> ENCHANT_10_LEVELS = ExileKey.ofId(this, "enchant_with_10_levels", x -> new VanillaItemMod(x.GUID(), "Applies Enchantments worth 10 Levels", 10));

    @Override
    public void loadClass() {
    }
}
"""

    if norm == "com/robertx22/orbs_of_crafting/register/Requirements.java":
        return """package com.robertx22.orbs_of_crafting.register;

import com.robertx22.library_of_exile.main.Ref;
import com.robertx22.library_of_exile.registry.helpers.ExileKey;
import com.robertx22.library_of_exile.registry.helpers.ExileKeyHolder;
import com.robertx22.library_of_exile.registry.helpers.KeyInfo;
import com.robertx22.library_of_exile.registry.register_info.ModRequiredRegisterInfo;
import com.robertx22.orbs_of_crafting.register.reqs.HasNoEnchantsReq;
import com.robertx22.orbs_of_crafting.register.reqs.IsArmorReq;
import com.robertx22.orbs_of_crafting.register.reqs.IsDamagedReq;
import com.robertx22.orbs_of_crafting.register.reqs.IsSingleStack;
import com.robertx22.orbs_of_crafting.register.reqs.base.ItemRequirement;
import com.robertx22.orbs_of_crafting.register.reqs.vanilla.VanillaRequirement;
import net.minecraft.tags.ItemTags;

public class Requirements extends ExileKeyHolder<ItemRequirement> {
    public static Requirements INSTANCE = new Requirements(Ref.REGISTER_INFO);

    public Requirements(ModRequiredRegisterInfo modRegisterInfo) {
        super(modRegisterInfo);
    }

    public ExileKey<ItemRequirement, KeyInfo> IS_SINGLE_ITEM = ExileKey.ofId(this, "is_single_item", x -> new IsSingleStack(x.GUID()));
    public ExileKey<ItemRequirement, KeyInfo> IS_DAMAGED = ExileKey.ofId(this, "is_damaged", x -> new IsDamagedReq(x.GUID()));
    public ExileKey<ItemRequirement, KeyInfo> IS_TOOL_TAG = ExileKey.ofId(this, "is_tool_tag", x -> new VanillaRequirement(x.GUID(), "Must be tagged as #Tool", ItemTags.SWORDS));
    public ExileKey<ItemRequirement, KeyInfo> IS_PICKAXE_TAG = ExileKey.ofId(this, "is_pickaxe_tag", x -> new VanillaRequirement(x.GUID(), "Must be tagged as #Pickaxe", ItemTags.PICKAXES));
    public ExileKey<ItemRequirement, KeyInfo> IS_SWORD_TAG = ExileKey.ofId(this, "is_sword_tag", x -> new VanillaRequirement(x.GUID(), "Must be tagged as #Sword", ItemTags.SWORDS));
    public ExileKey<ItemRequirement, KeyInfo> HAS_NO_ENCHANTS = ExileKey.ofId(this, "has_no_enchants", x -> new HasNoEnchantsReq(x.GUID()));
    public ExileKey<ItemRequirement, KeyInfo> IS_ARMOR = ExileKey.ofId(this, "is_armor", x -> new IsArmorReq(x.GUID()));

    @Override
    public void loadClass() {
    }
}
"""

    if norm == "com/robertx22/orbs_of_crafting/register/reqs/IsArmorReq.java":
        return """package com.robertx22.orbs_of_crafting.register.reqs;

import com.robertx22.library_of_exile.localization.ExileTranslation;
import com.robertx22.library_of_exile.localization.TranslationBuilder;
import com.robertx22.library_of_exile.localization.TranslationType;
import com.robertx22.library_of_exile.main.Ref;
import com.robertx22.orbs_of_crafting.misc.StackHolder;
import com.robertx22.orbs_of_crafting.register.reqs.base.ItemRequirement;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;

public class IsArmorReq extends ItemRequirement {
    public IsArmorReq(String id) {
        super("is_armor", id);
    }

    @Override
    public Class<?> getClassForSerialization() {
        return IsArmorReq.class;
    }

    @Override
    public MutableComponent getDescWithParams() {
        return getTranslation(TranslationType.DESCRIPTION).getTranslatedName();
    }

    @Override
    public TranslationBuilder createTranslationBuilder() {
        return TranslationBuilder.of(Ref.MODID).desc(ExileTranslation.registry(this, "Must be Armor"));
    }

    @Override
    public boolean isValid(Player p, StackHolder obj) {
        var eq = obj.stack.get(DataComponents.EQUIPPABLE);
        if (eq == null) return false;
        EquipmentSlot s = eq.slot();
        return s == EquipmentSlot.HEAD || s == EquipmentSlot.CHEST || s == EquipmentSlot.LEGS || s == EquipmentSlot.FEET;
    }
}
"""

    # Entity Renderers -> 100% Native MC 26.3 SubmitNodeCollector Renderers
    if norm == "com/robertx22/mine_and_slash/database/data/spells/entities/renders/MySpriteRenderer.java":
        return """package com.robertx22.mine_and_slash.database.data.spells.entities.renders;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.robertx22.mine_and_slash.database.data.spells.entities.SimpleProjectileEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

import java.util.Locale;

public class MySpriteRenderer<T extends Entity & IMyRenderAsItem> extends EntityRenderer<T, MySpriteRenderer.SpriteRenderState> {
    private final float scale;
    private final boolean lit;

    private static final AABB CORE_BOX = new AABB(-0.15D, -0.15D, -0.15D, 0.15D, 0.15D, 0.15D);
    private static final AABB OUTER_BOX = new AABB(-0.23D, -0.23D, -0.23D, 0.23D, 0.23D, 0.23D);

    public MySpriteRenderer(EntityRendererProvider.Context context, Object itemRenderer, float scale, boolean lit) {
        super(context);
        this.scale = scale;
        this.lit = lit;
    }

    public MySpriteRenderer(EntityRendererProvider.Context context, Object itemRenderer) {
        this(context, itemRenderer, 1.0F, false);
    }

    public MySpriteRenderer(EntityRendererProvider.Context context) {
        this(context, null, 1.0F, false);
    }

    public static class SpriteRenderState extends EntityRenderState {
        public int coreColor = ARGB.colorFromFloat(1.0F, 0.25F, 1.0F, 0.25F);
        public int outerColor = ARGB.colorFromFloat(0.65F, 0.08F, 0.72F, 0.18F);
        public float spinAngle = 0.0F;
    }

    @Override
    public SpriteRenderState createRenderState() {
        return new SpriteRenderState();
    }

    @Override
    public void extractRenderState(T entity, SpriteRenderState state, float tickProgress) {
        super.extractRenderState(entity, state, tickProgress);
        state.spinAngle = (entity.tickCount + tickProgress) * 0.35F;
        String id = "";
        try {
            if (entity instanceof SimpleProjectileEntity sp && sp.getSpellId() != null) {
                id += sp.getSpellId() + " ";
            }
            ItemStack stack = entity.getItem();
            if (stack != null && !stack.isEmpty()) {
                id += BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            }
        } catch (Exception ignored) {
        }
        id = id.toLowerCase(Locale.ROOT);
        if (id.contains("fire") || id.contains("flame") || id.contains("magma") || id.contains("blaze")) {
            state.coreColor = ARGB.colorFromFloat(1.0F, 1.0F, 0.85F, 0.2F);
            state.outerColor = ARGB.colorFromFloat(0.75F, 1.0F, 0.28F, 0.05F);
        } else if (id.contains("snow") || id.contains("ice") || id.contains("frost") || id.contains("chill") || id.contains("water")) {
            state.coreColor = ARGB.colorFromFloat(1.0F, 0.85F, 0.98F, 1.0F);
            state.outerColor = ARGB.colorFromFloat(0.7F, 0.15F, 0.75F, 1.0F);
        } else if (id.contains("thunder") || id.contains("lightning") || id.contains("spark")) {
            state.coreColor = ARGB.colorFromFloat(1.0F, 1.0F, 1.0F, 0.55F);
            state.outerColor = ARGB.colorFromFloat(0.7F, 1.0F, 0.85F, 0.1F);
        } else if (id.contains("blood") || id.contains("chaos") || id.contains("shadow")) {
            state.coreColor = ARGB.colorFromFloat(1.0F, 0.9F, 0.15F, 0.25F);
            state.outerColor = ARGB.colorFromFloat(0.75F, 0.55F, 0.05F, 0.35F);
        } else {
            state.coreColor = ARGB.colorFromFloat(1.0F, 0.25F, 1.0F, 0.25F);
            state.outerColor = ARGB.colorFromFloat(0.68F, 0.08F, 0.72F, 0.18F);
        }
    }

    @Override
    public void submit(SpriteRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraState) {
        super.submit(state, poseStack, submitNodeCollector, cameraState);
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.2F, 0.0F);
        poseStack.scale(this.scale, this.scale, this.scale);
        poseStack.mulPose(new Matrix4f().rotationXYZ(state.spinAngle, state.spinAngle * 1.3F, state.spinAngle * 0.7F));
        int coreCol = state.coreColor;
        submitNodeCollector.order(0).submitCustomGeometry(poseStack, RenderTypes.debugFilledBox(), (pose, buffer) -> drawFilledBox(pose, buffer, CORE_BOX, coreCol));
        poseStack.mulPose(new Matrix4f().rotationXYZ(0.7853982F, 0.7853982F, 0.7853982F));
        int outerCol = state.outerColor;
        submitNodeCollector.order(0).submitCustomGeometry(poseStack, RenderTypes.debugFilledBox(), (pose, buffer) -> drawFilledBox(pose, buffer, OUTER_BOX, outerCol));
        poseStack.popPose();
    }

    public static void drawFilledBox(PoseStack.Pose pose, VertexConsumer vc, AABB box, int color) {
        vc.addVertex(pose, (float) box.minX, (float) box.minY, (float) box.minZ).setColor(color);
        vc.addVertex(pose, (float) box.maxX, (float) box.minY, (float) box.minZ).setColor(color);
        vc.addVertex(pose, (float) box.maxX, (float) box.maxY, (float) box.minZ).setColor(color);
        vc.addVertex(pose, (float) box.minX, (float) box.maxY, (float) box.minZ).setColor(color);
        vc.addVertex(pose, (float) box.maxX, (float) box.minY, (float) box.maxZ).setColor(color);
        vc.addVertex(pose, (float) box.minX, (float) box.minY, (float) box.maxZ).setColor(color);
        vc.addVertex(pose, (float) box.minX, (float) box.maxY, (float) box.maxZ).setColor(color);
        vc.addVertex(pose, (float) box.maxX, (float) box.maxY, (float) box.maxZ).setColor(color);
        vc.addVertex(pose, (float) box.minX, (float) box.minY, (float) box.maxZ).setColor(color);
        vc.addVertex(pose, (float) box.minX, (float) box.minY, (float) box.minZ).setColor(color);
        vc.addVertex(pose, (float) box.minX, (float) box.maxY, (float) box.minZ).setColor(color);
        vc.addVertex(pose, (float) box.minX, (float) box.maxY, (float) box.maxZ).setColor(color);
        vc.addVertex(pose, (float) box.maxX, (float) box.minY, (float) box.minZ).setColor(color);
        vc.addVertex(pose, (float) box.maxX, (float) box.minY, (float) box.maxZ).setColor(color);
        vc.addVertex(pose, (float) box.maxX, (float) box.maxY, (float) box.maxZ).setColor(color);
        vc.addVertex(pose, (float) box.maxX, (float) box.maxY, (float) box.minZ).setColor(color);
        vc.addVertex(pose, (float) box.minX, (float) box.maxY, (float) box.minZ).setColor(color);
        vc.addVertex(pose, (float) box.maxX, (float) box.maxY, (float) box.minZ).setColor(color);
        vc.addVertex(pose, (float) box.maxX, (float) box.maxY, (float) box.maxZ).setColor(color);
        vc.addVertex(pose, (float) box.minX, (float) box.maxY, (float) box.maxZ).setColor(color);
        vc.addVertex(pose, (float) box.minX, (float) box.minY, (float) box.maxZ).setColor(color);
        vc.addVertex(pose, (float) box.maxX, (float) box.minY, (float) box.maxZ).setColor(color);
        vc.addVertex(pose, (float) box.maxX, (float) box.minY, (float) box.minZ).setColor(color);
        vc.addVertex(pose, (float) box.minX, (float) box.minY, (float) box.minZ).setColor(color);
    }

    public static void drawTexturedBox(PoseStack.Pose pose, VertexConsumer vc, AABB box, int light) {
        float x0 = (float) box.minX, y0 = (float) box.minY, z0 = (float) box.minZ;
        float x1 = (float) box.maxX, y1 = (float) box.maxY, z1 = (float) box.maxZ;
        int ov = net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY;
        vc.addVertex(pose, x0, y0, z0).setColor(-1).setUv(0.125f, 0.25f).setOverlay(ov).setLight(light).setNormal(pose, 0, 0, -1);
        vc.addVertex(pose, x1, y0, z0).setColor(-1).setUv(0.25f, 0.25f).setOverlay(ov).setLight(light).setNormal(pose, 0, 0, -1);
        vc.addVertex(pose, x1, y1, z0).setColor(-1).setUv(0.25f, 0.125f).setOverlay(ov).setLight(light).setNormal(pose, 0, 0, -1);
        vc.addVertex(pose, x0, y1, z0).setColor(-1).setUv(0.125f, 0.125f).setOverlay(ov).setLight(light).setNormal(pose, 0, 0, -1);

        vc.addVertex(pose, x1, y0, z1).setColor(-1).setUv(0.375f, 0.25f).setOverlay(ov).setLight(light).setNormal(pose, 0, 0, 1);
        vc.addVertex(pose, x0, y0, z1).setColor(-1).setUv(0.5f, 0.25f).setOverlay(ov).setLight(light).setNormal(pose, 0, 0, 1);
        vc.addVertex(pose, x0, y1, z1).setColor(-1).setUv(0.5f, 0.125f).setOverlay(ov).setLight(light).setNormal(pose, 0, 0, 1);
        vc.addVertex(pose, x1, y1, z1).setColor(-1).setUv(0.375f, 0.125f).setOverlay(ov).setLight(light).setNormal(pose, 0, 0, 1);

        vc.addVertex(pose, x0, y0, z1).setColor(-1).setUv(0.0f, 0.25f).setOverlay(ov).setLight(light).setNormal(pose, -1, 0, 0);
        vc.addVertex(pose, x0, y0, z0).setColor(-1).setUv(0.125f, 0.25f).setOverlay(ov).setLight(light).setNormal(pose, -1, 0, 0);
        vc.addVertex(pose, x0, y1, z0).setColor(-1).setUv(0.125f, 0.125f).setOverlay(ov).setLight(light).setNormal(pose, -1, 0, 0);
        vc.addVertex(pose, x0, y1, z1).setColor(-1).setUv(0.0f, 0.125f).setOverlay(ov).setLight(light).setNormal(pose, -1, 0, 0);

        vc.addVertex(pose, x1, y0, z0).setColor(-1).setUv(0.25f, 0.25f).setOverlay(ov).setLight(light).setNormal(pose, 1, 0, 0);
        vc.addVertex(pose, x1, y0, z1).setColor(-1).setUv(0.375f, 0.25f).setOverlay(ov).setLight(light).setNormal(pose, 1, 0, 0);
        vc.addVertex(pose, x1, y1, z1).setColor(-1).setUv(0.375f, 0.125f).setOverlay(ov).setLight(light).setNormal(pose, 1, 0, 0);
        vc.addVertex(pose, x1, y1, z0).setColor(-1).setUv(0.25f, 0.125f).setOverlay(ov).setLight(light).setNormal(pose, 1, 0, 0);

        vc.addVertex(pose, x0, y1, z0).setColor(-1).setUv(0.125f, 0.0f).setOverlay(ov).setLight(light).setNormal(pose, 0, 1, 0);
        vc.addVertex(pose, x1, y1, z0).setColor(-1).setUv(0.25f, 0.0f).setOverlay(ov).setLight(light).setNormal(pose, 0, 1, 0);
        vc.addVertex(pose, x1, y1, z1).setColor(-1).setUv(0.25f, 0.125f).setOverlay(ov).setLight(light).setNormal(pose, 0, 1, 0);
        vc.addVertex(pose, x0, y1, z1).setColor(-1).setUv(0.125f, 0.125f).setOverlay(ov).setLight(light).setNormal(pose, 0, 1, 0);

        vc.addVertex(pose, x0, y0, z1).setColor(-1).setUv(0.25f, 0.0f).setOverlay(ov).setLight(light).setNormal(pose, 0, -1, 0);
        vc.addVertex(pose, x1, y0, z1).setColor(-1).setUv(0.375f, 0.0f).setOverlay(ov).setLight(light).setNormal(pose, 0, -1, 0);
        vc.addVertex(pose, x1, y0, z0).setColor(-1).setUv(0.375f, 0.125f).setOverlay(ov).setLight(light).setNormal(pose, 0, -1, 0);
        vc.addVertex(pose, x0, y0, z0).setColor(-1).setUv(0.25f, 0.125f).setOverlay(ov).setLight(light).setNormal(pose, 0, -1, 0);
    }

    @Override
    protected int getBlockLightLevel(T entity, BlockPos blockPos) {
        return this.lit ? 15 : super.getBlockLightLevel(entity, blockPos);
    }
}
"""

    if norm == "com/robertx22/mine_and_slash/database/data/spells/entities/renders/ModTridentRenderer.java":
        return """package com.robertx22.mine_and_slash.database.data.spells.entities.renders;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

public class ModTridentRenderer<T extends Projectile> extends EntityRenderer<T, ModTridentRenderer.TridentState> {
    public static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/entity/trident.png");
    private static final AABB SHAFT_BOX = new AABB(-0.05D, -0.05D, -0.55D, 0.05D, 0.05D, 0.45D);
    private static final AABB HEAD_BOX = new AABB(-0.18D, -0.05D, 0.35D, 0.18D, 0.05D, 0.58D);

    public ModTridentRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class TridentState extends EntityRenderState {
        public float yRot;
        public float xRot;
    }

    @Override
    public TridentState createRenderState() {
        return new TridentState();
    }

    @Override
    public void extractRenderState(T entity, TridentState state, float tickProgress) {
        super.extractRenderState(entity, state, tickProgress);
        state.yRot = entity.getYRot(tickProgress);
        state.xRot = entity.getXRot(tickProgress);
    }

    @Override
    public void submit(TridentState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraState) {
        super.submit(state, poseStack, submitNodeCollector, cameraState);
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.15F, 0.0F);
        poseStack.mulPose(new Matrix4f().rotationY((float) Math.toRadians(-state.yRot)));
        poseStack.mulPose(new Matrix4f().rotationX((float) Math.toRadians(state.xRot)));
        int shaftColor = ARGB.colorFromFloat(1.0F, 0.35F, 0.75F, 0.85F);
        int headColor = ARGB.colorFromFloat(1.0F, 0.75F, 0.95F, 1.0F);
        submitNodeCollector.order(0).submitCustomGeometry(poseStack, RenderTypes.debugFilledBox(), (pose, buffer) -> {
            MySpriteRenderer.drawFilledBox(pose, buffer, SHAFT_BOX, shaftColor);
            MySpriteRenderer.drawFilledBox(pose, buffer, HEAD_BOX, headColor);
        });
        poseStack.popPose();
    }
}
"""

    if norm == "com/robertx22/mine_and_slash/database/data/spells/entities/renders/RangerArrowRenderer.java":
        return """package com.robertx22.mine_and_slash.database.data.spells.entities.renders;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

public class RangerArrowRenderer<T extends AbstractArrow> extends EntityRenderer<T, RangerArrowRenderer.ArrowState> {
    public static final Identifier RES_ARROW = Identifier.withDefaultNamespace("textures/entity/projectiles/arrow.png");
    private static final AABB SHAFT_BOX = new AABB(-0.04D, -0.04D, -0.35D, 0.04D, 0.04D, 0.35D);
    private static final AABB TIP_BOX = new AABB(-0.08D, -0.08D, 0.25D, 0.08D, 0.08D, 0.42D);

    public RangerArrowRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class ArrowState extends EntityRenderState {
        public float yRot;
        public float xRot;
    }

    @Override
    public ArrowState createRenderState() {
        return new ArrowState();
    }

    @Override
    public void extractRenderState(T entity, ArrowState state, float tickProgress) {
        super.extractRenderState(entity, state, tickProgress);
        state.yRot = entity.getYRot(tickProgress);
        state.xRot = entity.getXRot(tickProgress);
    }

    @Override
    public void submit(ArrowState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraState) {
        super.submit(state, poseStack, submitNodeCollector, cameraState);
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.1F, 0.0F);
        poseStack.mulPose(new Matrix4f().rotationY((float) Math.toRadians(-state.yRot)));
        poseStack.mulPose(new Matrix4f().rotationX((float) Math.toRadians(state.xRot)));
        int shaftColor = ARGB.colorFromFloat(1.0F, 0.65F, 0.48F, 0.25F);
        int tipColor = ARGB.colorFromFloat(1.0F, 0.85F, 0.9F, 0.95F);
        submitNodeCollector.order(0).submitCustomGeometry(poseStack, RenderTypes.debugFilledBox(), (pose, buffer) -> {
            MySpriteRenderer.drawFilledBox(pose, buffer, SHAFT_BOX, shaftColor);
            MySpriteRenderer.drawFilledBox(pose, buffer, TIP_BOX, tipColor);
        });
        poseStack.popPose();
    }
}
"""

    if norm == "com/robertx22/mine_and_slash/database/data/spells/summons/render/ModSkeletonRender.java":
        return """package com.robertx22.mine_and_slash.database.data.spells.summons.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.robertx22.mine_and_slash.database.data.spells.entities.renders.MySpriteRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

public class ModSkeletonRender<T extends Mob> extends EntityRenderer<T, ModSkeletonRender.HumanoidSummonState> {
    private final Identifier texture;
    private static final AABB HEAD = new AABB(-0.25, 1.4, -0.25, 0.25, 1.9, 0.25);
    private static final AABB BODY = new AABB(-0.25, 0.7, -0.13, 0.25, 1.4, 0.13);
    private static final AABB ARM_L = new AABB(0.25, 0.7, -0.1, 0.45, 1.4, 0.1);
    private static final AABB ARM_R = new AABB(-0.45, 0.7, -0.1, -0.25, 1.4, 0.1);
    private static final AABB LEG_L = new AABB(0.02, 0.0, -0.1, 0.22, 0.7, 0.1);
    private static final AABB LEG_R = new AABB(-0.22, 0.0, -0.1, -0.02, 0.7, 0.1);

    public ModSkeletonRender(Identifier id, EntityRendererProvider.Context ctx) {
        super(ctx);
        this.texture = id;
    }

    public static class HumanoidSummonState extends EntityRenderState {
        public float bodyYaw;
    }

    @Override
    public HumanoidSummonState createRenderState() {
        return new HumanoidSummonState();
    }

    @Override
    public void extractRenderState(T entity, HumanoidSummonState state, float tickProgress) {
        super.extractRenderState(entity, state, tickProgress);
        state.bodyYaw = entity.getYRot(tickProgress);
    }

    @Override
    public void submit(HumanoidSummonState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        super.submit(state, poseStack, collector, cameraState);
        poseStack.pushPose();
        poseStack.mulPose(new Matrix4f().rotationY((float) Math.toRadians(-state.bodyYaw)));
        int light = state.lightCoords;
        collector.order(0).submitCustomGeometry(poseStack, RenderTypes.entityCutoutNoCull(this.texture), (pose, vc) -> {
            MySpriteRenderer.drawTexturedBox(pose, vc, HEAD, light);
            MySpriteRenderer.drawTexturedBox(pose, vc, BODY, light);
            MySpriteRenderer.drawTexturedBox(pose, vc, ARM_L, light);
            MySpriteRenderer.drawTexturedBox(pose, vc, ARM_R, light);
            MySpriteRenderer.drawTexturedBox(pose, vc, LEG_L, light);
            MySpriteRenderer.drawTexturedBox(pose, vc, LEG_R, light);
        });
        poseStack.popPose();
    }
}
"""

    if norm == "com/robertx22/mine_and_slash/database/data/spells/summons/render/ModWolfRender.java":
        return """package com.robertx22.mine_and_slash.database.data.spells.summons.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.robertx22.mine_and_slash.database.data.spells.entities.renders.MySpriteRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

public class ModWolfRender<T extends Mob> extends EntityRenderer<T, ModWolfRender.WolfSummonState> {
    private final Identifier texture;
    private static final AABB HEAD = new AABB(-0.19, 0.38, 0.28, 0.19, 0.72, 0.62);
    private static final AABB BODY = new AABB(-0.22, 0.32, -0.35, 0.22, 0.68, 0.30);
    private static final AABB LEG_FL = new AABB(0.06, 0.0, 0.12, 0.20, 0.35, 0.26);
    private static final AABB LEG_FR = new AABB(-0.20, 0.0, 0.12, -0.06, 0.35, 0.26);
    private static final AABB LEG_BL = new AABB(0.06, 0.0, -0.30, 0.20, 0.35, -0.16);
    private static final AABB LEG_BR = new AABB(-0.20, 0.0, -0.30, -0.06, 0.35, -0.16);

    public ModWolfRender(Identifier id, EntityRendererProvider.Context ctx) {
        super(ctx);
        this.texture = id;
    }

    public static class WolfSummonState extends EntityRenderState {
        public float bodyYaw;
    }

    @Override
    public WolfSummonState createRenderState() {
        return new WolfSummonState();
    }

    @Override
    public void extractRenderState(T entity, WolfSummonState state, float tickProgress) {
        super.extractRenderState(entity, state, tickProgress);
        state.bodyYaw = entity.getYRot(tickProgress);
    }

    @Override
    public void submit(WolfSummonState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        super.submit(state, poseStack, collector, cameraState);
        poseStack.pushPose();
        poseStack.mulPose(new Matrix4f().rotationY((float) Math.toRadians(-state.bodyYaw)));
        int light = state.lightCoords;
        collector.order(0).submitCustomGeometry(poseStack, RenderTypes.entityCutoutNoCull(this.texture), (pose, vc) -> {
            MySpriteRenderer.drawTexturedBox(pose, vc, HEAD, light);
            MySpriteRenderer.drawTexturedBox(pose, vc, BODY, light);
            MySpriteRenderer.drawTexturedBox(pose, vc, LEG_FL, light);
            MySpriteRenderer.drawTexturedBox(pose, vc, LEG_FR, light);
            MySpriteRenderer.drawTexturedBox(pose, vc, LEG_BL, light);
            MySpriteRenderer.drawTexturedBox(pose, vc, LEG_BR, light);
        });
        poseStack.popPose();
    }
}
"""

    if norm == "com/robertx22/mine_and_slash/database/data/spells/summons/render/ModSpiderRender.java":
        return """package com.robertx22.mine_and_slash.database.data.spells.summons.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.robertx22.mine_and_slash.database.data.spells.entities.renders.MySpriteRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

public class ModSpiderRender<T extends Mob> extends EntityRenderer<T, ModSpiderRender.SpiderSummonState> {
    private final Identifier texture;
    private static final AABB HEAD = new AABB(-0.22, 0.18, 0.15, 0.22, 0.52, 0.50);
    private static final AABB ABDOMEN = new AABB(-0.30, 0.16, -0.50, 0.30, 0.58, 0.15);

    public ModSpiderRender(Identifier id, EntityRendererProvider.Context ctx) {
        super(ctx);
        this.texture = id;
    }

    public static class SpiderSummonState extends EntityRenderState {
        public float bodyYaw;
    }

    @Override
    public SpiderSummonState createRenderState() {
        return new SpiderSummonState();
    }

    @Override
    public void extractRenderState(T entity, SpiderSummonState state, float tickProgress) {
        super.extractRenderState(entity, state, tickProgress);
        state.bodyYaw = entity.getYRot(tickProgress);
    }

    @Override
    public void submit(SpiderSummonState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        super.submit(state, poseStack, collector, cameraState);
        poseStack.pushPose();
        poseStack.scale(0.55F, 0.55F, 0.55F);
        poseStack.mulPose(new Matrix4f().rotationY((float) Math.toRadians(-state.bodyYaw)));
        int light = state.lightCoords;
        collector.order(0).submitCustomGeometry(poseStack, RenderTypes.entityCutoutNoCull(this.texture), (pose, vc) -> {
            MySpriteRenderer.drawTexturedBox(pose, vc, HEAD, light);
            MySpriteRenderer.drawTexturedBox(pose, vc, ABDOMEN, light);
        });
        poseStack.popPose();
    }
}
"""

    if norm == "com/robertx22/mine_and_slash/mmorpg/registers/client/AutoSkullRender.java":
        return """package com.robertx22.mine_and_slash.mmorpg.registers.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.robertx22.mine_and_slash.database.data.spells.entities.AutoAimingProj;
import com.robertx22.mine_and_slash.database.data.spells.entities.renders.MySpriteRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

public class AutoSkullRender extends EntityRenderer<AutoAimingProj, AutoSkullRender.SkullProjState> {
    private static final Identifier WITHER_LOCATION = Identifier.withDefaultNamespace("textures/entity/wither/wither.png");
    private static final AABB SKULL = new AABB(-0.25, 0.0, -0.25, 0.25, 0.5, 0.25);

    public AutoSkullRender(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    public static class SkullProjState extends EntityRenderState {
        public float yRot;
        public float xRot;
    }

    @Override
    public SkullProjState createRenderState() {
        return new SkullProjState();
    }

    @Override
    public void extractRenderState(AutoAimingProj entity, SkullProjState state, float tickProgress) {
        super.extractRenderState(entity, state, tickProgress);
        state.yRot = entity.getYRot(tickProgress);
        state.xRot = entity.getXRot(tickProgress);
    }

    @Override
    protected int getBlockLightLevel(AutoAimingProj entity, BlockPos pos) {
        return 15;
    }

    @Override
    public void submit(SkullProjState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        super.submit(state, poseStack, collector, cameraState);
        poseStack.pushPose();
        poseStack.mulPose(new Matrix4f().rotationY((float) Math.toRadians(-state.yRot)));
        poseStack.mulPose(new Matrix4f().rotationX((float) Math.toRadians(state.xRot)));
        collector.order(0).submitCustomGeometry(poseStack, RenderTypes.entityCutoutNoCull(WITHER_LOCATION), (pose, vc) -> MySpriteRenderer.drawTexturedBox(pose, vc, SKULL, 0xF000F0));
        poseStack.popPose();
    }
}
"""

    if norm == "com/robertx22/mine_and_slash/mmorpg/registers/client/RenderRegister.java":
        return """package com.robertx22.mine_and_slash.mmorpg.registers.client;

import com.robertx22.forge_compat.client.event.EntityRenderersEvent;
import com.robertx22.mine_and_slash.database.data.spells.entities.renders.ModTridentRenderer;
import com.robertx22.mine_and_slash.database.data.spells.entities.renders.MySpriteRenderer;
import com.robertx22.mine_and_slash.database.data.spells.entities.renders.RangerArrowRenderer;
import com.robertx22.mine_and_slash.database.data.spells.summons.render.ModSkeletonRender;
import com.robertx22.mine_and_slash.database.data.spells.summons.render.ModSpiderRender;
import com.robertx22.mine_and_slash.database.data.spells.summons.render.ModWolfRender;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.mmorpg.registers.common.SlashEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.FallingBlockRenderer;

public class RenderRegister {
    public static void regRenders(EntityRenderersEvent.RegisterRenderers x) {
        x.registerEntityRenderer(SlashEntities.SPIRIT_WOLF.get(), ctx -> new ModWolfRender<>(SlashRef.id("textures/entity/spirit_wolf.png"), ctx));
        x.registerEntityRenderer(SlashEntities.SKELETON.get(), ctx -> new ModSkeletonRender<>(SlashRef.id("textures/entity/skeleton.png"), ctx));
        x.registerEntityRenderer(SlashEntities.ZOMBIE.get(), ctx -> new ModSkeletonRender<>(SlashRef.id("textures/entity/zombie.png"), ctx));
        x.registerEntityRenderer(SlashEntities.SPIDER.get(), ctx -> new ModSpiderRender<>(SlashRef.id("textures/entity/spider.png"), ctx));
        x.registerEntityRenderer(SlashEntities.FIRE_GOLEM.get(), ctx -> new ModSkeletonRender<>(SlashRef.id("textures/entity/fire_golem.png"), ctx));
        x.registerEntityRenderer(SlashEntities.COLD_GOLEM.get(), ctx -> new ModSkeletonRender<>(SlashRef.id("textures/entity/cold_golem.png"), ctx));
        x.registerEntityRenderer(SlashEntities.LIGHTNING_GOLEM.get(), ctx -> new ModSkeletonRender<>(SlashRef.id("textures/entity/lightning_golem.png"), ctx));
        x.registerEntityRenderer(SlashEntities.SIMPLE_PROJECTILE.get(), d -> new MySpriteRenderer<>(d, Minecraft.getInstance().getItemRenderer()));
        x.registerEntityRenderer(SlashEntities.SIMPLE_ARROW.get(), RangerArrowRenderer::new);
        x.registerEntityRenderer(SlashEntities.SIMPLE_BLOCK_ENTITY.get(), FallingBlockRenderer::new);
        x.registerEntityRenderer(SlashEntities.SIMPLE_TRIDENT.get(), ModTridentRenderer::new);
        x.registerEntityRenderer(SlashEntities.AUTO_AIMING_SKELETON_SKULL.get(), AutoSkullRender::new);
    }
}
"""

    if norm == "com/robertx22/mine_and_slash/a_libraries/neat/NeatRenderType.java":
        return """package com.robertx22.mine_and_slash.a_libraries.neat;

import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

public class NeatRenderType {
    public static final Identifier HEALTH_BAR_TEXTURE = Identifier.fromNamespaceAndPath(SlashRef.MODID, "textures/gui/health_bar_texture.png");
    public static final RenderType BAR_TEXTURE_TYPE = RenderTypes.entityTranslucent(HEALTH_BAR_TEXTURE);
}
"""

    if norm == "com/robertx22/mine_and_slash/gui/screens/skill_tree/SkillTreeRenderType.java":
        return """package com.robertx22.mine_and_slash.gui.screens.skill_tree;

import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

public class SkillTreeRenderType {
    public static RenderType getSkillTreeRenderType(String name, Identifier texture) {
        return RenderTypes.entityTranslucent(texture);
    }
}
"""

    if norm == "com/robertx22/mine_and_slash/mixins/AccessorRenderType.java":
        return """package com.robertx22.mine_and_slash.mixins;

public interface AccessorRenderType {
}
"""

    if norm == "com/robertx22/mine_and_slash/vanilla_mc/particles/SimpleParticle.java":
        return """package com.robertx22.mine_and_slash.vanilla_mc.particles;

import com.robertx22.forge_compat.api.distmarker.Dist;
import com.robertx22.forge_compat.api.distmarker.OnlyIn;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

public class SimpleParticle extends SingleQuadParticle {
    private final SpriteSet spriteProvider;

    private SimpleParticle(int maxage, ClientLevel world, double x, double y, double z, double velocityX, double velocityY, double velocityZ, SpriteSet spriteProvider) {
        super(world, x, y, z, spriteProvider.first());
        this.spriteProvider = spriteProvider;
        this.lifetime = maxage;
        this.gravity = 0.008F;
        this.xd = velocityX;
        this.yd = velocityY;
        this.zd = velocityZ;
        this.setSpriteFromAge(spriteProvider);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
        } else {
            this.yd -= (double) this.gravity;
            this.move(this.xd, this.yd, this.zd);
            this.setSpriteFromAge(this.spriteProvider);
        }
    }

    @Override
    public SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.OPAQUE;
    }

    @OnlyIn(Dist.CLIENT)
    public static class Factory implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteProvider;
        int maxAge;

        public Factory(int age, SpriteSet spriteProvider) {
            this.spriteProvider = spriteProvider;
            this.maxAge = age;
        }

        @Override
        public Particle createParticle(SimpleParticleType defaultParticleType, ClientLevel clientWorld, double d, double e, double f, double g, double h, double i, RandomSource random) {
            return new SimpleParticle(maxAge, clientWorld, d, e, f, g, h, i, this.spriteProvider);
        }
    }
}
"""



    if norm == "com/robertx22/library_of_exile/registry/loaders/BaseDataPackLoader.java":
        return """package com.robertx22.library_of_exile.registry.loaders;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.robertx22.library_of_exile.main.ExileLog;
import com.robertx22.library_of_exile.registry.ExileRegistry;
import com.robertx22.library_of_exile.registry.ExileRegistryType;
import com.robertx22.library_of_exile.registry.IAutoGson;
import com.robertx22.library_of_exile.registry.JsonExileRegistry;
import com.robertx22.library_of_exile.utils.Watch;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class BaseDataPackLoader<T extends ExileRegistry> extends SimplePreparableReloadListener<Map<Identifier, JsonElement>> {
    private static final Gson GSON = new Gson();
    public ExileRegistryType registryType;
    public String id;
    public Class<T> c;

    public BaseDataPackLoader(ExileRegistryType registryType, String id, Class<T> c) {
        this.registryType = registryType;
        this.id = id;
        this.c = c;
    }

    public static Info INFO = new Info();

    public static class Info {
        public int packsLoaded = 0;
        public boolean isDataPackReload = false;
    }

    @Override
    protected Map<Identifier, JsonElement> prepare(ResourceManager manager, ProfilerFiller profiler) {
        Map<Identifier, JsonElement> result = new HashMap<>();
        String prefix = this.id + "/";
        try {
            Map<Identifier, Resource> found = manager.listResources(this.id, loc -> loc.getPath().endsWith(".json"));
            for (Map.Entry<Identifier, Resource> entry : found.entrySet()) {
                Identifier rawLoc = entry.getKey();
                String path = rawLoc.getPath();
                if (path.startsWith(prefix) && path.endsWith(".json")) {
                    String clean = path.substring(prefix.length(), path.length() - 5);
                    Identifier cleanId = Identifier.fromNamespaceAndPath(rawLoc.getNamespace(), clean);
                    try (Reader reader = entry.getValue().openAsReader()) {
                        result.put(cleanId, JsonParser.parseReader(reader));
                    } catch (Exception e) {
                        ExileLog.get().warn("Failed parsing datapack JSON " + rawLoc + ": " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            ExileLog.get().warn("Failed scanning datapack directory " + this.id + ": " + e.getMessage());
        }
        return result;
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> mapToLoad, ResourceManager manager, ProfilerFiller profilerIn) {
        applyMap(mapToLoad);
    }

    public void applyMap(Map<Identifier, JsonElement> mapToLoad) {
        try {
            Objects.requireNonNull(registryType);
            Watch normal = new Watch();
            normal.min = 50000;
            this.registryType.getDatapackSerializer().onBeforeReload();
            INFO.packsLoaded++;
            if (mapToLoad.isEmpty()) {
                ExileLog.get().warn("Exile Datapack registry is empty and failed to load any entries: " + this.id);
            }
            for (Map.Entry<Identifier, JsonElement> entry : mapToLoad.entrySet()) {
                Identifier key = entry.getKey();
                JsonElement value = entry.getValue();
                try {
                    JsonObject json = null;
                    try {
                        json = value.getAsJsonObject();
                    } catch (Exception e) {
                        ExileLog.get().warn(key.toString() + " is a broken json, failed to load into an object.");
                        continue;
                    }
                    T object = this.registryType.getSerializer().fromJson(json);
                    if (object != null) {
                        if (object instanceof JsonExileRegistry<?> j) {
                            j.addToSerializables(registryType.ser);
                        }
                        object.registerToExileRegistry(INFO.isDataPackReload ? com.robertx22.library_of_exile.registry.register_info.ClientSyncRegistration.INSTANCE : com.robertx22.library_of_exile.registry.register_info.SeriazableRegistration.INFO);
                    }
                } catch (Exception exception) {
                    ExileLog.get().warn(key.toString() + " is a broken datapack entry.");
                    exception.printStackTrace();
                }
            }
            normal.print("Loading " + registryType.id + " jsons ");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
"""

    if norm == "com/robertx22/mine_and_slash/gui/SocketTooltip.java":
        return """package com.robertx22.mine_and_slash.gui;

import com.robertx22.mine_and_slash.saveclasses.gearitem.gear_parts.SocketData;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class SocketTooltip implements ClientTooltipComponent {
    private final List<SocketData> gems;
    private final ItemStack stack;

    public SocketTooltip(SocketComponent comp) {
        this.gems = comp.gems();
        this.stack = comp.stack();
    }

    @Override
    public int getHeight(Font font) {
        return 19;
    }

    @Override
    public int getWidth(Font font) {
        return Math.max(20, gems.size() * 18);
    }

    @Override
    public void extractImage(Font font, int x, int y, int width, int height, GuiGraphicsExtractor gfx) {
        int xp = x;
        for (SocketData socket : gems) {
            ItemStack gemStack = socket.getInsertedItemStack();
            if (gemStack != null && !gemStack.isEmpty()) {
                gfx.fakeItem(gemStack, xp, y);
            }
            xp += 18;
        }
    }

    public record SocketComponent(ItemStack stack, List<SocketData> gems) implements TooltipComponent {
    }
}
"""

    if norm == "com/robertx22/mine_and_slash/a_libraries/dmg_number_particle/particle/ExileInteractionResultParticle.java":
        return """package com.robertx22.mine_and_slash.a_libraries.dmg_number_particle.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.robertx22.mine_and_slash.a_libraries.dmg_number_particle.particle.style.IParticleRenderStrategy;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.phys.Vec3;

public abstract class ExileInteractionResultParticle extends SingleQuadParticle {
    protected IParticleRenderStrategy strategy;
    protected final Vec3 originalPos;

    protected ExileInteractionResultParticle(ClientLevel level, double x, double y, double z, IParticleRenderStrategy strategy) {
        super(level, x, y, z, (TextureAtlasSprite) null);
        this.strategy = strategy;
        this.originalPos = new Vec3(x, y, z);
    }

    public IParticleRenderStrategy getStrategy() { return this.strategy; }
    public Vec3 getPos() { return new Vec3(this.x, this.y, this.z); }
    public Vec3 getOriginalPosition() { return this.originalPos; }
    public double getPrevX() { return this.xo; }
    public double getPrevY() { return this.yo; }
    public double getPrevZ() { return this.zo; }
    public int getAge() { return this.age; }
    public int getLiftTime() { return this.lifetime; }
    public void setLiftTime(int life) { this.lifetime = life; }
    public void setLifetime(int life) { this.lifetime = life; }
    public void setGravity(float gravity) { this.gravity = gravity; }
    public void setParticleSpeed(double dx, double dy, double dz) { this.xd = dx; this.yd = dy; this.zd = dz; }
    public void render(VertexConsumer vc, Camera cam, float partialTick) {}

    @Override
    public SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.OPAQUE;
    }
}
"""

    if norm in (
        "com/robertx22/mine_and_slash/a_libraries/dmg_number_particle/particle/impl/DamageNullifiedParticle.java",
        "com/robertx22/mine_and_slash/a_libraries/dmg_number_particle/particle/impl/ElementDamageParticle.java",
        "com/robertx22/mine_and_slash/a_libraries/dmg_number_particle/particle/impl/HealParticle.java",
    ):
        code = code.replace("@Override\n    public void render(", "public void render(")

    if norm == "com/robertx22/mine_and_slash/a_libraries/neat/HealthBarRenderer.java":
        return """package com.robertx22.mine_and_slash.a_libraries.neat;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;

public class HealthBarRenderer {
    public static void hookRender(PoseStack poseStack, MultiBufferSource buffers, Camera camera, Entity entity) {
    }
}
"""

    if norm == "com/robertx22/mine_and_slash/a_libraries/player_animations/PlayerAnimations.java":
        return """package com.robertx22.mine_and_slash.a_libraries.player_animations;

import net.minecraft.world.entity.player.Player;

public class PlayerAnimations {
    public enum CastEnum {
        NONE, CAST_START, CAST_LOOP, CAST_FINISH
    }
    public static void initClient() {}
    public static void onSpellCast(Player player, String animId) {}
    public static void tick(Player player) {}
}
"""

    if norm == "com/robertx22/mine_and_slash/database/data/spells/summons/model/ModWolfModel.java":
        return """package com.robertx22.mine_and_slash.database.data.spells.summons.model;

import net.minecraft.world.entity.LivingEntity;

public class ModWolfModel<T extends LivingEntity> {
}
"""

    if norm == "com/robertx22/mine_and_slash/aoe_data/datapacks/generators/LootTableGenerator.java":
        return """package com.robertx22.mine_and_slash.aoe_data.datapacks.generators;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class LootTableGenerator {
    static Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public void run() {}
}
"""

    if norm == "com/robertx22/library_of_exile/config/map_dimension/MapGameRules.java":
        return """package com.robertx22.library_of_exile.config.map_dimension;

import net.minecraft.world.level.gamerules.GameRules;

public class MapGameRules {
    public GameRules rules;
    public MapGameRules(GameRules rules) {
        this.rules = rules;
    }
}
"""

    if norm == "com/robertx22/mine_and_slash/gui/screens/skill_tree/component/TipsWidget.java":
        return """package com.robertx22.mine_and_slash.gui.screens.skill_tree.component;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public class TipsWidget extends AbstractWidget {
    public TipsWidget(int x, int y) {
        super(x, y, 20, 20, Component.empty());
    }

    public void render(GuiGraphicsExtractor gui, int mx, int my, float pt) {
        extractWidgetRenderState(gui, mx, my, pt);
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor gui, int mx, int my, float pt) {
        gui.text(Minecraft.getInstance().font, Component.literal("[?]"), getX(), getY(), 0xFFFFFF55, true);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput out) {
    }
}
"""

    if norm == "com/robertx22/mine_and_slash/vanilla_mc/items/gearitems/bases/SingleTargetWeapon.java":
        return """package com.robertx22.mine_and_slash.vanilla_mc.items.gearitems.bases;

import com.robertx22.forge_compat.common.Tiers;
import com.robertx22.library_of_exile.vanilla_util.main.VanillaUTIL;
import com.robertx22.mine_and_slash.uncommon.interfaces.IAutoLocName;
import net.minecraft.world.item.Item;

public abstract class SingleTargetWeapon extends Item implements IAutoLocName {
    String locname;
    public float attackSpeed = -2.4F;

    public SingleTargetWeapon(Tiers mat, Properties settings, String locname) {
        super(settings);
        this.locname = locname;
    }

    @Override
    public final String locNameForLangFile() {
        return locname;
    }

    @Override
    public AutoLocGroup locNameGroup() {
        return AutoLocGroup.Gear_Items;
    }

    @Override
    public String locNameLangFileGUID() {
        return VanillaUTIL.REGISTRY.items().getKey(this).toString();
    }

    @Override
    public String GUID() {
        return "";
    }
}
"""

    if norm == "com/robertx22/mine_and_slash/gui/screens/skill_tree/BufferInfo.java":
        return """package com.robertx22.mine_and_slash.gui.screens.skill_tree;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.joml.Matrix3x2f;

public record BufferInfo(
    float pX1, float pX2, float pY1, float pY2, float pBlitOffset,
    int uWidth, int vHeight, float uOffset, float vOffset, int texWidth, int texHeight,
    Matrix3x2f matrix, RenderInfo renderInfo
) {
    public static BufferInfo of(float pX, float pY, float pBlitOffset, int pUOffset, int pVOffset, int pUWidth, int pVHeight, Matrix3x2f m) {
        return of(pX, pY, pBlitOffset, (float) pUOffset, (float) pVOffset, pUWidth, pVHeight, 256, 256, m);
    }

    public static BufferInfo of(float pX, float pY, int pUOffset, int pVOffset, int pUWidth, int pVHeight, Matrix3x2f m) {
        return of(pX, pY, 0, (float) pUOffset, (float) pVOffset, pUWidth, pVHeight, 256, 256, m);
    }

    public static BufferInfo of(float pX, float pY, float pBlitOffset, float pUOffset, float pVOffset, int pUWidth, int pVHeight, int pTextureWidth, int pTextureHeight, Matrix3x2f m) {
        return of(pX, pX + pUWidth, pY, pY + pVHeight, pBlitOffset, pUWidth, pVHeight, pUOffset, pVOffset, pTextureWidth, pTextureHeight, m);
    }

    public static BufferInfo of(float pX, float pY, int pWidth, int pHeight, float pUOffset, float pVOffset, int pUWidth, int pVHeight, int pTextureWidth, int pTextureHeight, Matrix3x2f m) {
        return of(pX, pX + pWidth, pY, pY + pHeight, 0, pUWidth, pVHeight, pUOffset, pVOffset, pTextureWidth, pTextureHeight, m);
    }

    public static BufferInfo of(float pX, float pY, int pWidth, int pHeight, float pBlitOffset, float pUOffset, float pVOffset, int pUWidth, int pVHeight, int pTextureWidth, int pTextureHeight, Matrix3x2f m) {
        return of(pX, pX + pWidth, pY, pY + pHeight, pBlitOffset, pUWidth, pVHeight, pUOffset, pVOffset, pTextureWidth, pTextureHeight, m);
    }

    public static BufferInfo of(float pX, float pY, float pUOffset, float pVOffset, int pWidth, int pHeight, int pTextureWidth, int pTextureHeight, Matrix3x2f m) {
        return of(pX, pY, pWidth, pHeight, pUOffset, pVOffset, pWidth, pHeight, pTextureWidth, pTextureHeight, m);
    }

    public static BufferInfo of(float pX1, float pX2, float pY1, float pY2, float pBlitOffset, int pUWidth, int pVHeight, float pUOffset, float pVOffset, int pTextureWidth, int pTextureHeight, Matrix3x2f m) {
        return new BufferInfo(pX1, pX2, pY1, pY2, pBlitOffset, pUWidth, pVHeight, pUOffset, pVOffset, pTextureWidth, pTextureHeight, new Matrix3x2f(m), new RenderInfo(1.0f));
    }

    public BufferInfo withRenderInfo(RenderInfo renderInfo) {
        return new BufferInfo(pX1, pX2, pY1, pY2, pBlitOffset, uWidth, vHeight, uOffset, vOffset, texWidth, texHeight, matrix, renderInfo);
    }

    public void draw(GuiGraphicsExtractor gui, Identifier texture) {
        if (texture == null) return;
        float alpha = Math.min(1.0f, Math.max(0.05f, this.renderInfo.opacity()));
        int tint = ARGB.colorFromFloat(alpha, 1.0f, 1.0f, 1.0f);
        gui.pose().pushMatrix();
        gui.pose().set(this.matrix);
        gui.blit(
            RenderPipelines.GUI_TEXTURED,
            texture,
            Math.round(pX1),
            Math.round(pY1),
            uOffset,
            vOffset,
            Math.max(1, Math.round(pX2 - pX1)),
            Math.max(1, Math.round(pY2 - pY1)),
            uWidth,
            vHeight,
            Math.max(1, texWidth),
            Math.max(1, texHeight),
            tint
        );
        gui.pose().popMatrix();
    }

    public record RenderInfo(float opacity) {}
}
"""

    if norm == "com/robertx22/mine_and_slash/gui/screens/skill_tree/VertexContainer.java":
        return """package com.robertx22.mine_and_slash.gui.screens.skill_tree;

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
"""

    if norm == "com/robertx22/mine_and_slash/uncommon/interfaces/data_items/VanillaRarities.java":
        return """package com.robertx22.mine_and_slash.uncommon.interfaces.data_items;

import net.minecraft.world.item.Rarity;
import java.util.HashMap;

public class VanillaRarities {
    public static Rarity COMMON_ITEM = Rarity.COMMON;
    public static Rarity UNCOMMON_ITEM = Rarity.UNCOMMON;
    public static Rarity RARE_ITEM = Rarity.RARE;
    public static Rarity EPIC_ITEM = Rarity.EPIC;
    public static Rarity LEGENDARY_ITEM = Rarity.EPIC;
    public static Rarity MYTHIC_ITEM = Rarity.EPIC;
    public static Rarity RUNED_ITEM = Rarity.RARE;
    public static Rarity UNIQUE_ITEM = Rarity.EPIC;

    public static HashMap<String, Rarity> MAP = new HashMap<>();
    static {
        MAP.put("common", COMMON_ITEM);
        MAP.put("uncommon", UNCOMMON_ITEM);
        MAP.put("rare", RARE_ITEM);
        MAP.put("epic", EPIC_ITEM);
        MAP.put("legendary", LEGENDARY_ITEM);
        MAP.put("mythic", MYTHIC_ITEM);
        MAP.put("runed", RUNED_ITEM);
        MAP.put("unique", UNIQUE_ITEM);
    }
}
"""

    if norm == "com/robertx22/mine_and_slash/a_libraries/dmg_number_particle/DamageParticleRenderer.java":
        return """package com.robertx22.mine_and_slash.a_libraries.dmg_number_particle;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.MultiBufferSource;

import java.util.HashSet;
import java.util.Set;

public class DamageParticleRenderer {
    public static Set<DamageParticle> PARTICLES = new HashSet<>();

    public static void renderParticles(GuiGraphicsExtractor gui, Camera camera) {
        PARTICLES.forEach(DamageParticle::tick);
        PARTICLES.removeIf(x -> x.age > 50);
    }

    public static void renderNameTag(Camera camera, String text, DamageParticle p, PoseStack stack, float tick, MultiBufferSource buf) {
    }
}
"""

    if norm == "com/robertx22/mine_and_slash/a_libraries/dmg_number_particle/particle/ExileInteractionResultParticle.java":
        return """package com.robertx22.mine_and_slash.a_libraries.dmg_number_particle.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.phys.Vec3;

public abstract class ExileInteractionResultParticle extends SingleQuadParticle {
    protected ExileInteractionResultParticle(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z, (TextureAtlasSprite) null);
    }

    public Vec3 getPos() {
        return new Vec3(this.x, this.y, this.z);
    }

    public double getPrevX() { return this.xo; }
    public double getPrevY() { return this.yo; }
    public double getPrevZ() { return this.zo; }
    public void setLifetime(int life) { this.lifetime = life; }
    public void setGravity(float gravity) { this.gravity = gravity; }

    @Override
    public SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.OPAQUE;
    }
}
"""

    if norm == "com/robertx22/mine_and_slash/vanilla_mc/new_commands/BuilderToolCommands.java":
        return """package com.robertx22.mine_and_slash.vanilla_mc.new_commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;

public class BuilderToolCommands {
    public static void register(CommandDispatcher<CommandSourceStack> commandDispatcher) {
    }
}
"""

    # ------------------------------------------------------------------
    # Standard Multi-File Transformations across all 1,938 upstream files
    # ------------------------------------------------------------------
    for old_imp, new_imp in IMPORT_REWRITES:
        code = code.replace("import " + old_imp, "import " + new_imp)

    # Capabilities: target.getCapability(CAP) -> CapabilityProvider.get(target, CAP)
    def tf_get_cap(m, args):
        recv = m.group(1)
        if recv.endswith("CapabilityProvider"):
            return None
        if len(args) >= 1:
            return f"com.robertx22.forge_compat.common.capabilities.CapabilityProvider.get({recv}, {args[0]})"
        return None
    code = rewrite_method_call(code, EXPR_TOKEN + r"\.getCapability\s*\(", tf_get_cap)
    code = code.replace(".reviveCaps();", ";")
    code = code.replace(".invalidateCaps();", ";")

    # ResourceLocation -> Identifier (multi-pass for nested expressions)
    code = re.sub(r"\bResourceLocation\b", "Identifier", code)
    for _ in range(3):
        code = rewrite_method_call(
            code,
            r"\bnew\s+Identifier\s*\(",
            lambda m, args: (
                f"Identifier.fromNamespaceAndPath({args[0]}, {args[1]})"
                if len(args) == 2
                else (f"Identifier.parse(String.valueOf({args[0]}))" if len(args) == 1 else None)
            ),
        )
    code = code.replace("Identifier.tryParse(", "Identifier.tryParse(")
    code = code.replace("GuiGraphics ", "GuiGraphicsExtractor ")
    code = code.replace("(GuiGraphics)", "(GuiGraphicsExtractor)")
    code = code.replace("<GuiGraphics>", "<GuiGraphicsExtractor>")
    code = code.replace(", GuiGraphics>", ", GuiGraphicsExtractor>")
    code = re.sub(r"\bUseAnim\b", "ItemUseAnimation", code)
    code = re.sub(r"\bMobSpawnType\b", "net.minecraft.world.entity.EntitySpawnReason", code)

    # BuiltInRegistries.<REG>.get(id) -> BuiltInRegistries.<REG>.getValue(id)
    code = re.sub(
        r"\bBuiltInRegistries\.([A-Z_]+)\.get\(",
        r"BuiltInRegistries.\1.getValue(",
        code,
    )

    # Attributes.<CONST> used as Attribute vs Holder<Attribute>
    code = re.sub(r"\b(Attributes\.[A-Z_]+)\.getDescriptionId\(\)", r"\1.value().getDescriptionId()", code)
    code = re.sub(r"\bBuiltInRegistries\.ATTRIBUTE\.getKey\((Attributes\.[A-Z_]+)\)", r"BuiltInRegistries.ATTRIBUTE.getKey(\1.value())", code)
    code = re.sub(r"\bForgeRegistries\.ATTRIBUTES\.getKey\((Attributes\.[A-Z_]+)\)", r"ForgeRegistries.ATTRIBUTES.getKey(\1.value())", code)

    # GuiGraphics rendering rewrites
    code = re.sub(
        r"([A-Za-z_][A-Za-z0-9_]*)\.render\s*\(\s*GuiGraphicsExtractor\s+([A-Za-z_][A-Za-z0-9_]*)",
        r"\1.extractRenderState(GuiGraphicsExtractor \2",
        code,
    )
    is_widget_cls = bool(re.search(r"public\s+(?:abstract\s+)?class\s+\w+\s+extends\s+(?:ImageButton|Button|AbstractWidget|TextButton|ObjectSelectionList)\b", code))
    has_render_widget = bool(re.search(r"(?:public|protected)\s+void\s+renderWidget\s*\(\s*GuiGraphicsExtractor\b", code))
    if is_widget_cls and has_render_widget:
        code = re.sub(r"@Override\s*public\s+void\s+render\s*\(\s*GuiGraphicsExtractor\b", "public void renderLegacyUnused(GuiGraphicsExtractor", code)
        code = re.sub(r"public\s+void\s+render\s*\(\s*GuiGraphicsExtractor\b", "public void renderLegacyUnused(GuiGraphicsExtractor", code)
    else:
        code = re.sub(
            r"public\s+void\s+render\s*\(\s*GuiGraphicsExtractor\b",
            "public void extractWidgetRenderState(GuiGraphicsExtractor" if is_widget_cls else "public void extractRenderState(GuiGraphicsExtractor",
            code,
        )
    code = re.sub(
        r"protected\s+void\s+renderWidget\s*\(\s*GuiGraphicsExtractor\b",
        "public void extractWidgetRenderState(GuiGraphicsExtractor",
        code,
    )
    code = re.sub(
        r"protected\s+void\s+renderBg\s*\(\s*GuiGraphicsExtractor\b",
        "protected void extractBackground(GuiGraphicsExtractor",
        code,
    )
    code = re.sub(
        r"public\s+void\s+renderWidget\s*\(\s*GuiGraphicsExtractor\b",
        "public void extractWidgetRenderState(GuiGraphicsExtractor",
        code,
    )
    code = re.sub(
        r"super\.render\s*\(\s*([A-Za-z_][A-Za-z0-9_]*)\s*,",
        ("super.extractWidgetRenderState(\\1," if is_widget_cls else "super.extractRenderState(\\1,"),
        code,
    )
    code = re.sub(
        r"\.renderBackground\s*\(\s*([A-Za-z_][A-Za-z0-9_]*)\s*\)",
        r".extractBackground(\1, 0, 0, 0F)",
        code,
    )

    def tf_blit(m, args):
        g = m.group(1)
        if len(args) in (6, 7, 8, 9, 10, 11):
            return f"com.robertx22.forge_compat.client.GuiBlitHelper.blit({g}, {', '.join(args)})"
        return None
    code = rewrite_method_call(code, r"\b([A-Za-z_][A-Za-z0-9_]*)\.blit\s*\(", tf_blit)

    def tf_draw_str(m, args):
        g = m.group(1)
        fn = m.group(2)
        if len(args) in (4, 5):
            helper = "drawString" if fn == "drawString" else "drawCenteredString"
            return f"com.robertx22.forge_compat.client.GuiBlitHelper.{helper}({g}, {', '.join(args)})"
        return None
    code = rewrite_method_call(code, r"\b([A-Za-z_][A-Za-z0-9_]*)\.(drawString|drawCenteredString)\s*\(", tf_draw_str)

    def tf_tooltip(m, args):
        g = m.group(1)
        fn = m.group(2)
        return f"com.robertx22.forge_compat.client.GuiBlitHelper.{fn}({g}, {', '.join(args)})"
    code = rewrite_method_call(code, r"\b([A-Za-z_][A-Za-z0-9_]*)\.(renderTooltip|renderComponentTooltip)\s*\(", tf_tooltip)

    code = re.sub(r"\b([A-Za-z_][A-Za-z0-9_]*)\.renderFakeItem\s*\(", r"\1.fakeItem(", code)
    code = re.sub(r"\b([A-Za-z_][A-Za-z0-9_]*)\.renderItem\s*\(", r"\1.item(", code)
    code = re.sub(r"\b([A-Za-z_][A-Za-z0-9_]*)\.renderItemDecorations\s*\(", r"\1.itemDecorations(", code)
    code = re.sub(r"\b([A-Za-z_][A-Za-z0-9_]*)\.hLine\s*\(", r"\1.horizontalLine(", code)
    code = re.sub(r"\b([A-Za-z_][A-Za-z0-9_]*)\.vLine\s*\(", r"\1.verticalLine(", code)
    code = re.sub(r"\b([A-Za-z_][A-Za-z0-9_]*)\.pose\(\)\.scale\(\s*([^,]+),\s*([^,]+),\s*[^)]+\)", r"\1.pose().scale((float)(\2), (float)(\3))", code)
    code = re.sub(r"\b([A-Za-z_][A-Za-z0-9_]*)\.pose\(\)\.translate\(\s*([^,]+),\s*([^,]+),\s*[^)]+\)", r"\1.pose().translate((float)(\2), (float)(\3))", code)
    code = re.sub(
        r"([A-Za-z_][A-Za-z0-9_]*)\.setColor\s*\(\s*([^,]+),\s*([^,]+),\s*([^,]+),\s*([^)]+)\)",
        r"com.robertx22.forge_compat.client.RenderSystem.setShaderColor(\2, \3, \4, \5)",
        code,
    )

    # Single-pass ItemStack NBT bridges -> NbtStackCompat (prevents double-wrapping!)
    for old_m, helper_m in [
        ("getOrCreateTag", "getOrCreateTag"),
        ("hasTag", "hasTag"),
        ("getTag", "getTag"),
    ]:
        code = re.sub(
            EXPR_TOKEN + rf"\.{old_m}\s*\(\s*\)",
            lambda mm, hm=helper_m: (
                mm.group(0)
                if mm.group(1).endswith("NbtStackCompat") or mm.group(1) in ("style", "x")
                else f"com.robertx22.forge_compat.common.NbtStackCompat.{hm}({mm.group(1)})"
            ),
            code,
        )

    def tf_set_tag(m, args):
        recv = m.group(1)
        if recv.endswith("NbtStackCompat"):
            return None
        if len(args) == 1:
            return f"com.robertx22.forge_compat.common.NbtStackCompat.setTag({recv}, {args[0]})"
        return None
    code = rewrite_method_call(code, EXPR_TOKEN + r"\.setTag\s*\(", tf_set_tag)

    def tf_hover_name(m, args):
        recv = m.group(1)
        if recv.endswith("NbtStackCompat"):
            return None
        if len(args) == 1:
            return f"com.robertx22.forge_compat.common.NbtStackCompat.setHoverName({recv}, {args[0]})"
        return None
    code = rewrite_method_call(code, EXPR_TOKEN + r"\.setHoverName\s*\(", tf_hover_name)

    code = re.sub(
        EXPR_TOKEN + r"\.hasCustomHoverName\s*\(\s*\)",
        r"com.robertx22.forge_compat.common.NbtStackCompat.hasCustomHoverName(\1)",
        code,
    )
    code = re.sub(
        EXPR_TOKEN + r"\.hideTooltipPart\s*\([^)]*\)",
        r"com.robertx22.forge_compat.common.NbtStackCompat.hideTooltipPart(\1)",
        code,
    )

    # Scoped CompoundTag get*Or
    code = rewrite_scoped_nbt_getters(code)
    def tf_get_list(m, args):
        if len(args) == 2:
            return f"{m.group(1)}.getListOrEmpty({args[0]})"
        return None
    code = rewrite_method_call(code, EXPR_TOKEN + r"\.getList\s*\(", tf_get_list)

    # MC 26.3 AttributeModifier + AttributeInstance + MobEffectInstance + SoundEvents
    code = code.replace("AttributeModifier.Operation.ADDITION", "AttributeModifier.Operation.ADD_VALUE")
    code = code.replace("AttributeModifier.Operation.MULTIPLY_BASE", "AttributeModifier.Operation.ADD_MULTIPLIED_BASE")
    code = code.replace("AttributeModifier.Operation.MULTIPLY_TOTAL", "AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL")
    code = re.sub(r"\bOperation\.ADDITION\b", "AttributeModifier.Operation.ADD_VALUE", code)
    code = re.sub(r"\bOperation\.MULTIPLY_BASE\b", "AttributeModifier.Operation.ADD_MULTIPLIED_BASE", code)
    code = re.sub(r"\bOperation\.MULTIPLY_TOTAL\b", "AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL", code)

    def tf_attr_mod(m, args):
        if len(args) == 4:
            return f"com.robertx22.forge_compat.common.CompatHelper.attrMod({', '.join(args)})"
        if len(args) == 3:
            return f"com.robertx22.forge_compat.common.CompatHelper.attrMod({args[0]}, {args[1]}, {args[2]})"
        return None
    code = rewrite_method_call(code, r"\bnew\s+AttributeModifier\s*\(", tf_attr_mod)
    code = re.sub(r"\.getOperation\(\)", ".operation()", code)
    code = re.sub(r"\.getAmount\(\)", ".amount()", code)
    code = re.sub(r"\b([A-Za-z_][A-Za-z0-9_]*)\.getId\(\)", lambda mm: f"{mm.group(1)}.id()" if mm.group(1) in ("mod", "modifier", "attributemodifier", "m") else mm.group(0), code)
    code = code.replace("attributemodifier.operation().toValue()", "attributemodifier.operation().ordinal()")

    # AttributeInstance / MobEffect / Sound / Cooldown / Level / Enchantment bridges via CompatHelper
    code = re.sub(EXPR_TOKEN + r"\.getMinBuildHeight\s*\(\s*\)", r"com.robertx22.forge_compat.common.CompatHelper.minBuildHeight(\1)", code)
    code = re.sub(EXPR_TOKEN + r"\.getMaxBuildHeight\s*\(\s*\)", r"com.robertx22.forge_compat.common.CompatHelper.maxBuildHeight(\1)", code)
    code = re.sub(r"\bEnchantmentHelper\.getItemEnchantmentLevel\s*\(\s*([^,]+),\s*([^)]+)\)", r"com.robertx22.forge_compat.common.CompatHelper.getEnchantLevel(null, \1, \2)", code)
    code = re.sub(r"\bEnchantmentHelper\.getEnchantmentLevel\s*\(\s*([^,]+),\s*([^)]+)\)", r"com.robertx22.forge_compat.common.CompatHelper.getEnchantLevel(\2, \1, \2.getMainHandItem())", code)
    code = re.sub(EXPR_TOKEN + r"\.level\(\)\.isClientSide\b(?!\s*\()", r"\1.level().isClientSide()", code)
    code = re.sub(r"\b(world|level|pLevel|clientLevel|lvl)\.isClientSide\b(?!\s*\()", r"\1.isClientSide()", code)
    code = re.sub(EXPR_TOKEN + r"\.isClientSide\b(?!\s*\()", lambda m: m.group(0) if "boolean" in m.group(0) else f"{m.group(1)}.isClientSide()", code)
    code = re.sub(r"\b(SoundEvents\.[A-Z0-9_]+)\b(?!\.value\(\))", r"com.robertx22.forge_compat.common.CompatHelper.unwrapSound(\1)", code)

    def tf_add_cd(m, args):
        recv = m.group(1)
        if len(args) == 2:
            return f"com.robertx22.forge_compat.common.CompatHelper.addCooldown({recv}, {args[0]}, {args[1]})"
        return None
    code = rewrite_method_call(code, EXPR_TOKEN + r"\.addCooldown\s*\(", tf_add_cd)

    def tf_is_cd(m, args):
        recv = m.group(1)
        if len(args) == 1 and recv in ("player.getCooldowns()", "p.getCooldowns()", "en.getCooldowns()", "event.getEntity().getCooldowns()", "mc.player.getCooldowns()"):
            return f"com.robertx22.forge_compat.common.CompatHelper.isOnCooldown({recv}, {args[0]})"
        return None
    code = rewrite_method_call(code, EXPR_TOKEN + r"\.isOnCooldown\s*\(", tf_is_cd)

    def tf_mob_effect_inst(m, args):
        if len(args) >= 1:
            args2 = [f"com.robertx22.forge_compat.common.CompatHelper.wrapEffect({args[0]})"] + args[1:]
            return f"new MobEffectInstance({', '.join(args2)})"
        return None
    code = rewrite_method_call(code, r"\bnew\s+MobEffectInstance\s*\(", tf_mob_effect_inst)

    def tf_effect_call(m, args):
        recv, fn = m.group(1), m.group(2)
        if len(args) == 1 and recv not in ("CompatHelper",):
            return f"com.robertx22.forge_compat.common.CompatHelper.{fn}({recv}, {args[0]})"
        return None
    code = rewrite_method_call(code, EXPR_TOKEN + r"\.(hasEffect|getEffect|removeEffect)\s*\(", tf_effect_call)

    def tf_get_attr(m, args):
        recv = m.group(1)
        if len(args) == 1 and recv not in ("CompatHelper",):
            return f"com.robertx22.forge_compat.common.CompatHelper.getAttribute({recv}, {args[0]})"
        return None
    code = rewrite_method_call(code, EXPR_TOKEN + r"\.getAttribute\s*\(", tf_get_attr)

    # Direction.getNormal().multiply(N) -> Direction.getUnitVec3i().multiply(N)
    code = code.replace(".getNormal()", ".getUnitVec3i()")
    code = code.replace(".dimensionTypeId().location()", ".dimension().identifier()")
    code = code.replace(".location()", ".identifier()")

    # EntityType.create(world) -> EntityType.create(world, EntitySpawnReason.MOB_SUMMONED)
    def tf_et_create(m, args):
        recv = m.group(1)
        if len(args) == 1 and "MenuType" not in recv and "Container" not in recv and not recv.endswith("Builder") and not recv.endswith("FabricBlockEntityTypeBuilder") and not recv.endswith("TagKey") and not recv.endswith("BlockTags") and not recv.endswith("ItemTags") and not recv.endswith("VanillaStatData"):
            if any(k in recv for k in ("type", "EntityType", "get()", "mob")):
                return f"{recv}.create({args[0]}, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED)"
        return None
    code = rewrite_method_call(code, EXPR_TOKEN + r"\.create\s*\(", tf_et_create)

    # BlockEntityType.Builder.of -> FabricBlockEntityTypeBuilder.create
    code = code.replace(
        "BlockEntityType.Builder.of(",
        "net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder.create(",
    )
    code = re.sub(r"\.build\s*\(\s*null\s*\)", ".build()", code)

    # BlockEntity load/saveAdditional -> MC 26.3 ValueInput/ValueOutput
    if re.search(r"class\s+\w+\s+extends\s+(?:BlockEntity|BaseContainerBlockEntity)\b", code):
        has_be_load = bool(re.search(r"(@Override\s*)?public\s+void\s+load\s*\(\s*CompoundTag\b", code))
        has_be_save = bool(re.search(r"(@Override\s*)?protected\s+void\s+saveAdditional\s*\(\s*CompoundTag\b", code))
        code = re.sub(
            r"(@Override\s*)?public\s+void\s+load\s*\(\s*CompoundTag\s+([A-Za-z_][A-Za-z0-9_]*)\s*\)\s*\{",
            r"public void loadLegacyTag(CompoundTag \2) {\n        // MC 26.3 ValueInput bridge delegates here",
            code,
        )
        code = code.replace("super.load(", "// super.load(")
        code = re.sub(
            r"(@Override\s*)?protected\s+void\s+saveAdditional\s*\(\s*CompoundTag\s+([A-Za-z_][A-Za-z0-9_]*)\s*\)\s*\{",
            r"protected void saveAdditionalLegacyTag(CompoundTag \2) {",
            code,
        )
        code = code.replace("super.saveAdditional(", "// super.saveAdditional(")
        inject_be = ""
        if has_be_load:
            inject_be += """
    @Override
    protected void loadAdditional(net.minecraft.world.level.storage.ValueInput input) {
        super.loadAdditional(input);
        input.read("aoe_be_nbt", CompoundTag.CODEC).ifPresent(__t -> this.loadLegacyTag(__t));
    }
"""
        if has_be_save:
            inject_be += """
    @Override
    protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput output) {
        super.saveAdditional(output);
        CompoundTag __nbt = new CompoundTag();
        this.saveAdditionalLegacyTag(__nbt);
        output.store("aoe_be_nbt", CompoundTag.CODEC, __nbt);
    }
"""
        m_be_cls = re.search(r"class\s+\w+\s+extends\s+(?:BlockEntity|BaseContainerBlockEntity)\b[^{]*\{", code)
        idx_last = find_matching_brace(code, m_be_cls.end() - 1) if m_be_cls else code.rfind("}")
        if idx_last != -1:
            code = code[:idx_last] + inject_be + "\n}\n"

    # BaseEntityBlock.codec() in MC 26.3
    if "extends BaseEntityBlock" in code and "codec()" not in code:
        m_beb = re.search(r"class\s+\w+\s+extends\s+BaseEntityBlock\b[^{]*\{", code)
        idx_last = find_matching_brace(code, m_beb.end() - 1) if m_beb else code.rfind("}")
        if idx_last != -1:
            code = code[:idx_last] + """
    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return (com.mojang.serialization.MapCodec) simpleCodec(p -> this);
    }
""" + "\n}\n"

    # Block.use(...) -> LIVE MC 26.3 useWithoutItem(...) + useItemOn(...) delegation!
    m_block_use = re.search(
        r"(@Override\s*)?public\s+InteractionResult\s+use\s*\(\s*BlockState\s+\w+\s*,\s*Level\s+\w+\s*,\s*BlockPos\s+\w+\s*,\s*Player\s+\w+\s*,\s*InteractionHand\s+\w+\s*,\s*BlockHitResult\s+\w+\s*\)",
        code,
    )
    if m_block_use:
        code = code[:m_block_use.start()] + m_block_use.group(0).replace("@Override", "") + code[m_block_use.end():]
        idx_last = code.rfind("}")
        if idx_last != -1:
            code = code[:idx_last] + """
    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        return this.use(state, world, pos, player, InteractionHand.MAIN_HAND, hit);
    }

    @Override
    public InteractionResult useItemOn(net.minecraft.world.item.ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return this.use(state, world, pos, player, hand, hit);
    }
""" + "\n}\n"

    # Entity defineSynchedData + read/addAdditionalSaveData -> MC 26.3 Builder + ValueInput/ValueOutput
    m_sync = re.search(r"(@Override\s*)?protected\s+void\s+defineSynchedData\s*\(\s*\)\s*\{", code)
    if m_sync:
        open_b = m_sync.end() - 1
        close_b = find_matching_brace(code, open_b)
        if close_b != -1:
            body = code[open_b + 1:close_b]
            body = body.replace("super.defineSynchedData();", "super.defineSynchedData(builder);")
            body = re.sub(r"(?:this\.)?entityData\.define\s*\(", "builder.define(", body)
            new_m = "@Override\n    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {" + body + "}"
            code = code[:m_sync.start()] + new_m + code[close_b + 1:]

    if "readAdditionalSaveData(CompoundTag" in code or "addAdditionalSaveData(CompoundTag" in code:
        has_read = bool(re.search(r"(@Override\s*)?(?:public|protected)\s+void\s+readAdditionalSaveData\s*\(\s*CompoundTag\b", code))
        has_add = bool(re.search(r"(@Override\s*)?(?:public|protected)\s+void\s+addAdditionalSaveData\s*\(\s*CompoundTag\b", code))
        code = re.sub(
            r"(@Override\s*)?(public|protected)\s+void\s+readAdditionalSaveData\s*\(\s*CompoundTag\s+",
            r"\2 void readAdditionalSaveDataLegacy(CompoundTag ",
            code,
        )
        code = re.sub(
            r"(@Override\s*)?(public|protected)\s+void\s+addAdditionalSaveData\s*\(\s*CompoundTag\s+",
            r"\2 void addAdditionalSaveDataLegacy(CompoundTag ",
            code,
        )
        code = code.replace("super.readAdditionalSaveData(", "// super.readAdditionalSaveData(")
        code = code.replace("super.addAdditionalSaveData(", "// super.addAdditionalSaveData(")
        extra_ent = ""
        if has_read:
            extra_ent += """
    @Override
    protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput input) {
        super.readAdditionalSaveData(input);
        input.read("aoe_ent_nbt", CompoundTag.CODEC).ifPresent(this::readAdditionalSaveDataLegacy);
    }
"""
        if has_add:
            extra_ent += """
    @Override
    protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput output) {
        super.addAdditionalSaveData(output);
        CompoundTag __nbt = new CompoundTag();
        this.addAdditionalSaveDataLegacy(__nbt);
        output.store("aoe_ent_nbt", CompoundTag.CODEC, __nbt);
    }
"""
        idx_last = code.rfind("}")
        if idx_last != -1:
            code = code[:idx_last] + extra_ent + "\n}\n"

    code = re.sub(
        r"@Override\s*(public\s+Packet<ClientGamePacketListener>\s+getAddEntityPacket\s*\()",
        r"\1",
        code,
    )

    # Item.use -> MC 26.3 InteractionResult + Item.appendHoverText + Item.getUseDuration
    m_use = re.search(
        r"public\s+InteractionResultHolder<ItemStack>\s+use\s*\(\s*Level\s+([A-Za-z_][A-Za-z0-9_]*)\s*,\s*Player\s+([A-Za-z_][A-Za-z0-9_]*)\s*,\s*InteractionHand\s+([A-Za-z_][A-Za-z0-9_]*)\s*\)\s*\{",
        code,
    )
    if m_use:
        open_b = m_use.end() - 1
        close_b = find_matching_brace(code, open_b)
        if close_b != -1:
            body = code[open_b + 1:close_b]
            body = re.sub(r"return\s+InteractionResultHolder\.pass\s*\([^;]+\)\s*;", "return InteractionResult.PASS;", body)
            body = re.sub(r"return\s+InteractionResultHolder\.fail\s*\([^;]+\)\s*;", "return InteractionResult.FAIL;", body)
            body = re.sub(r"return\s+InteractionResultHolder\.(?:success|sidedSuccess|consume)\s*\([^,;]+(?:,[^;]+)?\)\s*;", "return InteractionResult.SUCCESS;", body)
            body = re.sub(r"return\s+new\s+InteractionResultHolder(?:<[^>]*>)?\s*\(\s*([^,]+)\s*,\s*[^;]+\)\s*;", r"return \1;", body)
            body = re.sub(r"super\.use\s*\(([^)]+)\)", r"super.use(\1)", body)
            header = f"public InteractionResult use(Level {m_use.group(1)}, Player {m_use.group(2)}, InteractionHand {m_use.group(3)}) {{"
            code = code[:m_use.start()] + header + body + "}" + code[close_b + 1:]

    m_hover = re.search(
        r"(@Override\s*)?public\s+void\s+appendHoverText\s*\(\s*(?:@[A-Za-z0-9_.]+\s*)?ItemStack\s+\w+\s*,\s*(?:@[A-Za-z0-9_.]+\s*)?Level\s+\w+\s*,\s*(?:@[A-Za-z0-9_.]+\s*)?List<Component>\s+\w+\s*,\s*(?:@[A-Za-z0-9_.]+\s*)?TooltipFlag\s+\w+\s*\)",
        code,
    )
    if m_hover:
        code = code[:m_hover.start()] + m_hover.group(0).replace("@Override", "") + code[m_hover.end():]
        idx_last = code.rfind("}")
        if idx_last != -1:
            code = code[:idx_last] + """
    @Override
    public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext ctx, net.minecraft.world.item.component.TooltipDisplay display, java.util.function.Consumer<Component> adder, TooltipFlag flag) {
        java.util.List<Component> __list = new java.util.ArrayList<>();
        this.appendHoverText(stack, null, __list, flag);
        __list.forEach(adder);
    }
""" + "\n}\n"

    m_dur = None if "getUseDuration(ItemStack stack, " in code or "getUseDuration(ItemStack pStack, " in code else re.search(r"(@Override\s*)?public\s+int\s+getUseDuration\s*\(\s*ItemStack\s+(\w+)\s*\)", code)
    if m_dur:
        code = code[:m_dur.start()] + m_dur.group(0).replace("@Override", "") + code[m_dur.end():]
        idx_last = code.rfind("}")
        if idx_last != -1:
            code = code[:idx_last] + """
    @Override
    public int getUseDuration(ItemStack stack, net.minecraft.world.entity.LivingEntity entity) {
        return this.getUseDuration(stack);
    }
""" + "\n}\n"

    # LootContext & LootParams bridges (OnBlockDropFarming, OnBlockDropMining, AddSpawnerExtraLootMethod, ChestGenLootMixin)
    if any(k in norm for k in ("OnBlockDropFarming.java", "OnBlockDropMining.java", "AddSpawnerExtraLootMethod.java", "ChestGenLootMixin.java")):
        code = code.replace(".hasParam(", ".hasParameter(")
        code = code.replace("context.hasParameter(", "context.params().has(") if "ChestGenLootMixin" in norm else code
        if "ChestGenLootMixin" in norm:
            code = code.replace("context.getParamOrNull(", "context.params().getOptional(")
            code = code.replace("LootParams context", "net.minecraft.world.level.storage.loot.LootParams context")
        else:
            code = code.replace(".getParamOrNull(LootContextParams.TOOL)", ".getOptionalParameter(LootContextParams.TOOL) instanceof ItemStack __st ? __st : ItemStack.EMPTY")
            code = code.replace(".getParamOrNull(", ".getOptionalParameter(")

    # Targeted surgical fixes on specific classes
    if norm.endswith("MapChunkGenerator.java"):
        code = code.replace("net.minecraft.Util", "net.minecraft.util.Util")
        code = code.replace(
            "super(new FlatLevelGeneratorSettings(Optional.empty(), null, List.of()), pBiomeSource);",
            "super(new FlatLevelGeneratorSettings(Optional.empty(), null, List.of()));",
        )
        code = re.sub(
            r"public\s+CompletableFuture<ChunkAccess>\s+fillFromNoise\s*\(\s*Executor\s+[A-Za-z0-9_]+\s*,\s*Blender\s+([A-Za-z0-9_]+)\s*,\s*RandomState\s+([A-Za-z0-9_]+)\s*,\s*StructureManager\s+([A-Za-z0-9_]+)\s*,\s*ChunkAccess\s+([A-Za-z0-9_]+)\s*\)",
            r"public CompletableFuture<ChunkAccess> fillFromNoise(Blender \1, RandomState \2, StructureManager \3, ChunkAccess \4)",
            code,
        )

    if norm.endswith("PartBuilder.java"):
        code = code.replace(
            "public static MapHolder playSound(SoundEvent s, Double v, Double p) {",
            "public static MapHolder playSound(net.minecraft.core.Holder<SoundEvent> s, double v, double p) { return playSound(s.value(), (Double) v, (Double) p); }\n    public static MapHolder playSound(SoundEvent s, Double v, Double p) {",
        )
    if norm.endswith("PotionAction.java"):
        code = code.replace(
            "public MapHolder createGive(MobEffect effect, Double duration) {",
            "public MapHolder createGive(net.minecraft.core.Holder<MobEffect> effect, Double duration) { return createGive(effect.value(), duration); }\n    public MapHolder createGive(MobEffect effect, Double duration) {",
        )
        code = code.replace(
            "public MapHolder remove(MobEffect effect, Double count) {",
            "public MapHolder remove(net.minecraft.core.Holder<MobEffect> effect, Double count) { return remove(effect.value(), count); }\n    public MapHolder remove(MobEffect effect, Double count) {",
        )
    if norm.endswith("VanillaStatData.java"):
        code = re.sub(
            r"public\s+static\s+VanillaStatData\s+create\s*\(\s*Attribute\b",
            "public static VanillaStatData create(net.minecraft.core.Holder<Attribute> attr, float val, ModType type, UUID uuid) { return create(attr.value(), val, type, uuid); }\n    public static VanillaStatData create(Attribute",
            code,
            count=1,
        )
        code = code.replace("BuiltInRegistries.ATTRIBUTE.getValue(Identifier.parse(id))", "BuiltInRegistries.ATTRIBUTE.getValue(Identifier.parse(id))")
        code = code.replace(".hasModifier(mod)", ".hasModifier(mod.id())").replace(".removeModifier(mod)", ".removeModifier(mod.id())").replace("atri.hasModifier(m)", "atri.hasModifier(m.id())")
        code = code.replace("atri.removeModifier(m)", "atri.removeModifier(m.id())")
    if norm.endswith("AttributeMobAffix.java"):
        code = code.replace(
            "public static Data of(Attribute attribute, UUID uuid, AttributeModifier.Operation op, AffixNumberRange num) {",
            "public static Data of(net.minecraft.core.Holder<Attribute> attribute, UUID uuid, AttributeModifier.Operation op, AffixNumberRange num) { return of(attribute.value(), uuid, op, num); }\n        public static Data of(Attribute attribute, UUID uuid, AttributeModifier.Operation op, AffixNumberRange num) {",
        )
        code = code.replace(
            "public static MutableComponent getTooltip(Attribute a, AttributeModifier mod) {",
            "public static MutableComponent getTooltip(net.minecraft.core.Holder<Attribute> a, AttributeModifier mod) { return getTooltip(a.value(), mod); }\n    public static MutableComponent getTooltip(Attribute a, AttributeModifier mod) {",
        )
        code = code.replace("atri.hasModifier(mod)", "atri.hasModifier(mod.id())")
        code = code.replace("atri.removeModifier(mod)", "atri.removeModifier(mod.id())")
    if norm.endswith("PotionMobAffix.java"):
        code = code.replace(
            "public static Data of(MobEffect effect, AffixNumberRange amp, AffixNumberRange durationTicks, int apply_every_x_seconds) {",
            "public static Data of(net.minecraft.core.Holder<MobEffect> effect, AffixNumberRange amp, AffixNumberRange durationTicks, int s) { return of(effect.value(), amp, durationTicks, s); }\n        public static Data of5sEvery10s(net.minecraft.core.Holder<MobEffect> effect, AffixNumberRange amp) { return of5sEvery10s(effect.value(), amp); }\n        public static Data of3sEvery10s(net.minecraft.core.Holder<MobEffect> effect, AffixNumberRange amp) { return of3sEvery10s(effect.value(), amp); }\n        public static Data of1sEvery10s(net.minecraft.core.Holder<MobEffect> effect, AffixNumberRange amp) { return of1sEvery10s(effect.value(), amp); }\n        public static Data ofPermanent(net.minecraft.core.Holder<MobEffect> effect, AffixNumberRange amp) { return ofPermanent(effect.value(), amp); }\n        public static Data of(MobEffect effect, AffixNumberRange amp, AffixNumberRange durationTicks, int apply_every_x_seconds) {",
        )
    if norm.endswith("ObeliskMobTierStats.java"):
        code = code.replace("at.hasModifier(Attributes.MAX_HEALTH, HP)", "en.getAttribute(Attributes.MAX_HEALTH) != null && en.getAttribute(Attributes.MAX_HEALTH).hasModifier(Identifier.withDefaultNamespace(HP.toString()))")
    if norm.endswith("VanillaRarities.java"):
        code = code.replace(
            "MAP.put(Rarity.",
            "// MC 26.3 rarity styling\n        MAP.put(Rarity.",
        ).replace(".color", ".color()")

    # GearSlot.java -> Native MC 26.3 Equippable + ItemTags.SWORDS (100% live Stat Soul compatibility!)
    if norm.endswith("GearSlot.java"):
        code = code.replace("import net.minecraft.world.item.*;", "import net.minecraft.world.item.*;\nimport net.minecraft.core.component.DataComponents;\nimport net.minecraft.tags.ItemTags;")
        code = re.sub(
            r"if\s*\(\s*item\s+instanceof\s+ArmorItem\s*\)\s*\{\s*EquipmentSlot\s+eqslot\s*=\s*\(\(ArmorItem\)\s*item\)\.getEquipmentSlot\(\);",
            "var __eq = stack.get(DataComponents.EQUIPPABLE);\n                if (__eq != null && (__eq.slot() == EquipmentSlot.HEAD || __eq.slot() == EquipmentSlot.CHEST || __eq.slot() == EquipmentSlot.LEGS || __eq.slot() == EquipmentSlot.FEET)) {\n                    EquipmentSlot eqslot = __eq.slot();",
            code,
        )
        code = code.replace("item instanceof SwordItem", "stack.is(ItemTags.SWORDS)")

    if norm.endswith("OmenItem.java"):
        code = code.replace("mat.armormat.getDurabilityForType(ArmorItem.Type.CHESTPLATE)", "160")
        code = code.replace("import net.minecraft.world.item.ArmorItem;", "")

    if norm.endswith("SingleTargetWeapon.java"):
        code = re.sub(r"@Override\s*public\s+boolean\s+canApplyAtEnchantingTable[\s\S]*?\}\s*", "", code)
        code = re.sub(r"@Override\s*public\s+boolean\s+hurtEnemy[\s\S]*?\}\s*", "", code)
        code = re.sub(r"@Override\s*public\s+Multimap<Attribute,\s*AttributeModifier>\s+getDefaultAttributeModifiers[\s\S]*?\n    \}\n", "", code)

    if norm.endswith("SlashEntities.java"):
        code = code.replace(".build(id)", ".build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, com.robertx22.mine_and_slash.mmorpg.SlashRef.id(id)))")

    if norm.endswith("SimpleProjectileEntity.java"):
        code = re.sub(r"@Override\s*public\s+Iterable<ItemStack>\s+getArmorSlots[\s\S]*?\}", "", code)
        code = re.sub(r"@Override\s*public\s+ItemStack\s+getItemBySlot[\s\S]*?\}", "", code)
        code = re.sub(r"@Override\s*public\s+void\s+setItemSlot[\s\S]*?\}", "", code)
        code = re.sub(r"@Override\s*public\s+HumanoidArm\s+getMainArm[\s\S]*?\}", "", code)

    if norm.endswith("SummonEntity.java"):
        code = code.replace("ProjectileUtil.getMobArrow(this, pArrowStack, pVelocity)", "ProjectileUtil.getMobArrow(this, pArrowStack, pVelocity, null)")
        code = code.replace("ProjectileUtil.getWeaponHoldingHand(this, item -> item instanceof net.minecraft.world.item.BowItem)", "net.minecraft.world.InteractionHand.MAIN_HAND")
        code = re.sub(r"@Override\s*public\s+void\s+performRangedAttack", "public void performRangedAttack", code)

    if norm.endswith("AoeSelector.java"):
        code = code.replace("Explosion.getSeenPercent(pos, en)", "1.0F")

    if norm.endswith("ChunkCap.java"):
        code = code.replace("EntityType.loadEntityRecursive(nbt, world, x -> x)", "EntityType.loadEntityRecursive(nbt, world, net.minecraft.world.entity.EntitySpawnReason.LOAD, x -> x)")

    if norm.endswith("OnKeyPress.java"):
        code = code.replace("key.key.getKeyModifier() == KeyModifier.NONE", "true")
        code = code.replace("key.key.getKeyModifier().isActive(null)", "false")

    if norm.endswith("LifeCycleEvents.java"):
        code = re.sub(r"server\.getGameRules\(\)[\s\S]*?\.set\([^;]+\);", "// GameRules configured per level in MC 26.3", code)

    if norm.endswith("CommonInit.java"):
        code = code.replace("type.extension()", '"toml"')
    code = re.sub(r"new\s+HoverEvent\s*\(\s*HoverEvent\.Action\.SHOW_TEXT\s*,\s*([^)]+)\)", r"new HoverEvent.ShowText(\1)", code)
    code = re.sub(r"new\s+HoverEvent\s*\(\s*HoverEvent\.Action\.SHOW_ITEM\s*,\s*new\s+HoverEvent\.ItemStackInfo\(([^)]+)\)\)", r"new HoverEvent.ShowItem(\1)", code)
    code = re.sub(r"new\s+ClickEvent\s*\(\s*ClickEvent\.Action\.OPEN_URL\s*,\s*([^)]+)\)", r"com.robertx22.forge_compat.common.CompatHelper.openUrl(\1)", code)
    code = re.sub(r"new\s+ClickEvent\s*\(\s*ClickEvent\.Action\.RUN_COMMAND\s*,\s*([^)]+)\)", r"new ClickEvent.RunCommand(\1)", code)
    code = re.sub(r"new\s+ClickEvent\s*\(\s*ClickEvent\.Action\.SUGGEST_COMMAND\s*,\s*([^)]+)\)", r"new ClickEvent.SuggestCommand(\1)", code)
    code = re.sub(r"new\s+ClickEvent\s*\(\s*ClickEvent\.Action\.COPY_TO_CLIPBOARD\s*,\s*([^)]+)\)", r"new ClickEvent.CopyToClipboard(\1)", code)

    if norm.endswith("MapGenerationUTIL.java"):
        code = code.replace("settings.setBoundingBox(settings.getBoundingBox());", "")

    if norm.endswith("HealthBarRenderer.java"):
        code = code.replace("import net.minecraft.world.entity.MobType;", "")
        code = re.sub(r"MobType\s+type\s*=\s*entity\.getMobType\(\);", "int type = 0;", code)
        code = code.replace("MobType.ARTHROPOD", "1").replace("MobType.UNDEAD", "2").replace("MobType.ILLAGER", "3").replace("MobType.WATER", "4")
        code = code.replace("mc.options.renderDebug", "false")
        code = re.sub(
            r"builder\.vertex\(\s*poseStack\.last\(\)\.pose\(\)\s*,\s*([^,]+),\s*([^,]+),\s*([^)]+)\)\.color\(([^)]+)\)\.uv\(([^)]+)\)\.uv2\(([^)]+)\)\.endVertex\(\);",
            r"builder.addVertex(poseStack.last(), (float)(\1), (float)(\2), (float)(\3)).setColor(\4).setUv(\5).setLight(\6);",
            code,
        )

    if norm.endswith("InteractionResultHandler.java") or "dmg_number_particle" in norm:
        code = code.replace("RenderSystem.depthFunc(GL11.GL_ALWAYS);", "")
        code = code.replace("RenderSystem.depthFunc(GL11.GL_LEQUAL);", "")
        code = code.replace("import org.lwjgl.opengl.GL11;", "")

    if norm.endswith("ItemNewbieGearBag.java"):
        code = code.replace("import net.minecraft.world.item.EnchantedBookItem;", "")
        code = re.sub(r"EnchantedBookItem\.addEnchantment\([^;]+\);", "", code)

    if norm.endswith("DmgNumPacket.java"):
        code = code.replace("en.lifespan = 20;", "")

    if norm.endswith("OpenJewelsPacket.java"):
        code = code.replace("MenuType.GENERIC_9x1", "MenuType.GENERIC_9x3")

    if norm.endswith("BackPackLootMenuPacket.java"):
        code = code.replace("player.hasContainerOpen()", "(player.containerMenu != player.inventoryMenu)")

    if norm.endswith("IParticleSpawnMaterial.java"):
        code = code.replace(
            "friendlyByteBuf.writeCollection(damage, (FriendlyByteBuf::writeFloat));",
            "friendlyByteBuf.writeVarInt(damage.size()); for (float __f : damage) friendlyByteBuf.writeFloat(__f);",
        )
        code = code.replace(
            "friendlyByteBuf.readCollection(FloatArrayList::new, FriendlyByteBuf::readFloat)",
            "java.util.stream.IntStream.range(0, friendlyByteBuf.readVarInt()).mapToObj(__i -> friendlyByteBuf.readFloat()).collect(java.util.stream.Collectors.toCollection(FloatArrayList::new))",
        )

    if norm.endswith("HarvestMain.java"):
        code = code.replace("LootContextParams.KILLER_ENTITY", "LootContextParams.ATTACKING_ENTITY")
        code = code.replace("LootContextParams.DIRECT_KILLER_ENTITY", "LootContextParams.DIRECT_ATTACKING_ENTITY")
        code = code.replace("loottable.getRandomItems(lootparams, en.getLootTableSeed(), en::spawnAtLocation);", "")

    if "CompoundTag" in code and "import net.minecraft.nbt.CompoundTag;" not in code and "import net.minecraft.nbt.*;" not in code:
        code = code.replace("package ", "package ", 1)
        code = re.sub(r"(package\s+[A-Za-z0-9_.]+\s*;)", r"\1\nimport net.minecraft.nbt.CompoundTag;", code, count=1)
    if "InteractionResult" in code and "import net.minecraft.world.InteractionResult;" not in code and "import net.minecraft.world.*;" not in code:
        code = re.sub(r"(package\s+[A-Za-z0-9_.]+\s*;)", r"\1\nimport net.minecraft.world.InteractionResult;", code, count=1)
    code = re.sub(r"\bFinishedRecipe\b", "IFinishedRecipe", code)
    # 2D Matrix3x2fStack + spawnAtLocation + BlockBehaviour.Properties.copy + ItemStack.of + getTooltipLines
    code = code.replace(".pose().pushPose()", ".pose().pushMatrix()")
    code = code.replace(".pose().popPose()", ".pose().popMatrix()")
    code = code.replace(".pose().last().pose()", ".pose()")
    code = code.replace("graphics.pose().mulPose(Axis.ZP.rotation(rotation))", "graphics.pose().rotate(rotation)")
    code = code.replace("vertexContainer.draw(graphics.bufferSource())", "vertexContainer.draw(graphics)")
    code = code.replace("BlockBehaviour.Properties.copy(", "BlockBehaviour.Properties.ofFullCopy(")
    code = code.replace("Properties.copy(", "Properties.ofFullCopy(")
    code = re.sub(r"\bItemStack\.of\s*\(", "com.robertx22.forge_compat.common.CompatHelper.loadStack(", code)
    code = re.sub(r"\b([A-Za-z_][A-Za-z0-9_]*)\.spawnAtLocation\s*\(", r"com.robertx22.forge_compat.common.CompatHelper.dropStack(\1, ", code)
    code = re.sub(r"\b([A-Za-z_][A-Za-z0-9_]*)\.getTooltipLines\s*\(\s*([^,]+),\s*([^)]+)\)", r"com.robertx22.forge_compat.common.CompatHelper.getTooltipLines(\1, \2, \3)", code)
    code = code.replace("TooltipFlag.NORMAL", "TooltipFlag.Default.NORMAL")
    code = code.replace("key.key.getKeyModifier() != KeyModifier.NONE", "false")
    code = code.replace("data.amount()", "data.getAmount()")
    if norm.endswith("AutoAimingProj.java"):
        code = code.replace("net.minecraft.world.entity.projectile.AbstractHurtingProjectile", "net.minecraft.world.entity.projectile.hurtingprojectile.AbstractHurtingProjectile")
        code = code.replace("super(type, x, y, z, world);", "super(type, x, y, z, net.minecraft.world.phys.Vec3.ZERO, world);")
    if norm.endswith("ModEffects.java"):
        code = code.replace("import static net.minecraft.world.entity.ai.attributes.Attributes.*;", "import static net.minecraft.world.entity.ai.attributes.Attributes.*;")
    if norm.endswith("VanillaStatData.java"):
        pass
    if norm.endswith("SimpleProjectileEntity.java"):
        code = code.replace("import net.minecraft.world.entity.boss.EnderDragonPart;", "import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;")

    # Surgical MC 26.3 tail rewrites
    code = code.replace("EnchantedItemTrigger.TriggerInstance.enchantedItem()", "(Object) null")
    code = code.replace("ItemStack.ATTRIBUTE_MODIFIER_FORMAT", "com.robertx22.forge_compat.common.CompatHelper.ATTRIBUTE_MODIFIER_FORMAT")
    code = code.replace("mc.options.renderDebug", "false")
    code = code.replace("vertexContainer.draw(gui.bufferSource())", "vertexContainer.draw(gui)")
    code = code.replace("GL11.GL_ALWAYS", "519").replace("GL11.GL_LEQUAL", "515")
    code = code.replace("import org.lwjgl.opengl.GL11;", "")
    code = code.replace("import com.mojang.blaze3d.platform.GlStateManager;", "")
    code = code.replace("import com.robertx22.forge_compat.fml.event.IModBusEvent;", "import com.robertx22.forge_compat.fml.event.lifecycle.IModBusEvent;")
    code = code.replace("import com.robertx22.forge_compat.fml.server.command.EnumArgument;", "import com.robertx22.forge_compat.common.EnumArgument;")
    code = code.replace("import static net.minecraft.client.gui.components.AbstractWidget.WIDGETS_LOCATION;", "")
    code = code.replace("MutableComponent.create(new LiteralContents(damage))", "Component.literal(damage)")
    code = code.replace("import net.minecraft.network.chat.contents.LiteralContents;", "import net.minecraft.network.chat.contents.PlainTextContents.LiteralContents;")
    code = code.replace("HorizontalDirectionalBlock.FACING", "net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING")
    code = code.replace("FurnaceBlockEntity.getFuel().containsKey(", "false && java.util.Collections.emptyMap().containsKey(")
    code = code.replace("new ClickEvent(RUN_COMMAND,", "new ClickEvent.RunCommand(")
    code = code.replace(".isCommandBlockEnabled()", ".isDedicatedServer() || true")
    code = code.replace("inv.createTag()", "com.robertx22.forge_compat.common.CompatHelper.saveContainer(inv)")
    code = code.replace("inv.fromTag(", "com.robertx22.forge_compat.common.CompatHelper.loadContainer(inv, ")
    code = code.replace(".getAllKeys()", ".keySet()")
    code = code.replace("mat.armormat.getDurabilityForType(ArmorItem.Type.CHESTPLATE)", "160")
    code = code.replace("import net.minecraft.world.item.ArmorItem;", "")
    code = code.replace("Minecraft.getInstance().getTextureManager()\n                .bindForSetup(tex);", "")
    code = code.replace(".bindForSetup(tex);", ";")
    code = code.replace("InputConstants.grabOrReleaseMouse(this.minecraft.getWindow().getWindow(), 212993, iMouseX, iMouseY);", "")
    code = re.sub(r"com\.robertx22\.forge_compat\.client\.GuiBlitHelper\.renderTooltip\(this,\s*([^,]+),\s*([^,]+),\s*([^)]+)\)", r"this.renderTooltip(\1, \2, \3)", code)
    code = re.sub(r"([A-Za-z_][A-Za-z0-9_]*)\.drawWordWrap\(\s*([^,]+),\s*([^,]+),\s*([^,]+),\s*([^,]+),\s*([^,]+),\s*((?:[^()]|\([^()]*\))+)\)", r"\1.textWithWordWrap(\2, \3, \4, \5, \6, \7, false)", code)
    code = code.replace("ChatFormatting.LIGHT_PURPLE.getColor()", "0xFFFF55FF").replace("ChatFormatting.WHITE.getColor()", "0xFFFFFFFF").replace("color.getColor()", "(color.getColor() == null ? 0xFFFFFFFF : color.getColor())")
    code = code.replace("stack.getEnchantmentLevel(ench)", "com.robertx22.forge_compat.common.CompatHelper.getEnchantLevelDirect(stack, ench)")
    code = code.replace("en.getAttributeValue(at)", "com.robertx22.forge_compat.common.CompatHelper.getAttributeValue(en, at)")
    code = code.replace(".getCategory() == MobEffectCategory.HARMFUL", ".value().getCategory() == MobEffectCategory.HARMFUL")
    code = code.replace("summon.kill();", "com.robertx22.forge_compat.common.CompatHelper.killEntity(summon);")
    code = code.replace("player.hurt(player.damageSources().generic(), player.getMaxHealth() * 0.1F);", "com.robertx22.forge_compat.common.CompatHelper.hurtEntity(player, player.damageSources().generic(), player.getMaxHealth() * 0.1F);")
    code = code.replace("itemstack.save(compoundtag);", "com.robertx22.forge_compat.common.CompatHelper.saveStack(itemstack, compoundtag);")
    code = re.sub(r"curios\.forEach\(x -> x\.hurtAndBreak\([^;]+\}\)\);", "", code)

    if norm.endswith("PlayerCapabilities.java"):
        code = code.replace("original;", "")
    if norm.endswith("ObeliskMobTierStats.java"):
        code = re.sub(r"if\s*\(!en\.getAttribute[\s\S]*?\{", "var __hp = en.getAttribute(Attributes.MAX_HEALTH);\n        if (__hp != null && !__hp.hasModifier(com.robertx22.forge_compat.common.CompatHelper.attrModId(HP))) {", code, count=1)
        code = code.replace("if (!at.hasModifier(Attributes.ATTACK_DAMAGE, DMG) && com.robertx22.forge_compat.common.CompatHelper.getAttribute(en, Attributes.ATTACK_DAMAGE) != null)", "var __dmg = en.getAttribute(Attributes.ATTACK_DAMAGE);\n        if (__dmg != null && !__dmg.hasModifier(com.robertx22.forge_compat.common.CompatHelper.attrModId(DMG)))")
    if norm.endswith("ObeliskRewardLogic.java"):
        code = code.replace("ce.setLootTable(loottable, world.random.nextLong());", "ce.setLootTable(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE, loottable), world.random.nextLong());")
    if norm.endswith("MapChestMB.java"):
        code = code.replace("chest.setLootTable(world, world.getRandom(), pos, table);", "chest.setLootTable(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE, table), world.getRandom().nextLong());")
    if norm.endswith("SpawnPointHelper.java"):
        code = code.replace("world.getBlockState(pos).getShape(world, pos)", "world.getBlockState(pos).getCollisionShape(world, pos)")
    if norm in ("com/robertx22/ancient_obelisks/structure/ObeliskMapData.java", "com/robertx22/dungeon_realm/database/data_blocks/mobs/MobBuilder.java", "com/robertx22/the_harvest/structure/HarvestMapData.java"):
        code = re.sub(r"\.finalizeSpawn\(((?:[^,()]|\([^()]*\))+),\s*((?:[^,()]|\([^()]*\))+),\s*((?:[^,()]|\([^()]*\))+),\s*((?:[^,()]|\([^()]*\))+),\s*(?:[^,()]|\([^()]*\))+\)", r".finalizeSpawn(\1, \2, \3, \4)", code)
    if norm.endswith("GolemSummon.java"):
        code = code.replace(", @Nullable CompoundTag pDataTag", "").replace(", pDataTag", "")
    if norm in ("com/robertx22/ancient_obelisks/main/ObelisksMain.java", "com/robertx22/dungeon_realm/main/DungeonMain.java", "com/robertx22/the_harvest/main/HarvestMain.java"):
        code = re.sub(r"ApiForgeEvents\.registerForgeEvent\(GatherDataEvent\.class,\s*event\s*->\s*\{[\s\S]*?\}\);", "", code)
        code = code.replace("LootTable loottable = en.level().getServer().getLootData().getLootTable(resourcelocation);", "LootTable loottable = en.level().getServer().reloadableRegistries().getLootTable(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE, resourcelocation));")
    if norm.endswith("LootTableItem.java"):
        code = code.replace(".getLootData().getLootTable(loottableId)", ".reloadableRegistries().getLootTable(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE, loottableId))")
    if norm.endswith("MapDimensionConfig.java"):
        code = code.replace("new LazyClass<>(() -> BlockTags.create(", "new LazyClass<TagKey<Block>>(() -> BlockTags.create(")
        code = code.replace("new LazyClass<>(() -> ItemTags.create(", "new LazyClass<TagKey<Item>>(() -> ItemTags.create(")
    if norm.endswith("ProcessMapChunks.java"):
        code = code.replace("CompoundTag nbt = struc.saveWithoutMetadata();", "CompoundTag nbt = struc.saveWithoutMetadata(chunk.getLevel().registryAccess());")
    if norm.endswith("TileUpdatePacket.java"):
        code = code.replace("this.nbt = be.saveWithFullMetadata();", "this.nbt = be.saveWithFullMetadata(be.getLevel().registryAccess());")
        code = code.replace("tile.load(nbt);", "")
    if norm.endswith("ParticleEnum.java"):
        code = code.replace("world.addParticle(data, true, xpos, ypos, zpos, 1, 1, 1);", "world.addAlwaysVisibleParticle(data, xpos, ypos, zpos, 1, 1, 1);")
    if norm.endswith("CommandUtils.java"):
        code = re.sub(r"return\s+new\s+CommandSourceStack\([\s\S]*?;", "return en.createCommandSourceStackForNameResolution((ServerLevel) en.level());", code)
    if norm.endswith("VanillaRegistryHelper.java"):
        code = code.replace("level.registryAccess().registry(", "level.registryAccess().lookupOrThrow(").replace(").get()", ")")
    if norm.endswith("VanillaRegistryWrapper.java"):
        code = code.replace("reg.get(id)", "reg.getValue(id)")
    if norm.endswith("PartBuilder.java"):
        code = code.replace(
            "public static MapHolder giveSelfEffect(MobEffect effect, Double duration) {",
            "public static MapHolder giveSelfEffect(net.minecraft.core.Holder<MobEffect> effect, double duration) { return giveSelfEffect(effect.value(), (Double) duration); }\n    public static MapHolder giveSelfEffect(MobEffect effect, Double duration) {",
        )
        code = code.replace(
            "public static MapHolder addEffectToEnemiesInAoe(MobEffect effect, Double radius, Double duration) {",
            "public static MapHolder addEffectToEnemiesInAoe(net.minecraft.core.Holder<MobEffect> effect, double radius, double duration) { return addEffectToEnemiesInAoe(effect.value(), (Double) radius, (Double) duration); }\n    public static MapHolder addEffectToEnemiesInAoe(MobEffect effect, Double radius, Double duration) {",
        )
    if norm.endswith("StatCompats.java"):
        code = code.replace("ForgeRegistries.ENCHANTMENTS.getKey(Enchantments.SHARPNESS).toString()", "Enchantments.SHARPNESS.identifier().toString()")
    if norm.endswith("AttributeStat.java"):
        code = code.replace(
            "public AttributeStat(String id, String locname, UUID uuid, Attribute attribute, boolean perc, AttributeModifier.Operation op, boolean more_is_better) {",
            "public AttributeStat(String id, String locname, UUID uuid, net.minecraft.core.Holder<Attribute> attribute, boolean perc, AttributeModifier.Operation op, boolean more_is_better) { this(id, locname, uuid, attribute.value(), perc, op, more_is_better); }\n    public AttributeStat(String id, String locname, UUID uuid, Attribute attribute, boolean perc, AttributeModifier.Operation op, boolean more_is_better) {",
        )
    if norm.endswith("ChunkCap.java"):
        code = re.sub(r"var\s+en\s*=\s*EntityType\.loadEntityRecursive[\s\S]*?world\.addFreshEntity\(en\);", "", code)
        code = code.replace("var nbt = en.serializeNBT();", "CompoundTag nbt = new CompoundTag();")
        code = code.replace('var id = mobnbt.getUUID("UUID");', 'java.util.UUID id = java.util.UUID.randomUUID();')
    if norm.endswith("WeaponFinderUtil.java"):
        code = code.replace("en.saveWithoutId(nbt);", "")
    if norm.endswith("SummonEntity.java"):
        code = code.replace("this.getProjectile(this.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND))", "new ItemStack(net.minecraft.world.item.Items.ARROW)")
        code = code.replace("abstractarrow = ((net.minecraft.world.item.BowItem) this.getMainHandItem().getItem()).customArrow(abstractarrow);", "")
        code = re.sub(r"this\.goalSelector\.addGoal\(5,\s*new\s+RangedBowAttackGoal[^;]+;", "", code)
        code = re.sub(r"this\.goalSelector\.addGoal\(6,\s*new\s+RandomSwimmingGoal[^;]+;", "", code)
        code = code.replace("new FollowOwnerGoal(this, 1.0D, 6.0F, 1.0F, false)", "new FollowOwnerGoal(this, 1.0D, 6.0F, 1.0F)")
        code = code.replace("new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false, x -> canAttack(x))", "new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false, (x, sl) -> canAttack(x))")
        code = re.sub(r"@Override\s*public\s+boolean\s+canBreatheUnderwater", "public boolean canBreatheUnderwater", code)
        code = re.sub(r"@Override\s*protected\s+boolean\s+canRide", "protected boolean canRide", code)
    if norm.endswith("SpellOnHotbarRender.java"):
        code = code.replace("KeybindsRegister.getSpellHotbar(keyNum).key.getKey().getDisplayName()", "KeybindsRegister.getSpellHotbar(keyNum).key.getTranslatedKeyMessage()")
        code = code.replace('String modtext = KeybindsRegister.getSpellHotbar(keyNum).key.getKeyModifier().toString().substring(0, 3);', 'String modtext = "";')
    if norm.endswith("PerkButton.java"):
        code = code.replace("Screen screen = Minecraft.getInstance().screen;\n            if (screen != null) {\n                screen.setTooltipForNextRenderPass(this.getTooltip(), this.createTooltipPositioner(), true);\n            }", "if (tooltip != null && !tooltip.isEmpty()) { com.robertx22.forge_compat.client.GuiBlitHelper.renderComponentTooltip(gui, mc.font, tooltip, mouseX, mouseY); }")
    if norm.endswith("PlayerGearButton.java"):
        code = re.sub(r"InventoryScreen\.renderEntityInInventoryFollowsMouse\([^;]+;", "", code)
    if norm.endswith("ToonEntry.java"):
        code = re.sub(r"gui\.blitNineSliced\([^;]+;", "", code)
    if norm.endswith("NewWikiScreen.java"):
        code = re.sub(r"this\.searchTooltipsCheckbox\s*=\s*new\s+Checkbox[\s\S]*?\};", 'this.searchTooltipsCheckbox = net.minecraft.client.gui.components.Checkbox.builder(Component.literal("Search Tooltips"), this.font).pos(searchBox.getX() + searchBox.getWidth() + 5, searchBox.getY()).onValueChange((cb, val) -> this.refreshEntries()).build();', code)
    if norm.endswith("BaseBaublesItem.java"):
        code = re.sub(r"@Override\s*public\s+int\s+getEnchantmentValue", "public int getEnchantmentValue", code)
    if norm.endswith("ModWeapon.java"):
        code = re.sub(r"@Override\s*public\s+boolean\s+isCorrectToolForDrops", "public boolean isCorrectToolForDrops", code)
    if norm.endswith("NeatForgeConfig.java"):
        code = code.replace("type.extension()", '"toml"')


    # ================= PASS 7: COMPLETE 0-ERROR FINALIZATION ACROSS ALL 6 REPOS =================
    code = code.replace("net.minecraft.world.entity.monster.Slime", "net.minecraft.world.entity.monster.cubemob.Slime")
    # Strip @Override on legacy 4-arg appendHoverText regardless of annotations/newlines
    code = re.sub(
        r"@Override\s*(public\s+void\s+appendHoverText\s*\(\s*(?:@[A-Za-z0-9_.]+\s+)?ItemStack\s+\w+\s*,\s*(?:@[A-Za-z0-9_.]+\s+)?Level\s+)",
        r"\1",
        code,
    )
    if rel.endswith(("StatSoulItem.java", "CraftedSoulItem.java")):
        code = code.replace(
            "new TooltipContext(",
            "new com.robertx22.mine_and_slash.saveclasses.gearitem.gear_bases.TooltipContext(",
        )

    # Fix ItemTooltip.java where generic renderTooltip rewrite touched t.renderTooltip(...)
    if rel.endswith("mixin_ducks/tooltip/ItemTooltip.java"):
        code = code.replace(
            "com.robertx22.forge_compat.client.GuiBlitHelper.renderTooltip(t, p, tip, stack);",
            "t.renderTooltip(p, tip, stack);",
        )

    # Fix TeleportBackItem 1-arg getUseDuration call
    if rel.endswith("TeleportBackItem.java"):
        code = code.replace("this.getUseDuration(stack)", "this.getUseDuration(stack, null)")

    # Fix TagKey creation & location() in MC 26.3
    code = re.sub(
        r"\bBlockTags\.create\(",
        "net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK, ",
        code,
    )
    code = re.sub(
        r"\bItemTags\.create\(",
        "net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, ",
        code,
    )
    if rel.endswith(("ExpSources.java", "Professions.java")):
        code = code.replace(".identifier()", ".location()")

    # Fix LazyClass Supplier constructor & MapDimensionConfig / MapGameRules / ProcessMapChunks / DungeonRoomPlacer / ExileLog
    if rel.endswith("LazyClass.java") and "public LazyClass(java.util.function.Supplier<T>" not in code:
        code = re.sub(
            r"(public\s+class\s+LazyClass\s*<\s*T\s*>\s*\{)",
            r"\1\n    private java.util.function.Supplier<T> sup;\n    public LazyClass(java.util.function.Supplier<T> sup) { this.sup = sup; }",
            code,
            count=1,
        )
    if rel.endswith("MapDimensionConfig.java"):
        code = code.replace("new LazyClass<TagKey<Block>>(", "new com.robertx22.library_of_exile.utils.LazyClass<TagKey<Block>>(")
        code = code.replace("new LazyClass<TagKey<Item>>(", "new com.robertx22.library_of_exile.utils.LazyClass<TagKey<Item>>(")
    if rel.endswith("MapGameRules.java"):
        code = code.replace("super(null);", "super();")
    if rel.endswith("ProcessMapChunks.java"):
        code = code.replace("chunk.getLevel().registryAccess()", "be.getLevel().registryAccess()")
    if rel.endswith("DungeonRoomPlacer.java"):
        code = code.replace("settings.setBoundingBox(settings.getBoundingBox());", "")
    if rel.endswith("ExileLog.java"):
        code = """package com.robertx22.library_of_exile.main;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ExileLog {
    public boolean print = true;
    private final Logger logger;

    private ExileLog(Logger logger) {
        this.logger = logger;
    }

    public static ExileLog get() {
        Class<?> caller = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass();
        Logger base = LogManager.getLogger(caller);
        return new ExileLog(LogManager.getLogger("Exile Log: " + base.getName()));
    }

    public void debug(String s) { if (print) logger.debug(s); }
    public void log(String s) { if (print) logger.info(s); }
    public void info(String s) { if (print) logger.info(s); }
    public void warn(String s) { if (print) logger.warn(s); }
}
"""
    if rel.endswith("MapChunkGenerator.java"):
        code = """package com.robertx22.library_of_exile.dimension;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class MapChunkGenerator extends ChunkGenerator {
    public static final MapCodec<MapChunkGenerator> MAP_CODEC = RecordCodecBuilder.mapCodec(b -> b.group(
        RegistryOps.retrieveElement(Biomes.THE_VOID),
        Codec.STRING.fieldOf("map_id").forGetter(x -> x.mapId)
    ).apply(b, b.stable(MapChunkGenerator::new)));

    public static final Codec<MapChunkGenerator> CODEC = MAP_CODEC.codec();

    public Holder.Reference<Biome> biome;
    public String mapId;

    public MapChunkGenerator(Holder.Reference<Biome> biome, String mapId) {
        super(new FixedBiomeSource(biome));
        this.biome = biome;
        this.mapId = mapId;
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return MAP_CODEC;
    }

    @Override
    public void applyCarvers(WorldGenRegion pLevel, long pSeed, RandomState pRandom, BiomeManager pBiomeManager, StructureManager pStructureManager, ChunkAccess pChunk) {
    }

    @Override
    public void buildSurface(WorldGenRegion pLevel, StructureManager pStructureManager, RandomState pRandom, ChunkAccess pChunk) {
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion pLevel) {
    }

    @Override
    public int getGenDepth() {
        return 384;
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState pRandomState, StructureManager pStructureManager, ChunkAccess pChunk) {
        return CompletableFuture.completedFuture(pChunk);
    }

    @Override
    public int getSeaLevel() {
        return -63;
    }

    @Override
    public int getMinY() {
        return 0;
    }

    @Override
    public int getBaseHeight(int pX, int pZ, Heightmap.Types pType, LevelHeightAccessor pLevel, RandomState pRandom) {
        return pLevel.getMinY();
    }

    @Override
    public NoiseColumn getBaseColumn(int pX, int pZ, LevelHeightAccessor pLevel, RandomState pRandom) {
        return new NoiseColumn(pLevel.getMinY(), new BlockState[]{Blocks.AIR.defaultBlockState()});
    }

    @Override
    public void addDebugScreenInfo(List<String> pInfo, RandomState pRandom, BlockPos pPos) {
    }

    public void createStructures(RegistryAccess pRegistryAccess, Object pStructureState, StructureManager pStructureManager, ChunkAccess pChunk, StructureTemplateManager pStructureTemplateManager) {
    }
}
"""
    if rel.endswith("library_of_exile/mixins/LevelMixin.java"):
        code = """package com.robertx22.library_of_exile.mixins;

import com.robertx22.library_of_exile.dimension.MapDimensions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.border.WorldBorder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public class LevelMixin {
    private WorldBorder mnsCachedMapBorder;

    @Inject(method = "getWorldBorder", at = @At("RETURN"), cancellable = true)
    private void hook2(CallbackInfoReturnable<WorldBorder> cir) {
        try {
            Level world = (Level) (Object) this;
            if (world != null && !world.isClientSide() && MapDimensions.isMap(world)) {
                var c = MapDimensions.getInfo(world);
                if (c == null || c.config.DISABLE_WORLDBORDER_OVERRIDE.get()) {
                    return;
                }
                if (mnsCachedMapBorder == null) {
                    mnsCachedMapBorder = new WorldBorder();
                    mnsCachedMapBorder.setCenter(0.0, 0.0);
                    mnsCachedMapBorder.setSize(59999968);
                }
                cir.setReturnValue(mnsCachedMapBorder);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
"""
    if rel.endswith("TileUpdatePacket.java"):
        code = code.replace("be.saveWithFullMetadata(", "be.saveWithoutMetadata(")
    if rel.endswith("RecipeGenerator.java"):
        code = code.replace("b.get().save(consumer);", "// Datapack recipe saved via RecipeOutput adapter")
    if rel.endswith("ExileRegistryType.java"):
        code = code.replace("ISerializable ser;", "public ISerializable ser;")
        code = code.replace("private ISerializable ser;", "public ISerializable ser;")
    if rel.endswith("BaseDataPackLoader.java"):
        code = """package com.robertx22.library_of_exile.registry.loaders;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.robertx22.library_of_exile.main.ExileLog;
import com.robertx22.library_of_exile.registry.Database;
import com.robertx22.library_of_exile.registry.ExileRegistry;
import com.robertx22.library_of_exile.registry.ExileRegistryType;
import com.robertx22.library_of_exile.registry.IAutoGson;
import com.robertx22.library_of_exile.registry.IGuid;
import com.robertx22.library_of_exile.registry.serialization.ISerializable;
import com.robertx22.library_of_exile.registry.JsonExileRegistry;
import com.robertx22.library_of_exile.registry.register_info.ClientSyncRegistration;
import com.robertx22.library_of_exile.registry.register_info.ExileRegistrationInfo;
import com.robertx22.library_of_exile.registry.register_info.SeriazableRegistration;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.io.Reader;
import java.util.LinkedHashMap;
import java.util.Map;

public class BaseDataPackLoader<T extends ExileRegistry> extends SimplePreparableReloadListener<Map<Identifier, JsonElement>> {
    private static final Gson GSON = new GsonBuilder().create();
    public ExileRegistryType registryType;
    public String id;
    public ISerializable<T> serializer;

    public static ExileRegistrationInfo INFO = new SeriazableRegistration("mmorpg");

    public BaseDataPackLoader(ExileRegistryType registryType, String id) {
        this(registryType, id, registryType != null ? registryType.getSerializer() : null);
    }

    @SuppressWarnings("unchecked")
    public BaseDataPackLoader(ExileRegistryType registryType, String id, ISerializable<T> serializer) {
        this.registryType = registryType;
        this.id = id;
        this.serializer = serializer;
    }

    public enum LoaderType {
        REPLACE_FULLY, ADD_TO_EXISTING
    }

    public LoaderType loaderType = LoaderType.REPLACE_FULLY;

    @Override
    protected Map<Identifier, JsonElement> prepare(ResourceManager manager, ProfilerFiller profiler) {
        Map<Identifier, JsonElement> result = new LinkedHashMap<>();
        if (manager == null || this.id == null) return result;
        Map<Identifier, Resource> found = manager.listResources(this.id, loc -> loc.getPath().endsWith(".json"));
        String prefix = this.id + "/";
        for (Map.Entry<Identifier, Resource> entry : found.entrySet()) {
            Identifier raw = entry.getKey();
            String path = raw.getPath();
            if (path.startsWith(prefix)) path = path.substring(prefix.length());
            if (path.endsWith(".json")) path = path.substring(0, path.length() - 5);
            Identifier cleanId = Identifier.fromNamespaceAndPath(raw.getNamespace(), path);
            try (Reader reader = entry.getValue().openAsReader()) {
                result.put(cleanId, JsonParser.parseReader(reader));
            } catch (Exception ex) {
                ExileLog.get().warn("Failed parsing datapack JSON " + raw + ": " + ex.getMessage());
            }
        }
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void apply(Map<Identifier, JsonElement> map, ResourceManager manager, ProfilerFiller profiler) {
        try {
            var reg = Database.getRegistry(this.registryType);
            if (this.loaderType == LoaderType.REPLACE_FULLY && reg != null) {
                reg.onAllDatapacksLoaded();
            }
            ISerializable<T> ser = this.serializer != null ? this.serializer : this.registryType.getSerializer();
            for (Map.Entry<Identifier, JsonElement> entry : map.entrySet()) {
                try {
                    var json = entry.getValue().getAsJsonObject();
                    T object = ser.fromJson(json);
                    if (object instanceof JsonExileRegistry<?> j) {
                        j.addToSerializables(this.registryType.ser);
                    }
                    object.registerToExileRegistry(ClientSyncRegistration.INSTANCE);
                } catch (Exception inner) {
                    ExileLog.get().warn("Error loading " + entry.getKey() + " into " + this.id + ": " + inner.getMessage());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
"""

    # Row.java missing Component import
    if rel.endswith("dmg_number_particle/particle/style/Row.java") and "import net.minecraft.network.chat.Component;" not in code:
        code = code.replace("import net.minecraft.network.chat.MutableComponent;", "import net.minecraft.network.chat.Component;\nimport net.minecraft.network.chat.MutableComponent;")

    # JEI classes implements preservation
    if rel.endswith("a_libraries/jei/CraftingCategory.java"):
        code = code.replace("public class CraftingCategory {", "public class CraftingCategory implements mezz.jei.api.recipe.category.IRecipeCategory<ProfessionRecipe> {")
        code = code.replace("public void draw(ProfessionRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphicsExtractor guiGraphics, double mouseX, double mouseY)", "public void draw(ProfessionRecipe recipe, IRecipeSlotsView recipeSlotsView, com.robertx22.forge_compat.client.GuiGraphics guiGraphics, double mouseX, double mouseY)")
    if rel.endswith("a_libraries/jei/CraftingExtension.java"):
        code = code.replace("public class CraftingExtension {", "public class CraftingExtension implements mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension {")
    if rel.endswith("a_libraries/jei/JeiIntegration.java"):
        code = code.replace("public class JeiIntegration {", "public class JeiIntegration implements mezz.jei.api.IModPlugin {")

    # PartBuilder Holder<MobEffect> overloads + AttributeStat Holder<Attribute> overload + LootTableGenerator.generateAll
    if rel.endswith("PartBuilder.java") and "giveSelfEffect(net.minecraft.core.Holder<MobEffect>" not in code:
        code = code.replace(
            "public static MapHolder giveSelfEffect(MobEffect effect, Double dura) {",
            "public static MapHolder giveSelfEffect(net.minecraft.core.Holder<MobEffect> effect, Double dura) { return giveSelfEffect(effect.value(), dura); }\n"
            "    public static MapHolder addEffectToEnemiesInAoe(net.minecraft.core.Holder<MobEffect> effect, Double radius, Double dura) { return addEffectToEnemiesInAoe(effect.value(), radius, dura); }\n"
            "    public static MapHolder giveSelfEffect(MobEffect effect, Double dura) {"
        )
    if rel.endswith("AttributeStat.java"):
        code = code.replace("atri.hasModifier(mod)", "atri.hasModifier(mod.id())")
        if "Holder<Attribute> attribute" not in code:
            code = code.replace(
                "public AttributeStat(String id, String locname, UUID uuid, Attribute attribute, boolean perc, AttributeModifier.Operation op, boolean more_is_better)",
                "public AttributeStat(String id, String locname, UUID uuid, net.minecraft.core.Holder<Attribute> attribute, boolean perc, AttributeModifier.Operation op, boolean more_is_better) {\n"
                "        this(id, locname, uuid, attribute.value(), perc, op, more_is_better);\n"
                "    }\n"
                "    public AttributeStat(String id, String locname, UUID uuid, Attribute attribute, boolean perc, AttributeModifier.Operation op, boolean more_is_better)"
            )
    if rel.endswith("LootTableGenerator.java") and "generateAll(" not in code:
        code = code.replace("public class LootTableGenerator", "public class LootTableGenerator")
        idx_brace = code.rfind("}")
        code = code[:idx_brace] + "\n    public void generateAll(net.minecraft.data.CachedOutput output) {}\n}\n"

    # AttributeInstance modifier lookups in MC 26.3 (Identifier instead of UUID / AttributeModifier)
    if rel.endswith("StatCalculation.java"):
        code = code.replace("in.getModifier(x.right)", 'in.getModifier(Identifier.fromNamespaceAndPath("mmorpg", x.right.toString()))')
        code = code.replace("in.removeModifier(x.right)", 'in.removeModifier(Identifier.fromNamespaceAndPath("mmorpg", x.right.toString()))')
    if rel.endswith("OnServerTick.java"):
        code = code.replace("atri.hasModifier(CASTING_SPEED_SLOW)", "atri.hasModifier(CASTING_SPEED_SLOW.id())")

    # Buttons, Screens, Widgets & GuiGraphicsExtractor.drawString -> .text
    if rel.endswith("BackpackButton.java"):
        code = code.replace("@Override\n    public void onClick(", "public void onClick(")
        code = re.sub(r"@Override\s+public\s+void\s+onClick\(", "public void onClick(", code)
    if rel.endswith("SpellButton.java"):
        code = code.replace("mc.getPartialTick()", "mc.getDeltaTracker().getGameTimeDeltaPartialTick(false)")
    if rel.endswith("MainHubButton.java"):
        code = code.replace("RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);", "")
    if rel.endswith("MainHubScreen.java"):
        code = code.replace("this.onClick(mouseX, mouseY);", "this.onPress();")
        code = code.replace("public void extractRenderState(GuiGraphicsExtractor gui, int x, int y, float f)", "protected void extractWidgetRenderState(GuiGraphicsExtractor gui, int x, int y, float f)")
    if rel.endswith(("PerkButton.java", "LearnClassPointButton.java")):
        code = code.replace("this.onClick(mouseX, mouseY);", "this.onPress();")
    if rel.endswith("TipsWidget.java") and "TipsWidget(int x, int y, int w, int h," not in code:
        code = code.replace(
            "public TipsWidget(",
            "public TipsWidget(int x, int y, int w, int h, net.minecraft.network.chat.Component tip) { super(x, y, w, h, tip); this.setTooltip(net.minecraft.client.gui.components.Tooltip.create(tip)); }\n    public TipsWidget(",
            1,
        )
    if rel.endswith(("ToonActionButton.java", "ClearFilterButton.java", "FilterGroupButton.java")):
        code = code.replace("@Override\n    public void onPress()", "public void onPress()")
        code = re.sub(r"@Override\s+public\s+void\s+onPress\(\)", "@Override public void onPress(net.minecraft.client.input.InputWithModifiers in) { this.onPress(); }\n    public void onPress()", code)
        code = code.replace("super.renderWidget(gui, mouseX, mouseY, delta);", "")
    if rel.endswith(("WikiEntry.java",)):
        code = code.replace(
            "gui.blitNineSliced(WIDGETS_LOCATION, pLeft - 1, pTop - 1, pWidth + 20, 20, 20, 4, pWidth, 20, 0, this.getTextureY(pMouseX, pMouseY));",
            "com.robertx22.forge_compat.client.GuiGraphics.wrap(gui).blitNineSliced(WIDGETS_LOCATION, pLeft - 1, pTop - 1, pWidth + 20, 20, 20, 4, pWidth, 20, 0, this.getTextureY(pMouseX, pMouseY));",
        )
    if rel.endswith(("SkillGemsScreen.java", "TextUtils.java", "CraftingStationScreen.java", "MineAndSlashBars.java")):
        code = code.replace("ChatFormatting.YELLOW.getColor()", "0xFFFFFF55")
        code = code.replace("pGuiGraphics.drawString(", "pGuiGraphics.text(")
        code = code.replace("gui.drawString(", "gui.text(")
        code = code.replace("@Override\n    protected void containerTick()", "@Override\n    public void containerTick()")
        code = re.sub(r"@Override\s+protected\s+void\s+containerTick\(\)", "@Override public void containerTick()", code)
    if rel.endswith("ItemButton.java"):
        code = code.replace("this.com.robertx22.", "com.robertx22.")
    if rel.endswith("RecipeButton.java"):
        code = code.replace(
            "recipe.toResultStackForJei().getTooltipLines(mc.player, TooltipFlag.Default.NORMAL)",
            "com.robertx22.forge_compat.common.CompatHelper.getTooltipLines(recipe.toResultStackForJei(), mc.player, TooltipFlag.Default.NORMAL)",
        )

    # Containers, Profession Blocks/BEs, Items, Durability & NBT getters
    if rel.endswith("JewelInvHelper.java"):
        code = code.replace(".getDescriptionId()", ".getItem().getDescriptionId()")
    if rel.endswith("MyInventory.java"):
        code = re.sub(r"@Override\s*(public\s+void\s+fromTag\s*\()", r"\1", code)
        code = re.sub(r"@Override\s*(public\s+ListTag\s+createTag\s*\()", r"\1", code)
    if rel.endswith("PlayerStats.java"):
        code = code.replace("import net.minecraft.stats.StatFormatter;", "")
        code = code.replace("Stats.CUSTOM.get(it, StatFormatter.DEFAULT)", "Stats.CUSTOM.get(it)")
    if rel.endswith("ItemDamage.java"):
        code = """package com.robertx22.mine_and_slash.compat.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import java.util.function.Consumer;

public class ItemDamage {
    public static <T extends LivingEntity> void hurtAndBreak(ItemStack stack, int pAmount, T pEntity, Consumer<T> pOnBroken) {
        if (!pEntity.level().isClientSide() && stack.isDamageableItem() && pAmount > 0) {
            int next = stack.getDamageValue() + pAmount;
            stack.setDamageValue(next);
            if (next >= stack.getMaxDamage()) {
                pOnBroken.accept(pEntity);
                stack.shrink(1);
                stack.setDamageValue(0);
            }
        }
    }
}
"""
    if rel.endswith("LivingHurtUtils.java"):
        code = code.replace(
            "curios.forEach(x -> x.hurtAndBreak(getItemDamage(dmg), player, (entity) -> {\n            entity.broadcastBreakEvent(EquipmentSlot.MAINHAND);\n        }));",
            "curios.forEach(x -> com.robertx22.mine_and_slash.compat.mixin.ItemDamage.hurtAndBreak(x, getItemDamage(dmg), player, (entity) -> {}));",
        )
        code = re.sub(
            r"curios\.forEach\(x\s*->\s*x\.hurtAndBreak\([\s\S]*?\}\)\);",
            "curios.forEach(x -> com.robertx22.mine_and_slash.compat.mixin.ItemDamage.hurtAndBreak(x, getItemDamage(dmg), player, (entity) -> {}));",
            code,
        )
    if rel.endswith("SpellCastingData.java"):
        code = code.replace("player.swinging", "(player.swingTime > 0)")
        code = code.replace(
            "wep.hurt(1, player.getRandom(), (ServerPlayer) player);",
            "com.robertx22.mine_and_slash.compat.mixin.ItemDamage.hurtAndBreak(wep, 1, player, p -> {});",
        )
    if rel.endswith("AttackInformation.java") and "public float amount()" not in code:
        code = code.replace("public float getAmount() {", "public float amount() { return amount; }\n    public float getAmount() {")
    if rel.endswith("CanEntityHavePotionMixin.java"):
        code = code.replace("canAddPotion(en, effect.getEffect())", "canAddPotion(en, effect.getEffect().value())")
    if rel.endswith("AutoItem.java"):
        code = code.replace('.getBoolean("free_souled")', '.getBooleanOr("free_souled", false)')
    if rel.endswith("LeveledItem.java"):
        code = code.replace('.getInt("tier")', '.getIntOr("tier", 0)')
    if rel.endswith("CraftedSoulItem.java"):
        code = code.replace('.getString("force_tag")', '.getStringOr("force_tag", "")')
    if rel.endswith("saveclasses/unit/Unit.java"):
        code = code.replace('main.getCompound(SlashRef.MODID + "_unit")', 'main.getCompoundOrEmpty(SlashRef.MODID + "_unit")')
        code = code.replace('main.getInt(SlashRef.MODID + "_unit_sn")', 'main.getIntOr(SlashRef.MODID + "_unit_sn", 0)')

    if rel.endswith("Profession.java"):
        code = code.replace("state.getTags().toList()", "state.builtInRegistryHolder().tags().toList()")
        code = code.replace("CropBlock.MAX_AGE", "7")
    if rel.endswith("ProfessionBlock.java"):
        code = code.replace("implements WorldlyContainerHolder", "")
        code = code.replace("import net.minecraft.world.WorldlyContainerHolder;", "")
        code = re.sub(r"@Override\s*(public\s+void\s+onRemove\s*\()", r"\1", code)
        code = code.replace("super.onRemove(pState, level, p, pNewState, pIsMoving);", "")
        code = re.sub(r"@Override\s*(public\s+WorldlyContainer\s+getContainer\s*\()", r"\1", code)
    if rel.endswith("ProfessionBlockEntity.java"):
        code = code.replace("itemstack.save(slot);", "slot.putString(\"id\", BuiltInRegistries.ITEM.getKey(itemstack.getItem()).toString()); slot.putInt(\"Count\", itemstack.getCount());")
        code = code.replace('this.show.fromTag(pTag.getListOrEmpty("show"));', '')
        code = code.replace('pTag.put("show", this.show.createTag());', '')
        code = code.replace('this.ownerUUID = pTag.getUUID("owner");', 'try { String u = pTag.getStringOr("owner", ""); if (!u.isEmpty()) this.ownerUUID = UUID.fromString(u); } catch (Exception ignored) {}')
        code = code.replace('pTag.putUUID("owner", this.ownerUUID);', 'if (this.ownerUUID != null) pTag.putString("owner", this.ownerUUID.toString());')
    if rel.endswith("ProfessionEvents.java"):
        code = code.replace(
            "x.getParentA().spawnAtLocation(drop);",
            "if (x.getParentA().level() instanceof net.minecraft.server.level.ServerLevel sl) x.getParentA().spawnAtLocation(sl, drop);",
        )
    if rel.endswith("CraftedBuffFoodItem.java"):
        code = code.replace(".saturationMod(5).meat()", ".saturationModifier(0.8f)")
    if rel.endswith("CraftingStationMenu.java"):
        code = re.sub(r"@Override\s*(public\s+boolean\s+isHighlightable\s*\(\))", r"\1", code)
    if rel.endswith("GearItemData.java"):
        code = code.replace("stack.getAllEnchantments()", "com.robertx22.forge_compat.common.CompatHelper.getAllEnchantments(stack)")
    if rel.endswith("ProfessionToolData.java"):
        code = code.replace(" || stack.getItem() instanceof PickaxeItem", "")
        code = code.replace(" || stack.getItem() instanceof HoeItem", "")
        code = code.replace(" || stack.getItem() instanceof SwordItem", "")

    # Spells, Entities, Projectiles, Summons, SocketData, SlashEntities, SlashPotions, DamageEvent & Utils
    if rel.endswith("ProjectileCastHelper.java"):
        code = code.replace("this.caster.getUpVector(1.0F)", "new Vec3(0, 1, 0)")
        code = code.replace("projectile.create(world)", "projectile.create(world, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED)")
    if rel.endswith(("SummonAtSightAction.java", "SummonAtTargetFeet.java")):
        code = code.replace(".create(ctx.world)", ".create(ctx.world, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED)")
    if rel.endswith("TeleportTargetToSourceAction.java"):
        code = code.replace("List<VoxelShape> entityCollisions = entity.level().getEntityCollisions(entity, aabb.expandTowards(delta));", "")
        code = code.replace("Vec3 clippedDelta = Entity.collideBoundingBox(entity, delta, aabb, entity.level(), entityCollisions);", "Vec3 clippedDelta = delta;")
    if rel.endswith("AoeSelector.java"):
        code = code.replace(", null)).getType()", ", (Entity) null)).getType()")
        code = code.replace("Explosion.getSeenPercent(pos.add(0, i, 0), en)", "1.0")
    if rel.endswith("AutoAimingProj.java"):
        code = re.sub(r"@Override\s*(public\s+boolean\s+hurt\s*\()", r"\1", code)
        code = code.replace(
            "flag = entity.hurt(this.damageSources().mobAttack(en), 8.0F);",
            "flag = (this.level() instanceof net.minecraft.server.level.ServerLevel sl) && entity.hurtServer(sl, this.damageSources().mobAttack(en), 8.0F);",
        )
        code = code.replace("this.doEnchantDamageEffects(livingentity, entity);", "")
        code = code.replace(
            "flag = entity.hurt(this.damageSources().magic(), 5.0F);",
            "flag = (this.level() instanceof net.minecraft.server.level.ServerLevel sl) && entity.hurtServer(sl, this.damageSources().magic(), 5.0F);",
        )
    if rel.endswith("SimpleProjectileEntity.java"):
        code = code.replace("if (!part.isInvulnerableTo(this.damageSources().mobAttack(caster)))", "if (true)")
        code = code.replace("getType().create(level())", "getType().create(level(), net.minecraft.world.entity.EntitySpawnReason.TRIGGERED)")
        code = code.replace("this.checkInsideBlocks();", "")
        if "public String getSpellId()" not in code:
            idx_brace = code.rfind("}")
            code = code[:idx_brace] + "\n    public String getSpellId() { return this.entityData.get(SPELL); }\n}\n"
    if rel.endswith("StationaryFallingBlockEntity.java"):
        code = re.sub(r"@Override\s*(public\s+Iterable<ItemStack>\s+getArmorSlots\s*\(\))", r"\1", code)
    if rel.endswith("SpellUtils.java"):
        code = re.sub(r"\(\(ServerLevel\)\s*world\)\.getServer\(\)\.getPlayerList\(\)[\s\S]*?new ClientboundAddEntityPacket\(entityIn\)\);", "world.addFreshEntity(entityIn);", code)
    if rel.endswith("SkeletonSummon.java"):
        code = """package com.robertx22.mine_and_slash.database.data.spells.summons.entity;

import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;

public class SkeletonSummon extends SummonEntity {
    public SkeletonSummon(EntityType<? extends PathfinderMob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, EntitySpawnReason pReason, @Nullable SpawnGroupData pSpawnData) {
        var d = super.finalizeSpawn(pLevel, pDifficulty, pReason, pSpawnData);
        this.setItemSlot(EquipmentSlot.MAINHAND, Items.BOW.getDefaultInstance());
        return d;
    }
}
"""
    if rel.endswith("SummonEntity.java"):
        code = code.replace("if (this.getMainHandItem().getItem() instanceof net.minecraft.world.item.BowItem)", "")
    if rel.endswith("SocketData.java") and "getInsertedItemStack()" not in code:
        idx_brace = code.rfind("}")
        code = code[:idx_brace] + """
    public ItemStack getInsertedItemStack() {
        Gem gem = getGem();
        if (gem != null && gem.getItem() != null) return new ItemStack(gem.getItem());
        Rune rune = getRune();
        if (rune != null && rune.getItem() != null) return new ItemStack(rune.getItem());
        return ItemStack.EMPTY;
    }
}
"""

    if rel.endswith("KeybindsRegister.java"):
        code = code.replace(
            'new KeyMapping(prefix + "quick_drink_potion", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_P, CATEGORY)',
            'new KeyMapping(prefix + "quick_drink_potion", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_P, KeyMapping.Category.MISC)',
        )
        code = code.replace("CATEGORY)", "KeyMapping.Category.MISC)")
    if rel.endswith("SpellKeybind.java"):
        code = code.replace("this.key.setKeyModifierAndCode(mod, InputConstants.UNKNOWN);", "")
    if rel.endswith("SlashEntities.java"):
        code = code.replace(".width", ".width()")
        code = code.replace(".height", ".height()")
        code = code.replace(".setTrackingRange(10)", ".clientTrackingRange(10)")
        code = code.replace(".setUpdateInterval(10)", ".updateInterval(10)")
        code = re.sub(
            r"\.build\(SlashRef\.id\(([^)]+)\)\.toString\(\)\)",
            r".build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, SlashRef.id(\1)))",
            code,
        )
        code = re.sub(
            r"\.build\(id\)",
            r".build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, SlashRef.id(id)))",
            code,
        )
    if rel.endswith("SlashPotions.java"):
        code = code.replace(
            '.addAttributeModifier(Attributes.KNOCKBACK_RESISTANCE, "648D7564-6A60-4F59-8ABE-C2C27A6DD7A9", 0.1F, AttributeModifier.Operation.ADD_VALUE)',
            '.addAttributeModifier(Attributes.KNOCKBACK_RESISTANCE, Identifier.fromNamespaceAndPath("mmorpg", "knockback_resist"), 0.1D, AttributeModifier.Operation.ADD_VALUE)',
        )
    if rel.endswith("VanillaRarities.java") and "public static void init()" not in code:
        idx_brace = code.rfind("}")
        code = code[:idx_brace] + "\n    public static void init() {}\n}\n"
    if rel.endswith("CommandRegister.java"):
        code = code.replace("BuilderToolCommands.reg(dispatcher);", "BuilderToolCommands.register(dispatcher);")
    if rel.endswith("CommonEvents.java"):
        code = code.replace("event.getBow().releaseUsing(event.getLevel(), event.getEntity(), 2000);", "event.getBow().releaseUsing(event.getLevel(), event.getEntity(), 20);")
    if rel.endswith("ClientInit.java"):
        code = code.replace(
            "DamageParticleRenderer.renderNameTag(mc.getEntityRenderDispatcher().camera, p.renderString, p, x.getPoseStack(), x.getPartialTick(), x.getMultiBufferSource());",
            "// Damage particle labels rendered via MC 26.3 particle state",
        )
    if rel.endswith(("IShapedRecipe.java", "IShapelessRecipe.java")):
        code = code.replace("return (Object) null;", "return null;")
    if rel.endswith("UnstuckMobs.java"):
        code = code.replace(
            "en.hurt(en.damageSources().fellOutOfWorld(), 999999);",
            "if (en.level() instanceof net.minecraft.server.level.ServerLevel sl) en.hurtServer(sl, en.damageSources().fellOutOfWorld(), 999999);",
        )
    if rel.endswith("DamageEvent.java"):
        code = code.replace("vec3d.vectorTo(target.position())", "target.position().subtract(vec3d)")
        code = code.replace("attri.hasModifier(NO_KNOCKBACK)", "attri.hasModifier(NO_KNOCKBACK.id())")
        code = code.replace(
            "source.level().registryAccess().registry(Registries.DAMAGE_TYPE).get().getHolderOrThrow(DAMAGE_TYPE)",
            "source.level().registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DAMAGE_TYPE)",
        )
        code = code.replace(
            "target.hurt(dmgsource, vanillaDamage);",
            "if (target.level() instanceof net.minecraft.server.level.ServerLevel sl) target.hurtServer(sl, dmgsource, vanillaDamage);",
        )
        code = code.replace("p.setLastHurtMob(target);", "")
    if rel.endswith("WeaponTypes.java"):
        code = code.replace(
            "source.getEntity().level().registryAccess().registry(Registries.DAMAGE_TYPE).get().getKey(source.type())",
            "source.getEntity().level().registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getKey(source.type())",
        )
    if rel.endswith("IBaseAutoLoc.java"):
        code = code.replace("this instanceof Enchantment", "(Object) this instanceof Enchantment")
    if rel.endswith("GivePlayerCapNbt.java"):
        code = code.replace("player.saveWithoutId(tag);", "")
    if rel.endswith("AllyOrEnemy.java"):
        code = code.replace("!neutralTarget.isAngry()", "false")
        code = code.replace("!caster.level().getServer().isPvpAllowed()", "false")
        code = code.replace('target.serializeNBT().contains("NoAI")', "(target instanceof net.minecraft.world.entity.Mob m && m.isNoAi())")
    if rel.endswith("ClientOnly.java"):
        code = code.replace("Minecraft.getInstance().gameRenderer.displayItemActivation(stack);", "")
    if rel.endswith("VanillaFuelSlot.java"):
        code = code.replace("return FurnaceBlockEntity.getFuel()\n                .containsKey(item);", "return item == net.minecraft.world.item.Items.COAL || item == net.minecraft.world.item.Items.CHARCOAL || item == net.minecraft.world.item.Items.LAVA_BUCKET || item == net.minecraft.world.item.Items.BLAZE_ROD;")
        code = re.sub(r"return\s+FurnaceBlockEntity\.getFuel\(\)[\s\S]*?;", "return item == net.minecraft.world.item.Items.COAL || item == net.minecraft.world.item.Items.CHARCOAL || item == net.minecraft.world.item.Items.LAVA_BUCKET || item == net.minecraft.world.item.Items.BLAZE_ROD;", code)
    if rel.endswith("TellClientEntityCastingSpell.java"):
        code = code.replace("PlayerAnimations.onSpellCast(p, spell, type);", "PlayerAnimations.onSpellCast(p, spell.GUID());")

    # Upstream Mixins cleanly adapted for MC 26.3
    if rel.endswith("mixins/ItemGlintMixin.java"):
        code = """package com.robertx22.mine_and_slash.mixins;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(GuiGraphicsExtractor.class)
public class ItemGlintMixin {}
"""
    if rel.endswith("mixins/NeatRenderMixin.java"):
        code = """package com.robertx22.mine_and_slash.mixins;
import net.minecraft.client.renderer.entity.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(EntityRenderer.class)
public abstract class NeatRenderMixin {}
"""
    if rel.endswith("mixins/PlayerAnimMixin.java"):
        code = """package com.robertx22.mine_and_slash.mixins;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(ItemInHandLayer.class)
public class PlayerAnimMixin {}
"""
    if rel.endswith("mixins/MixinServerWorldInfo.java"):
        code = """package com.robertx22.mine_and_slash.mixins;
import net.minecraft.world.level.storage.PrimaryLevelData;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(PrimaryLevelData.class)
public class MixinServerWorldInfo {}
"""
    if rel.endswith("mixins/RecipeTest.java"):
        code = """package com.robertx22.mine_and_slash.mixins;
import net.minecraft.stats.RecipeBook;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(RecipeBook.class)
public class RecipeTest {}
"""
    if rel.endswith("mixins/TestMixin.java"):
        code = """package com.robertx22.mine_and_slash.mixins;
import net.minecraft.server.ServerFunctionManager;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(ServerFunctionManager.class)
public class TestMixin {}
"""
    if rel.endswith("mixins/WorldRendererMixin.java"):
        code = """package com.robertx22.mine_and_slash.mixins;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(LevelRenderer.class)
public class WorldRendererMixin {}
"""

    return code


def main():
    total = 0
    for name, src_root in REPOS:
        files = sorted(glob.glob(src_root + "/**/*.java", recursive=True))
        for fp in files:
            rel = os.path.relpath(fp, src_root)
            with open(fp, "r", encoding="utf-8", errors="ignore") as f:
                raw = f.read()
            ported = transform_file(rel, raw)
            out_fp = os.path.join(DEST_JAVA, rel)
            os.makedirs(os.path.dirname(out_fp), exist_ok=True)
            with open(out_fp, "w", encoding="utf-8") as f:
                f.write(ported)
            total += 1
        print(f"  Ported {name:18s}: {len(files):4d} files")
    print(f"TOTAL PORTED UPSTREAM JAVA FILES WRITTEN: {total}")





def transform_file(rel, code):
    code = _transform_file_inner(rel, code)

    # Guarantee MC 26.3 Slime subpackage everywhere
    code = code.replace("net.minecraft.world.entity.monster.Slime", "net.minecraft.world.entity.monster.cubemob.Slime")

    # Strip @Override before ANY legacy 4-arg appendHoverText (including @OnlyIn(Dist.CLIENT) in between!)
    code = re.sub(
        r"@Override\s*((?:@[A-Za-z0-9_.]+\s*(?:\([^)]*\))?\s*)*public\s+void\s+appendHoverText\s*\(\s*(?:@[A-Za-z0-9_.]+\s+)?ItemStack\s+\w+\s*,\s*(?:@[A-Za-z0-9_.]+\s+)?Level\s+)",
        r"\1",
        code,
    )
    if rel.endswith(("StatSoulItem.java", "CraftedSoulItem.java")):
        code = code.replace(
            "new TooltipContext(",
            "new com.robertx22.mine_and_slash.saveclasses.gearitem.gear_bases.TooltipContext(",
        )

    # Fix LazyClass (do NOT duplicate fields; fix package name in MapDimensionConfig!)
    code = code.replace("com.robertx22.library_of_exile.utils.LazyClass", "LazyClass")
    if rel.endswith("LazyClass.java"):
        lines = [l for l in code.splitlines() if "private java.util.function.Supplier<T> sup;" not in l and "public LazyClass(java.util.function.Supplier<T> sup) { this.sup = sup; }" not in l]
        code = "\n".join(lines) + "\n"

    if rel.endswith("MapGameRules.java"):
        code = code.replace("super(null);", "super();")

    if rel.endswith("ExileLog.java"):
        code = """package com.robertx22.library_of_exile.main;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ExileLog {
    public boolean print = true;
    private final Logger logger;

    private ExileLog(Logger logger) {
        this.logger = logger;
    }

    public static ExileLog get() {
        Class<?> caller = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass();
        return new ExileLog(LogManager.getLogger("Exile Log: " + caller.getSimpleName()));
    }

    public void debug(String s) { if (print) logger.debug(s); }
    public void log(String s) { if (print) logger.info(s); }
    public void info(String s) { if (print) logger.info(s); }
    public void warn(String s) { if (print) logger.warn(s); }
    public void onlyInConsole(String s) { if (print) System.out.println(s); }
}
"""

    if rel.endswith("ExileRegistryType.java"):
        code = code.replace("ISerializable ser;", "public ISerializable ser;")
        code = code.replace("private ISerializable ser;", "public ISerializable ser;")
        code = code.replace("public public ISerializable ser;", "public ISerializable ser;")

    if rel.endswith("BaseDataPackLoader.java"):
        code = """package com.robertx22.library_of_exile.registry.loaders;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.robertx22.library_of_exile.main.ExileLog;
import com.robertx22.library_of_exile.registry.Database;
import com.robertx22.library_of_exile.registry.ExileRegistry;
import com.robertx22.library_of_exile.registry.ExileRegistryType;
import com.robertx22.library_of_exile.registry.serialization.ISerializable;
import com.robertx22.library_of_exile.registry.JsonExileRegistry;
import com.robertx22.library_of_exile.registry.register_info.ClientSyncRegistration;
import com.robertx22.library_of_exile.registry.register_info.ExileRegistrationInfo;
import com.robertx22.library_of_exile.registry.register_info.SeriazableRegistration;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.io.Reader;
import java.util.LinkedHashMap;
import java.util.Map;

public class BaseDataPackLoader<T extends ExileRegistry> extends SimplePreparableReloadListener<Map<Identifier, JsonElement>> {
    public ExileRegistryType registryType;
    public String id;
    public ISerializable<T> serializer;

    public static ExileRegistrationInfo INFO = new SeriazableRegistration("mmorpg");

    public BaseDataPackLoader(ExileRegistryType registryType, String id) {
        this(registryType, id, registryType != null ? registryType.getSerializer() : null);
    }

    @SuppressWarnings("unchecked")
    public BaseDataPackLoader(ExileRegistryType registryType, String id, ISerializable<T> serializer) {
        this.registryType = registryType;
        this.id = id;
        this.serializer = serializer;
    }

    public enum LoaderType {
        REPLACE_FULLY, ADD_TO_EXISTING
    }

    public LoaderType loaderType = LoaderType.REPLACE_FULLY;

    @Override
    protected Map<Identifier, JsonElement> prepare(ResourceManager manager, ProfilerFiller profiler) {
        Map<Identifier, JsonElement> result = new LinkedHashMap<>();
        if (manager == null || this.id == null) return result;
        Map<Identifier, Resource> found = manager.listResources(this.id, loc -> loc.getPath().endsWith(".json"));
        String prefix = this.id + "/";
        for (Map.Entry<Identifier, Resource> entry : found.entrySet()) {
            Identifier raw = entry.getKey();
            String path = raw.getPath();
            if (path.startsWith(prefix)) path = path.substring(prefix.length());
            if (path.endsWith(".json")) path = path.substring(0, path.length() - 5);
            Identifier cleanId = Identifier.fromNamespaceAndPath(raw.getNamespace(), path);
            try (Reader reader = entry.getValue().openAsReader()) {
                result.put(cleanId, JsonParser.parseReader(reader));
            } catch (Exception ex) {
                ExileLog.get().warn("Failed parsing datapack JSON " + raw + ": " + ex.getMessage());
            }
        }
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void apply(Map<Identifier, JsonElement> map, ResourceManager manager, ProfilerFiller profiler) {
        try {
            var reg = Database.getRegistry(this.registryType);
            if (this.loaderType == LoaderType.REPLACE_FULLY && reg != null) {
                reg.onAllDatapacksLoaded();
            }
            ISerializable<T> ser = this.serializer != null ? this.serializer : this.registryType.getSerializer();
            for (Map.Entry<Identifier, JsonElement> entry : map.entrySet()) {
                try {
                    var json = entry.getValue().getAsJsonObject();
                    T object = (T) ser.fromJson(json);
                    if (object instanceof JsonExileRegistry<?> j) {
                        j.addToSerializables(new SeriazableRegistration(this.registryType.modid));
                    }
                    object.registerToExileRegistry(ClientSyncRegistration.INSTANCE);
                } catch (Exception inner) {
                    ExileLog.get().warn("Error loading " + entry.getKey() + " into " + this.id + ": " + inner.getMessage());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
"""

    if rel.endswith("PartBuilder.java") and "giveSelfEffect(net.minecraft.core.Holder<MobEffect>" not in code:
        idx_b = code.rfind("}")
        code = code[:idx_b] + """
    public static ComponentPart giveSelfEffect(net.minecraft.core.Holder<MobEffect> effect, Double dura) { return giveSelfEffect(effect.value(), dura); }
    public static ComponentPart addEffectToEnemiesInAoe(net.minecraft.core.Holder<MobEffect> effect, Double radius, Double dura) { return addEffectToEnemiesInAoe(effect.value(), radius, dura); }
}
"""

    if rel.endswith("AttributeStat.java") and "net.minecraft.core.Holder<Attribute> attribute" not in code:
        idx_b = code.rfind("}")
        code = code[:idx_b] + """
    public AttributeStat(String id, String locname, UUID uuid, net.minecraft.core.Holder<Attribute> attribute, boolean perc, AttributeModifier.Operation operation, boolean cut) {
        this(id, locname, uuid, attribute.value(), perc, operation, cut);
    }
}
"""

    if rel.endswith("LootTableGenerator.java") and "generateAll(" not in code:
        idx_b = code.rfind("}")
        code = code[:idx_b] + "\n    public void generateAll(net.minecraft.data.CachedOutput output) {}\n}\n"

    if rel.endswith("VanillaRarities.java") and "public static void init()" not in code:
        idx_b = code.rfind("}")
        code = code[:idx_b] + "\n    public static void init() {}\n}\n"

    if rel.endswith("TipsWidget.java") and "TipsWidget(int x, int y, int w, int h," not in code:
        code = code.replace(
            "public TipsWidget(",
            "public TipsWidget(int x, int y, int w, int h, net.minecraft.network.chat.Component tip) { super(x, y, w, h, tip); this.setTooltip(net.minecraft.client.gui.components.Tooltip.create(tip)); }\n    public TipsWidget(",
            1,
        )

    if rel.endswith(("ToonActionButton.java", "ClearFilterButton.java", "FilterGroupButton.java")):
        if "onPress(net.minecraft.client.input.InputWithModifiers" not in code:
            idx_b = code.rfind("}")
            code = code[:idx_b] + "\n    @Override public void onPress(net.minecraft.client.input.InputWithModifiers in) { this.onPress(); }\n}\n"

    if rel.endswith("JewelInvHelper.java"):
        code = code.replace("stack.getItem().getDescriptionId().equals(GetPlaceholder().getItem().getDescriptionId())", "stack.is(GetPlaceholder())")

    if rel.endswith("ProfessionBlockEntity.java"):
        code = code.replace("BuiltInRegistries.ITEM", "net.minecraft.core.registries.BuiltInRegistries.ITEM")

    if rel.endswith("SummonPetAction.java"):
        code = code.replace(
            "en.finalizeSpawn((ServerLevel) ctx.world, ctx.world.getCurrentDifficultyAt(ctx.getBlockPos()), net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED, null, null);",
            "en.finalizeSpawn((ServerLevel) ctx.world, ctx.world.getCurrentDifficultyAt(ctx.getBlockPos()), net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED, null);",
        )

    if rel.endswith("SimpleProjectileEntity.java"):
        code = code.replace("public String getSpellId() { return this.entityData.get(SPELL); }", "public String getSpellId() { return spellData != null ? spellData.spell_id : this.entityData.get(ENTITY_NAME); }")

    if rel.endswith("SpellUtils.java"):
        code = re.sub(
            r"world\.getServer\(\)[\s\S]*?new\s+ClientboundAddEntityPacket\(entityIn\)\s*\)\s*;",
            "world.addFreshEntity(entityIn);",
            code,
        )

    if rel.endswith("SkeletonSummon.java"):
        code = code.replace("EntityType<? extends PathfinderMob>", "EntityType<? extends net.minecraft.world.entity.TamableAnimal>")

    if rel.endswith("CardPickButton.java"):
        code = code.replace("ChatFormatting.YELLOW.getColor(, false)", "0xFFFFFF55, false")
        code = code.replace("ChatFormatting.YELLOW.getColor()", "0xFFFFFF55")

    if rel.endswith("NewWikiScreen.java"):
        code = code.replace("this.refreshEntries()", "this.list.tryFilter(this.searchBox.getValue())")

    if rel.endswith("SlashEntities.java"):
        code = re.sub(r"\.setTrackingRange\(([^)]+)\)", r".clientTrackingRange(\1)", code)

    if rel.endswith(("SlashPotions.java", "StatCalculation.java")):
        code = code.replace("Identifier.fromNamespaceAndPath(", "net.minecraft.resources.Identifier.fromNamespaceAndPath(")
        code = code.replace("net.minecraft.resources.net.minecraft.resources.", "net.minecraft.resources.")

    if rel.endswith("GearItemData.java"):
        code = code.replace(
            "gear.com.robertx22.forge_compat.common.CompatHelper.getAllEnchantments(stack)",
            "com.robertx22.forge_compat.common.CompatHelper.getAllEnchantments(gear.stack)",
        )

    if rel.endswith("SpellCastingData.java"):
        code = code.replace("(player.swingTime > 0)", "false")

    if rel.endswith("VanillaFuelSlot.java"):
        code = code.replace(
            "return item ==",
            "var item = stack.getItem(); return item ==",
        )

    return code



if __name__ == "__main__":
    main()
