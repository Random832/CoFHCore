package cofh.core.inventory.container;

import cofh.core.network.packet.client.ContainerGuiPacket;
import cofh.core.network.packet.server.ContainerConfigPacket;
import cofh.core.util.filter.BaseFluidFilter;
import cofh.core.util.filter.IFilterOptions;
import cofh.core.util.filter.IFilterableItem;
import cofh.core.util.filter.IFilterableTile;
import cofh.core.util.helpers.FilterHelper;
import cofh.lib.inventory.container.slot.SlotFalseCopy;
import cofh.lib.inventory.container.slot.SlotLocked;
import cofh.lib.inventory.wrapper.InvWrapperFluids;
import cofh.lib.util.helpers.MathHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;

import static cofh.core.init.CoreContainers.FLUID_FILTER_CONTAINER;
import static cofh.core.util.helpers.FilterHelper.hasFilter;

public class FluidFilterContainer extends ContainerCoFH implements IFilterOptions {

    protected BlockEntity tile;
    protected IFilterableTile filterableTile;

    protected ItemStack filterStack;
    protected IFilterableItem filterableItem;
    public SlotLocked lockedSlot;

    protected BaseFluidFilter filter;
    protected InvWrapperFluids filterInventory;

    public final boolean held;

    public FluidFilterContainer(int windowId, Level world, Inventory inventory, Player player, boolean held, BlockPos pos) {

        super(FLUID_FILTER_CONTAINER.get(), windowId, inventory, player);

        this.held = held;

        if (held) {
            filterStack = hasFilter(player.getMainHandItem()) ? player.getMainHandItem() : player.getOffhandItem();
            filterableItem = (IFilterableItem) filterStack.getItem();
            filter = (BaseFluidFilter) filterableItem.getFilter(filterStack);
        } else {
            tile = world.getBlockEntity(pos);
            filterableTile = (IFilterableTile) tile;
            filter = (BaseFluidFilter) filterableTile.getFilter();
        }
        allowSwap = false;

        int slots = filter.size();
        filterInventory = new InvWrapperFluids(this, filter.getFluids(), slots) {
            @Override
            public void setChanged() {

                filter.setFluids(filterInventory.getStacks());
            }
        };

        int rows = MathHelper.clamp(slots / 3, 1, 3);
        int rowSize = slots / rows;

        int xOffset = 62 - 9 * rowSize;
        int yOffset = 44 - 9 * rows;

        for (int i = 0; i < filter.size(); ++i) {
            addSlot(new SlotFalseCopy(filterInventory, i, xOffset + i % rowSize * 18, yOffset + i / rowSize * 18));
        }
        bindPlayerInventory(inventory);
    }

    @Override
    protected void bindPlayerInventory(Inventory inventory) {

        if (held) {
            int xOffset = getPlayerInventoryHorizontalOffset();
            int yOffset = getPlayerInventoryVerticalOffset();

            for (int i = 0; i < 3; ++i) {
                for (int j = 0; j < 9; ++j) {
                    addSlot(new Slot(inventory, j + i * 9 + 9, xOffset + j * 18, yOffset + i * 18));
                }
            }
            for (int i = 0; i < 9; ++i) {
                if (i == inventory.selected) {
                    lockedSlot = new SlotLocked(inventory, i, xOffset + i * 18, yOffset + 58);
                    addSlot(lockedSlot);
                } else {
                    addSlot(new Slot(inventory, i, xOffset + i * 18, yOffset + 58));
                }
            }
        } else {
            super.bindPlayerInventory(inventory);
        }
    }

    public IFilterableTile getFilterableTile() {

        return filterableTile;
    }

    public int getFilterSize() {

        return filter.size();
    }

    public List<FluidStack> getFilterStacks() {

        return filterInventory.getStacks();
    }

    @Override
    protected int getMergeableSlotCount() {

        return filterInventory.getContainerSize();
    }

    @Override
    public boolean stillValid(Player player) {

        if (held) {
            return lockedSlot.getItem() == filterStack;
        }
        if (!FilterHelper.hasFilter(filterableTile)) {
            return false;
        }
        return tile != null && !tile.isRemoved() && tile.getBlockPos().distToCenterSqr(player.position()) <= 64D;
    }

    @Override
    public void broadcastChanges() {

        super.broadcastChanges();
        ContainerGuiPacket.sendToClient(this, player);
    }

    @Override
    public void removed(Player playerIn) {

        filter.setFluids(filterInventory.getStacks());

        if (held) {
            filter.write(filterStack.getOrCreateTag());
            filterableItem.onFilterChanged(filterStack);
        } else {
            filterableTile.onFilterChanged();
        }
        super.removed(playerIn);
    }

    // region NETWORK
    @Override
    public FriendlyByteBuf getConfigPacket(FriendlyByteBuf buffer) {

        buffer.writeBoolean(getAllowList());
        buffer.writeBoolean(getCheckNBT());

        return buffer;
    }

    @Override
    public void handleConfigPacket(FriendlyByteBuf buffer) {

        filter.setAllowList(buffer.readBoolean());
        filter.setCheckNBT(buffer.readBoolean());
    }

    @Override
    public FriendlyByteBuf getGuiPacket(FriendlyByteBuf buffer) {

        byte size = (byte) filter.getFluids().size();
        buffer.writeByte(size);
        for (int i = 0; i < size; ++i) {
            buffer.writeFluidStack(getFilterStacks().get(i));
        }
        return buffer;
    }

    @Override
    public void handleGuiPacket(FriendlyByteBuf buffer) {

        byte size = buffer.readByte();
        List<FluidStack> fluidStacks = new ArrayList<>(size);
        for (int i = 0; i < size; ++i) {
            fluidStacks.add(buffer.readFluidStack());
        }
        filterInventory.readFromSource(fluidStacks);
    }
    // endregion

    // region IFilterOptions
    @Override
    public boolean getAllowList() {

        return filter.getAllowList();
    }

    @Override
    public boolean setAllowList(boolean allowList) {

        boolean ret = filter.setAllowList(allowList);
        ContainerConfigPacket.sendToServer(this);
        return ret;
    }

    @Override
    public boolean getCheckNBT() {

        return filter.getCheckNBT();
    }

    @Override
    public boolean setCheckNBT(boolean checkNBT) {

        boolean ret = filter.setCheckNBT(checkNBT);
        ContainerConfigPacket.sendToServer(this);
        return ret;
    }
    // endregion
}
