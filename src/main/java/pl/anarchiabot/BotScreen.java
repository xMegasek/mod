package pl.anarchiabot;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.List;

public class BotScreen extends Screen {
    // zapamietane miedzy otwarciami okna
    private static String savedNick = "";
    private static String savedHost = "anarchia.gg";
    private static String savedVersion = "";
    private static String savedPass = "";
    private static boolean autoRules = true;

    private TextFieldWidget nickField, hostField, versionField, passField, chatField, slotField;
    private ButtonWidget rulesButton;

    public BotScreen() {
        super(Text.literal("Anarchia Bot"));
    }

    @Override
    protected void init() {
        int left = this.width / 2 - 200;
        int top = 28;

        nickField = new TextFieldWidget(textRenderer, left, top + 10, 120, 18, Text.literal("Nick"));
        nickField.setMaxLength(16);
        nickField.setText(savedNick.isEmpty() ? "Bot" + (int) (Math.random() * 9000 + 1000) : savedNick);
        addDrawableChild(nickField);

        hostField = new TextFieldWidget(textRenderer, left + 126, top + 10, 120, 18, Text.literal("Serwer"));
        hostField.setMaxLength(100);
        hostField.setText(savedHost);
        addDrawableChild(hostField);

        versionField = new TextFieldWidget(textRenderer, left + 252, top + 10, 60, 18, Text.literal("Wersja"));
        versionField.setPlaceholder(Text.literal("auto"));
        versionField.setText(savedVersion);
        addDrawableChild(versionField);

        passField = new TextFieldWidget(textRenderer, left + 318, top + 10, 82, 18, Text.literal("Haslo"));
        passField.setPlaceholder(Text.literal("haslo /login"));
        passField.setText(savedPass);
        addDrawableChild(passField);

        addDrawableChild(ButtonWidget.builder(Text.literal("Dolacz"), b -> {
            saveFields();
            String host = hostField.getText().trim();
            int port = 25565;
            if (host.contains(":")) {
                String[] p = host.split(":", 2);
                host = p[0];
                try {
                    port = Integer.parseInt(p[1].trim());
                } catch (NumberFormatException ignored) {
                }
            }
            BotManager.get().connect(host, port, nickField.getText().trim(), versionField.getText().trim(),
                    autoRules, passField.getText());
        }).dimensions(left, top + 34, 95, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Rozlacz"), b -> BotManager.get().stop())
                .dimensions(left + 100, top + 34, 95, 20).build());

        rulesButton = addDrawableChild(ButtonWidget.builder(rulesLabel(), b -> {
            autoRules = !autoRules;
            b.setMessage(rulesLabel());
            BotManager.get().setAutoRules(autoRules);
        }).dimensions(left + 200, top + 34, 200, 20).build());

        // log zajmuje srodek; pola na dole
        int bottom = this.height - 54;

        chatField = new TextFieldWidget(textRenderer, left, bottom, 300, 18, Text.literal("Czat/komenda"));
        chatField.setMaxLength(256);
        chatField.setPlaceholder(Text.literal("wiadomosc lub /komenda"));
        addDrawableChild(chatField);

        addDrawableChild(ButtonWidget.builder(Text.literal("Wyslij"), b -> sendChat())
                .dimensions(left + 306, bottom - 1, 94, 20).build());

        slotField = new TextFieldWidget(textRenderer, left, bottom + 24, 60, 18, Text.literal("Slot"));
        slotField.setMaxLength(3);
        slotField.setPlaceholder(Text.literal("slot"));
        addDrawableChild(slotField);

        addDrawableChild(ButtonWidget.builder(Text.literal("Kliknij slot"), b -> {
            try {
                BotManager.get().clickSlot(Integer.parseInt(slotField.getText().trim()));
            } catch (NumberFormatException e) {
                BotManager.get().addLog("[mod] Podaj numer slotu (liczba).");
            }
        }).dimensions(left + 66, bottom + 23, 90, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Pokaz GUI"), b -> BotManager.get().simple("window"))
                .dimensions(left + 160, bottom + 23, 80, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Zamknij GUI bota"), b -> BotManager.get().simple("closeWindow"))
                .dimensions(left + 244, bottom + 23, 100, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("X"), b -> close())
                .dimensions(left + 348, bottom + 23, 52, 20).build());

        setInitialFocus(chatField);
    }

    private Text rulesLabel() {
        return Text.literal("Auto-akceptacja regulaminu: " + (autoRules ? "WL" : "WYL"));
    }

    private void saveFields() {
        savedNick = nickField.getText();
        savedHost = hostField.getText();
        savedVersion = versionField.getText();
        savedPass = passField.getText();
    }

    private void sendChat() {
        String t = chatField.getText();
        if (!t.isBlank()) {
            BotManager.get().chat(t);
            BotManager.get().addLog("> " + t);
            chatField.setText("");
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Enter w polu czatu = wyslij
        if ((keyCode == 257 || keyCode == 335) && chatField != null && chatField.isFocused()) {
            sendChat();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        int left = this.width / 2 - 200;
        BotManager m = BotManager.get();

        ctx.drawCenteredTextWithShadow(textRenderer, this.title, this.width / 2, 8, 0xFFFFFF);
        ctx.drawTextWithShadow(textRenderer, "Nick", left, 28, 0xAAAAAA);
        ctx.drawTextWithShadow(textRenderer, "Serwer[:port]", left + 126, 28, 0xAAAAAA);
        ctx.drawTextWithShadow(textRenderer, "Wersja", left + 252, 28, 0xAAAAAA);
        ctx.drawTextWithShadow(textRenderer, "Haslo", left + 318, 28, 0xAAAAAA);

        String status = m.isStarting() ? "Uruchamianie..." : m.isConnected() ? "POLACZONY" : m.isRunning() ? "Laczenie..." : "Rozlaczony";
        int color = m.isConnected() ? 0x55FF55 : (m.isRunning() || m.isStarting()) ? 0xFFFF55 : 0xFF5555;
        ctx.drawTextWithShadow(textRenderer, "Status: " + status, left, 90, color);

        // log
        int logTop = 102;
        int logBottom = this.height - 62;
        ctx.fill(left, logTop, left + 400, logBottom, 0xAA000000);
        List<String> lines = m.getLog();
        int maxLines = Math.max(1, (logBottom - logTop - 4) / 10);
        int idx = 0;
        // zawijanie: bierzemy ostatnie wiersze po zawinieciu
        java.util.ArrayList<String> wrapped = new java.util.ArrayList<>();
        for (String l : lines) {
            for (var part : textRenderer.wrapLines(Text.literal(l), 392)) {
                StringBuilder sb = new StringBuilder();
                part.accept((i, style, cp) -> {
                    sb.appendCodePoint(cp);
                    return true;
                });
                wrapped.add(sb.toString());
            }
        }
        int start = Math.max(0, wrapped.size() - maxLines);
        for (int i = start; i < wrapped.size(); i++) {
            ctx.drawTextWithShadow(textRenderer, wrapped.get(i), left + 4, logTop + 3 + idx * 10, 0xFFFFFF);
            idx++;
        }
    }

    @Override
    public void removed() {
        if (nickField != null) saveFields();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
