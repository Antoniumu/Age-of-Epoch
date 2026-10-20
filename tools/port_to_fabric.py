#!/usr/bin/env python3
"""
port_to_fabric.py - Age of Exile (MC 1.16.5 Forge) -> Fabric MC 26.3 migration.

Mechanical renames across all Java sources:
  * net.minecraft 1.16.5 "official" names -> 26.3 Mojang (unobfuscated) names
  * net.minecraftforge.* -> com.robertx22.forge_compat.* (in-repo adapter layer)
  * constructor rewrites (StringTextComponent -> Component.literal, ...)
  * ItemStack NBT helpers (pre-1.20.5 tag API -> custom_data component helpers)
  * unambiguous simple-name renames + auto-added imports

Run from anywhere:  python3 tools/port_to_fabric.py [--check]
"""

import os
import re
import sys

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ROOT = os.path.join(REPO, "src", "main", "java")

FORGE_OLD = "net.minecraftforge."
FORGE_NEW = "com.robertx22.forge_compat."

# ---------------------------------------------------------------------------
# Full import renames: old FQCN -> new FQCN
# ---------------------------------------------------------------------------
IMPORT_TABLE = {
    "net.minecraft.util.ResourceLocation": "net.minecraft.resources.ResourceLocation",
    "net.minecraft.util.RegistryKey": "net.minecraft.resources.ResourceKey",
    "net.minecraft.util.text.ITextComponent": "net.minecraft.network.chat.Component",
    "net.minecraft.util.text.IFormattableTextComponent": "net.minecraft.network.chat.MutableComponent",
    "net.minecraft.util.text.StringTextComponent": "net.minecraft.network.chat.Component",
    "net.minecraft.util.text.TranslationTextComponent": "net.minecraft.network.chat.Component",
    "net.minecraft.util.text.TextComponent": "net.minecraft.network.chat.Component",
    "net.minecraft.util.text.TextFormatting": "net.minecraft.ChatFormatting",
    "net.minecraft.util.text.event.ClickEvent": "net.minecraft.network.chat.ClickEvent",
    "net.minecraft.util.text.event.HoverEvent": "net.minecraft.network.chat.HoverEvent",
    "net.minecraft.util.text.Style": "net.minecraft.network.chat.Style",
    "net.minecraft.util.DamageSource": "net.minecraft.world.damagesource.DamageSource",
    "net.minecraft.util.EntityDamageSource": "net.minecraft.world.damagesource.DamageSource",
    "net.minecraft.util.IndirectEntityDamageSource": "net.minecraft.world.damagesource.DamageSource",
    "net.minecraft.util.CombatRules": "net.minecraft.world.damagesource.CombatRules",
    "net.minecraft.util.Hand": "net.minecraft.world.InteractionHand",
    "net.minecraft.util.ActionResult": "net.minecraft.world.InteractionResult",
    "net.minecraft.util.ActionResultType": "net.minecraft.world.InteractionResult",
    "net.minecraft.util.Direction": "net.minecraft.core.Direction",
    "net.minecraft.util.DrinkHelper": "net.minecraft.world.item.ItemUtils",
    "net.minecraft.util.IItemProvider": "net.minecraft.world.level.ItemLike",
    "net.minecraft.util.IReorderingProcessor": "net.minecraft.util.FormattedCharSequence",
    "net.minecraft.util.Mirror": "net.minecraft.world.level.block.Mirror",
    "net.minecraft.util.NonNullList": "net.minecraft.core.NonNullList",
    "net.minecraft.util.Rotation": "net.minecraft.world.level.block.Rotation",
    "net.minecraft.util.SharedSeedRandom": "net.minecraft.world.level.levelgen.WorldgenRandom",
    "net.minecraft.util.SoundCategory": "net.minecraft.sounds.SoundSource",
    "net.minecraft.util.SoundEvent": "net.minecraft.sounds.SoundEvent",
    "net.minecraft.util.SoundEvents": "net.minecraft.sounds.SoundEvents",
    "net.minecraft.util.Tuple": "com.robertx22.forge_compat.util.Tuple",
    "net.minecraft.util.math.AxisAlignedBB": "net.minecraft.world.phys.AABB",
    "net.minecraft.util.math.BlockPos": "net.minecraft.core.BlockPos",
    "net.minecraft.util.math.BlockRayTraceResult": "net.minecraft.world.phys.BlockHitResult",
    "net.minecraft.util.math.ChunkPos": "net.minecraft.world.level.ChunkPos",
    "net.minecraft.util.math.EntityRayTraceResult": "net.minecraft.world.phys.EntityHitResult",
    "net.minecraft.util.math.MathHelper": "net.minecraft.util.Mth",
    "net.minecraft.util.math.MutableBoundingBox": "net.minecraft.world.level.levelgen.structure.BoundingBox",
    "net.minecraft.util.math.RayTraceContext": "net.minecraft.world.phys.ClipContext",
    "net.minecraft.util.math.RayTraceResult": "net.minecraft.world.phys.HitResult",
    "net.minecraft.util.math.vector.Matrix4f": "org.joml.Matrix4f",
    "net.minecraft.util.math.vector.Vector3d": "net.minecraft.world.phys.Vec3",
    "net.minecraft.util.math.vector.Vector3f": "org.joml.Vector3f",
    "net.minecraft.util.registry.Registry": "net.minecraft.core.Registry",
    "net.minecraft.util.registry.RegistryLookupCodec": "net.minecraft.core.RegistryLookupCodec",
    "net.minecraft.util.registry.DefaultedRegistry": "net.minecraft.core.DefaultedMappedRegistry",
    "net.minecraft.block.AbstractBlock": "net.minecraft.world.level.block.state.BlockBehaviour",
    "net.minecraft.block.BeetrootBlock": "net.minecraft.world.level.block.BeetrootBlock",
    "net.minecraft.block.Block": "net.minecraft.world.level.block.Block",
    "net.minecraft.block.BlockState": "net.minecraft.world.level.block.state.BlockState",
    "net.minecraft.block.Blocks": "net.minecraft.world.level.block.Blocks",
    "net.minecraft.block.CropsBlock": "net.minecraft.world.level.block.CropBlock",
    "net.minecraft.block.FireBlock": "net.minecraft.world.level.block.FireBlock",
    "net.minecraft.block.HorizontalBlock": "net.minecraft.world.level.block.HorizontalDirectionalBlock",
    "net.minecraft.block.ITileEntityProvider": "net.minecraft.world.level.block.EntityBlock",
    "net.minecraft.block.LeavesBlock": "net.minecraft.world.level.block.LeavesBlock",
    "net.minecraft.block.RedstoneTorchBlock": "net.minecraft.world.level.block.RedstoneTorchBlock",
    "net.minecraft.block.SoundType": "net.minecraft.world.level.block.SoundType",
    "net.minecraft.block.material.Material": "com.robertx22.forge_compat.material.Material",
    "net.minecraft.client.Minecraft": "net.minecraft.client.Minecraft",
    "net.minecraft.client.gui.AbstractGui": "net.minecraft.client.gui.Gui",
    "net.minecraft.client.gui.FontRenderer": "net.minecraft.client.gui.Font",
    "net.minecraft.client.gui.ScreenManager": "net.minecraft.client.gui.MenuScreens",
    "net.minecraft.client.gui.screen.ChatScreen": "net.minecraft.client.gui.screens.ChatScreen",
    "net.minecraft.client.gui.screen.Screen": "net.minecraft.client.gui.screens.Screen",
    "net.minecraft.client.gui.screen.inventory.ContainerScreen": "net.minecraft.client.gui.screens.inventory.AbstractContainerScreen",
    "net.minecraft.client.gui.screen.inventory.InventoryScreen": "net.minecraft.client.gui.screens.inventory.InventoryScreen",
    "net.minecraft.client.gui.widget.Widget": "net.minecraft.client.gui.components.Renderable",
    "net.minecraft.client.gui.widget.button.Button": "net.minecraft.client.gui.components.Button",
    "net.minecraft.client.gui.widget.button.ImageButton": "net.minecraft.client.gui.components.ImageButton",
    "net.minecraft.client.renderer.ActiveRenderInfo": "net.minecraft.client.Camera",
    "net.minecraft.client.renderer.BufferBuilder": "com.mojang.blaze3d.vertex.BufferBuilder",
    "net.minecraft.client.renderer.GameRenderer": "net.minecraft.client.renderer.GameRenderer",
    "net.minecraft.client.renderer.IRenderTypeBuffer": "net.minecraft.client.renderer.MultiBufferSource",
    "net.minecraft.client.renderer.ItemRenderer": "net.minecraft.client.renderer.entity.ItemRenderer",
    "net.minecraft.client.renderer.LightTexture": "net.minecraft.client.renderer.LightTexture",
    "net.minecraft.client.renderer.RenderType": "net.minecraft.client.renderer.RenderType",
    "net.minecraft.client.renderer.RenderTypeLookup": "net.minecraft.client.renderer.ItemBlockRenderTypes",
    "net.minecraft.client.renderer.Tessellator": "com.mojang.blaze3d.vertex.Tessellator",
    "net.minecraft.client.renderer.WorldRenderer": "net.minecraft.client.renderer.LevelRenderer",
    "net.minecraft.client.renderer.entity.ArrowRenderer": "net.minecraft.client.renderer.entity.ArrowRenderer",
    "net.minecraft.client.renderer.entity.EntityRenderer": "net.minecraft.client.renderer.entity.EntityRenderer",
    "net.minecraft.client.renderer.entity.EntityRendererManager": "net.minecraft.client.renderer.entity.EntityRenderDispatcher",
    "net.minecraft.client.renderer.entity.FallingBlockRenderer": "net.minecraft.client.renderer.entity.FallingBlockRenderer",
    "net.minecraft.client.renderer.entity.SpriteRenderer": "net.minecraft.client.renderer.entity.ThrownItemRenderer",
    "net.minecraft.client.renderer.entity.model.TridentModel": "net.minecraft.client.model.TridentModel",
    "net.minecraft.client.renderer.model.ItemCameraTransforms": "net.minecraft.client.renderer.block.model.ItemTransform",
    "net.minecraft.client.renderer.texture.AtlasTexture": "net.minecraft.client.renderer.texture.TextureAtlas",
    "net.minecraft.client.renderer.texture.OverlayTexture": "net.minecraft.client.renderer.texture.OverlayTexture",
    "net.minecraft.client.renderer.vertex.DefaultVertexFormats": "com.mojang.blaze3d.vertex.DefaultVertexFormats",
    "net.minecraft.client.resources.I18n": "net.minecraft.client.resources.language.I18n",
    "net.minecraft.client.resources.JsonReloadListener": "net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener",
    "net.minecraft.client.settings.KeyBinding": "net.minecraft.client.KeyMapping",
    "net.minecraft.client.util.ITooltipFlag": "net.minecraft.world.item.TooltipFlag",
    "net.minecraft.client.world.ClientWorld": "net.minecraft.client.multiplayer.ClientLevel",
    "com.mojang.blaze3d.matrix.MatrixStack": "com.mojang.blaze3d.vertex.PoseStack",
    "net.minecraft.command.CommandSource": "net.minecraft.commands.CommandSourceStack",
    "net.minecraft.command.Commands": "net.minecraft.commands.Commands",
    "net.minecraft.command.ICommandSource": "net.minecraft.commands.CommandSource",
    "net.minecraft.command.ISuggestionProvider": "net.minecraft.commands.SharedSuggestionProvider",
    "net.minecraft.command.arguments.EntityArgument": "net.minecraft.commands.arguments.EntityArgument",
    "net.minecraft.data.DirectoryCache": "net.minecraft.data.HashCache",
    "net.minecraft.data.IDataProvider": "net.minecraft.data.DataProvider",
    "net.minecraft.data.ShapedRecipeBuilder": "net.minecraft.data.recipes.ShapedRecipeBuilder",
    "net.minecraft.data.ShapelessRecipeBuilder": "net.minecraft.data.recipes.ShapelessRecipeBuilder",
    "net.minecraft.enchantment.Enchantment": "net.minecraft.world.item.enchantment.Enchantment",
    "net.minecraft.enchantment.EnchantmentData": "net.minecraft.world.item.enchantment.EnchantmentInstance",
    "net.minecraft.enchantment.EnchantmentHelper": "net.minecraft.world.item.enchantment.EnchantmentHelper",
    "net.minecraft.enchantment.Enchantments": "net.minecraft.world.item.enchantment.Enchantments",
    "net.minecraft.entity.Entity": "net.minecraft.world.entity.Entity",
    "net.minecraft.entity.EntityClassification": "net.minecraft.world.entity.MobCategory",
    "net.minecraft.entity.EntitySpawnPlacementRegistry": "net.minecraft.world.entity.SpawnPlacementRegistry",
    "net.minecraft.entity.EntityType": "net.minecraft.world.entity.EntityType",
    "net.minecraft.entity.ILivingEntityData": "net.minecraft.world.entity.SpawnGroupData",
    "net.minecraft.entity.IRendersAsItem": "com.robertx22.forge_compat.client.IRendersAsItem",
    "net.minecraft.entity.LivingEntity": "net.minecraft.world.entity.LivingEntity",
    "net.minecraft.entity.MobEntity": "net.minecraft.world.entity.Mob",
    "net.minecraft.entity.MoverType": "net.minecraft.world.entity.MoverType",
    "net.minecraft.entity.SpawnReason": "net.minecraft.world.entity.MobSpawnType",
    "net.minecraft.entity.ai.attributes.Attribute": "net.minecraft.world.entity.ai.attributes.Attribute",
    "net.minecraft.entity.ai.attributes.AttributeModifier": "net.minecraft.world.entity.ai.attributes.AttributeModifier",
    "net.minecraft.entity.ai.attributes.AttributeModifierManager": "net.minecraft.world.entity.ai.attributes.AttributeMap",
    "net.minecraft.entity.ai.attributes.Attributes": "net.minecraft.world.entity.ai.attributes.Attributes",
    "net.minecraft.entity.ai.attributes.ModifiableAttributeInstance": "net.minecraft.world.entity.ai.attributes.AttributeInstance",
    "net.minecraft.entity.boss.dragon.EnderDragonEntity": "net.minecraft.world.entity.boss.enderdragon.EnderDragon",
    "net.minecraft.entity.boss.dragon.EnderDragonPartEntity": "net.minecraft.world.entity.boss.enderdragon.EnderDragonPart",
    "net.minecraft.entity.effect.LightningBoltEntity": "net.minecraft.world.entity.LightningBolt",
    "net.minecraft.entity.item.ArmorStandEntity": "net.minecraft.world.entity.decoration.ArmorStand",
    "net.minecraft.entity.item.ExperienceOrbEntity": "net.minecraft.world.entity.ExperienceOrb",
    "net.minecraft.entity.item.FallingBlockEntity": "net.minecraft.world.entity.item.FallingBlockEntity",
    "net.minecraft.entity.item.ItemEntity": "net.minecraft.world.entity.item.ItemEntity",
    "net.minecraft.entity.merchant.villager.VillagerEntity": "net.minecraft.world.entity.npc.Villager",
    "net.minecraft.entity.monster.IMob": "net.minecraft.world.entity.monster.Enemy",
    "net.minecraft.entity.monster.SlimeEntity": "net.minecraft.world.entity.monster.Slime",
    "net.minecraft.entity.passive.AnimalEntity": "net.minecraft.world.entity.animal.Animal",
    "net.minecraft.entity.passive.TameableEntity": "net.minecraft.world.entity.animal.TamableAnimal",
    "net.minecraft.entity.player.PlayerEntity": "net.minecraft.world.entity.player.Player",
    "net.minecraft.entity.player.PlayerInventory": "net.minecraft.world.entity.player.Inventory",
    "net.minecraft.entity.player.ServerPlayerEntity": "net.minecraft.server.level.ServerPlayer",
    "net.minecraft.entity.projectile.AbstractArrowEntity": "net.minecraft.world.entity.projectile.AbstractArrow",
    "net.minecraft.entity.projectile.EvokerFangsEntity": "net.minecraft.world.entity.projectile.EvokerFangs",
    "net.minecraft.entity.projectile.FireworkRocketEntity": "net.minecraft.world.entity.projectile.FireworkRocketEntity",
    "net.minecraft.entity.projectile.ProjectileEntity": "net.minecraft.world.entity.projectile.Projectile",
    "net.minecraft.entity.projectile.ProjectileHelper": "net.minecraft.world.entity.projectile.ProjectileUtil",
    "net.minecraft.entity.projectile.TridentEntity": "net.minecraft.world.entity.projectile.ThrownTrident",
    "net.minecraft.inventory.CraftingInventory": "net.minecraft.world.inventory.CraftingContainer",
    "net.minecraft.inventory.EnderChestInventory": "net.minecraft.world.entity.player.EnderChestInventory",
    "net.minecraft.inventory.EquipmentSlotType": "net.minecraft.world.entity.EquipmentSlot",
    "net.minecraft.inventory.IInventory": "net.minecraft.world.Container",
    "net.minecraft.inventory.ISidedInventory": "net.minecraft.world.WorldlyContainer",
    "net.minecraft.inventory.Inventory": "net.minecraft.world.SimpleContainer",
    "net.minecraft.inventory.ItemStackHelper": "net.minecraft.world.ContainerHelper",
    "net.minecraft.inventory.container.ChestContainer": "net.minecraft.world.inventory.ChestMenu",
    "net.minecraft.inventory.container.ClickType": "net.minecraft.world.inventory.ClickType",
    "net.minecraft.inventory.container.Container": "net.minecraft.world.inventory.AbstractContainerMenu",
    "net.minecraft.inventory.container.ContainerType": "net.minecraft.world.inventory.MenuType",
    "net.minecraft.inventory.container.INamedContainerProvider": "net.minecraft.world.MenuProvider",
    "net.minecraft.inventory.container.SimpleNamedContainerProvider": "net.minecraft.world.SimpleMenuProvider",
    "net.minecraft.inventory.container.Slot": "net.minecraft.world.inventory.Slot",
    "net.minecraft.item.ArmorItem": "net.minecraft.world.item.ArmorItem",
    "net.minecraft.item.ArmorMaterial": "net.minecraft.world.item.ArmorMaterial",
    "net.minecraft.item.BlockItem": "net.minecraft.world.item.BlockItem",
    "net.minecraft.item.BlockItemUseContext": "net.minecraft.world.item.context.BlockPlaceContext",
    "net.minecraft.item.BlockNamedItem": "net.minecraft.world.item.ItemNameBlockItem",
    "net.minecraft.item.BowItem": "net.minecraft.world.item.BowItem",
    "net.minecraft.item.CrossbowItem": "net.minecraft.world.item.CrossbowItem",
    "net.minecraft.item.EnchantedBookItem": "net.minecraft.world.item.EnchantedBookItem",
    "net.minecraft.item.IArmorMaterial": "net.minecraft.world.item.ArmorMaterial",
    "net.minecraft.item.Item": "net.minecraft.world.item.Item",
    "net.minecraft.item.Item.Properties": "net.minecraft.world.item.Item.Properties",
    "net.minecraft.item.ItemGroup": "net.minecraft.world.item.CreativeModeTab",
    "net.minecraft.item.ItemStack": "net.minecraft.world.item.ItemStack",
    "net.minecraft.item.ItemTier": "net.minecraft.world.item.Tier",
    "net.minecraft.item.Items": "net.minecraft.world.item.Items",
    "net.minecraft.item.UseAction": "net.minecraft.world.item.UseAnim",
    "net.minecraft.item.crafting.IRecipe": "net.minecraft.world.item.crafting.Recipe",
    "net.minecraft.item.crafting.IRecipeSerializer": "net.minecraft.world.item.crafting.RecipeSerializer",
    "net.minecraft.item.crafting.IRecipeType": "net.minecraft.world.item.crafting.RecipeType",
    "net.minecraft.item.crafting.Ingredient": "net.minecraft.world.item.crafting.Ingredient",
    "net.minecraft.loot.LootContext": "net.minecraft.world.level.storage.loot.LootContext",
    "net.minecraft.loot.LootParameterSets": "net.minecraft.world.level.storage.loot.LootContextParamSets",
    "net.minecraft.loot.LootParameters": "net.minecraft.world.level.storage.loot.LootContextParams",
    "net.minecraft.loot.LootTable": "net.minecraft.world.level.storage.loot.LootTable",
    "net.minecraft.loot.LootTables": "net.minecraft.world.level.storage.loot.LootTables",
    "net.minecraft.loot.conditions.BlockStateProperty": "net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition",
    "net.minecraft.loot.conditions.ILootCondition": "net.minecraft.world.level.storage.loot.predicates.LootItemCondition",
    "net.minecraft.nbt.CompoundNBT": "net.minecraft.nbt.CompoundTag",
    "net.minecraft.nbt.INBT": "net.minecraft.nbt.Tag",
    "net.minecraft.nbt.JsonToNBT": "net.minecraft.nbt.TagParser",
    "net.minecraft.nbt.ListNBT": "net.minecraft.nbt.ListTag",
    "net.minecraft.nbt.NBTUtil": "net.minecraft.nbt.NbtUtils",
    "net.minecraft.nbt.StringNBT": "net.minecraft.nbt.StringTag",
    "net.minecraft.nbt.IntNBT": "net.minecraft.nbt.IntTag",
    "net.minecraft.nbt.DoubleNBT": "net.minecraft.nbt.DoubleTag",
    "net.minecraft.nbt.ByteNBT": "net.minecraft.nbt.ByteTag",
    "net.minecraft.nbt.LongNBT": "net.minecraft.nbt.LongTag",
    "net.minecraft.nbt.ShortNBT": "net.minecraft.nbt.ShortTag",
    "net.minecraft.nbt.FloatNBT": "net.minecraft.nbt.FloatTag",
    "net.minecraft.network.IPacket": "net.minecraft.network.protocol.Packet",
    "net.minecraft.network.NetworkManager": "net.minecraft.network.Connection",
    "net.minecraft.network.PacketBuffer": "net.minecraft.network.FriendlyByteBuf",
    "net.minecraft.network.datasync.DataParameter": "net.minecraft.network.syncher.EntityDataAccessor",
    "net.minecraft.network.datasync.DataSerializers": "net.minecraft.network.syncher.EntityDataSerializers",
    "net.minecraft.network.datasync.EntityDataManager": "net.minecraft.network.syncher.SynchedEntityData",
    "net.minecraft.network.play.server.SEntityVelocityPacket": "net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket",
    "net.minecraft.network.play.server.SPlayEntityEffectPacket": "net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket",
    "net.minecraft.network.play.server.SSpawnObjectPacket": "net.minecraft.network.protocol.game.ClientboundAddEntityPacket",
    "net.minecraft.network.play.server.STitlePacket": "com.robertx22.forge_compat.network.ClientboundTitleCompatPacket",
    "net.minecraft.network.play.server.SUpdateTileEntityPacket": "net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket",
    "net.minecraft.particles.BasicParticleType": "net.minecraft.core.particles.SimpleParticleType",
    "net.minecraft.particles.BlockParticleData": "net.minecraft.core.particles.BlockParticleOption",
    "net.minecraft.particles.IParticleData": "net.minecraft.core.particles.ParticleOptions",
    "net.minecraft.particles.ParticleType": "net.minecraft.core.particles.ParticleType",
    "net.minecraft.particles.ParticleTypes": "net.minecraft.core.particles.ParticleTypes",
    "net.minecraft.particles.RedstoneParticleData": "net.minecraft.core.particles.DustParticleOptions",
    "net.minecraft.potion.Effect": "net.minecraft.world.effect.MobEffect",
    "net.minecraft.potion.EffectInstance": "net.minecraft.world.effect.MobEffectInstance",
    "net.minecraft.potion.EffectType": "net.minecraft.world.effect.MobEffectCategory",
    "net.minecraft.potion.Effects": "net.minecraft.world.effect.MobEffects",
    "net.minecraft.profiler.IProfiler": "net.minecraft.util.profiling.ProfilerFiller",
    "net.minecraft.resources.DataPackRegistries": "com.robertx22.forge_compat.resources.DataPackRegistries",
    "net.minecraft.resources.IResourceManager": "net.minecraft.server.packs.resources.ResourceManager",
    "net.minecraft.resources.SimpleReloadableResourceManager": "net.minecraft.server.packs.resources.SimpleReloadableResourceManager",
    "net.minecraft.server.MinecraftServer": "net.minecraft.server.MinecraftServer",
    "net.minecraft.server.management.PlayerList": "net.minecraft.server.players.PlayerList",
    "net.minecraft.state.BooleanProperty": "net.minecraft.world.level.block.state.properties.BooleanProperty",
    "net.minecraft.state.DirectionProperty": "net.minecraft.world.level.block.state.properties.DirectionProperty",
    "net.minecraft.state.StateContainer": "net.minecraft.world.level.block.state.StateDefinition",
    "net.minecraft.tags.EntityTypeTags": "net.minecraft.tags.EntityTypeTags",
    "net.minecraft.tags.ITag": "net.minecraft.tags.Tag",
    "net.minecraft.tags.ItemTags": "net.minecraft.tags.ItemTags",
    "net.minecraft.tags.Tag": "net.minecraft.tags.Tag",
    "net.minecraft.tileentity.BeaconTileEntity": "net.minecraft.world.level.block.entity.BeaconBlockEntity",
    "net.minecraft.tileentity.ChestTileEntity": "net.minecraft.world.level.block.entity.ChestBlockEntity",
    "net.minecraft.tileentity.FurnaceTileEntity": "net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity",
    "net.minecraft.tileentity.ITickableTileEntity": "com.robertx22.forge_compat.blocks.ITickableBlockEntity",
    "net.minecraft.tileentity.LockableLootTileEntity": "net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity",
    "net.minecraft.tileentity.SignTileEntity": "net.minecraft.world.level.block.entity.SignBlockEntity",
    "net.minecraft.tileentity.TileEntity": "net.minecraft.world.level.block.entity.BlockEntity",
    "net.minecraft.tileentity.TileEntityType": "net.minecraft.world.level.block.entity.BlockEntityType",
    "net.minecraft.world.Blockreader": "net.minecraft.world.level.BlockGetter",
    "net.minecraft.world.BossInfo": "net.minecraft.server.level.ServerBossEvent",
    "net.minecraft.world.DifficultyInstance": "net.minecraft.world.DifficultyInstance",
    "net.minecraft.world.DimensionType": "net.minecraft.world.level.dimension.DimensionType",
    "net.minecraft.world.Explosion": "net.minecraft.world.level.Explosion",
    "net.minecraft.world.GameRules": "net.minecraft.world.level.GameRules",
    "net.minecraft.world.GameType": "net.minecraft.world.level.GameType",
    "net.minecraft.world.IBlockReader": "net.minecraft.world.level.BlockGetter",
    "net.minecraft.world.IServerWorld": "net.minecraft.world.level.ServerLevelAccessor",
    "net.minecraft.world.IWorld": "net.minecraft.world.level.LevelAccessor",
    "net.minecraft.world.IWorldReader": "net.minecraft.world.level.LevelReader",
    "net.minecraft.world.World": "net.minecraft.world.level.Level",
    "net.minecraft.world.biome.Biome": "net.minecraft.world.level.biome.Biome",
    "net.minecraft.world.biome.BiomeManager": "net.minecraft.world.level.biome.BiomeManager",
    "net.minecraft.world.biome.Biomes": "net.minecraft.world.level.biome.Biomes",
    "net.minecraft.world.biome.provider.BiomeProvider": "net.minecraft.world.level.biome.BiomeSource",
    "net.minecraft.world.biome.provider.SingleBiomeProvider": "net.minecraft.world.level.biome.FixedBiomeSource",
    "net.minecraft.world.chunk.Chunk": "net.minecraft.world.level.chunk.LevelChunk",
    "net.minecraft.world.chunk.IChunk": "net.minecraft.world.level.chunk.ChunkAccess",
    "net.minecraft.world.gen.ChunkGenerator": "net.minecraft.world.level.chunk.ChunkGenerator",
    "net.minecraft.world.gen.GenerationStage": "net.minecraft.world.level.levelgen.GenerationStep",
    "net.minecraft.world.gen.Heightmap": "net.minecraft.world.level.levelgen.Heightmap",
    "net.minecraft.world.gen.WorldGenRegion": "net.minecraft.world.level.WorldGenRegion",
    "net.minecraft.world.gen.feature.StructureFeature": "net.minecraft.world.level.levelgen.structure.Structure",
    "net.minecraft.world.gen.feature.jigsaw.JigsawPattern": "net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool",
    "net.minecraft.world.gen.feature.structure.JigsawStructure": "net.minecraft.world.level.levelgen.structure.structures.JigsawStructure",
    "net.minecraft.world.gen.feature.structure.Structure": "net.minecraft.world.level.levelgen.structure.Structure",
    "net.minecraft.world.gen.feature.structure.StructureManager": "net.minecraft.world.level.levelgen.structure.StructureManager",
    "net.minecraft.world.gen.feature.structure.VillageConfig": "com.robertx22.forge_compat.worldgen.VillageConfig",
    "net.minecraft.world.gen.feature.template.PlacementSettings": "net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings",
    "net.minecraft.world.gen.feature.template.ProcessorLists": "net.minecraft.world.level.levelgen.structure.templatesystem.ProcessorLists",
    "net.minecraft.world.gen.feature.template.StructureProcessorList": "net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList",
    "net.minecraft.world.gen.feature.template.Template": "net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate",
    "net.minecraft.world.gen.feature.template.TemplateManager": "net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager",
    "net.minecraft.world.gen.settings.DimensionStructuresSettings": "com.robertx22.forge_compat.worldgen.DimensionStructuresSettings",
    "net.minecraft.world.gen.settings.StructureSeparationSettings": "com.robertx22.forge_compat.worldgen.StructureSeparationSettings",
    "net.minecraft.world.server.ServerWorld": "net.minecraft.server.level.ServerLevel",
    "net.minecraft.advancements.ICriterionInstance": "net.minecraft.advancements.critereon.CriterionTriggerInstance",
    "net.minecraft.advancements.criterion.EnchantedItemTrigger": "net.minecraft.advancements.critereon.EnchantedItemTrigger",
    "net.minecraft.advancements.criterion.StatePropertiesPredicate": "net.minecraft.advancements.critereon.StatePropertiesPredicate",
}

WILDCARD_TABLE = {
    "net.minecraft.util.text.*": "net.minecraft.network.chat.*",
    "net.minecraft.util.math.*": "net.minecraft.core.*",
    "net.minecraft.item.*": "net.minecraft.world.item.*",
    "net.minecraft.loot.*": "net.minecraft.world.level.storage.loot.*",
    "net.minecraft.entity.*": "net.minecraft.world.entity.*",
    "net.minecraft.client.particle.*": "net.minecraft.client.particle.*",
    "net.minecraft.data.*": "net.minecraft.data.*",
    "net.minecraft.advancements.criterion.*": "net.minecraft.advancements.critereon.*",
    "net.minecraft.command.arguments.*": "net.minecraft.commands.arguments.*",
    "net.minecraft.inventory.*": "net.minecraft.world.inventory.*",
    "net.minecraft.block.*": "net.minecraft.world.level.block.*",
    "net.minecraft.particles.*": "net.minecraft.core.particles.*",
    "net.minecraft.potion.*": "net.minecraft.world.effect.*",
    "net.minecraft.tileentity.*": "net.minecraft.world.level.block.entity.*",
}

# simple-name renames driven by a file's own imports (handles ambiguity like
# Container = menu vs inventory). Ordered longest-name-first at match time.
IMPORT_DERIVED_ONLY = True

# Global unambiguous simple-name renames: (old, new)
SIMPLE_PAIRS = [
    ("IFormattableTextComponent", "MutableComponent"),
    ("TranslationTextComponent", "Component"),
    ("StringTextComponent", "Component"),
    ("TextFormatting", "ChatFormatting"),
    ("ITextComponent", "Component"),
    ("PacketBuffer", "FriendlyByteBuf"),
    ("CompoundNBT", "CompoundTag"),
    ("ListNBT", "ListTag"),
    ("StringNBT", "StringTag"),
    ("INBT", "Tag"),
    ("NBTUtil", "NbtUtils"),
    ("JsonToNBT", "TagParser"),
    ("MatrixStack", "PoseStack"),
    ("MathHelper", "Mth"),
    ("AxisAlignedBB", "AABB"),
    ("BlockRayTraceResult", "BlockHitResult"),
    ("EntityRayTraceResult", "EntityHitResult"),
    ("RayTraceResult", "HitResult"),
    ("RayTraceContext", "ClipContext"),
    ("PlayerEntity", "Player"),
    ("ServerPlayerEntity", "ServerPlayer"),
    ("PlayerInventory", "Inventory"),
    ("MobEntity", "Mob"),
    ("TileEntity", "BlockEntity"),
    ("TileEntityType", "BlockEntityType"),
    ("ITileEntityProvider", "EntityBlock"),
    ("EquipmentSlotType", "EquipmentSlot"),
    ("ContainerScreen", "AbstractContainerScreen"),
    ("ContainerType", "MenuType"),
    ("INamedContainerProvider", "MenuProvider"),
    ("SimpleNamedContainerProvider", "SimpleMenuProvider"),
    ("IRecipeSerializer", "RecipeSerializer"),
    ("IRecipeType", "RecipeType"),
    ("EffectInstance", "MobEffectInstance"),
    ("EffectType", "MobEffectCategory"),
    ("UseAction", "UseAnim"),
    ("ItemGroup", "CreativeModeTab"),
    ("IItemProvider", "ItemLike"),
    ("SoundCategory", "SoundSource"),
    ("ActionResultType", "InteractionResult"),
    ("ActionResult", "InteractionResult"),
    ("IWorldReader", "LevelReader"),
    ("IServerWorld", "ServerLevelAccessor"),
    ("IBlockReader", "BlockGetter"),
    ("ICommandSource", "CommandSource"),
    ("ISuggestionProvider", "SharedSuggestionProvider"),
    ("CommandSource", "CommandSourceStack"),
    ("FontRenderer", "Font"),
    ("ITooltipFlag", "TooltipFlag"),
    ("KeyBinding", "KeyMapping"),
    ("ActiveRenderInfo", "Camera"),
    ("IRenderTypeBuffer", "MultiBufferSource"),
    ("RenderTypeLookup", "ItemBlockRenderTypes"),
    ("AtlasTexture", "TextureAtlas"),
    ("WorldRenderer", "LevelRenderer"),
    ("EntityRendererManager", "EntityRenderDispatcher"),
    ("SpriteRenderer", "ThrownItemRenderer"),
    ("ItemCameraTransforms", "ItemTransform"),
    ("BlockItemUseContext", "BlockPlaceContext"),
    ("BlockNamedItem", "ItemNameBlockItem"),
    ("EnchantmentData", "EnchantmentInstance"),
    ("AttributeModifierManager", "AttributeMap"),
    ("ModifiableAttributeInstance", "AttributeInstance"),
    ("EntityClassification", "MobCategory"),
    ("EntitySpawnPlacementRegistry", "SpawnPlacementRegistry"),
    ("SpawnReason", "MobSpawnType"),
    ("ILivingEntityData", "SpawnGroupData"),
    ("ExperienceOrbEntity", "ExperienceOrb"),
    ("ArmorStandEntity", "ArmorStand"),
    ("LightningBoltEntity", "LightningBolt"),
    ("VillagerEntity", "Villager"),
    ("SlimeEntity", "Slime"),
    ("AnimalEntity", "Animal"),
    ("TameableEntity", "TamableAnimal"),
    ("EnderDragonEntity", "EnderDragon"),
    ("EnderDragonPartEntity", "EnderDragonPart"),
    ("AbstractArrowEntity", "AbstractArrow"),
    ("EvokerFangsEntity", "EvokerFangs"),
    ("FireworkRocketEntity", "FireworkRocketEntity"),
    ("TridentEntity", "ThrownTrident"),
    ("ProjectileEntity", "Projectile"),
    ("ProjectileHelper", "ProjectileUtil"),
    ("AbstractBlock", "BlockBehaviour"),
    ("HorizontalBlock", "HorizontalDirectionalBlock"),
    ("CropsBlock", "CropBlock"),
    ("DirectoryCache", "HashCache"),
    ("IDataProvider", "DataProvider"),
    ("IProfiler", "ProfilerFiller"),
    ("IResourceManager", "ResourceManager"),
    ("SimpleReloadableResourceManager", "SimpleReloadableResourceManager"),
    ("StateContainer", "StateDefinition"),
    ("LootParameterSets", "LootContextParamSets"),
    ("LootParameters", "LootContextParams"),
    ("DataParameter", "EntityDataAccessor"),
    ("DataSerializers", "EntityDataSerializers"),
    ("EntityDataManager", "SynchedEntityData"),
    ("IPacket", "Packet"),
    ("NetworkManager", "Connection"),
    ("BasicParticleType", "SimpleParticleType"),
    ("BlockParticleData", "BlockParticleOption"),
    ("IParticleData", "ParticleOptions"),
    ("RedstoneParticleData", "DustParticleOptions"),
    ("EffectInstance", "MobEffectInstance"),
    ("ItemTier", "Tier"),
    ("CraftingInventory", "CraftingContainer"),
    ("EnderChestInventory", "EnderChestInventory"),
    ("ISidedInventory", "WorldlyContainer"),
    ("IInventory", "Container"),
    ("ItemStackHelper", "ContainerHelper"),
    ("ChestContainer", "ChestMenu"),
    ("BiomeProvider", "BiomeSource"),
    ("SingleBiomeProvider", "FixedBiomeSource"),
    ("GenerationStage", "GenerationStep"),
    ("StructureFeature", "Structure"),
    ("JigsawPattern", "StructureTemplatePool"),
    ("JigsawStructure", "JigsawStructure"),
    ("PlacementSettings", "StructurePlaceSettings"),
    ("StructureProcessorList", "StructureProcessorList"),
    ("TemplateManager", "StructureTemplateManager"),
    ("DimensionStructuresSettings", "DimensionStructuresSettings"),
    ("StructureSeparationSettings", "StructureSeparationSettings"),
    ("MutableBoundingBox", "BoundingBox"),
    ("SharedSeedRandom", "WorldgenRandom"),
    ("IReorderingProcessor", "FormattedCharSequence"),
    ("DrinkHelper", "ItemUtils"),
    ("EntityDamageSource", "DamageSource"),
    ("IndirectEntityDamageSource", "DamageSource"),
    ("SUpdateTileEntityPacket", "ClientboundBlockEntityDataPacket"),
    ("SEntityVelocityPacket", "ClientboundSetEntityMotionPacket"),
    ("SPlayEntityEffectPacket", "ClientboundUpdateMobEffectPacket"),
    ("SSpawnObjectPacket", "ClientboundAddEntityPacket"),
    ("STitlePacket", "ClientboundTitleCompatPacket"),
    ("BeaconTileEntity", "BeaconBlockEntity"),
    ("ChestTileEntity", "ChestBlockEntity"),
    ("FurnaceTileEntity", "AbstractFurnaceBlockEntity"),
    ("LockableLootTileEntity", "RandomizableContainerBlockEntity"),
    ("SignTileEntity", "SignBlockEntity"),
    ("ITickableTileEntity", "ITickableBlockEntity"),
    ("AbstractGui", "Gui"),
    ("ScreenManager", "MenuScreens"),
    ("GameType", "GameType"),
    ("ServerWorld", "ServerLevel"),
    ("ClientWorld", "ClientLevel"),
    ("ICriterionInstance", "CriterionTriggerInstance"),
    ("RegistryKey", "ResourceKey"),
    ("JsonReloadListener", "SimpleJsonResourceReloadListener"),
    ("BlockStateProperty", "LootItemBlockStatePropertyCondition"),
    ("ILootCondition", "LootItemCondition"),
    ("ResourceLocation", "ResourceLocation"),
]

# Method-level renames 1.16.5 -> 26.3 (exact call-pattern strings)
METHOD_RENAMES = [
    (".getCommandSenderWorld()", ".level()"),
    (".getPosX()", ".getX()"),
    (".getPosY()", ".getY()"),
    (".getPosZ()", ".getZ()"),
    ("createSpawnPacket(", "getAddEntityPacket("),
    (".sendPacket(", ".send("),
    (".getMotion()", ".getDeltaMovement()"),
    (".setMotion(", ".setDeltaMovement("),
    (".getDistance(", ".distanceTo("),
    ("stack.getDisplayName()", "stack.getHoverName()"),
    ("player.inventory.", "player.getInventory()."),
    ("readAdditional(CompoundTag", "readAdditionalSaveData(CompoundTag"),
    ("writeAdditional(CompoundTag", "addAdditionalSaveData(CompoundTag"),
]

# Registry constants (info.loenwind.autosave's own Registry.GLOBAL_REGISTRY untouched)
REGISTRY_CONSTANTS = {
    "Registry.ITEM": "BuiltInRegistries.ITEM",
    "Registry.BLOCK": "BuiltInRegistries.BLOCK",
    "Registry.ENTITY_TYPE": "BuiltInRegistries.ENTITY_TYPE",
    "Registry.MOB_EFFECT": "BuiltInRegistries.MOB_EFFECT",
    "Registry.PARTICLE_TYPE": "BuiltInRegistries.PARTICLE_TYPE",
    "Registry.ATTRIBUTE": "BuiltInRegistries.ATTRIBUTE",
    "Registry.SOUND_EVENT": "BuiltInRegistries.SOUND_EVENT",
    "Registry.RECIPE_TYPE": "BuiltInRegistries.RECIPE_TYPE",
    "Registry.RECIPE_SERIALIZER": "BuiltInRegistries.RECIPE_SERIALIZER",
    "Registry.BLOCK_ENTITY_TYPE": "BuiltInRegistries.BLOCK_ENTITY_TYPE",
    "Registry.MENU": "BuiltInRegistries.MENU",
    "Registry.CHUNK_GENERATOR": "BuiltInRegistries.CHUNK_GENERATOR",
    "Registry.DIMENSION_REGISTRY": "Registries.DIMENSION_TYPE",
    "Registry.BIOME_REGISTRY": "Registries.BIOME",
}

# new FQCN per renamed simple name (for auto-import insertion); filled from IMPORT_TABLE
AUTO_IMPORT = {}


def build_auto_import():
    for old, new in IMPORT_TABLE.items():
        osimple, nsimple = old.rsplit(".", 1)[-1], new.rsplit(".", 1)[-1]
        if osimple != nsimple:
            AUTO_IMPORT.setdefault(nsimple, new)
    # helper / registry types introduced by the rewrites themselves
    AUTO_IMPORT["StackNBT"] = "com.robertx22.age_of_exile.uncommon.utilityclasses.StackNBT"
    AUTO_IMPORT["BuiltInRegistries"] = "net.minecraft.core.registries.BuiltInRegistries"
    AUTO_IMPORT["Registries"] = "net.minecraft.core.registries.Registries"


def sim_replace(code: str, pairs):
    """Simultaneous word-boundary replacement of pairs (longest old first)."""
    pairs = [(o, n) for o, n in pairs if o != n]
    if not pairs:
        return code
    pairs = sorted(set(pairs), key=lambda p: -len(p[0]))
    alternation = "|".join(re.escape(o) for o, _ in pairs)
    mapping = dict(pairs)
    return re.sub(r"\b(" + alternation + r")\b", lambda m: mapping[m.group(1)], code)


def process(path: str, check_only: bool, report: dict):
    with open(path, "r", encoding="utf-8") as f:
        original = f.read()

    import_pairs = []          # (old_simple, new_simple) driven by this file's imports
    extra_imports = set()
    body_lines = []
    import_region_done = False
    last_import_idx = -1
    lines = original.splitlines(keepends=True)

    for idx, line in enumerate(lines):
        stripped = line.lstrip()
        if stripped.startswith("import "):
            last_import_idx = idx
            m = re.match(r"import\s+(static\s+)?([\w.*]+)\s*;", stripped)
            if m:
                is_static, name = m.groups()
                if name.startswith(FORGE_OLD):
                    newname = FORGE_NEW + name[len(FORGE_OLD):]
                    line = line.replace(name, newname)
                elif name.endswith(".*") and name in WILDCARD_TABLE:
                    line = line.replace(name, WILDCARD_TABLE[name])
                elif name in IMPORT_TABLE:
                    newname = IMPORT_TABLE[name]
                    osimple, nsimple = name.rsplit(".", 1)[-1], newname.rsplit(".", 1)[-1]
                    if osimple != nsimple:
                        import_pairs.append((osimple, nsimple))
                    line = line.replace(name, newname)
                else:
                    # static import of a member (pkg.Class.member) - remap the class prefix
                    best = None
                    for key in IMPORT_TABLE:
                        if name.startswith(key + ".") and (best is None or len(key) > len(best)):
                            best = key
                    if best:
                        newname = IMPORT_TABLE[best] + name[len(best):]
                        osimple, nsimple = best.rsplit(".", 1)[-1], IMPORT_TABLE[best].rsplit(".", 1)[-1]
                        if osimple != nsimple:
                            import_pairs.append((osimple, nsimple))
                        line = line.replace(name, newname)
        body_lines.append(line)

    code = "".join(body_lines)
    code = code.replace(FORGE_OLD, FORGE_NEW)

    # constructors
    code = code.replace("new StringTextComponent(", "Component.literal(")
    code = code.replace("new TranslationTextComponent(", "Component.translatable(")
    code = re.sub(r"new ResourceLocation\(([^,()]+),([^,()]+)\)", r"ResourceLocation.fromNamespaceAndPath(\1,\2)", code)
    code = re.sub(r"new ResourceLocation\(([^,()]+)\)", r"ResourceLocation.parse(\1)", code)

    # ItemStack NBT
    code = re.sub(r"([\w.\[\]]+)\.getOrCreateTag\(\)", r"StackNBT.getOrCreate(\1)", code)
    code = re.sub(r"([\w.\[\]]+)\.hasTag\(\)", r"StackNBT.has(\1)", code)
    code = re.sub(r"([\w.\[\]]+)\.setTag\(([^;]+?)\)", r"StackNBT.set(\1, \2)", code)
    code = re.sub(r"([\w.\[\]]+)\.getTag\(\)", r"StackNBT.get(\1)", code)

    # registry constants
    for old, new in REGISTRY_CONSTANTS.items():
        code = code.replace(old, new)

    # method renames
    for old, new in METHOD_RENAMES:
        code = code.replace(old, new)

    # simple names: import-driven first, then global pairs
    pairs = list(import_pairs) + SIMPLE_PAIRS
    before_simple = code
    code = sim_replace(code, pairs)

    # auto-import new simple names that appear but are not imported
    used = set(re.findall(r"\b[A-Z][A-Za-z0-9_]*\b", code))
    imported_simples = set(re.findall(r"import\s+(?:static\s+)?[\w.]*\.([A-Za-z0-9_*]+)\s*;", code))
    wildcards = re.findall(r"import\s+([\w.]+)\.\*\s*;", code)
    for simple in sorted(used):
        fqcn = AUTO_IMPORT.get(simple)
        if not fqcn or simple in imported_simples:
            continue
        pkg = fqcn.rsplit(".", 1)[0]
        if any(pkg == w or pkg.startswith(w + ".") for w in wildcards):
            # java wildcards are not recursive; only skip exact package
            if pkg in wildcards:
                continue
        extra_imports.add(fqcn)

    if extra_imports:
        lines = code.splitlines(keepends=True)
        insert_at = 0
        for i, l in enumerate(lines):
            if l.lstrip().startswith("import ") or (l.strip() == "" and insert_at == i):
                insert_at = i + 1
        block = "".join("import %s;\n" % f for f in sorted(extra_imports))
        code = "".join(lines[:insert_at]) + block + "".join(lines[insert_at:])

    if code != original:
        report.setdefault("changed", []).append(path)
        if not check_only:
            with open(path, "w", encoding="utf-8") as f:
                f.write(code)
    return code


def main():
    check_only = "--check" in sys.argv
    build_auto_import()
    report = {}
    total = 0
    for dirpath, _d, filenames in os.walk(ROOT):
        for fn in filenames:
            if fn.endswith(".java"):
                total += 1
                process(os.path.join(dirpath, fn), check_only, report)

    # leftover scan: exact old FQCNs still imported
    old_names = [k for k, v in IMPORT_TABLE.items() if k != v] + [k for k, v in WILDCARD_TABLE.items() if k != v]
    old_pat = re.compile(r"^\s*import\s+(?:static\s+)?(" + "|".join(re.escape(n) for n in sorted(old_names, key=len, reverse=True)) + r")(?:\.[\w*]+)*\s*;|^\s*import\s+(?:static\s+)?net\.minecraftforge[\w.]*\s*;", re.M)
    leftovers = {}
    for dirpath, _d, filenames in os.walk(ROOT):
        for fn in filenames:
            if fn.endswith(".java"):
                p = os.path.join(dirpath, fn)
                with open(p, encoding="utf-8") as f:
                    txt = f.read()
                for m in old_pat.finditer(txt):
                    leftovers.setdefault(m.group(1) or "net.minecraftforge", []).append(p)
    print(f"processed {total} files, changed {len(report.get('changed', []))}")
    if leftovers:
        print("\n=== LEFTOVER OLD IMPORTS ===")
        for k, v in sorted(leftovers.items()):
            print(f"{k}  ({len(v)} files) e.g. {v[0]}")
    else:
        print("no leftover old imports")


if __name__ == "__main__":
    main()
