package com.aoironeon1898.caelum.client.screens.machines;

import com.aoironeon1898.caelum.Caelum;
import com.aoironeon1898.caelum.common.content.machines.menus.StellarCrusherMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class StellarCrusherScreen extends BaseMachineScreen<StellarCrusherMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Caelum.MODID, "textures/gui/stellar_crusher.png");

    public StellarCrusherScreen(StellarCrusherMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
    }

    // ★★★ ここでタイトルの位置を調整できます！ ★★★
    @Override
    protected int getTitleLabelX() {
        // 例: 左端に寄せたい場合は「8」などを返します
        // 中央揃えがいい場合は、このメソッドごと消せば親クラスの標準設定(中央)になります
        return 8;
    }

    // Y座標（高さ）を変えたい場合
    /*
    @Override
    protected int getTitleLabelY() {
        return 10;
    }
    */

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        // 背景
        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        // エネルギーバー
        this.renderEnergyBar(guiGraphics, TEXTURE,
                12, 16, 176, 8, 8, 52);

        // 矢印
        this.renderProgressArrow(guiGraphics, TEXTURE,
                76, 37, 176, 0, 30, 8);

        // 粉砕機のアニメーション
        renderCrusherAnimation(guiGraphics, x, y);
    }

    private void renderCrusherAnimation(GuiGraphics guiGraphics, int x, int y) {
        if (menu.isCrafting()) {
            int loopSpeed = 4;
            int maxFrameIndex = 3;

            int cycleLength = maxFrameIndex * 2;

            int partX = x + 77;
            int partY = y + 10;
            int partWidth = 26;
            int partHeight = 27;

            // ★重要修正ポイント:
            // 矢印画像が v=0 にあるので、アニメ用の絵が v=0 だと被ってしまいます。
            // テクスチャ画像を見て、アニメ用の絵を描いたY座標を指定してください。
            // (例: 矢印の下に描いたなら 16 など)
            int textureU = 176;
            int textureV = 62; // ← 画像に合わせてここを書き換えてください！
            // ---------------------------------------------------

            long gameTime = this.minecraft.level.getGameTime();
            int step = (int)((gameTime / loopSpeed) % cycleLength);

            int currentFrame = step;
            if (step > maxFrameIndex) {
                currentFrame = cycleLength - step;
            }

            guiGraphics.blit(TEXTURE,
                    partX, partY,
                    textureU, textureV + (currentFrame * partHeight),
                    partWidth, partHeight);
        }
    }
}