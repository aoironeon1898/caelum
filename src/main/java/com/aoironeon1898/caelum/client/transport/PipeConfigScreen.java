package com.aoironeon1898.caelum.client.transport;

import com.aoironeon1898.caelum.Caelum;
import com.aoironeon1898.caelum.common.content.logistics.blocks.EnumPipeMode;
import com.aoironeon1898.caelum.common.content.logistics.data.SlotMappingRule;
import com.aoironeon1898.caelum.common.content.logistics.menu.PipeConfigMenu;
import com.aoironeon1898.caelum.common.content.logistics.tile.CompositePipeBlockEntity;
import com.aoironeon1898.caelum.common.network.ModMessages;
import com.aoironeon1898.caelum.common.network.packet.PacketUpdatePipeConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

import java.util.ArrayList;
import java.util.List;

public class PipeConfigScreen extends AbstractContainerScreen<PipeConfigMenu> {

    // --- GUI配置設定 ---
    private static final ResourceLocation TEXTURE = new ResourceLocation(Caelum.MODID, "textures/gui/pipe_config.png");
    private static final int TEXTURE_WIDTH = 512;
    private static final int TEXTURE_HEIGHT = 512;

    private static final int WINDOW_WIDTH = 176;
    private static final int WINDOW_HEIGHT = 285;
    private static final int INVENTORY_LABEL_Y = 190;

    // タブ
    private static final int TAB_Y_OFFSET = -22;
    private static final int TAB_WIDTH = 22;
    private static final int TAB_HEIGHT = 20;
    private static final int TAB_SPACING = 24;

    // 入力エリア
    private static final int GHOST_SLOT_X = 8;
    private static final int GHOST_SLOT_Y = 40;
    private static final int TYPE_BUTTON_X = 28;
    private static final int TYPE_BUTTON_Y = 39;
    private static final int TYPE_BUTTON_W = 30;
    private static final int TYPE_BUTTON_H = 20;
    private static final int TAG_INPUT_X = 62;
    private static final int TAG_INPUT_Y = 41;
    private static final int TAG_INPUT_W = 55;
    private static final int TAG_INPUT_H = 16;
    private static final int SLOT_INPUT_X = 120;
    private static final int SLOT_INPUT_Y = 41;
    private static final int SLOT_INPUT_W = 20;
    private static final int SLOT_INPUT_H = 16;
    private static final int ADD_BUTTON_X = 145;
    private static final int ADD_BUTTON_Y = 39;
    private static final int ADD_BUTTON_W = 24;
    private static final int ADD_BUTTON_H = 20;

    // リストエリア
    private static final int LIST_START_Y = 67;
    private static final int LIST_ROW_HEIGHT = 20;
    private static final int LIST_MAX_ROWS = 6;

    // リスト内要素
    private static final int LIST_ITEM_X = 11;
    private static final int LIST_SLOT_TEXT_X = 100;
    private static final int LIST_DELETE_BTN_X = 143;
    private static final int LIST_ROW_WIDTH = 152;

    // スクロールバー
    private static final int SCROLLBAR_X = 163;
    private static final int SCROLLBAR_Y = LIST_START_Y;
    private static final int SCROLLBAR_WIDTH = 6;
    private static final int SCROLLBAR_HEIGHT = (LIST_ROW_HEIGHT * LIST_MAX_ROWS);

    // 設定ボタン
    private static final int CONFIG_BUTTONS_Y = 16;
    private static final int MODE_BUTTON_X = 46;
    private static final int MODE_BUTTON_W = 60;
    private static final int MODE_BUTTON_H = 20;
    private static final int FILTER_BUTTON_X = (63 + MODE_BUTTON_X);
    private static final int FILTER_BUTTON_W = 60;
    private static final int FILTER_BUTTON_H = 20;

    // ---------------------------------------------------------

    private Direction currentDirection = Direction.NORTH;
    private EnumPipeMode currentMode = EnumPipeMode.NORMAL;
    private boolean isWhitelist = true;
    private final List<SlotMappingRule> currentRules = new ArrayList<>();
    private final List<Direction> availableDirections = new ArrayList<>();

    private boolean isTagInputMode = false;
    private ItemStack inputGhostItem = ItemStack.EMPTY;

    private EditBox inputSlotId;
    private EditBox inputTag;
    private Button modeButton;
    private Button filterButton;
    private Button typeToggleButton;

    private float scrollOffs = 0.0f;
    private boolean isScrolling = false;
    private int startIndex = 0;

    public PipeConfigScreen(PipeConfigMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = WINDOW_WIDTH;
        this.imageHeight = WINDOW_HEIGHT;
        this.inventoryLabelY = INVENTORY_LABEL_Y;
    }

    @Override
    protected void init() {
        super.init();
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        Level level = Minecraft.getInstance().level;

        availableDirections.clear();
        BlockPos pipePos = menu.pipe.getBlockPos();

        for (Direction dir : Direction.values()) {
            BlockPos neighbor = pipePos.relative(dir);
            BlockEntity be = level.getBlockEntity(neighbor);

            // インベントリ機能を持っていて、かつパイプではない場合のみ追加
            if (be != null && be.getCapability(ForgeCapabilities.ITEM_HANDLER, dir.getOpposite()).isPresent()) {
                if (!(be instanceof CompositePipeBlockEntity)) {
                    availableDirections.add(dir);
                }
            }
        }

        if (availableDirections.isEmpty()) {
            availableDirections.add(Direction.NORTH);
        }

        if (!availableDirections.contains(currentDirection)) {
            currentDirection = availableDirections.get(0);
        }

        int tabX = x;
        for (Direction dir : availableDirections) {
            BlockPos neighborPos = pipePos.relative(dir);
            BlockState state = level.getBlockState(neighborPos);
            ItemStack blockIcon = new ItemStack(state.getBlock().asItem());

            addRenderableWidget(new BlockTabButton(tabX, y + TAB_Y_OFFSET, TAB_WIDTH, TAB_HEIGHT, dir, blockIcon, (btn) -> {
                this.currentDirection = dir;
                loadServerData(dir);
            }));

            tabX += TAB_SPACING;
        }

        typeToggleButton = addRenderableWidget(Button.builder(Component.literal("Item"), btn -> {
            isTagInputMode = !isTagInputMode;
            updateInputVisibility();
        }).bounds(x + TYPE_BUTTON_X, y + TYPE_BUTTON_Y, TYPE_BUTTON_W, TYPE_BUTTON_H).build());

        inputTag = new EditBox(font, x + TAG_INPUT_X, y + TAG_INPUT_Y, TAG_INPUT_W, TAG_INPUT_H, Component.literal("Tag"));
        inputTag.setMaxLength(64);
        inputTag.setVisible(false);
        addRenderableWidget(inputTag);

        inputSlotId = new EditBox(font, x + SLOT_INPUT_X, y + SLOT_INPUT_Y, SLOT_INPUT_W, SLOT_INPUT_H, Component.literal("Slot"));
        inputSlotId.setValue("-1");
        inputSlotId.setFilter(s -> {
            if (s.isEmpty()) return true;
            if (s.equals("-")) return true;
            try {
                int val = Integer.parseInt(s);
                return val >= -1;
            } catch (NumberFormatException e) {
                return false;
            }
        });
        addRenderableWidget(inputSlotId);

        addRenderableWidget(Button.builder(Component.literal("Add"), btn -> {
            addRule();
        }).bounds(x + ADD_BUTTON_X, y + ADD_BUTTON_Y, ADD_BUTTON_W, ADD_BUTTON_H).build());

        modeButton = addRenderableWidget(Button.builder(Component.literal("Mode"), btn -> {
            // モード切替: NORMAL -> PROVIDE -> REQUEST -> DISABLED -> NORMAL
            EnumPipeMode next = switch (currentMode) {
                case NORMAL -> EnumPipeMode.PROVIDE;
                case PROVIDE -> EnumPipeMode.REQUEST;
                case REQUEST -> EnumPipeMode.DISABLED;
                default -> EnumPipeMode.NORMAL;
            };
            currentMode = next;
            updateButtons();
            sendUpdatePacket();
        }).bounds(x + MODE_BUTTON_X, y + CONFIG_BUTTONS_Y, MODE_BUTTON_W, MODE_BUTTON_H).build());

        filterButton = addRenderableWidget(Button.builder(Component.literal("Filter"), btn -> {
            isWhitelist = !isWhitelist;
            updateButtons();
            sendUpdatePacket();
        }).bounds(x + FILTER_BUTTON_X, y + CONFIG_BUTTONS_Y, FILTER_BUTTON_W, FILTER_BUTTON_H).build());

        loadServerData(currentDirection);
        updateInputVisibility();
    }

    private class BlockTabButton extends Button {
        private final ItemStack icon;
        private final Direction direction;

        public BlockTabButton(int x, int y, int width, int height, Direction dir, ItemStack icon, OnPress onPress) {
            super(x, y, width, height, Component.empty(), onPress, DEFAULT_NARRATION);
            this.direction = dir;
            this.icon = icon;
            String dirName = dir.getName().substring(0, 1).toUpperCase() + dir.getName().substring(1);
            this.setTooltip(Tooltip.create(Component.literal(dirName)));
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            super.renderWidget(graphics, mouseX, mouseY, partialTick);
            if (!icon.isEmpty()) {
                int itemX = this.getX() + (this.width - 16) / 2;
                int itemY = this.getY() + (this.height - 16) / 2;
                graphics.renderFakeItem(icon, itemX, itemY);
            } else {
                String label = direction.getName().substring(0, 1).toUpperCase();
                int textW = font.width(label);
                graphics.drawString(font, label, this.getX() + (this.width - textW) / 2, this.getY() + (this.height - 8) / 2, 0xFFFFFF, true);
            }
        }
    }

    private void updateInputVisibility() {
        if (typeToggleButton != null) {
            typeToggleButton.setMessage(Component.literal(isTagInputMode ? "Tag" : "Item"));
        }
        if (inputTag != null) {
            inputTag.setVisible(isTagInputMode);
        }
    }

    private void loadServerData(Direction dir) {
        this.currentMode = menu.pipe.getMode(dir);
        this.isWhitelist = menu.pipe.isWhitelist(dir);
        this.currentRules.clear();
        this.currentRules.addAll(menu.pipe.getRules(dir));

        this.inputGhostItem = ItemStack.EMPTY;
        if(inputSlotId != null) inputSlotId.setValue("-1");
        if(inputTag != null) inputTag.setValue("");

        this.scrollOffs = 0.0f;
        this.startIndex = 0;

        updateButtons();
    }

    private void updateButtons() {
        if (modeButton == null) return;
        String modeColor = switch (currentMode) {
            case REQUEST -> "§9"; // Blue
            case PROVIDE -> "§6"; // Orange
            case DISABLED -> "§c"; // Red
            case NONE -> "§c";
            default -> "§7"; // Gray (Normal)
        };
        String modeName = (currentMode == EnumPipeMode.DISABLED || currentMode == EnumPipeMode.NONE) ? "DISABLED" : currentMode.name();
        modeButton.setMessage(Component.literal(modeColor + modeName));

        String filterText = isWhitelist ? "§fWhite" : "§8Black";
        filterButton.setMessage(Component.literal(filterText));
    }

    private void addRule() {
        int slot = -1;
        try { slot = Integer.parseInt(inputSlotId.getValue()); } catch(Exception ignored){}

        if (slot < -1) return;

        if (isTagInputMode) {
            String tagStr = inputTag.getValue();
            if (tagStr.isEmpty() && slot == -1) return;

            ResourceLocation rl = ResourceLocation.tryParse(tagStr);
            if (rl == null) return;

            TagKey<Item> tagKey = TagKey.create(Registries.ITEM, rl);
            Level level = Minecraft.getInstance().level;
            if (level != null) {
                var registry = level.registryAccess().registryOrThrow(Registries.ITEM);
                var tag = registry.getTag(tagKey);
                if (tag.isEmpty() || tag.get().size() == 0) return;
            }

            currentRules.add(new SlotMappingRule(tagStr, slot));
            inputTag.setValue("");
        } else {
            if (inputGhostItem.isEmpty() && slot == -1) return;
            currentRules.add(new SlotMappingRule(inputGhostItem, slot));
            inputGhostItem = ItemStack.EMPTY;
        }

        inputSlotId.setValue("-1");

        if (currentRules.size() > LIST_MAX_ROWS) {
            this.scrollOffs = 1.0f;
        }

        sendUpdatePacket();
    }

    private void deleteRule(int index) {
        if (index >= 0 && index < currentRules.size()) {
            currentRules.remove(index);
            int maxScroll = currentRules.size() - LIST_MAX_ROWS;
            if (maxScroll <= 0) scrollOffs = 0.0f;
            else scrollOffs = Mth.clamp(scrollOffs, 0.0f, 1.0f);
            sendUpdatePacket();
        }
    }

    private void sendUpdatePacket() {
        ModMessages.sendToServer(new PacketUpdatePipeConfig(menu.pipe.getBlockPos(), currentDirection, currentMode, new ArrayList<>(currentRules), isWhitelist));
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (inputTag.isFocused()) {
            if (inputTag.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
            if (this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
                return true;
            }
        }

        if (inputSlotId.isFocused()) {
            if (inputSlotId.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
            if (this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
                return true;
            }
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (currentRules.size() > LIST_MAX_ROWS) {
            int maxScroll = currentRules.size() - LIST_MAX_ROWS;
            float step = 1.0f / maxScroll;
            this.scrollOffs = Mth.clamp(this.scrollOffs - (float) (delta * step), 0.0f, 1.0f);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.isScrolling) {
            int yMin = (height - imageHeight) / 2 + SCROLLBAR_Y;
            int yMax = yMin + SCROLLBAR_HEIGHT;
            this.scrollOffs = ((float) mouseY - yMin - 7.5f) / ((float) (yMax - yMin) - 15.0f);
            this.scrollOffs = Mth.clamp(this.scrollOffs, 0.0f, 1.0f);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        int tabIndex = availableDirections.indexOf(currentDirection);
        if (tabIndex != -1) {
            graphics.fill(x + (tabIndex * TAB_SPACING), y + TAB_Y_OFFSET + TAB_HEIGHT - 2, x + (tabIndex * TAB_SPACING) + TAB_WIDTH, y + TAB_Y_OFFSET + TAB_HEIGHT, 0xFFFFFF00);
        }

        if (!isTagInputMode && !inputGhostItem.isEmpty()) {
            graphics.renderFakeItem(inputGhostItem, x + GHOST_SLOT_X + 1, y + GHOST_SLOT_Y + 1);
        }

        int totalRows = currentRules.size();
        if (totalRows <= LIST_MAX_ROWS) {
            startIndex = 0;
            scrollOffs = 0.0f;
        } else {
            startIndex = (int) ((double) (scrollOffs * (totalRows - LIST_MAX_ROWS)) + 0.5D);
        }

        int listStartY = y + LIST_START_Y;
        int rowWidth = LIST_ROW_WIDTH;

        for (int i = 0; i < LIST_MAX_ROWS; i++) {
            int dataIndex = startIndex + i;
            if (dataIndex >= totalRows) break;

            int rowY = listStartY + (i * LIST_ROW_HEIGHT);

            int rowX = x + LIST_ITEM_X - 2;
            graphics.fill(rowX, rowY, rowX + rowWidth, rowY + LIST_ROW_HEIGHT - 2, 0xFFC6C6C6);
            graphics.fill(rowX, rowY, rowX + rowWidth, rowY + 1, 0xFF373737);
            graphics.fill(rowX, rowY, rowX + 1, rowY + LIST_ROW_HEIGHT - 2, 0xFF373737);
            graphics.fill(rowX, rowY + LIST_ROW_HEIGHT - 3, rowX + rowWidth, rowY + LIST_ROW_HEIGHT - 2, 0xFFFFFFFF);
            graphics.fill(rowX + rowWidth - 1, rowY, rowX + rowWidth, rowY + LIST_ROW_HEIGHT - 2, 0xFFFFFFFF);

            SlotMappingRule rule = currentRules.get(dataIndex);

            if (rule.isTagMode()) {
                String tagStr = rule.getTagFilter();
                if (tagStr.length() > 14) tagStr = tagStr.substring(0, 14) + "..";
                graphics.drawString(font, "#" + tagStr, x + LIST_ITEM_X, rowY + 5, 0x0000AA, false);
            } else {
                if (!rule.getFilterItem().isEmpty()) {
                    graphics.renderFakeItem(rule.getFilterItem(), x + LIST_ITEM_X, rowY + 1);
                } else {
                    graphics.drawString(font, "(Any)", x + LIST_ITEM_X, rowY + 5, 0x555555, false);
                }
            }

            String slotText = rule.getTargetSlotIndex() == -1 ? "Auto" : "Slot:" + rule.getTargetSlotIndex();
            graphics.drawString(font, slotText, x + LIST_SLOT_TEXT_X, rowY + 5, 0x555555, false);

            graphics.drawString(font, "[x]", x + LIST_DELETE_BTN_X, rowY + 5, 0xFF5555, false);
        }

        if (totalRows > LIST_MAX_ROWS) {
            int barX = x + SCROLLBAR_X;
            int barY = y + SCROLLBAR_Y;
            int barH = SCROLLBAR_HEIGHT;

            graphics.fill(barX, barY, barX + SCROLLBAR_WIDTH, barY + barH, 0xFF000000);

            int knobHeight = 15;
            int knobY = barY + (int) ((barH - knobHeight) * scrollOffs);
            graphics.fill(barX, knobY, barX + SCROLLBAR_WIDTH, knobY + knobHeight, 0xFF808080);
            graphics.fill(barX, knobY, barX + SCROLLBAR_WIDTH - 1, knobY + knobHeight - 1, 0xFFC0C0C0);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        if (!isTagInputMode) {
            if (mouseX >= x + GHOST_SLOT_X && mouseX <= x + GHOST_SLOT_X + 18 && mouseY >= y + GHOST_SLOT_Y && mouseY <= y + GHOST_SLOT_Y + 18) {
                ItemStack carried = menu.getCarried();
                inputGhostItem = carried.copy();
                return true;
            }
        }

        if (currentRules.size() > LIST_MAX_ROWS) {
            int barX = x + SCROLLBAR_X;
            int barY = y + SCROLLBAR_Y;
            if (mouseX >= barX && mouseX <= barX + SCROLLBAR_WIDTH && mouseY >= barY && mouseY <= barY + SCROLLBAR_HEIGHT) {
                this.isScrolling = true;
                return true;
            }
        }

        int listStartY = y + LIST_START_Y;
        for (int i = 0; i < LIST_MAX_ROWS; i++) {
            int dataIndex = startIndex + i;
            if (dataIndex >= currentRules.size()) break;

            int rowY = listStartY + (i * LIST_ROW_HEIGHT);
            if (mouseX >= x + LIST_DELETE_BTN_X - 2 && mouseX <= x + LIST_DELETE_BTN_X + 15 && mouseY >= rowY && mouseY <= rowY + 12) {
                deleteRule(dataIndex);
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            this.isScrolling = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        graphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight, TEXTURE_WIDTH, TEXTURE_HEIGHT);
    }
}