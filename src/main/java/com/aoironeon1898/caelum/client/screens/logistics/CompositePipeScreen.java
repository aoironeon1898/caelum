package com.aoironeon1898.caelum.client.screens.logistics;

import com.aoironeon1898.caelum.common.content.logistics.menus.CompositePipeMenu;
import com.aoironeon1898.caelum.common.content.logistics.entities.CompositePipeBlockEntity;
import com.aoironeon1898.caelum.common.content.logistics.entities.CompositePipeBlockEntity.PipeRule;
import com.aoironeon1898.caelum.common.content.logistics.entities.CompositePipeBlockEntity.IOMode; // ★追加
import com.aoironeon1898.caelum.common.network.ModMessages;
import com.aoironeon1898.caelum.common.network.packets.PacketUpdatePipeRules;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CompositePipeScreen extends AbstractContainerScreen<CompositePipeMenu> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("caelum", "textures/gui/logistics/composite_pipe.png");

    // =========================================================================
    //   レイアウト設定
    // =========================================================================
    private static final int WINDOW_WIDTH = 207;
    private static final int WINDOW_HEIGHT = 285;

    private static final int FILTER_SLOT_X = 16;
    private static final int FILTER_SLOT_Y = 32;

    private static final int TAG_BTN_X = 50;
    private static final int TAG_BTN_Y = 23;
    private static final int TAG_BTN_W = 30;
    private static final int TAG_BTN_H = 18;

    private static final int ADD_BTN_X = 83;
    private static final int ADD_BTN_Y = 23;
    private static final int ADD_BTN_W = 40;
    private static final int ADD_BTN_H = 18;

    private static final int SCROLL_X = 7;
    private static final int SCROLL_Y = 43;
    private static final int SCROLL_W = WINDOW_WIDTH - 46;
    private static final int SCROLL_H = 106;
    private static final int ROW_HEIGHT = 21;

    private static final int SLIDER_OFFSET_Y = 5;
    private static final int SLIDER_HEIGHT = 20;

    private static final int DIR_BTN_Y = -22;
    private static final int DIR_BTN_SIZE = 20;
    private static final int TARGET_X = 150;
    private static final int TARGET_Y = 5;
    private static final int UPG_X = 175;
    private static final int UPG_Y = 45;

    // =========================================================================

    private float scrollOffs = 0.0f;
    private boolean isScrolling = false;
    private Direction selectedDirection = null;
    private final Map<Direction, Button> dirButtons = new EnumMap<>(Direction.class);

    private final Map<Direction, List<RuleEntry>> rulesPerSide = new EnumMap<>(Direction.class);
    private int selectedRuleIndex = -1;

    private EditBox indexEdit;
    private RuleSlider tickSlider;
    private RuleSlider amountSlider;

    private List<TagKey<Item>> currentItemTags = new ArrayList<>();
    private int currentTagIndex = -1;
    private ItemStack lastFilterStack = ItemStack.EMPTY;
    private Button tagButton;

    private List<Component> tooltipToRender = null;
    private ItemStack tooltipStackToRender = ItemStack.EMPTY;

    // クライアント側データクラス
    private static class RuleEntry {
        int sortIndex;
        int tick;
        int amount;
        boolean isFluid;
        ItemStack stack;
        String tagName;
        IOMode mode; // ★モード追加

        RuleEntry(int sortIndex, int tick, int amount, boolean isFluid, ItemStack stack, String tagName, IOMode mode) {
            this.sortIndex = sortIndex;
            this.tick = tick;
            this.amount = amount;
            this.isFluid = isFluid;
            this.stack = stack;
            this.tagName = tagName;
            this.mode = mode != null ? mode : IOMode.INPUT; // デフォルトはINPUT
        }
    }

    public CompositePipeScreen(CompositePipeMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = WINDOW_WIDTH;
        this.imageHeight = WINDOW_HEIGHT;
        this.inventoryLabelY = 193;

        // リスト初期化 & サーバーデータの読み込み
        for (Direction dir : Direction.values()) {
            List<RuleEntry> clientList = new ArrayList<>();
            if (menu.blockEntity != null) {
                List<PipeRule> serverList = menu.blockEntity.rules.get(dir);
                if (serverList != null) {
                    for (PipeRule r : serverList) {
                        // ★モードを読み込む
                        clientList.add(new RuleEntry(r.sortIndex, r.tick, r.amount, r.isFluid, r.filterStack, r.tagName, r.mode));
                    }
                }
            }
            rulesPerSide.put(dir, clientList);
        }
    }

    private List<RuleEntry> getCurrentRules() {
        if (selectedDirection == null) return new ArrayList<>();
        return rulesPerSide.get(this.selectedDirection);
    }

    // ★パケット送信時にモードを含める
    private void sendUpdatePacket() {
        if (this.menu.blockEntity != null && selectedDirection != null) {
            BlockPos pos = this.menu.blockEntity.getBlockPos();
            List<RuleEntry> clientRules = getCurrentRules();

            List<PipeRule> serverRules = new ArrayList<>();
            for (RuleEntry entry : clientRules) {
                // コンストラクタ引数が7つになっているはず
                serverRules.add(new PipeRule(
                        entry.sortIndex, entry.tick, entry.amount,
                        entry.isFluid, entry.stack, entry.tagName, entry.mode
                ));
            }

            ModMessages.INSTANCE.sendToServer(new PacketUpdatePipeRules(pos, selectedDirection, serverRules));
        }
    }

    @Override
    protected void init() {
        super.init();

        List<Direction> validDirections = new ArrayList<>();
        if (this.minecraft.level != null) {
            BlockPos pos = this.menu.blockEntity.getBlockPos();
            for (Direction dir : Direction.values()) {
                BlockState state = this.minecraft.level.getBlockState(pos.relative(dir));
                if (!state.isAir() && state.getBlock() != this.menu.blockEntity.getBlockState().getBlock()) {
                    validDirections.add(dir);
                }
            }
        }
        if (validDirections.isEmpty()) validDirections.add(Direction.UP);

        if (selectedDirection == null || !validDirections.contains(selectedDirection)) {
            selectedDirection = validDirections.get(0);
        }

        int totalBtnWidth = validDirections.size() * (DIR_BTN_SIZE + 2);
        int startX = this.leftPos + (this.imageWidth - totalBtnWidth) / 2;
        int startY = this.topPos + DIR_BTN_Y;

        dirButtons.clear();
        for (int i = 0; i < validDirections.size(); i++) {
            Direction dir = validDirections.get(i);

            ItemStack iconStack = ItemStack.EMPTY;
            if (this.minecraft.level != null) {
                BlockState state = this.minecraft.level.getBlockState(this.menu.blockEntity.getBlockPos().relative(dir));
                if (!state.isAir()) iconStack = new ItemStack(state.getBlock());
            }
            if (iconStack.isEmpty()) iconStack = new ItemStack(Items.BARRIER);

            BlockIconButton btn = new BlockIconButton(startX + (i * (DIR_BTN_SIZE + 2)), startY, DIR_BTN_SIZE, DIR_BTN_SIZE, dir, iconStack, (b) -> {
                this.selectedDirection = dir;
                this.selectedRuleIndex = -1;
                updateEditorsVisibility();
                updateButtons();
            });
            btn.setTooltip(Tooltip.create(
                    Component.literal(dir.getName().toUpperCase()).withStyle(ChatFormatting.GOLD)
                            .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                            .append(iconStack.getHoverName().copy().withStyle(ChatFormatting.WHITE))
            ));

            this.addRenderableWidget(btn);
            dirButtons.put(dir, btn);
        }

        this.tagButton = Button.builder(Component.literal("Tag"), (b) -> cycleTagMode())
                .bounds(this.leftPos + TAG_BTN_X, this.topPos + TAG_BTN_Y, TAG_BTN_W, TAG_BTN_H).build();
        this.tagButton.active = false;
        this.addRenderableWidget(this.tagButton);

        this.addRenderableWidget(Button.builder(Component.literal("Add"), (b) -> addNewRule())
                .bounds(this.leftPos + ADD_BTN_X, this.topPos + ADD_BTN_Y, ADD_BTN_W, ADD_BTN_H).build());

        int areaY = this.topPos + SCROLL_Y + SCROLL_H + SLIDER_OFFSET_Y;

        this.indexEdit = new EditBox(this.font, this.leftPos + SCROLL_X, areaY, 30, SLIDER_HEIGHT, Component.literal("Index"));
        this.indexEdit.setMaxLength(3);
        this.indexEdit.setFilter(s -> s.matches("\\d*"));
        this.indexEdit.setResponder(val -> {
            List<RuleEntry> rules = getCurrentRules();
            if (selectedRuleIndex >= 0 && selectedRuleIndex < rules.size()) {
                if (!val.isEmpty()) {
                    try {
                        rules.get(selectedRuleIndex).sortIndex = Integer.parseInt(val);
                        sendUpdatePacket();
                    } catch (NumberFormatException ignored) {}
                }
            }
        });
        this.addRenderableWidget(this.indexEdit);

        int remainingW = SCROLL_W - 34;
        int sliderW = (remainingW / 2) - 2;
        int slider1X = this.leftPos + SCROLL_X + 34;
        int slider2X = slider1X + sliderW + 4;

        this.tickSlider = new RuleSlider(slider1X, areaY, sliderW, SLIDER_HEIGHT, Component.literal("Tick: "), 1, 100, 20, (val) -> {
            List<RuleEntry> rules = getCurrentRules();
            if (selectedRuleIndex >= 0 && selectedRuleIndex < rules.size()) {
                rules.get(selectedRuleIndex).tick = val;
                sendUpdatePacket();
            }
        });
        this.addRenderableWidget(this.tickSlider);

        this.amountSlider = new RuleSlider(slider2X, areaY, sliderW, SLIDER_HEIGHT, Component.literal("Amt: "), 1, 64, 1, (val) -> {
            List<RuleEntry> rules = getCurrentRules();
            if (selectedRuleIndex >= 0 && selectedRuleIndex < rules.size()) {
                rules.get(selectedRuleIndex).amount = val;
                sendUpdatePacket();
            }
        });
        this.addRenderableWidget(this.amountSlider);

        updateButtons();
        updateEditorsVisibility();
    }

    @Override
    public void containerTick() {
        super.containerTick();
        if (this.indexEdit != null) {
            this.indexEdit.tick();
        }
        if (this.menu.slots.size() > 36) {
            ItemStack stack = this.menu.getSlot(36).getItem();
            if (!ItemStack.matches(stack, lastFilterStack)) {
                this.lastFilterStack = stack.copy();
                updateFilterInfo(stack);
            }
        }
    }

    private void updateFilterInfo(ItemStack stack) {
        if (stack.isEmpty()) {
            this.currentItemTags.clear();
            this.currentTagIndex = -1;
            this.tagButton.active = false;
        } else {
            this.currentItemTags = stack.getTags().collect(Collectors.toList());
            this.currentTagIndex = -1;
            this.tagButton.active = !this.currentItemTags.isEmpty();
        }
    }

    private void cycleTagMode() {
        if (lastFilterStack.isEmpty() || currentItemTags.isEmpty()) return;
        currentTagIndex++;
        if (currentTagIndex >= currentItemTags.size()) currentTagIndex = -1;
    }

    private void addNewRule() {
        if (lastFilterStack.isEmpty()) return;
        if (selectedDirection == null) return;

        String tagName = null;
        ItemStack iconStack = lastFilterStack.copy();

        boolean isFluid = false;
        if (iconStack.getItem() instanceof BucketItem) {
            isFluid = true;
        }

        if (currentTagIndex >= 0 && currentTagIndex < currentItemTags.size()) {
            tagName = "#" + currentItemTags.get(currentTagIndex).location().toString();
        }

        List<RuleEntry> rules = getCurrentRules();
        int nextIndex = rules.isEmpty() ? 1 : rules.stream().mapToInt(r -> r.sortIndex).max().orElse(0) + 1;

        // ★初期値は INPUT (吸い出し)
        rules.add(new RuleEntry(nextIndex, 20, 1, isFluid, iconStack, tagName, IOMode.INPUT));

        sendUpdatePacket();
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);

        this.tooltipToRender = null;
        this.tooltipStackToRender = ItemStack.EMPTY;

        Slot hoveredSlot = this.hoveredSlot;
        if (hoveredSlot != null && hoveredSlot.hasItem() && hoveredSlot.index == 36 && currentTagIndex != -1) {
            if (currentTagIndex >= 0 && currentTagIndex < currentItemTags.size()) {
                String tagString = "#" + currentItemTags.get(currentTagIndex).location();
                List<Component> tooltip = new ArrayList<>();
                tooltip.add(Component.literal(tagString).withStyle(ChatFormatting.GREEN));
                tooltip.add(Component.literal("Click 'Tag' to cycle").withStyle(ChatFormatting.GRAY));
                graphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
            }
        } else {
            this.renderTooltip(graphics, mouseX, mouseY);
        }

        if (!lastFilterStack.isEmpty()) {
            int textX = this.leftPos + FILTER_SLOT_X;
            int textY = this.topPos + FILTER_SLOT_Y - 9;
            String label = (currentTagIndex == -1) ? "Item" : "Tag";
            int color = (currentTagIndex == -1) ? 0xFFFFFF : 0x55FF55;
            graphics.drawString(this.font, label, textX, textY, color, true);
        }

        drawFixedLabels(graphics);
        renderScrollArea(graphics, mouseX, mouseY);

        if (this.tooltipToRender != null) {
            graphics.renderComponentTooltip(this.font, this.tooltipToRender, mouseX, mouseY);
        } else if (!this.tooltipStackToRender.isEmpty()) {
            graphics.renderTooltip(this.font, this.tooltipStackToRender, mouseX, mouseY);
        }
    }

    private void drawFixedLabels(GuiGraphics graphics) {
        int tx = this.leftPos + TARGET_X, ty = this.topPos + TARGET_Y;
        graphics.fill(tx - 1, ty - 1, tx + 17, ty + 17, 0xFF000000);
        graphics.fill(tx, ty, tx + 16, ty + 16, 0xFF555555);

        ItemStack targetIcon = new ItemStack(Blocks.CHEST);
        if (selectedDirection != null && this.minecraft.level != null) {
            BlockState state = this.minecraft.level.getBlockState(this.menu.blockEntity.getBlockPos().relative(selectedDirection));
            if (!state.isAir()) targetIcon = new ItemStack(state.getBlock());
        }
        graphics.renderFakeItem(targetIcon, tx, ty);
        graphics.drawString(this.font, "Target", tx + 20, ty + 4, 0xFFFFFF);

        int ux = this.leftPos + UPG_X, uy = this.topPos + UPG_Y;
        graphics.fill(ux, uy, ux + 18, uy + 18, 0xFF8B8B8B);
        graphics.fill(ux + 1, uy + 1, ux + 17, uy + 17, 0xFF373737);
        graphics.drawString(this.font, "UPG", ux, uy - 10, 0x404040, false);
    }

    private void renderScrollArea(GuiGraphics graphics, int mouseX, int mouseY) {
        int scrollX = this.leftPos + SCROLL_X;
        int scrollY = this.topPos + SCROLL_Y;
        int scrollBarX = scrollX + SCROLL_W - 8;

        graphics.fill(scrollX, scrollY, scrollX + SCROLL_W, scrollY + SCROLL_H, 0xFFC6C6C6);
        graphics.fill(scrollBarX, scrollY, scrollBarX + 6, scrollY + SCROLL_H, 0xFF000000);

        int knobHeight = 15;
        int knobY = scrollY + (int) ((SCROLL_H - knobHeight) * this.scrollOffs);
        graphics.fill(scrollBarX + 1, knobY, scrollBarX + 5, knobY + knobHeight, 0xFF808080);

        graphics.enableScissor(scrollX, scrollY, scrollX + SCROLL_W, scrollY + SCROLL_H);

        List<RuleEntry> rules = getCurrentRules();
        int contentStartY = scrollY - (int) (this.scrollOffs * ((rules.size() * ROW_HEIGHT) - SCROLL_H));
        if (contentStartY > scrollY) contentStartY = scrollY;

        for (int i = 0; i < rules.size(); i++) {
            RuleEntry rule = rules.get(i);
            int rowY = contentStartY + (i * ROW_HEIGHT);

            if (rowY + ROW_HEIGHT < scrollY || rowY > scrollY + SCROLL_H) continue;

            boolean isSelected = (i == selectedRuleIndex);
            int bgColor = isSelected ? 0xFFFFFFFF : ((i % 2 == 0) ? 0xFFDADADA : 0xFFEAEAEA);

            graphics.fill(scrollX, rowY, scrollX + SCROLL_W - 10, rowY + ROW_HEIGHT - 1, bgColor);
            if (isSelected) graphics.renderOutline(scrollX, rowY, SCROLL_W - 10, ROW_HEIGHT - 1, 0xFF0000FF);

            boolean isMouseInScrollArea = (mouseY >= scrollY && mouseY <= scrollY + SCROLL_H);

            // [x] ボタン
            int delBtnX = scrollX + 2;
            int delBtnY = rowY + 2;
            boolean isHoveringDelete = isMouseInScrollArea && (mouseX >= delBtnX && mouseX < delBtnX + 12 && mouseY >= delBtnY && mouseY < delBtnY + 12);
            int delColor = isHoveringDelete ? 0xFFFF7777 : 0xFFFF5555;
            graphics.fill(delBtnX, delBtnY, delBtnX + 12, delBtnY + 12, delColor);
            graphics.drawCenteredString(this.font, "x", scrollX + 8, rowY + 4, 0xFFFFFF);

            // ★ I/O 切り替えボタン (xボタンの隣)
            int modeBtnX = scrollX + 16;
            int modeBtnY = rowY + 2;
            boolean isHoveringMode = isMouseInScrollArea && (mouseX >= modeBtnX && mouseX < modeBtnX + 24 && mouseY >= modeBtnY && mouseY < modeBtnY + 12);

            // 色分け: INPUT(青) / OUTPUT(橙)
            int modeColor = rule.mode == IOMode.INPUT ? 0xFF0055AA : 0xFFAA5500;
            if (isHoveringMode) modeColor = rule.mode == IOMode.INPUT ? 0xFF0077CC : 0xFFFF7722;

            graphics.fill(modeBtnX, modeBtnY, modeBtnX + 24, modeBtnY + 12, modeColor);
            String modeText = rule.mode == IOMode.INPUT ? "IN" : "OUT";
            graphics.drawCenteredString(this.font, modeText, modeBtnX + 12, rowY + 4, 0xFFFFFF);

            // 文字列の位置を右にずらす
            graphics.drawString(this.font, "#" + rule.sortIndex, scrollX + 45, rowY + 6, 0x404040, false);
            graphics.drawString(this.font, rule.tick + "t", scrollX + 70, rowY + 6, 0x0000AA, false);
            graphics.drawString(this.font, rule.amount + (rule.isFluid ? "mB" : ""), scrollX + 105, rowY + 6, 0x00AA00, false);

            int iconX = scrollX + 130;
            int iconY = rowY + 2;

            if (rule.isFluid) {
                renderFluid(graphics, rule.stack, iconX, iconY);
            } else {
                graphics.renderFakeItem(rule.stack, iconX, iconY);
            }

            if (rule.tagName != null) {
                RenderSystem.disableDepthTest();
                graphics.drawString(this.font, "TAG", iconX - 22, iconY + 4, 0x00AA00, true);
                RenderSystem.enableDepthTest();
            }

            if (isMouseInScrollArea && mouseX >= iconX && mouseX <= iconX + 16 && mouseY >= iconY && mouseY <= iconY + 16) {
                if (rule.tagName != null) {
                    List<Component> list = new ArrayList<>();
                    list.add(Component.literal(rule.tagName).withStyle(ChatFormatting.GREEN));
                    this.tooltipToRender = list;
                } else if (rule.isFluid && rule.stack.getItem() instanceof BucketItem bucket) {
                    List<Component> list = new ArrayList<>();
                    list.add(new FluidStack(bucket.getFluid(), 1000).getDisplayName());
                    this.tooltipToRender = list;
                } else {
                    this.tooltipStackToRender = rule.stack;
                }
            }
        }
        graphics.disableScissor();
    }

    private void renderFluid(GuiGraphics graphics, ItemStack bucketStack, int x, int y) {
        if (!(bucketStack.getItem() instanceof BucketItem bucket)) {
            graphics.renderFakeItem(bucketStack, x, y);
            return;
        }

        Fluid fluid = bucket.getFluid();
        if (fluid == Fluids.EMPTY) return;

        IClientFluidTypeExtensions fluidTypeExtensions = IClientFluidTypeExtensions.of(fluid);
        ResourceLocation stillTexture = fluidTypeExtensions.getStillTexture();
        int color = fluidTypeExtensions.getTintColor();

        if (stillTexture != null) {
            TextureAtlasSprite sprite = Minecraft.getInstance()
                    .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                    .apply(stillTexture);

            float r = ((color >> 16) & 0xFF) / 255f;
            float g = ((color >> 8) & 0xFF) / 255f;
            float b = (color & 0xFF) / 255f;
            float a = ((color >> 24) & 0xFF) / 255f;

            RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS);
            RenderSystem.setShaderColor(r, g, b, a);
            graphics.blit(x, y, 0, 16, 16, sprite);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        }
    }

    private void updateButtons() {
        dirButtons.forEach((dir, btn) -> btn.active = (dir != this.selectedDirection));
    }

    private void updateEditorsVisibility() {
        List<RuleEntry> rules = getCurrentRules();
        boolean hasSelection = selectedRuleIndex >= 0 && selectedRuleIndex < rules.size();

        this.indexEdit.setVisible(hasSelection);
        this.tickSlider.visible = hasSelection;
        this.amountSlider.visible = hasSelection;

        if (hasSelection) {
            RuleEntry rule = rules.get(selectedRuleIndex);
            this.indexEdit.setValue(String.valueOf(rule.sortIndex));
            this.tickSlider.setValue(rule.tick);
            this.amountSlider.setValue(rule.amount);
            this.amountSlider.setMaxValue(rule.isFluid ? 1000 : 64);
        }
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, 512, 512);
    }

    // --- Mouse & Keyboard Inputs ---

    @Override
    public boolean mouseClicked(double x, double y, int btn) {
        if (this.indexEdit != null && this.indexEdit.mouseClicked(x, y, btn)) {
            return true;
        }

        if (btn == 0) {
            if (x >= leftPos + SCROLL_X && x < leftPos + SCROLL_X + SCROLL_W && y >= topPos + SCROLL_Y && y < topPos + SCROLL_Y + SCROLL_H) {
                List<RuleEntry> rules = getCurrentRules();
                int totalH = rules.size() * ROW_HEIGHT;
                int startY = (topPos + SCROLL_Y) - (int) (scrollOffs * (totalH - SCROLL_H));
                if (startY > topPos + SCROLL_Y) startY = topPos + SCROLL_Y;

                double relY = y - startY;
                if (relY >= 0) {
                    int clickedIndex = (int)(relY / ROW_HEIGHT);
                    if (clickedIndex >= 0 && clickedIndex < rules.size()) {
                        double relX = x - (leftPos + SCROLL_X);

                        // [x] 削除ボタン
                        if (relX >= 2 && relX < 14) {
                            rules.remove(clickedIndex);
                            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                            if (selectedRuleIndex == clickedIndex) selectedRuleIndex = -1;
                            else if (selectedRuleIndex > clickedIndex) selectedRuleIndex--;
                            updateEditorsVisibility();
                            sendUpdatePacket();
                            return true;
                        }

                        // ★ I/O 切り替えボタン (xボタンの隣)
                        if (relX >= 16 && relX < 40) {
                            RuleEntry rule = rules.get(clickedIndex);
                            // モード反転
                            rule.mode = (rule.mode == IOMode.INPUT) ? IOMode.OUTPUT : IOMode.INPUT;
                            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                            sendUpdatePacket(); // 変更即送信
                            return true;
                        }

                        selectedRuleIndex = clickedIndex;
                        updateEditorsVisibility();
                        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));

                        if (this.indexEdit != null && this.indexEdit.isVisible()) {
                            this.indexEdit.setFocused(true);
                        }
                        return true;
                    }
                }
            }

            int barX = leftPos + SCROLL_X + SCROLL_W - 8;
            if (x >= barX && x < barX + 6 && y >= topPos + SCROLL_Y && y < topPos + SCROLL_Y + SCROLL_H) {
                isScrolling = true;
                return true;
            }
        }

        if (this.indexEdit != null) {
            this.indexEdit.setFocused(false);
        }

        return super.mouseClicked(x, y, btn);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            this.minecraft.player.closeContainer();
            return true;
        }
        if (this.indexEdit != null && this.indexEdit.isVisible() && this.indexEdit.isFocused()) {
            if (this.indexEdit.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.indexEdit != null && this.indexEdit.isVisible() && this.indexEdit.isFocused()) {
            if (this.indexEdit.charTyped(codePoint, modifiers)) {
                return true;
            }
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (this.tickSlider != null && this.tickSlider.mouseScrolled(mouseX, mouseY, delta)) {
            return true;
        }
        if (this.amountSlider != null && this.amountSlider.mouseScrolled(mouseX, mouseY, delta)) {
            return true;
        }

        List<RuleEntry> rules = getCurrentRules();
        if (delta != 0 && rules.size() * ROW_HEIGHT > SCROLL_H) {
            scrollOffs = Mth.clamp((float)(scrollOffs - delta * 0.1f), 0.0f, 1.0f);
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override public boolean mouseReleased(double x, double y, int btn) { if (btn == 0) isScrolling = false; return super.mouseReleased(x, y, btn); }
    @Override public boolean mouseDragged(double x, double y, int btn, double dx, double dy) {
        if (isScrolling) {
            scrollOffs = Mth.clamp((float)(y - (topPos + SCROLL_Y) - 7.5F) / (SCROLL_H - 15.0F), 0.0f, 1.0f);
            return true;
        }
        return super.mouseDragged(x, y, btn, dx, dy);
    }

    private static class BlockIconButton extends Button {
        private final ItemStack icon;
        private final Direction dir;

        protected BlockIconButton(int x, int y, int w, int h, Direction dir, ItemStack icon, OnPress onPress) {
            super(x, y, w, h, Component.empty(), onPress, DEFAULT_NARRATION);
            this.dir = dir;
            this.icon = icon;
        }

        @Override
        public void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            super.renderWidget(graphics, mouseX, mouseY, partialTick);
            int iconX = this.getX() + (this.width - 16) / 2;
            int iconY = this.getY() + (this.height - 16) / 2;
            graphics.renderFakeItem(icon, iconX, iconY);
        }
    }

    private static class RuleSlider extends AbstractSliderButton {
        private final Component prefix;
        private final int min;
        private int max;
        private final SliderCallback callback;

        public RuleSlider(int x, int y, int w, int h, Component p, int min, int max, int val, SliderCallback cb) {
            super(x, y, w, h, Component.empty(), 0);
            this.prefix = p;
            this.min = min;
            this.max = max;
            this.callback = cb;
            this.setValue(val);
        }

        public void setValue(int val) {
            double pct = (double)(val - min) / (double)(max - min);
            this.value = Mth.clamp(pct, 0.0, 1.0);
            this.updateMessage();
        }

        public void setMaxValue(int newMax) {
            int current = getIntValue();
            this.max = newMax;
            this.setValue(current);
        }

        private int getIntValue() {
            return min + (int)(this.value * (max - min));
        }

        @Override
        protected void updateMessage() {
            this.setMessage(prefix.copy().append(String.valueOf(getIntValue())));
        }

        @Override
        protected void applyValue() {
            if (callback != null) {
                callback.onChange(getIntValue());
            }
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
            if (this.visible && this.isMouseOver(mouseX, mouseY)) {
                int current = getIntValue();
                if (delta > 0) current = Math.min(current + 1, max);
                else if (delta < 0) current = Math.max(current - 1, min);

                this.setValue(current);
                this.applyValue();
                return true;
            }
            return false;
        }

        @FunctionalInterface
        interface SliderCallback {
            void onChange(int val);
        }
    }
}