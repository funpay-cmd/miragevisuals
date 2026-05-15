#version 150

uniform vec2 ScreenSize;
uniform vec2 RectPos;
uniform vec2 RectSize;
uniform float RadiusTL;
uniform float RadiusTR;
uniform float RadiusBL;
uniform float RadiusBR;
uniform vec4 FillColor;
uniform vec4 BorderColor;
uniform float BorderWidth;

out vec4 fragColor;

float roundedBoxSDF(vec2 p, vec2 b, float tl, float tr, float bl, float br) {
    float r = (p.x > 0.0) ? ((p.y > 0.0) ? tr : br)
                          : ((p.y > 0.0) ? tl : bl);
    vec2 q = abs(p) - b + r;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r;
}

void main() {
    vec2 frag = gl_FragCoord.xy;
    vec2 center = RectPos + RectSize * 0.5;
    vec2 halfSize = RectSize * 0.5;
    vec2 p = frag - center;

    float dist = roundedBoxSDF(p, halfSize, RadiusTL, RadiusTR, RadiusBL, RadiusBR);

    float aa = 1.0;
    float coverage = 1.0 - smoothstep(-aa, aa, dist);
    if (coverage <= 0.0) {
        discard;
    }

    vec4 color = FillColor;
    if (BorderWidth > 0.0) {
        float borderEdge = -BorderWidth;
        float borderMix = 1.0 - smoothstep(borderEdge - aa, borderEdge, dist);
        color = mix(BorderColor, FillColor, borderMix);
    }

    fragColor = vec4(color.rgb, color.a * coverage);
}
