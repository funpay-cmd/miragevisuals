package com.miragevisuals.client.render;

import com.miragevisuals.client.MirageVisualsClient;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.lwjgl.opengl.GL20;

/**
 * Reads shader sources from the mod's resources and compiles them into a GL program.
 * We bypass Minecraft's shader pipeline entirely so we can use raw uniforms and
 * keep MirageVisuals's effects independent of vanilla render-layer juggling.
 */
final class ShaderCompiler {
    private ShaderCompiler() {
    }

    static String load(String resource) throws IOException {
        try (InputStream in = ShaderCompiler.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IOException("Shader resource not found: " + resource);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    static int compileProgram(String vshResource, String fshResource, String[] attribs) throws IOException {
        String vsh = load(vshResource);
        String fsh = load(fshResource);

        int vs = compileShader(GL20.GL_VERTEX_SHADER, vsh, vshResource);
        int fs;
        try {
            fs = compileShader(GL20.GL_FRAGMENT_SHADER, fsh, fshResource);
        } catch (RuntimeException ex) {
            GL20.glDeleteShader(vs);
            throw ex;
        }

        int program = GL20.glCreateProgram();
        GL20.glAttachShader(program, vs);
        GL20.glAttachShader(program, fs);
        if (attribs != null) {
            for (int i = 0; i < attribs.length; i++) {
                GL20.glBindAttribLocation(program, i, attribs[i]);
            }
        }
        GL20.glLinkProgram(program);
        if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == 0) {
            String log = GL20.glGetProgramInfoLog(program);
            GL20.glDeleteProgram(program);
            GL20.glDeleteShader(vs);
            GL20.glDeleteShader(fs);
            throw new RuntimeException("Shader link failed (" + vshResource + " + " + fshResource + "): " + log);
        }

        GL20.glDetachShader(program, vs);
        GL20.glDetachShader(program, fs);
        GL20.glDeleteShader(vs);
        GL20.glDeleteShader(fs);

        MirageVisualsClient.LOGGER.info("Linked GL program {} ({} + {})", program, vshResource, fshResource);
        return program;
    }

    private static int compileShader(int type, String source, String label) {
        int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, source);
        GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == 0) {
            String log = GL20.glGetShaderInfoLog(shader);
            GL20.glDeleteShader(shader);
            throw new RuntimeException("Shader compile failed (" + label + "): " + log);
        }
        return shader;
    }
}
