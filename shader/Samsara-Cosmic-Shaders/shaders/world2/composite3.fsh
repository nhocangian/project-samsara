#version 120

/* DRAWBUFFERS:0 */

// Pass 4/4: cộng bloom, tonemap ACES và chỉnh màu kiểu "trưa hè rực rỡ":
// bão hoà cao, highlight vàng ấm, trời xanh đậm, tối góc nhẹ.
// final.fsh (VHS / IR / glitch) chạy sau và nhận ảnh đã chỉnh màu này.

const bool colortex1MipmapEnabled = true;

#define NM_BLOOM_STRENGTH 0.42  // [0.0 0.2 0.42 0.6 0.8]
#define NM_EXPOSURE       0.95  // [0.75 0.85 0.95 1.05 1.2]
#define NM_SATURATION     1.20  // [1.0 1.1 1.2 1.3 1.45]
#define NM_VIGNETTE       0.38  // [0.0 0.2 0.38 0.55]
#define NM_YELLOW_BOOST   0.40  // làm vàng hoa hướng dương đậm hơn [0.0 0.2 0.4 0.6]

#include "/lib/nam_memory_common.glsl"

uniform sampler2D colortex0;
uniform sampler2D colortex1;

varying vec2 texCoord;

void main() {
    vec3 col = texture2D(colortex0, texCoord).rgb;

    // Bloom nhiều tầng: lõi chặt + quầng rộng
    vec3 bloom = texture2D(colortex1, texCoord).rgb * 0.55
               + texture2D(colortex1, texCoord, 2.5).rgb * 0.35
               + texture2D(colortex1, texCoord, 4.5).rgb * 0.25;
    col += bloom * NM_BLOOM_STRENGTH;

    col = nmTonemap(col * NM_EXPOSURE);

    // Bão hoà
    float L = nmLuma(col);
    col = mix(vec3(L), col, NM_SATURATION);

    // Tăng riêng sắc vàng: ACES làm vàng sáng ngả trắng, nên kéo kênh xanh dương xuống
    // ở những pixel đã có R và G cao hơn B (vàng / vàng cam). Trời xanh và lá xanh không bị ảnh hưởng.
    float mx = max(col.r, col.g);
    float yellowness = clamp((min(col.r, col.g) - col.b) / max(mx, 1e-3), 0.0, 1.0);
    yellowness *= 1.0 - clamp(abs(col.r - col.g) / max(mx, 1e-3) * 2.0, 0.0, 1.0);
    col.b *= 1.0 - NM_YELLOW_BOOST * smoothstep(0.05, 0.5, yellowness);
    col.g *= 1.0 - NM_YELLOW_BOOST * 0.08 * yellowness;

    // Highlight ấm, bóng tối hơi trong (split toning nhẹ)
    float hl = smoothstep(0.35, 0.95, L);
    col *= mix(vec3(0.97, 0.99, 1.04), vec3(1.05, 1.0, 0.90), hl);

    // Tương phản dạng S nhẹ
    col = clamp(col, 0.0, 1.0);
    col = mix(col, col * col * (3.0 - 2.0 * col), 0.22);

    // Tối góc
    vec2 q = texCoord - 0.5;
    col *= 1.0 - dot(q, q) * NM_VIGNETTE;

    // Dither chống dải màu
    float dither = fract(52.9829189 * fract(dot(gl_FragCoord.xy, vec2(0.06711056, 0.00583715)))) - 0.5;
    col += dither / 255.0;

    gl_FragData[0] = vec4(clamp(col, 0.0, 1.0), 1.0);
}
