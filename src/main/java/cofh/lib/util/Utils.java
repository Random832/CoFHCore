package cofh.lib.util;

import cofh.lib.enchantment.EnchantmentCoFH;
import cofh.lib.tags.ItemTagsCoFH;
import cofh.lib.util.helpers.MathHelper;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.ForgeRegistries;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.nio.file.Path;

import static cofh.lib.util.Constants.MAX_CAPACITY;
import static cofh.lib.util.constants.NBTTags.TAG_ENCHANTMENTS;
import static net.minecraft.nbt.Tag.TAG_COMPOUND;
import static net.minecraft.nbt.Tag.TAG_LIST;
import static net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantmentLevel;

public class Utils {

    private Utils() {

    }

    public static boolean isModLoaded(String modid) {

        return ModList.get().isLoaded(modid);
    }

    public static boolean isClientWorld(Level world) {

        return world.isClientSide;
    }

    public static boolean isServerWorld(Level world) {

        return !world.isClientSide;
    }

    public static boolean isFakePlayer(Entity entity) {

        return entity instanceof FakePlayer;
    }

    public static boolean isCreativePlayer(Entity entity) {

        return entity instanceof Player player && player.isCreative();
    }

    public static EquipmentSlot handToEquipSlot(InteractionHand hand) {

        return hand.equals(InteractionHand.MAIN_HAND) ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
    }

    public static InteractionHand otherHand(InteractionHand hand) {

        return hand.equals(InteractionHand.MAIN_HAND) ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
    }

    public static EquipmentSlot otherHand(EquipmentSlot slot) {

        return slot.equals(EquipmentSlot.MAINHAND) ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND;
    }

    public static String createPrettyJSON(String jsonString) {

        JsonObject json = JsonParser.parseString(jsonString).getAsJsonObject();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        return gson.toJson(json);
    }

    public static void loadConfig(ForgeConfigSpec spec, Path path) {

        final CommentedFileConfig configData = CommentedFileConfig.builder(path).sync().autosave().writingMode(WritingMode.REPLACE).build();
        configData.load();
        spec.setConfig(configData);
    }

    public static boolean spawnLightningBolt(Level world, BlockPos pos) {

        return spawnLightningBolt(world, pos, null);
    }

    public static boolean spawnLightningBolt(Level world, BlockPos pos, Entity caster) {

        if (Utils.isServerWorld(world)) {
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(world);
            bolt.moveTo(Vec3.atBottomCenterOf(pos));
            bolt.setCause(caster instanceof ServerPlayer player ? player : null);
            world.addFreshEntity(bolt);
        }
        return true;
    }

    public static void spawnXpOrbs(Level world, int xp, Vec3 pos) {

        if (world == null) {
            return;
        }
        while (xp > 0) {
            int orbAmount = ExperienceOrb.getExperienceValue(xp);
            xp -= orbAmount;
            world.addFreshEntity(new ExperienceOrb(world, pos.x, pos.y, pos.z, orbAmount));
        }
    }

    public static boolean destroyBlock(Level world, BlockPos pos, boolean dropBlock, @Nullable Entity entityIn) {

        BlockState state = world.getBlockState(pos);
        if (state.isAir() || state.getDestroySpeed(world, pos) < 0 || (entityIn instanceof Player player && state.getDestroyProgress(player, world, pos) < 0)) {
            return false;
        } else {
            FluidState ifluidstate = world.getFluidState(pos);
            if (dropBlock) {
                BlockEntity tileentity = state.hasBlockEntity() ? world.getBlockEntity(pos) : null;
                Block.dropResources(state, world, pos, tileentity, entityIn, ItemStack.EMPTY);
            }
            return world.setBlock(pos, ifluidstate.createLegacyBlock(), 3);
        }
    }

    public static boolean isWrench(ItemStack item) {

        return item.is(ItemTagsCoFH.TOOLS_WRENCH);
    }

    @Nullable
    public static String getKeynameFromKeycode(int code) {

        return GLFW.glfwGetKeyName(code, -1);
    }

    @Nullable
    public static String getKeyNameFromScanCode(int code) {

        return GLFW.glfwGetKeyName(-1, code);
    }

    public static void openEntityScreen(ServerPlayer player, MenuProvider containerSupplier, Entity entity) {

        NetworkHooks.openScreen(player, containerSupplier, buf -> buf.writeVarInt(entity.getId()));
    }

    public static <E extends Entity> E getEntityFromBuf(FriendlyByteBuf buf, Class<E> type) {

        if (buf == null) {
            throw new IllegalArgumentException("Null packet buffer.");
        }
        return DistExecutor.unsafeCallWhenOn(Dist.CLIENT, () -> () -> {
            if (Minecraft.getInstance().level == null) {
                throw new IllegalStateException("Client world is null.");
            }
            int entityId = buf.readVarInt();
            Entity e = Minecraft.getInstance().level.getEntity(entityId);
            if (type.isInstance(e)) {
                // noinspection unchecked
                return (E) e;
            }
            throw new IllegalStateException("Client could not locate entity (id: " + entityId + ")  for entity container or the entity was of an invalid type. This is likely caused by a mod breaking client side entity lookup.");
        });
    }

    // region TIME CHECKS
    public static final int TIME_CONSTANT = 40;
    public static final int TIME_CONSTANT_HALF = TIME_CONSTANT / 2;
    public static final int TIME_CONSTANT_QUARTER = TIME_CONSTANT / 4;

    private static int timeConstant = 0;
    private static int timeConstantHalf = 0;
    private static int timeConstantQuarter = 0;

    public static void tickTimeConstants() {

        if (++timeConstant >= TIME_CONSTANT) {
            timeConstant = 0;
        }
        if (++timeConstantHalf >= TIME_CONSTANT_HALF) {
            timeConstantHalf = 0;
        }
        if (++timeConstantQuarter >= TIME_CONSTANT_QUARTER) {
            timeConstantQuarter = 0;
        }
    }

    public static boolean timeCheck() {

        return timeConstant == 0;
    }

    public static boolean timeCheckHalf() {

        return timeConstantHalf == 0;
    }

    public static boolean timeCheckQuarter() {

        return timeConstantQuarter == 0;
    }
    // endregion

    // region PARTICLE UTILS
    public static void spawnBlockParticlesClient(Level world, ParticleOptions particle, BlockPos pos, RandomSource rand, int count) {

        for (int i = 0; i < count; ++i) {
            double d0 = (double) pos.getX() + rand.nextDouble();
            double d1 = (double) pos.getY() + rand.nextDouble();
            double d2 = (double) pos.getZ() + rand.nextDouble();
            double d3 = (rand.nextDouble() - 0.5D) * 0.5D;
            double d4 = (rand.nextDouble() - 0.5D) * 0.5D;
            double d5 = (rand.nextDouble() - 0.5D) * 0.5D;
            world.addParticle(particle, d0, d1, d2, d3, d4, d5);
        }
    }

    public static void spawnParticles(Level world, ParticleOptions particle, double posX, double posY, double posZ, int particleCount, double xOffset, double yOffset, double zOffset, double speed) {

        if (isServerWorld(world)) {
            ((ServerLevel) world).sendParticles(particle, posX, posY + 1.0D, posZ, particleCount, xOffset, yOffset, zOffset, speed);
        } else {
            world.addParticle(particle, posX + xOffset, posY + yOffset, posZ + zOffset, 0.0D, 0.0D, 0.0D);
        }
    }

    public static void spawnParticles(Level level, ParticleOptions particle, int count, Vec3 pos, float posVar, Vec3 velocity, float velVar) {

        for (int i = 0; i < count; ++i) {
            spawnParticles(level, particle, pos, posVar, velocity, velVar);
        }
    }

    public static void spawnParticles(Level level, ParticleOptions particle, Vec3 pos, float posVar, Vec3 velocity, float velVar) {

        RandomSource rand = level.getRandom();
        spawnParticles(level, particle, pos.add(rand.nextFloat() * posVar * 2 - posVar, rand.nextFloat() * posVar * 2 - posVar, rand.nextFloat() * posVar * 2 - posVar),
                velocity.add(rand.nextFloat() * velVar * 2 - velVar, rand.nextFloat() * velVar * 2 - velVar, rand.nextFloat() * velVar * 2 - velVar));
    }

    public static void spawnParticles(Level level, ParticleOptions particle, Vec3 pos, Vec3 velocity) {

        level.addParticle(particle, pos.x, pos.y, pos.z, velocity.x, velocity.y, velocity.z);
    }
    // endregion

    // region ENTITY UTILS
    public static boolean addToPlayerInventory(Player player, ItemStack stack) {

        if (stack.isEmpty() || player == null) {
            return false;
        }
        if (stack.getItem() instanceof ArmorItem armorItem) {
            int index = armorItem.getSlot().getIndex();
            if (player.getInventory().armor.get(index).isEmpty()) {
                player.getInventory().armor.set(index, stack);
                return true;
            }
        }
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.items.size(); ++i) {
            if (inv.items.get(i).isEmpty()) {
                inv.items.set(i, stack.copy());
                return true;
            }
        }
        return false;
    }

    public static boolean addPotionEffectNoEvent(LivingEntity entity, MobEffectInstance effectInstanceIn) {

        if (!isPotionApplicableNoEvent(entity, effectInstanceIn)) {
            return false;
        } else {
            MobEffectInstance effectinstance = entity.getActiveEffectsMap().get(effectInstanceIn.getEffect());
            if (effectinstance == null) {
                entity.getActiveEffectsMap().put(effectInstanceIn.getEffect(), effectInstanceIn);
                entity.onEffectAdded(effectInstanceIn, null);
                return true;
            } else if (effectinstance.update(effectInstanceIn)) {
                entity.onEffectUpdated(effectinstance, true, null);
                return true;
            } else {
                return false;
            }
        }
    }

    public static boolean isPotionApplicableNoEvent(LivingEntity entity, MobEffectInstance potioneffectIn) {

        if (entity.getMobType() == MobType.UNDEAD) {
            MobEffect effect = potioneffectIn.getEffect();
            return effect != MobEffects.REGENERATION && effect != MobEffects.POISON;
        }
        return true;
    }

    public static boolean dropItemStackIntoWorld(ItemStack stack, Level world, Vec3 pos) {

        return dropItemStackIntoWorld(stack, world, pos, false);
    }

    public static boolean dropItemStackIntoWorldWithRandomness(ItemStack stack, Level world, BlockPos pos) {

        return dropItemStackIntoWorld(stack, world, Vec3.atCenterOf(pos), true);
    }

    public static boolean dropItemStackIntoWorldWithRandomness(ItemStack stack, Level world, Vec3 pos) {

        return dropItemStackIntoWorld(stack, world, pos, true);
    }

    public static boolean dropItemStackIntoWorld(ItemStack stack, Level world, Vec3 pos, boolean velocity) {

        if (stack.isEmpty()) {
            return false;
        }
        float x2 = 0.5F;
        float y2 = 0.0F;
        float z2 = 0.5F;

        if (velocity) {
            x2 = world.random.nextFloat() * 0.8F + 0.1F;
            y2 = world.random.nextFloat() * 0.8F + 0.1F;
            z2 = world.random.nextFloat() * 0.8F + 0.1F;
        }
        ItemEntity entity = new ItemEntity(world, pos.x + x2, pos.y + y2, pos.z + z2, stack.copy());

        if (velocity) {
            entity.setDeltaMovement(world.random.nextGaussian() * 0.05F, world.random.nextGaussian() * 0.05F + 0.2F, world.random.nextGaussian() * 0.05F);
        } else {
            entity.setDeltaMovement(-0.05, 0, 0);
        }
        world.addFreshEntity(entity);

        return true;
    }

    public static boolean dropDismantleStackIntoWorld(ItemStack stack, Level world, BlockPos pos) {

        if (stack.isEmpty()) {
            return false;
        }
        float f = 0.3F;
        double x2 = world.random.nextFloat() * f + (1.0F - f) * 0.5D;
        double y2 = world.random.nextFloat() * f + (1.0F - f) * 0.5D;
        double z2 = world.random.nextFloat() * f + (1.0F - f) * 0.5D;
        ItemEntity dropEntity = new ItemEntity(world, pos.getX() + x2, pos.getY() + y2, pos.getZ() + z2, stack);
        dropEntity.setPickUpDelay(10);
        world.addFreshEntity(dropEntity);

        return true;
    }

    public static boolean teleportEntityTo(Entity entity, BlockPos pos) {

        return teleportEntityTo(entity, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
    }

    public static boolean teleportEntityTo(Entity entity, double x, double y, double z) {

        if (entity instanceof LivingEntity) {
            return teleportEntityTo((LivingEntity) entity, x, y, z);
        } else {
            entity.moveTo(x, y, z, entity.getYRot(), entity.getXRot());
            entity.playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0F, 1.0F);
        }
        return true;
    }

    public static boolean teleportEntityTo(LivingEntity entity, double x, double y, double z) {

        EntityTeleportEvent.EnderEntity event = new EntityTeleportEvent.EnderEntity(entity, x, y, z);
        if (MinecraftForge.EVENT_BUS.post(event)) {
            return false;
        }
        if (entity instanceof ServerPlayer player && !isFakePlayer(entity)) {
            if (player.connection.getConnection().isConnected() && !player.isSleeping()) {
                if (entity.isPassenger()) {
                    entity.stopRiding();
                }
                entity.teleportTo(event.getTargetX(), event.getTargetY(), event.getTargetZ());
                entity.fallDistance = 0.0F;
            }
        } else {
            if (entity.isPassenger()) {
                entity.stopRiding();
            }
            entity.teleportTo(event.getTargetX(), event.getTargetY(), event.getTargetZ());
            entity.fallDistance = 0.0F;
        }
        entity.playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0F, 1.0F);
        return true;
    }
    // endregion

    // region ENCHANT UTILS
    public static Enchantment getEnchantment(String modId, String enchantId) {

        return ForgeRegistries.ENCHANTMENTS.getValue(new ResourceLocation(modId, enchantId));
    }

    public static int getEnchantedCapacity(int amount, int holding) {

        return MathHelper.clamp(amount + amount * holding / 2, 0, MAX_CAPACITY);
    }

    public static int getItemEnchantmentLevel(Enchantment ench, ItemStack stack) {

        if (ench == null || ench instanceof EnchantmentCoFH && !((EnchantmentCoFH) ench).isEnabled()) {
            return 0;
        }
        return EnchantmentHelper.getItemEnchantmentLevel(ench, stack);
    }

    public static int getHeldEnchantmentLevel(LivingEntity living, Enchantment ench) {

        if (ench == null || ench instanceof EnchantmentCoFH && !((EnchantmentCoFH) ench).isEnabled()) {
            return 0;
        }
        return Math.max(EnchantmentHelper.getItemEnchantmentLevel(ench, living.getMainHandItem()), EnchantmentHelper.getItemEnchantmentLevel(ench, living.getOffhandItem()));
    }

    public static int getMaxEquippedEnchantmentLevel(LivingEntity living, Enchantment ench) {

        if (ench == null || ench instanceof EnchantmentCoFH && !((EnchantmentCoFH) ench).isEnabled()) {
            return 0;
        }
        return getEnchantmentLevel(ench, living);
    }

    public static void addEnchantment(ItemStack stack, Enchantment ench, int level) {

        stack.enchant(ench, level);
    }

    public static void removeEnchantment(ItemStack stack, Enchantment ench) {

        if (stack.getTag() == null || !stack.getTag().contains(TAG_ENCHANTMENTS, TAG_LIST)) {
            return;
        }
        ListTag list = stack.getTag().getList(TAG_ENCHANTMENTS, TAG_COMPOUND);
        String encId = String.valueOf(ForgeRegistries.ENCHANTMENTS.getKey(ench));

        for (int i = 0; i < list.size(); ++i) {
            CompoundTag tag = list.getCompound(i);
            String id = tag.getString("id");
            if (encId.equals(id)) {
                list.remove(i);
                break;
            }
        }
        if (list.isEmpty()) {
            stack.removeTagKey(TAG_ENCHANTMENTS);
        }
    }
    // endregion

    // region REGISTRY NAME
    public static ResourceLocation getRegistryName(Block block) {

        return ForgeRegistries.BLOCKS.getKey(block);
    }

    public static ResourceLocation getRegistryName(Item item) {

        return ForgeRegistries.ITEMS.getKey(item);
    }

    public static ResourceLocation getRegistryName(Fluid fluid) {

        return ForgeRegistries.FLUIDS.getKey(fluid);
    }

    public static ResourceLocation getRegistryName(EntityType entity) {

        return ForgeRegistries.ENTITY_TYPES.getKey(entity);
    }

    public static ResourceLocation getRegistryName(MobEffect effect) {

        return ForgeRegistries.MOB_EFFECTS.getKey(effect);
    }
    // endregion

    // region NAMESPACE
    public static String getModId(Block block) {

        ResourceLocation loc = getRegistryName(block);
        return loc == null ? "" : loc.getNamespace();
    }

    public static String getName(Block block) {

        ResourceLocation loc = getRegistryName(block);
        return loc == null ? "" : loc.getPath();
    }

    public static String getModId(Item item) {

        ResourceLocation loc = getRegistryName(item);
        return loc == null ? "" : loc.getNamespace();
    }

    public static String getModId(ItemStack stack) {

        ResourceLocation loc = getRegistryName(stack.getItem());
        return loc == null ? "" : loc.getNamespace();
    }

    public static String getName(Item item) {

        ResourceLocation loc = getRegistryName(item);
        return loc == null ? "" : loc.getPath();
    }

    public static String getName(ItemStack stack) {

        ResourceLocation loc = getRegistryName(stack.getItem());
        return loc == null ? "" : loc.getPath();
    }

    public static String getModId(Fluid fluid) {

        ResourceLocation loc = getRegistryName(fluid);
        return loc == null ? "" : loc.getNamespace();
    }

    public static String getModId(FluidStack stack) {

        ResourceLocation loc = getRegistryName(stack.getFluid());
        return loc == null ? "" : loc.getNamespace();
    }

    public static String getName(Fluid fluid) {

        ResourceLocation loc = getRegistryName(fluid);
        return loc == null ? "" : loc.getPath();
    }

    public static String getName(FluidStack stack) {

        ResourceLocation loc = getRegistryName(stack.getFluid());
        return loc == null ? "" : loc.getPath();
    }
    // endregion
}
