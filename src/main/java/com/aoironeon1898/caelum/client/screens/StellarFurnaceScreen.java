package com.aoironeon1898.caelum.client.screens;

import com.aoironeon1898.caelum.Caelum;
import com.aoironeon1898.caelum.common.menus.StellarFurnaceMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

// ★変更1: 親クラスを BaseMachineScreen に変更
public class StellarFurnaceScreen extends BaseMachineScreen<StellarFurnaceMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Caelum.MODID, "textures/gui/stellar_furnace.png");

    public StellarFurnaceScreen(StellarFurnaceMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        // 1. 背景を描画
        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        // ★変更2: 親クラスの便利メソッドを使って「1行」で書く！
        // 引数: graphics, テクスチャ, x, y, u, v, 幅, 高さ, 現在値, 最大値

        // エネルギーバー (あなたの座標設定: x+12, y+16, w8, h52, u176, v10)
        this.renderEnergyBar(guiGraphics, TEXTURE,
                12, 16, 176, 10, 8, 52,
                menu.getEnergy(), menu.getMaxEnergy());

        // 矢印 (あなたの座標設定: x+76, y+37, u176, v0, 高さ8)
        // ※幅は矢印の最大サイズ(通常24px程度)を指定します。ここでは仮に24としています
        this.renderProgressArrow(guiGraphics, TEXTURE,
                76, 37, 176, 0, 30, 16,
                menu.getProgress(), menu.getMaxProgress());
    }

    // ★ renderEnergyBar や renderProgressArrow の定義は削除しました
    // (親クラス BaseMachineScreen にあるものを使います)

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        // 親クラスの render を呼べば、背景描画やツールチップも自動でやってくれます
        super.render(guiGraphics, mouseX, mouseY, delta);
    }
}