package cofh.core.network.packet.server;

import cofh.core.CoFHCore;
import cofh.core.util.filter.IFilterableTile;
import cofh.lib.network.packet.IPacketServer;
import cofh.lib.network.packet.PacketBase;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import static cofh.core.network.packet.PacketIDs.PACKET_FILTERABLE_GUI_OPEN;

public class TileFilterGuiOpenPacket extends PacketBase implements IPacketServer {

    public static byte TILE = 0;
    public static byte FILTER = 1;

    protected BlockPos pos;
    protected byte mode;

    public TileFilterGuiOpenPacket() {

        super(PACKET_FILTERABLE_GUI_OPEN, CoFHCore.PACKET_HANDLER);
    }

    @Override
    public void handleServer(ServerPlayer player) {

        Level world = player.level;
        if (!world.isLoaded(pos)) {
            return;
        }
        BlockEntity tile = world.getBlockEntity(pos);
        if (tile instanceof IFilterableTile filterable) {
            if (mode == TILE) {
                filterable.openGui(player);
            } else if (mode == FILTER) {
                filterable.openFilterGui(player);
            }
        }
    }

    @Override
    public void write(FriendlyByteBuf buf) {

        buf.writeBlockPos(pos);
        buf.writeByte(mode);
    }

    @Override
    public void read(FriendlyByteBuf buf) {

        pos = buf.readBlockPos();
        mode = buf.readByte();
    }

    public static void openFilterGui(IFilterableTile tile) {

        sendToServer(tile, FILTER);
    }

    public static void openTileGui(IFilterableTile tile) {

        sendToServer(tile, TILE);
    }

    protected static void sendToServer(IFilterableTile tile, byte mode) {

        TileFilterGuiOpenPacket packet = new TileFilterGuiOpenPacket();
        packet.pos = tile.pos();
        packet.mode = mode;
        packet.sendToServer();
    }

}
