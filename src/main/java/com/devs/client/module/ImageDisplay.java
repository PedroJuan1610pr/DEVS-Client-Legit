package com.devs.client.module;

import com.devs.client.hud.HudLayoutManager;
import com.devs.client.setting.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * Muestra un PNG o GIF en el HUD.
 * Pon el archivo en {@code .minecraft/config/devs/images/} y escribe el nombre en File Name.
 */
public final class ImageDisplay extends Module {
    private static final Logger LOG = LoggerFactory.getLogger("devs");
    private static final Identifier TEX_ID = Identifier.of("devs", "dynamic/image_display");

    private final MinecraftClient mc = MinecraftClient.getInstance();

    public final StringSetting fileName = add(new StringSetting("File Name", "image.png", 512));
    public final BooleanSetting reload = add(new BooleanSetting("Reload File", false));
    public final BooleanSetting openFolder = add(new BooleanSetting("Open Images Folder", false));
    public final NumberSetting width = add(new NumberSetting("Width (px)", 128, 16, 512, 4));
    public final NumberSetting opacity = add(new NumberSetting("Opacity", 1.0, 0.1, 1.0, 0.05));
    public final BooleanSetting keepAspect = add(new BooleanSetting("Keep Aspect", true));
    public final NumberSetting gifSpeed = add(new NumberSetting("GIF Speed", 1.0, 0.25, 3.0, 0.25));

    private final List<Frame> frames = new ArrayList<>();
    private int frameIndex;
    private long nextFrameAt;
    private int texW, texH;
    private String loadedPath = "";
    private String status = "No image";
    private NativeImageBackedTexture currentTex;

    private record Frame(NativeImage image, int delayMs) {}

    public ImageDisplay() {
        super("Image Display", "Shows a PNG or GIF from config/devs/images on your HUD.", "Misc");
    }

    public static Path imagesDir() {
        Path dir = FabricLoader.getInstance().getConfigDir().resolve("devs").resolve("images");
        try { Files.createDirectories(dir); } catch (Exception ignored) {}
        return dir;
    }

    @Override protected void onEnable() {
        imagesDir();
        tryLoad();
    }

    @Override protected void onDisable() {
        clearTexture();
        frames.clear();
        status = "Off";
    }

    @Override public void tick() {
        if (openFolder.get()) {
            openFolder.set(false);
            try {
                Path dir = imagesDir();
                java.awt.Desktop.getDesktop().open(dir.toFile());
                Notifications.push("Image Display", "Opened images folder");
            } catch (Exception e) {
                Notifications.push("Image Display", "Folder: " + imagesDir(), 0xFFFFD94D);
                LOG.warn("Could not open images folder", e);
            }
        }
        if (reload.get()) {
            reload.set(false);
            tryLoad();
            Notifications.push("Image Display", status.startsWith("OK") ? "Loaded" : status,
                status.startsWith("OK") ? 0xFF6FE3A1 : 0xFFFF4D6D);
        }
        // auto-reload if path setting changed
        String want = resolvePath().toString();
        if (enabled() && !want.equals(loadedPath) && !fileName.get().isBlank()) {
            tryLoad();
        }
        tickGif();
    }

    private void tickGif() {
        if (frames.size() <= 1) return;
        long now = System.currentTimeMillis();
        if (now < nextFrameAt) return;
        frameIndex = (frameIndex + 1) % frames.size();
        uploadFrame(frames.get(frameIndex).image());
        int delay = frames.get(frameIndex).delayMs();
        delay = Math.max(20, (int) (delay / Math.max(0.25, gifSpeed.get())));
        nextFrameAt = now + delay;
    }

    private Path resolvePath() {
        String name = fileName.get().trim();
        if (name.isEmpty()) return imagesDir().resolve("image.png");
        Path p = Path.of(name);
        if (p.isAbsolute()) return p;
        return imagesDir().resolve(name);
    }

    private void tryLoad() {
        clearTexture();
        frames.clear();
        frameIndex = 0;
        Path path = resolvePath();
        loadedPath = path.toString();
        if (!Files.isRegularFile(path)) {
            status = "Missing: " + path.getFileName();
            texW = 64; texH = 64;
            return;
        }
        String lower = path.getFileName().toString().toLowerCase(Locale.ROOT);
        try {
            if (lower.endsWith(".gif")) loadGif(path);
            else loadStill(path);
            if (frames.isEmpty()) {
                status = "Could not read image";
                return;
            }
            Frame first = frames.get(0);
            texW = first.image().getWidth();
            texH = first.image().getHeight();
            uploadFrame(first.image());
            nextFrameAt = System.currentTimeMillis() + first.delayMs();
            status = "OK " + texW + "x" + texH + (frames.size() > 1 ? " · " + frames.size() + " frames" : "");
        } catch (Exception e) {
            status = "Error: " + e.getClass().getSimpleName();
            LOG.warn("ImageDisplay load failed: {}", path, e);
            clearTexture();
            frames.clear();
        }
    }

    private void loadStill(Path path) throws Exception {
        try (InputStream in = Files.newInputStream(path)) {
            NativeImage img = NativeImage.read(in);
            frames.add(new Frame(img, 100));
        }
    }

    private void loadGif(Path path) throws Exception {
        try (ImageInputStream iis = ImageIO.createImageInputStream(Files.newInputStream(path))) {
            Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("gif");
            if (!readers.hasNext()) {
                // fallback: first frame only via ImageIO
                BufferedImage bi = ImageIO.read(path.toFile());
                if (bi != null) frames.add(new Frame(toNative(bi), 100));
                return;
            }
            ImageReader reader = readers.next();
            reader.setInput(iis, false);
            int n = reader.getNumImages(true);
            BufferedImage master = null;
            for (int i = 0; i < n; i++) {
                BufferedImage frame = reader.read(i);
                // composite simple: use frame as-is (most memes are full frames)
                if (master == null) {
                    master = new BufferedImage(frame.getWidth(), frame.getHeight(), BufferedImage.TYPE_INT_ARGB);
                }
                var g = master.createGraphics();
                g.drawImage(frame, 0, 0, null);
                g.dispose();
                BufferedImage copy = new BufferedImage(master.getWidth(), master.getHeight(), BufferedImage.TYPE_INT_ARGB);
                var g2 = copy.createGraphics();
                g2.drawImage(master, 0, 0, null);
                g2.dispose();
                int delay = readGifDelay(reader, i);
                frames.add(new Frame(toNative(copy), delay));
            }
            reader.dispose();
        }
    }

    private static int readGifDelay(ImageReader reader, int index) {
        try {
            IIOMetadata meta = reader.getImageMetadata(index);
            String metaFormat = meta.getNativeMetadataFormatName();
            IIOMetadataNode root = (IIOMetadataNode) meta.getAsTree(metaFormat);
            for (int i = 0; i < root.getLength(); i++) {
                if (!"GraphicControlExtension".equals(root.item(i).getNodeName())) continue;
                IIOMetadataNode gce = (IIOMetadataNode) root.item(i);
                String d = gce.getAttribute("delayTime");
                if (d != null && !d.isEmpty()) {
                    int cs = Integer.parseInt(d); // centiseconds
                    return Math.max(20, cs * 10);
                }
            }
        } catch (Exception ignored) {}
        return 100;
    }

    private static NativeImage toNative(BufferedImage bi) {
        int w = bi.getWidth(), h = bi.getHeight();
        NativeImage img = new NativeImage(w, h, false);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = bi.getRGB(x, y);
                img.setColorArgb(x, y, argb);
            }
        }
        return img;
    }

    private void uploadFrame(NativeImage src) {
        destroyCurrentTex();
        NativeImage copy = new NativeImage(src.getWidth(), src.getHeight(), false);
        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                copy.setColorArgb(x, y, src.getColorArgb(x, y));
            }
        }
        // 1.21.11: requiere Supplier<String> + NativeImage
        currentTex = new NativeImageBackedTexture(() -> "devs-image-display", copy);
        mc.getTextureManager().registerTexture(TEX_ID, currentTex);
    }

    private void destroyCurrentTex() {
        if (currentTex == null) return;
        try { mc.getTextureManager().destroyTexture(TEX_ID); } catch (Throwable ignored) {}
        try { currentTex.close(); } catch (Throwable ignored) {}
        currentTex = null;
    }

    private void clearTexture() {
        destroyCurrentTex();
        for (Frame f : frames) {
            try { f.image().close(); } catch (Throwable ignored) {}
        }
        frames.clear();
        loadedPath = "";
    }

    @Override public void renderHud(DrawContext ctx) {
        if (mc.player == null) return;
        if (ScreenshotClean.shouldHideHud()) return;
        if (mc.getDebugHud().shouldShowDebugHud()) return;

        int drawW = width.get().intValue();
        int drawH = drawW;
        if (keepAspect.get() && texW > 0 && texH > 0) {
            drawH = Math.max(8, Math.round(drawW * (texH / (float) texW)));
        }

        HudLayoutManager.begin(ctx, HudLayoutManager.IMAGE_DISPLAY, drawW, drawH);
        if (currentTex != null && texW > 0) {
            // full texture mapped to panel size
            ctx.drawTexture(RenderPipelines.GUI_TEXTURED, TEX_ID,
                0, 0, 0f, 0f,
                drawW, drawH, drawW, drawH, drawW, drawH);
        } else {
            ctx.fill(0, 0, drawW, drawH, 0x88000000);
            String msg = status.length() > 24 ? status.substring(0, 24) + "…" : status;
            ctx.drawTextWithShadow(mc.textRenderer, msg, 4, Math.max(2, drawH / 2 - 4), 0xFFFFAAAA);
        }
        HudLayoutManager.end(ctx);
    }
}
