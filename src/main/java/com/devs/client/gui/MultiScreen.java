package com.devs.client.gui;

import com.devs.client.multi.AccountManager;
import com.devs.client.multi.DevsAccount;
import com.devs.client.multi.MultiManager;
import com.devs.client.multi.MultiProfile;
import com.devs.client.multi.MultiProfileManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.List;

/** Panel Multi: cuentas cracked, perfiles, start/stop, bots. */
public final class MultiScreen extends Screen {
    private final Screen parent;
    private TextFieldWidget accountField;
    private TextFieldWidget serverField;
    private TextFieldWidget chatField;
    private TextFieldWidget profileField;
    private int selectedAccount = -1;
    private int selectedProfile = -1;
    private int scrollBots;

    public MultiScreen(Screen parent) {
        super(Text.literal("Devs Multi"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int w = 120;
        accountField = new TextFieldWidget(textRenderer, 20, 40, 140, 18, Text.literal("account"));
        accountField.setPlaceholder(Text.literal("Cracked name"));
        addSelectableChild(accountField);

        addDrawableChild(ButtonWidget.builder(Text.literal("Add Account"), b -> {
            if (AccountManager.get().add(accountField.getText())) accountField.setText("");
        }).dimensions(170, 38, 90, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Remove Acc"), b -> {
            List<DevsAccount> all = AccountManager.get().all();
            if (selectedAccount >= 0 && selectedAccount < all.size()) {
                AccountManager.get().remove(all.get(selectedAccount).id);
                selectedAccount = -1;
            }
        }).dimensions(265, 38, 90, 20).build());

        profileField = new TextFieldWidget(textRenderer, 20, 70, 140, 18, Text.literal("profile"));
        profileField.setPlaceholder(Text.literal("Profile name"));
        addSelectableChild(profileField);

        addDrawableChild(ButtonWidget.builder(Text.literal("New Profile"), b -> {
            MultiProfile p = MultiProfileManager.get().create(profileField.getText());
            if (p != null) profileField.setText("");
        }).dimensions(170, 68, 90, 20).build());

        serverField = new TextFieldWidget(textRenderer, 20, 100, 200, 18, Text.literal("server"));
        serverField.setPlaceholder(Text.literal("server.com:25565"));
        addSelectableChild(serverField);

        addDrawableChild(ButtonWidget.builder(Text.literal("Save → Profile"), b -> saveToProfile()).dimensions(230, 98, 110, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("START Multi"), b -> {
            MultiProfile p = currentProfileOrBuild();
            if (p == null) return;
            MultiManager.StartResult r = MultiManager.get().start(p);
            if (!r.ok()) {
                if (client != null && client.player != null)
                    client.player.sendMessage(Text.literal("[Multi] " + r.message()));
            }
        }).dimensions(20, height - 50, 110, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("STOP"), b -> {
            MultiManager.get().disconnectAll("Stopped from GUI");
        }).dimensions(140, height - 50, 80, 20).build());

        chatField = new TextFieldWidget(textRenderer, 230, height - 50, 160, 18, Text.literal("chat"));
        chatField.setPlaceholder(Text.literal("Broadcast chat"));
        addSelectableChild(chatField);

        addDrawableChild(ButtonWidget.builder(Text.literal("Send"), b -> {
            String m = chatField.getText();
            if (m != null && !m.isBlank()) {
                int n = MultiManager.get().broadcastChat(m);
                chatField.setText("");
                if (client != null && client.player != null)
                    client.player.sendMessage(Text.literal("[Multi] sent to " + n + " bots"));
            }
        }).dimensions(395, height - 52, 50, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Close"), b -> {
            if (client != null) client.setScreen(parent);
        }).dimensions(width - 70, height - 50, 50, 20).build());
    }

    private MultiProfile currentProfileOrBuild() {
        List<MultiProfile> profiles = MultiProfileManager.get().all();
        MultiProfile p;
        if (selectedProfile >= 0 && selectedProfile < profiles.size()) {
            p = new MultiProfile(profiles.get(selectedProfile));
        } else {
            p = MultiProfileManager.get().create("quick");
            if (p == null) p = MultiProfileManager.get().findByName("quick");
            if (p == null) return null;
            p = new MultiProfile(p);
        }
        if (!serverField.getText().isBlank()) p.serverAddress = serverField.getText().trim();
        p.accountIds.clear();
        for (DevsAccount a : AccountManager.get().all()) p.accountIds.add(a.id);
        p.normalize();
        MultiProfileManager.get().saveProfile(p);
        return p;
    }

    private void saveToProfile() {
        MultiProfile p = currentProfileOrBuild();
        if (p != null && client != null && client.player != null) {
            client.player.sendMessage(Text.literal("[Multi] Saved profile " + p.name));
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // fondo
        ctx.fill(0, 0, width, height, 0xEE101018);
        ctx.drawTextWithShadow(textRenderer, "Devs Multi (cracked)", 20, 12, 0xFF4DD2FF);
        ctx.drawTextWithShadow(textRenderer, MultiManager.get().isActive() ? "§aACTIVE" : "§7idle", 200, 12, 0xFFFFFFFF);

        accountField.render(ctx, mouseX, mouseY, delta);
        profileField.render(ctx, mouseX, mouseY, delta);
        serverField.render(ctx, mouseX, mouseY, delta);
        chatField.render(ctx, mouseX, mouseY, delta);

        // accounts list
        ctx.drawTextWithShadow(textRenderer, "Accounts", 20, 125, 0xFFAAAAAA);
        List<DevsAccount> accs = AccountManager.get().all();
        int y = 138;
        for (int i = 0; i < accs.size() && i < 12; i++) {
            DevsAccount a = accs.get(i);
            int col = i == selectedAccount ? 0xFF4DD2FF : 0xFFE0E0E0;
            ctx.drawTextWithShadow(textRenderer, a.displayName(), 24, y, col);
            y += 11;
        }

        // profiles
        ctx.drawTextWithShadow(textRenderer, "Profiles", 180, 125, 0xFFAAAAAA);
        List<MultiProfile> profiles = MultiProfileManager.get().all();
        y = 138;
        for (int i = 0; i < profiles.size() && i < 12; i++) {
            MultiProfile p = profiles.get(i);
            int col = i == selectedProfile ? 0xFF4DD2FF : 0xFFE0E0E0;
            ctx.drawTextWithShadow(textRenderer, p.name + " §8" + p.serverAddress, 184, y, col);
            y += 11;
        }

        // bots
        ctx.drawTextWithShadow(textRenderer, "Bots", 20, 280, 0xFFAAAAAA);
        y = 293;
        for (MultiManager.BotView b : MultiManager.get().bots()) {
            String line = b.name() + " §7" + b.status() + " §8" + b.detail();
            if (b.ready() && b.hasPos()) line += String.format(" §a%.0f %.0f %.0f", b.x(), b.y(), b.z());
            ctx.drawTextWithShadow(textRenderer, line, 24, y, 0xFFFFFFFF);
            y += 11;
            if (y > height - 60) break;
        }

        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.gui.Click click, boolean doubled) {
        double mouseX = clickX(click);
        double mouseY = clickY(click);
        if (mouseX >= 20 && mouseX <= 160 && mouseY >= 138) {
            int i = (int) ((mouseY - 138) / 11);
            List<DevsAccount> accs = AccountManager.get().all();
            if (i >= 0 && i < accs.size() && i < 12) selectedAccount = i;
        }
        if (mouseX >= 180 && mouseX <= 400 && mouseY >= 138) {
            int i = (int) ((mouseY - 138) / 11);
            List<MultiProfile> profiles = MultiProfileManager.get().all();
            if (i >= 0 && i < profiles.size() && i < 12) {
                selectedProfile = i;
                MultiProfile p = profiles.get(i);
                serverField.setText(p.serverAddress);
            }
        }
        return super.mouseClicked(click, doubled);
    }

    private static double clickX(net.minecraft.client.gui.Click click) {
        try { return (double) click.getClass().getMethod("x").invoke(click); } catch (Throwable ignored) {}
        try { return (double) click.getClass().getMethod("getX").invoke(click); } catch (Throwable ignored) {}
        try { return ((Number) click.getClass().getField("x").get(click)).doubleValue(); } catch (Throwable ignored) {}
        return 0;
    }

    private static double clickY(net.minecraft.client.gui.Click click) {
        try { return (double) click.getClass().getMethod("y").invoke(click); } catch (Throwable ignored) {}
        try { return (double) click.getClass().getMethod("getY").invoke(click); } catch (Throwable ignored) {}
        try { return ((Number) click.getClass().getField("y").get(click)).doubleValue(); } catch (Throwable ignored) {}
        return 0;
    }
}
