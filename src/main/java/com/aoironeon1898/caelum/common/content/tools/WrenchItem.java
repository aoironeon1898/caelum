package com.aoironeon1898.caelum.common.content.tools;

import com.aoironeon1898.caelum.common.content.logistics.blocks.CompositePipeBlock;
import com.aoironeon1898.caelum.common.content.logistics.blocks.EnumPipeMode;
import com.aoironeon1898.caelum.common.content.logistics.tile.CompositePipeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities; // ★追加

public class WrenchItem extends Item {
    public WrenchItem(Properties properties) { super(properties); }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        Player player = context.getPlayer();

        if (state.getBlock() instanceof CompositePipeBlock) {
            if (!level.isClientSide) {
                BlockEntity be = level.getBlockEntity(pos);
                if (!(be instanceof CompositePipeBlockEntity pipe)) return InteractionResult.FAIL;

                Vec3 hitVec = context.getClickLocation().subtract(pos.getX(), pos.getY(), pos.getZ());
                Direction targetDir = getTargetDirection(hitVec, context.getClickedFace());

                if (targetDir != null) {
                    EnumPipeMode currentMode = pipe.getMode(targetDir);

                    // --- ★ここから修正: 隣接ブロックの判定ロジック ---
                    BlockPos neighborPos = pos.relative(targetDir);
                    BlockState neighborState = level.getBlockState(neighborPos);
                    BlockEntity neighborBe = level.getBlockEntity(neighborPos);

                    boolean isNeighborPipe = neighborState.getBlock() instanceof CompositePipeBlock;
                    boolean hasInventory = neighborBe != null && neighborBe.getCapability(ForgeCapabilities.ITEM_HANDLER, targetDir.getOpposite()).isPresent();

                    // 何も接続できない場所なら失敗
                    if (!isNeighborPipe && !hasInventory) {
                        return InteractionResult.FAIL;
                    }

                    EnumPipeMode nextMode = EnumPipeMode.NORMAL;

                    // パターンA: 相手がパイプの場合 -> 接続(NORMAL) か 切断(NONE) のみ
                    if (isNeighborPipe) {
                        if (currentMode == EnumPipeMode.NONE || currentMode == EnumPipeMode.DISABLED) {
                            nextMode = EnumPipeMode.NORMAL; // 接続
                        } else {
                            nextMode = EnumPipeMode.DISABLED; // 切断
                        }
                    }
                    // パターンB: 相手がインベントリ(チェスト等)の場合 -> 全モード切替
                    else {
                        switch (currentMode) {
                            case NONE:    nextMode = EnumPipeMode.NORMAL; break;
                            case NORMAL:  nextMode = EnumPipeMode.PROVIDE; break; // 搬出
                            case PROVIDE: nextMode = EnumPipeMode.REQUEST; break; // 搬入
                            case REQUEST: nextMode = EnumPipeMode.DISABLED; break; // 切断
                            case DISABLED:nextMode = EnumPipeMode.NORMAL; break;
                            default:      nextMode = EnumPipeMode.NORMAL; break;
                        }
                    }
                    // ---------------------------------------------------

                    pipe.setMode(targetDir, nextMode);

                    float pitch = (nextMode == EnumPipeMode.DISABLED || nextMode == EnumPipeMode.NONE) ? 0.5f : 1.5f;
                    level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 1.0f, pitch);

                    if (player != null) {
                        String color = switch (nextMode) {
                            case PROVIDE -> "§6";
                            case REQUEST -> "§9";
                            case DISABLED, NONE -> "§c";
                            default -> "§7";
                        };
                        String text = (nextMode == EnumPipeMode.DISABLED || nextMode == EnumPipeMode.NONE) ? "DISCONNECTED" : nextMode.name();
                        player.sendSystemMessage(Component.literal(targetDir.getName().toUpperCase() + ": " + color + text));
                    }
                    return InteractionResult.SUCCESS;
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return super.useOn(context);
    }

    private Direction getTargetDirection(Vec3 hitVec, Direction face) {
        double x = hitVec.x, y = hitVec.y, z = hitVec.z;
        if (x < 0.25) return Direction.WEST;
        if (x > 0.75) return Direction.EAST;
        if (y < 0.25) return Direction.DOWN;
        if (y > 0.75) return Direction.UP;
        if (z < 0.25) return Direction.NORTH;
        if (z > 0.75) return Direction.SOUTH;
        return face;
    }
}