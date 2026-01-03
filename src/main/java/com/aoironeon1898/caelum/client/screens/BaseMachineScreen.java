package com.aoironeon1898.caelum.client.screens;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

// ★抽象クラス (abstract) として定義
public abstract class BaseMachineScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {

    public BaseMachineScreen(T menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        // タイトルを中央揃えにする計算
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    // エネルギーバーを描画する便利メソッド
    // texture: 画像ファイルの場所, x/y: 画面上の位置, u/v: 画像内の切り抜き位置, width/height: サイズ
    protected void renderEnergyBar(GuiGraphics graphics, ResourceLocation texture, int x, int y, int u, int v, int width, int height, int energy, int maxEnergy) {
        if (maxEnergy > 0) {
            int scaledHeight = (int)((float)energy / maxEnergy * height);
            // 下から上に向かって増えるように描画位置を調整
            graphics.blit(texture, this.leftPos + x, this.topPos + y + (height - scaledHeight), u, v + (height - scaledHeight), width, scaledHeight);
        }
    }

    // 矢印（進行度）を描画する便利メソッド
    protected void renderProgressArrow(GuiGraphics graphics, ResourceLocation texture, int x, int y, int u, int v, int width, int height, int progress, int maxProgress) {
        if (maxProgress > 0) {
            int scaledWidth = progress * width / maxProgress;
            graphics.blit(texture, this.leftPos + x, this.topPos + y, u, v, scaledWidth + 1, height);
        }
    }
}