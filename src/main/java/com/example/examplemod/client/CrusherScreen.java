package com.example.examplemod.client;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.menu.CrusherMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class CrusherScreen extends AbstractContainerScreen<CrusherMenu> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(ExampleMod.MODID, "textures/gui/crusher.png");

    public CrusherScreen(CrusherMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 8;
        this.titleLabelY = 6;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.setShaderTexture(0, TEXTURE);

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        graphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        if (menu.isCrafting()) {
            int progress = menu.getScaledProgress();
            graphics.blit(TEXTURE, x + 79, y + 35, 176, 14, progress, 17);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, delta);
        renderTooltip(graphics, mouseX, mouseY);

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        String lifeText = Component.translatable("gui.examplemod.life",
                menu.getRemainingLife(), menu.getMaxLife()).getString();
        graphics.drawString(this.font, lifeText, x + 8, y + 58, 0x404040, false);
        String energyText = Component.translatable("gui.examplemod.energy",
                menu.getEnergyStored(), menu.getMaxEnergyStored()).getString();
        graphics.drawString(this.font, energyText, x + 8, y + 68, 0x404040, false);
    }
}
