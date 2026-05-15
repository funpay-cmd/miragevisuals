#version 150

// Clean translucent rounded rectangle.
//
// The first iteration shipped a wide halo / glow / inner sheen to mimic
// the original mock; the user since asked us to "remove the glow and
// make the buttons more transparent". So this shader is now intentionally
// minimal:
//
//   * a softly anti-aliased rounded body, nothing past the edge;
//   * a faint vertical gradient inside (kept tiny so the buttons don't
//     read as glossy plastic);
//   * an optional thin rounded border;
//   * an optional top sheen, gated by the Highlight uniform — set
//     Highlight = 0 from Java to turn it off entirely.

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

    // Tight AA band — just enough to soften the corner without bleeding out.
    float aa = 1.0;
    float coverage = 1.0 - smoothstep(-aa, aa, dist);
    if (coverage <= 0.0) {
        discard;
    }

    // Faint vertical gradient: top slightly brighter than bottom.
    vec2 norm = relPos / max(halfSize, vec2(1.0));
    float vertical = clamp(norm.y * 0.5 + 0.5, 0.0, 1.0);
    vec3 baseTop = TintColor.rgb * 1.04;
    vec3 baseBot = TintColor.rgb * 0.96;
    vec3 body = mix(baseBot, baseTop, vertical);

    // Optional top sheen — Java currently passes a tiny value here so the
    // button reads as glass rather than matte, but the user can dial it
    // to 0 to kill it completely.
    if (Highlight > 0.0) {
        float topBand = smoothstep(halfSize.y * 0.2, halfSize.y, relPos.y);
        body += vec3(Highlight) * topBand * 0.06;
    }

    vec3 color = body;
    float alpha = TintColor.a * coverage;

    // Optional thin border — disabled when BorderWidth = 0.
    if (BorderWidth > 0.0) {
        float borderEdge = -BorderWidth;
        float borderMix = 1.0 - smoothstep(borderEdge - aa, borderEdge, dist);
        color = mix(BorderColor.rgb, color, borderMix);
        alpha = mix(BorderColor.a * coverage, alpha, borderMix);
    }

    fragColor = vec4(color, clamp(alpha, 0.0, 1.0));
}
