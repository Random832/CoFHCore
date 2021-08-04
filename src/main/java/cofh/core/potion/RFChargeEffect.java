package cofh.core.potion;

import cofh.lib.potion.EffectCoFH;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.EffectType;
import net.minecraftforge.energy.CapabilityEnergy;

public class RFChargeEffect extends EffectCoFH {

    private int chargeAmount;

    public RFChargeEffect(EffectType typeIn, int liquidColorIn, int chargeAmount) {

        super(typeIn, liquidColorIn);
        this.chargeAmount = chargeAmount;
    }

    @Override
    public void performEffect(LivingEntity entityLivingBaseIn, int amplifier) {

        if (entityLivingBaseIn instanceof ServerPlayerEntity) {
            ServerPlayerEntity entity = (ServerPlayerEntity) entityLivingBaseIn;

            if (chargeAmount <= 0) {
                // Main Inventory
                for (ItemStack stack : entity.inventory.mainInventory) {
                    stack.getCapability(CapabilityEnergy.ENERGY, null)
                            .ifPresent(c -> c.extractEnergy(chargeAmount, false));
                }
                // Armor Inventory
                for (ItemStack stack : entity.inventory.armorInventory) {
                    stack.getCapability(CapabilityEnergy.ENERGY, null)
                            .ifPresent(c -> c.extractEnergy(chargeAmount, false));
                }
                // Offhand
                for (ItemStack stack : entity.inventory.offHandInventory) {
                    stack.getCapability(CapabilityEnergy.ENERGY, null)
                            .ifPresent(c -> c.extractEnergy(chargeAmount, false));
                }
            } else {
                // Main Inventory
                for (ItemStack stack : entity.inventory.mainInventory) {
                    stack.getCapability(CapabilityEnergy.ENERGY, null)
                            .ifPresent(c -> c.receiveEnergy(chargeAmount, false));
                }
                // Armor Inventory
                for (ItemStack stack : entity.inventory.armorInventory) {
                    stack.getCapability(CapabilityEnergy.ENERGY, null)
                            .ifPresent(c -> c.receiveEnergy(chargeAmount, false));
                }
                // Offhand
                for (ItemStack stack : entity.inventory.offHandInventory) {
                    stack.getCapability(CapabilityEnergy.ENERGY, null)
                            .ifPresent(c -> c.receiveEnergy(chargeAmount, false));
                }
            }
        }
    }

}
