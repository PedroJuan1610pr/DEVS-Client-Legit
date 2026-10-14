package com.devs.client.module;

import com.devs.client.setting.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Utilidades de chat: hora, resaltado de tu nombre con sonido, y filtro anti-spam. Se engancha en ChatHudMixin. */
public final class ChatUtils extends Module {
    private final MinecraftClient mc = MinecraftClient.getInstance();
    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter HMS = DateTimeFormatter.ofPattern("HH:mm:ss");

    public final BooleanSetting timestamps = add(new BooleanSetting("Timestamps", true));
    public final BooleanSetting seconds = add(new BooleanSetting("Show Seconds", false));
    public final BooleanSetting highlight = add(new BooleanSetting("Highlight My Name", true));
    public final ColorSetting highlightColor = add(new ColorSetting("Highlight Color", 0xFFFFD94D));
    public final BooleanSetting mentionSound = add(new BooleanSetting("Mention Sound", true));
    public final BooleanSetting antiSpam = add(new BooleanSetting("Anti Spam", true));
    public final NumberSetting spamWindow = add(new NumberSetting("Spam Window (s)", 5, 1, 30, 1));

    private final Map<String, Long> recent = new HashMap<>();

    public ChatUtils() { super("Chat Utils", "Timestamps, name highlight + sound and anti-spam for chat.", "Misc"); }

    @Override protected void onDisable() { recent.clear(); }

    /** true = el mensaje es un repetido reciente y se descarta. */
    public boolean shouldBlock(Text msg) {
        if (!enabled() || !antiSpam.get()) return false;
        String key = msg.getString();
        if (key.isBlank() || key.startsWith("[DEVS]")) return false;
        long now = System.currentTimeMillis(), win = (long) (spamWindow.get() * 1000);
        if (recent.size() > 64) recent.values().removeIf(t -> now - t > win);
        Long last = recent.put(key, now);
        return last != null && now - last <= win;
    }

    /** Devuelve el mensaje con hora y resaltado de tu nombre. */
    public Text process(Text msg) {
        if (!enabled() || mc.player == null) return msg;
        String plain = msg.getString();
        Text out = msg;

        if (!plain.startsWith("[DEVS]")) {
            String me = mc.player.getName().getString();
            boolean mine = plain.startsWith("<" + me + ">");   // tus propios mensajes no cuentan como mención
            boolean mention = !mine && !me.isEmpty() && plain.toLowerCase(Locale.ROOT).contains(me.toLowerCase(Locale.ROOT));
            if (mention) {
                if (highlight.get()) out = highlightName(out, me);
                if (mentionSound.get())
                    if (mc.player != null) mc.player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.0f);
            }
        }
        if (timestamps.get()) {
            String t = LocalTime.now().format(seconds.get() ? HMS : HM);
            out = Text.literal("[" + t + "] ").formatted(Formatting.GRAY).append(out);
        }
        return out;
    }

    private Text highlightName(Text msg, String me) {
        MutableText out = Text.empty();
        String lower = me.toLowerCase(Locale.ROOT);
        TextColor tc = TextColor.fromRgb(highlightColor.get() & 0xFFFFFF);
        msg.<Boolean>visit((style, str) -> {
            String ls = str.toLowerCase(Locale.ROOT);
            if (ls.length() != str.length()) { out.append(Text.literal(str).setStyle(style)); return Optional.empty(); }
            int i = 0, idx;
            while ((idx = ls.indexOf(lower, i)) >= 0) {
                if (idx > i) out.append(Text.literal(str.substring(i, idx)).setStyle(style));
                out.append(Text.literal(str.substring(idx, idx + lower.length())).setStyle(style.withColor(tc).withBold(true)));
                i = idx + lower.length();
            }
            if (i < str.length()) out.append(Text.literal(str.substring(i)).setStyle(style));
            return Optional.empty();
        }, Style.EMPTY);
        return out;
    }
}
