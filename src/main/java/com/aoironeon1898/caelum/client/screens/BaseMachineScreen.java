package com.aoironeon1898.caelum.client.screens;

import com.aoironeon1898.caelum.common.menus.BaseMachineMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public abstract class BaseMachineScreen<T extends BaseMachineMenu> extends AbstractContainerScreen<T> {

    public BaseMachineScreen(T menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        // ★ここを変更！
        // 「座標を計算するメソッド」の結果を代入するようにします。
        this.titleLabelX = getTitleLabelX();
        this.titleLabelY = getTitleLabelY();
    }

    // ★新しいメソッド1：X座標（横）を決める
    // デフォルト（何も書かない場合）は「中央揃え」を返します
    protected int getTitleLabelX() {
        return (this.imageWidth - this.font.width(this.title)) / 2;
    }

    // ★新しいメソッド2：Y座標（縦）を決める
    // デフォルトは「6」（マイクラ標準の位置）
    protected int getTitleLabelY() {
        return 6;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    // ... (renderEnergyBar などの他のメソッドはそのまま) ...

    protected void renderEnergyBar(GuiGraphics graphics, ResourceLocation texture, int x, int y, int u, int v, int width, int height) {
        int energy = menu.getEnergy();
        int maxEnergy = menu.getMaxEnergy();

        if (maxEnergy > 0) {
            int scaledHeight = (int)((float)energy / maxEnergy * height);
            graphics.blit(texture, this.leftPos + x, this.topPos + y + (height - scaledHeight), u, v + (height - scaledHeight), width, scaledHeight);
        }
    }

    protected void renderProgressArrow(GuiGraphics graphics, ResourceLocation texture, int x, int y, int u, int v, int width, int height) {
        int progress = menu.getProgress();
        int maxProgress = menu.getMaxProgress();

        if (maxProgress > 0 && progress > 0) {
            int scaledWidth = progress * width / maxProgress;
            graphics.blit(texture, this.leftPos + x, this.topPos + y, u, v, scaledWidth, height);
        }
    }
}