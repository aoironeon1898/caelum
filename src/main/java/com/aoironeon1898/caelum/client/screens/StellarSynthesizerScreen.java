package com.aoironeon1898.caelum.client.screens;

import com.aoironeon1898.caelum.Caelum;
import com.aoironeon1898.caelum.common.menus.StellarSynthesizerMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

// ★変更1: BaseMachineScreen を継承
public class StellarSynthesizerScreen extends BaseMachineScreen<StellarSynthesizerMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Caelum.MODID, "textures/gui/stellar_synthesizer.png");

    public StellarSynthesizerScreen(StellarSynthesizerMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
    }

    @Override
    protected void init() {
        super.init();
    }

    // ★ここは独自の座標調整（+85など）があるため、そのまま残しました
    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int invX = this.inventoryLabelX + 85;
        int invY = this.inventoryLabelY;
        guiGraphics.drawString(this.font, this.playerInventoryTitle, invX, invY, 4210752, false);

        guiGraphics.pose().pushPose();
        float scale = 1.0f;
        guiGraphics.pose().scale(scale, scale, 1.0f);

        int xPos = (int) (85 / scale);
        int yPos = (int) (5 / scale);
        guiGraphics.drawString(this.font, this.title, xPos, yPos, 4210752, false);
        guiGraphics.pose().popPose();
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        // 背景描画
        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        // ★変更2: 親クラスの便利メソッドを使って1行で描画！

        // エネルギーバー (座標: x+12, y+16, u176, v10, 幅8, 高さ52)
        this.renderEnergyBar(guiGraphics, TEXTURE,
                12, 16, 176, 8, 8, 52);

        // 矢印 (座標: x+76, y+37, u176, v0, 高さ8)
        // ※矢印の横幅は一般的なサイズ「24」を指定しています。画像に合わせて調整してください。
        this.renderProgressArrow(guiGraphics, TEXTURE,
                76, 37, 176, 0, 30, 8);
    }
}