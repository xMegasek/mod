package pl.anarchiabot;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class AnarchiaBotMod implements ClientModInitializer {
    private static KeyBinding openKey;

    @Override
    public void onInitializeClient() {
        openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.anarchiabot.open",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                "category.anarchiabot"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openKey.wasPressed()) {
                // nie otwieraj, jesli juz jest jakis ekran (np. czat)
                if (client.currentScreen == null) {
                    client.setScreen(new BotScreen());
                }
            }
        });

        Runtime.getRuntime().addShutdownHook(new Thread(() -> BotManager.get().stop()));
    }
}
