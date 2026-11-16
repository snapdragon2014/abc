package com.example.piratetoken;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;

/**
 * Fortnite "Pirate Shop Access" ekranini sandik acilinca oynatir (animasyon + ses).
 * Animasyon reveal.png icindeki 70 karelik sprite sheet'ten, ses reveal.ogg'den gelir.
 * Test icin oyunda P tusuna bas. Oyuna girince ilk 12 saniye ekranda surum yazisi cikar.
 */
public class ChestHandler {

    private static final String VERSION = "2.0.0";

    private static final ResourceLocation SHEET =
            new ResourceLocation("piratetoken", "textures/gui/reveal.png");
    private static final ResourceLocation REVEAL_SOUND =
            new ResourceLocation("piratetoken:reveal");

    // sprite sheet: 3 sutun x 24 satir, her kare 384x130
    private static final int FW = 384, FH = 130, COLS = 3;
    private static final int SHEET_W = 1152, SHEET_H = 3120;
    private static final int FRAMES = 70;

    // 0-2 sn: saniyede 20 kare (40 kare), 2-5 sn: saniyede 10 kare (30 kare)
    private static final long FAST_MS = 2000;
    private static final long HOLD_END_MS = 7500;   // son kare bu sureye kadar ekranda kalir
    private static final long FADE_MS = 500;        // sonra solar
    private static final long CLOSE_FADE_MS = 300;  // sandik kapaninca hizli solar

    private boolean active = false;
    private boolean chestWasOpen = false;
    private long openTime = 0;
    private long closeTime = 0;

    private long diagStart = 0;
    private boolean pWasDown = false;

    private void start() {
        active = true;
        openTime = System.currentTimeMillis();
        closeTime = 0;
        try {
            Minecraft.getMinecraft().getSoundHandler()
                    .playSound(PositionedSoundRecord.create(REVEAL_SOUND, 1.0F));
        } catch (Exception ignored) { }
    }

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        if (event.gui instanceof GuiChest) {
            chestWasOpen = true;
            start();
        } else if (event.gui == null && chestWasOpen) {
            closeTime = System.currentTimeMillis();
            chestWasOpen = false;
        } else {
            chestWasOpen = false;
        }
    }

    @SubscribeEvent
    public void onDrawScreen(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (event.gui instanceof GuiChest) draw();
    }

    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.currentScreen != null) return;

        // P tusu = test
        boolean down = Keyboard.isKeyDown(Keyboard.KEY_P);
        if (down && !pWasDown) start();
        pWasDown = down;

        // yuklendi kontrolu (ilk 12 saniye)
        long now = System.currentTimeMillis();
        if (diagStart == 0) diagStart = now;
        if (now - diagStart < 12000) {
            mc.fontRendererObj.drawStringWithShadow(
                    "\u00a76Pirate Shop Access v" + VERSION + " \u00a7fyuklendi  |  P tusu = test", 4, 4, 0xFFFFFF);
        }

        draw();
    }

    private void draw() {
        if (!active) return;

        long now = System.currentTimeMillis();
        long elapsed = now - openTime;

        float alpha = 1f;
        if (elapsed > HOLD_END_MS) alpha = 1f - (elapsed - HOLD_END_MS) / (float) FADE_MS;
        if (closeTime > 0) alpha = Math.min(alpha, 1f - (now - closeTime) / (float) CLOSE_FADE_MS);
        if (alpha <= 0f) { active = false; return; }

        int idx;
        if (elapsed < FAST_MS) idx = (int) (elapsed / 50);
        else idx = 40 + (int) ((elapsed - FAST_MS) / 100);
        if (idx > FRAMES - 1) idx = FRAMES - 1;

        float u = (idx % COLS) * FW;
        float v = (idx / COLS) * FH;

        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution sr = new ScaledResolution(mc);
        float scale = Math.min(1.3f, sr.getScaledWidth() / 400f);

        GlStateManager.pushMatrix();
        GlStateManager.translate(sr.getScaledWidth() / 2f, 0f, 0f);
        GlStateManager.scale(scale, scale, 1f);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(1f, 1f, 1f, alpha);
        mc.getTextureManager().bindTexture(SHEET);
        Gui.drawModalRectWithCustomSizedTexture(-FW / 2, 0, u, v, FW, FH, SHEET_W, SHEET_H);
        GlStateManager.disableBlend();
        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.popMatrix();
    }
}
