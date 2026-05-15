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
 * Draws rounded rectangles via a small SDF-based fragment shader.
 *
 * <p>The shader (see {@code assets/miragevisuals/shaders/rounded_rect.{vsh,fsh}})
 * runs over a fullscreen quad and uses {@code gl_FragCoord} to compute the
 * signed distance to a rectangle in framebuffer pixel space, producing
 * smoothly anti-aliased corners and an optional border.</p>
 */
public final class RoundedRectRenderer {
    private static int program;
    private static int vao;
    private static int vbo;
    private static int uScreenSize;
    private static int uRectPos;
    private static int uRectSize;
    private static int uRadiusTL;
    private static int uRadiusTR;
    private static int uRadiusBL;
    private static int uRadiusBR;
    private static int uFillColor;
    private static int uBorderColor;
    private static int uBorderWidth;

    private static boolean initialized;
    private static boolean failed;

    private RoundedRectRenderer() {
    }

    /** Called from mod init; the actual GL objects are created lazily on the render thread. */
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
                "/assets/miragevisuals/shaders/rounded_rect.vsh",
                "/assets/miragevisuals/shaders/rounded_rect.fsh",
                new String[] {"Position"}
            );
            uScreenSize  = GL20.glGetUniformLocation(program, "ScreenSize");
            uRectPos     = GL20.glGetUniformLocation(program, "RectPos");
            uRectSize    = GL20.glGetUniformLocation(program, "RectSize");
            uRadiusTL    = GL20.glGetUniformLocation(program, "RadiusTL");
            uRadiusTR    = GL20.glGetUniformLocation(program, "RadiusTR");
            uRadiusBL    = GL20.glGetUniformLocation(program, "RadiusBL");
            uRadiusBR    = GL20.glGetUniformLocation(program, "RadiusBR");
            uFillColor   = GL20.glGetUniformLocation(program, "FillColor");
            uBorderColor = GL20.glGetUniformLocation(program, "BorderColor");
            uBorderWidth = GL20.glGetUniformLocation(program, "BorderWidth");

            vao = GL30.glGenVertexArrays();
            GL30.glBindVertexArray(vao);
            float[] quad = {
                0.0F, 0.0F,  1.0F, 0.0F,  1.0F, 1.0F,
                0.0F, 0.0F,  1.0F, 1.0F,  0.0F, 1.0F
            };
            FloatBuffer buf = BufferUtils.createFloatBuffer(quad.length);
            buf.put(quad).flip();
            vbo = GL15.glGenBuffers();
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buf, GL15.GL_STATIC_DRAW);
            GL20.glEnableVertexAttribArray(0);
            GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 0, 0L);
            GL30.glBindVertexArray(0);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
            initialized = true;
        } catch (Exception ex) {
            MirageVisualsClient.LOGGER.error("RoundedRectRenderer init failed", ex);
            failed = true;
        }
    }

    public static void draw(int x, int y, int width, int height, float radius, int argb) {
        draw(x, y, width, height, radius, radius, radius, radius, argb, 0, 0);
    }

    public static void draw(int x, int y, int width, int height,
                            float radiusTL, float radiusTR, float radiusBL, float radiusBR,
                            int argb, int borderArgb, float borderWidth) {
        ensureInitialized();
        if (!initialized || failed) {
            return;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        int fbW = mc.getWindow().getFramebufferWidth();
        int fbH = mc.getWindow().getFramebufferHeight();
        float scale = (float) mc.getWindow().getScaleFactor();

        float fx = x * scale;
        float fy = fbH - (y + height) * scale;
        float fw = width * scale;
        float fh = height * scale;

        float fillA = ((argb >> 24) & 0xFF) / 255.0F;
        float fillR = ((argb >> 16) & 0xFF) / 255.0F;
        float fillG = ((argb >>  8) & 0xFF) / 255.0F;
        float fillB = ( argb        & 0xFF) / 255.0F;

        float bA = ((borderArgb >> 24) & 0xFF) / 255.0F;
        float bR = ((borderArgb >> 16) & 0xFF) / 255.0F;
        float bG = ((borderArgb >>  8) & 0xFF) / 255.0F;
        float bB = ( borderArgb        & 0xFF) / 255.0F;

        GlState state = GlState.capture();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        RenderSystem.defaultBlendFunc();

        GL20.glUseProgram(program);
        GL30.glBindVertexArray(vao);
        GL20.glUniform2f(uScreenSize, fbW, fbH);
        GL20.glUniform2f(uRectPos, fx, fy);
        GL20.glUniform2f(uRectSize, fw, fh);
        GL20.glUniform1f(uRadiusTL, radiusTL * scale);
        GL20.glUniform1f(uRadiusTR, radiusTR * scale);
        GL20.glUniform1f(uRadiusBL, radiusBL * scale);
        GL20.glUniform1f(uRadiusBR, radiusBR * scale);
        GL20.glUniform4f(uFillColor, fillR, fillG, fillB, fillA);
        GL20.glUniform4f(uBorderColor, bR, bG, bB, bA);
        GL20.glUniform1f(uBorderWidth, borderWidth * scale);

        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);

        state.restore();
    }
}
