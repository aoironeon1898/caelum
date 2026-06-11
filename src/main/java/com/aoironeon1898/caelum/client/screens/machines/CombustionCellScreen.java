package com.aoironeon1898.caelum.client.screens.machines;

import com.aoironeon1898.caelum.Caelum;
import com.aoironeon1898.caelum.common.content.machines.menus.CombustionCellMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Combustion Cell GUI。stellar_furnace.png を流用（専用テクスチャは後で差し替え）。
 * 左にFEバー、中央の燃料スロット下に燃焼ゲージ。
 */
public class CombustionCellScreen extends BaseMachineScreen<CombustionCellMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Caelum.MODID, "textures/gui/stellar_furnace.png");

    public CombustionCellScreen(CombustionCellMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    @Override
    protected int getTitleLabelX() { return 8; }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        // 背景
        g.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        // FEバー（左）
        this.renderEnergyBar(g, TEXTURE, 12, 16, 176, 8, 8, 52);

        // 燃焼ゲージ：残り燃焼割合を炎スロット下のバーとして横に表示（progressArrow流用）
        this.renderProgressArrow(g, TEXTURE, 76, 37, 176, 0, 30, 8);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        super.render(g, mouseX, mouseY, delta);
    }
}
