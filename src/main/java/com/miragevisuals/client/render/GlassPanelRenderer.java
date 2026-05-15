package com.miragevisuals.client.render;

import com.miragevisuals.client.MirageVisualsClient;
import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.FloatBuffer;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

/**
 * Procedural frosted-glass primitive driven by the {@code glass_panel} shader.
 *
 * <p>The fragment shader computes a rounded-rectangle SDF in framebuffer-pixel
 * space, then synthesizes a glass-like body (vertical gradient, low-frequency
 * frost noise, top highlight band, soft inner glow, antialiased border) using
 * only the supplied uniforms. The final {@code TintColor.a} controls the
 * panel's overall translucency so the scene shows through.</p>
 */
public final class GlassPanelRenderer {
    private static int program;
    private static int vao;
    private static int vbo;

    private static int uScreenSize;
    private static int uPanelPos;
    private static int uPanelSize;
    private static int uRadius;
    private static int uTint;
    private static int uTintMix;
    private static int uHighlight;
    private static int uBorderWidth;
    private static int uBorderColor;

    private static boolean initialized;
    private static boolean failed;

    private GlassPanelRenderer() {
    }

    public static void markPending() {
        initialized = false;
        failed = false;
    }

    public static boolean isReady() {
        return initialized && !failed;
    }

    private static void ensureInitialized() {
        if (initialized || failed) {
            return;
        }
        try {
            program = ShaderCompiler.compileProgram(
                "/assets/miragevisuals/shaders/glass_panel.vsh",
                "/assets/miragevisuals/shaders/glass_panel.fsh",
                new String[] {"Position", "UV"}
            );
            uScreenSize  = GL20.glGetUniformLocation(program, "ScreenSize");
            uPanelPos    = GL20.glGetUniformLocation(program, "PanelPos");
            uPanelSize   = GL20.glGetUniformLocation(program, "PanelSize");
            uRadius      = GL20.glGetUniformLocation(program, "Radius");
            uTint        = GL20.glGetUniformLocation(program, "TintColor");
            uTintMix     = GL20.glGetUniformLocation(program, "TintMix");
            uHighlight   = GL20.glGetUniformLocation(program, "Highlight");
            uBorderWidth = GL20.glGetUniformLocation(program, "BorderWidth");
            uBorderColor = GL20.glGetUniformLocation(program, "BorderColor");

            float[] verts = {
                -1, -1, 0,  0, 0,
                 1, -1, 0,  1, 0,
                 1,  1, 0,  1, 1,
                -1, -1, 0,  0, 0,
                 1,  1, 0,  1, 1,
                -1,  1, 0,  0, 1,
            };
            vao = GL30.glGenVertexArrays();
            GL30.glBindVertexArray(vao);
            vbo = GL15.glGenBuffers();
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
            FloatBuffer buf = BufferUtils.createFloatBuffer(verts.length);
            buf.put(verts).flip();
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buf, GL15.GL_STATIC_DRAW);
            GL20.glEnableVertexAttribArray(0);
            GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 20, 0L);
            GL20.glEnableVertexAttribArray(1);
            GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, 20, 12L);
            GL30.glBindVertexArray(0);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
            initialized = true;
        } catch (Exception ex) {
            MirageVisualsClient.LOGGER.error("GlassPanelRenderer init failed", ex);
            failed = true;
        }
    }

    public static boolean drawPanel(int x, int y, int width, int height,
                                    float radius,
                                    float tintR, float tintG, float tintB, float tintA, float tintMix,
                                    float highlight,
                                    float borderWidth,
                                    float borderR, float borderG, float borderB, float borderA) {
        ensureInitialized();
        if (!initialized || failed) {
            return false;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        int fbW = mc.getWindow().getFramebufferWidth();
        int fbH = mc.getWindow().getFramebufferHeight();
        float scale = (float) mc.getWindow().getScaleFactor();

        float fx = x * scale;
        float fy = fbH - (y + height) * scale;
        float fw = width * scale;
        float fh = height * scale;
        float fr = radius * scale;
        float fbw = borderWidth * scale;

        GlState state = GlState.capture();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        RenderSystem.defaultBlendFunc();

        GL20.glUseProgram(program);
        GL30.glBindVertexArray(vao);
        GL20.glUniform2f(uScreenSize, fbW, fbH);
        GL20.glUniform2f(uPanelPos, fx, fy);
        GL20.glUniform2f(uPanelSize, fw, fh);
        GL20.glUniform1f(uRadius, fr);
        GL20.glUniform4f(uTint, tintR, tintG, tintB, tintA);
        GL20.glUniform1f(uTintMix, tintMix);
        GL20.glUniform1f(uHighlight, highlight);
        GL20.glUniform1f(uBorderWidth, fbw);
        GL20.glUniform4f(uBorderColor, borderR, borderG, borderB, borderA);

        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);

        state.restore();
        return true;
    }
}
