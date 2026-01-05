package com.aoironeon1898.caelum.common.network.packets;

import com.aoironeon1898.caelum.common.content.logistics.entities.CompositePipeBlockEntity;
import com.aoironeon1898.caelum.common.content.logistics.entities.modules.IPipeModule;
import com.aoironeon1898.caelum.common.content.logistics.entities.modules.ModuleType;
import com.aoironeon1898.caelum.common.content.logistics.entities.modules.item.ItemPipeModule;
import com.aoironeon1898.caelum.common.content.logistics.entities.modules.item.RoutingRule;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class PacketSyncDuct {
    private final BlockPos pos;
    private final Direction direction; // どの方角の設定か
    private final int actionType;      // 0:追加, 1:削除, 2:更新
    private final int ruleIndex;       // 何番目のルールか
    private final CompoundTag data;    // ルールの内容(NBT)

    public static final int ACTION_ADD = 0;
    public static final int ACTION_REMOVE = 1;
    public static final int ACTION_UPDATE = 2;

    public PacketSyncDuct(BlockPos pos, Direction direction, int actionType, int ruleIndex, CompoundTag data) {
        this.pos = pos;
        this.direction = direction;
        this.actionType = actionType;
        this.ruleIndex = ruleIndex;
        this.data = data;
    }

    // 書き込み
    public static void encode(PacketSyncDuct msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeEnum(msg.direction);
        buf.writeInt(msg.actionType);
        buf.writeInt(msg.ruleIndex);
        buf.writeNbt(msg.data);
    }

    // 読み込み
    public static PacketSyncDuct decode(FriendlyByteBuf buf) {
        return new PacketSyncDuct(buf.readBlockPos(), buf.readEnum(Direction.class), buf.readInt(), buf.readInt(), buf.readNbt());
    }

    // 処理実行 (Server Side)
    public static void handle(PacketSyncDuct msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            Level level = player.level();
            if (level.isLoaded(msg.pos)) {
                BlockEntity be = level.getBlockEntity(msg.pos);
                if (be instanceof CompositePipeBlockEntity pipe) {
                    // Itemモジュールを取得するためにリフレクションか、Getterを使う
                    // 今回はモジュールリストへのアクセス手段が必要ですが、簡易的にリフレクションでMapにアクセスするか、
                    // CompositePipeBlockEntityに `getModule(ModuleType)` を追加するのが正攻法です。
                    // ★ここでは、CompositePipeBlockEntityに以下のメソッドを追加したと仮定して呼び出します。
                    // IPipeModule module = pipe.getModule(ModuleType.ITEM);

                    // ※仮対応として、リフレクションで強引にmodulesフィールドを取ります
                    // (実実装ではGetterを作ってください)
                    try {
                        Field modulesField = CompositePipeBlockEntity.class.getDeclaredField("modules");
                        modulesField.setAccessible(true);
                        @SuppressWarnings("unchecked")
                        Map<ModuleType, IPipeModule> modules = (Map<ModuleType, IPipeModule>) modulesField.get(pipe);

                        IPipeModule module = modules.get(ModuleType.ITEM);
                        if (module instanceof ItemPipeModule itemModule) {
                            processAction(itemModule, msg, pipe);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }

    private static void processAction(ItemPipeModule module, PacketSyncDuct msg, CompositePipeBlockEntity pipe) {
        // 更新対象のリストを取得 (搬出:Output か 吸入:Input かは、GUI側で制御して送る方向を決める)
        // 今回の設計ではGUIのタブで切り替えますが、Packetには「今どっちのモードか」の情報が必要です。
        // 簡易化のため、「データの中にモード情報を含める」か、一旦「搬出ルール(Delivery)」のみ対象にします。

        // ★設計補足: 実はGUIでInput/Outputタブを切り替えるので、Packetにも「リスト種別」が必要でした。
        // ここでは「NBTのタグ」で判断するロジックにします。
        boolean isExtraction = msg.data.getBoolean("IsExtraction");
        List<RoutingRule> rules = isExtraction ? module.extractionRules.get(msg.direction) : module.deliveryRules.get(msg.direction);

        if (rules == null) return;

        switch (msg.actionType) {
            case ACTION_ADD:
                rules.add(new RoutingRule());
                break;

            case ACTION_REMOVE:
                if (msg.ruleIndex >= 0 && msg.ruleIndex < rules.size()) {
                    rules.remove(msg.ruleIndex);
                }
                break;

            case ACTION_UPDATE:
                if (msg.ruleIndex >= 0 && msg.ruleIndex < rules.size()) {
                    RoutingRule rule = rules.get(msg.ruleIndex);
                    // NBTから設定を復元
                    rule.deserializeNBT(msg.data);
                }
                break;
        }

        pipe.setChanged();
        // 必要なら周辺へブロック更新通知
        pipe.getLevel().sendBlockUpdated(pipe.getBlockPos(), pipe.getBlockState(), pipe.getBlockState(), 3);
    }
}