package com.aoironeon1898.caelum.client.screens;

import com.aoironeon1898.caelum.Caelum;
import com.aoironeon1898.caelum.common.menus.StellarFurnaceMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

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

        this.renderEnergyBar(guiGraphics, TEXTURE,
                12, 16, 176, 8, 8, 52);

        this.renderProgressArrow(guiGraphics, TEXTURE,
                76, 37, 176, 0, 30, 8);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        super.render(guiGraphics, mouseX, mouseY, delta);
    }
}