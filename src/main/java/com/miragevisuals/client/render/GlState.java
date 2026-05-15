package com.miragevisuals.client.render;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

/**
 * Tiny RAII-style helper for saving and restoring the bits of GL state
 * we touch during raw-GL shader passes (program, VAO, blend, depth, viewport).
 */
final class GlState {
    int prevProgram;
    int prevVao;
    int prevTex2D;
    int prevReadFbo;
    int prevDrawFbo;
    int[] prevViewport = new int[4];
    boolean prevBlend;
    boolean prevDepth;

    static GlState capture() {
        GlState s = new GlState();
        s.prevProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        s.prevVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        s.prevTex2D = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        s.prevReadFbo = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        s.prevDrawFbo = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, s.prevViewport);
        s.prevBlend = GL11.glIsEnabled(GL11.GL_BLEND);
        s.prevDepth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        return s;
    }

    void restore() {
        GL20.glUseProgram(prevProgram);
        GL30.glBindVertexArray(prevVao);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, prevTex2D);
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, prevReadFbo);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, prevDrawFbo);
        GL11.glViewport(prevViewport[0], prevViewport[1], prevViewport[2], prevViewport[3]);
        if (prevBlend) {
            GL11.glEnable(GL11.GL_BLEND);
        } else {
            GL11.glDisable(GL11.GL_BLEND);
        }
        if (prevDepth) {
            GL11.glEnable(GL11.GL_DEPTH_TEST);
        } else {
            GL11.glDisable(GL11.GL_DEPTH_TEST);
        }
    }
}
