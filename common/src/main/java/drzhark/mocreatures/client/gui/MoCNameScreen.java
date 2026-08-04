package drzhark.mocreatures.client.gui;

import dev.architectury.networking.NetworkManager;
import drzhark.mocreatures.network.MoCNamePayload;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * A small pop-up screen to name a tamed creature. Opened when the player right-clicks their tamed pet
 * with a (plain) Medallion. On confirm it sends the typed name to the server via {@link MoCNamePayload}.
 */
public class MoCNameScreen extends Screen {

    private final int entityId;
    private final String current;
    private EditBox nameField;

    public MoCNameScreen(int entityId, String current) {
        super(Component.literal("Name your pet"));
        this.entityId = entityId;
        this.current = current;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int cy = this.height / 2;
        this.nameField = new EditBox(this.font, cx - 100, cy - 10, 200, 20, Component.literal("Name"));
        this.nameField.setMaxLength(30);
        if (this.current != null && !this.current.isEmpty()) {
            this.nameField.setValue(this.current);
        }
        this.addRenderableWidget(this.nameField);
        this.setInitialFocus(this.nameField);

        this.addRenderableWidget(Button.builder(Component.literal("Done"), b -> confirm())
                .bounds(cx - 100, cy + 16, 95, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> this.onClose())
                .bounds(cx + 5, cy + 16, 95, 20).build());
    }

    private void confirm() {
        String name = this.nameField.getValue().trim();
        if (!name.isEmpty()) {
            NetworkManager.sendToServer(new MoCNamePayload(this.entityId, name));
        }
        this.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
