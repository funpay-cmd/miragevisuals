#version 150

// Soft frosted-glass panel.
//
// Renders a rounded rectangle with:
//   * a wide, smoothly anti-aliased coverage so the edges fade instead
//     of cutting hard against the scene (matches the reference mock);
//   * an outer halo / glow that extends past the body to give the
//     "floating blob" feel of the user's example;
//   * a gentle vertical light-to-base gradient inside the body;
//   * a soft top-light highlight that doesn't read as a hard band;
//   * an optional thin rounded border.
//
// The body is intentionally noise-free for a clean pastel look.

uniform vec2 ScreenSize;
uniform vec2 PanelPos;
uniform vec2 PanelSize;
uniform float Radius;
uniform vec4 TintColor;
uniform float TintMix;     // unused for now (kept for API compatibility)
uniform float Highlight;
uniform float BorderWidth;
uniform vec4 BorderColor;

in vec2 texCoord;
out vec4 fragColor;

float roundedBoxSDF(vec2 p, vec2 b, float r) {
    vec2 d = abs(p) - b + r;
    return length(max(d, 0.0)) + min(max(d.x, d.y), 0.0) - r;
}

void main() {
    vec2 pixelPos = gl_FragCoord.xy;
    vec2 center = PanelPos + PanelSize * 0.5;
    vec2 relPos = pixelPos - center;
    vec2 halfSize = PanelSize * 0.5;

    float dist = roundedBoxSDF(relPos, halfSize, Radius);

    // Outer halo: extends a few pixels past the body and fades out.
    float glowRadius = max(Radius * 0.9, 6.0);
    float glow = 1.0 - smoothstep(0.0, glowRadius, dist);
    glow = pow(max(glow, 0.0), 1.4);

    // Body coverage with a soft AA band so the edge feels diffused.
    float bodyAa = max(1.0, Radius * 0.18);
    float coverage = 1.0 - smoothstep(-bodyAa, bodyAa, dist);

    if (dist > glowRadius) {
        discard;
    }

    // Vertical gradient inside the panel.
    // gl_FragCoord.y grows upward in GL, so relPos.y > 0 = top of panel on screen.
    vec2 norm = relPos / max(halfSize, vec2(1.0));
    float vertical = clamp(norm.y * 0.5 + 0.5, 0.0, 1.0);
    vec3 baseTop = TintColor.rgb * 1.10;
    vec3 baseBot = TintColor.rgb * 0.90;
    vec3 body = mix(baseBot, baseTop, vertical);

    // Soft top light wash — 1 at the top edge, fading toward the middle.
    float topBand = smoothstep(halfSize.y - max(4.0, PanelSize.y * 0.5), halfSize.y, relPos.y);
    body += vec3(Highlight) * topBand * 0.10;

    // Soft inner sheen near the edge (very subtle).
    float edgeT = clamp(-dist, 0.0, max(Radius, 1.0));
    float innerSheen = smoothstep(max(Radius, 1.0), 0.0, edgeT);
    body += vec3(0.04) * innerSheen;

    // Combine body and glow: outside the body we just show a fading halo,
    // inside we show the body with optional border.
    vec3 color = body;
    float alpha = TintColor.a * coverage;

    // Optional border for buttons (set BorderWidth = 0 to disable).
    if (BorderWidth > 0.0) {
        float borderEdge = -BorderWidth;
        float borderMix = 1.0 - smoothstep(borderEdge - bodyAa, borderEdge, dist);
        color = mix(BorderColor.rgb, color, borderMix);
        alpha = mix(BorderColor.a * coverage, alpha, borderMix);
    }

    // Outer halo contribution (sampled outside the body).
    float outside = clamp(dist / glowRadius, 0.0, 1.0);
    float haloAlpha = TintColor.a * 0.45 * (1.0 - outside) * (1.0 - outside);
    // The halo color is the tint, slightly desaturated.
    vec3 haloColor = TintColor.rgb;

    float outsideMask = step(0.0, dist);          // 1 outside the body, 0 inside
    color = mix(color, haloColor, outsideMask * (1.0 - coverage));
    alpha = max(alpha, haloAlpha * (1.0 - coverage));

    // Slight overall glow even inside (additive top-up).
    alpha += glow * TintColor.a * 0.05 * coverage;

    fragColor = vec4(color, clamp(alpha, 0.0, 1.0));
}
