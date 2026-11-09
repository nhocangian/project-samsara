#version 120

/* DRAWBUFFERS:0 */

#include "/lib/loi_di_nhap_sky.glsl"

uniform sampler2D tex;
uniform sampler2D lightmap;
uniform float far;

varying vec2 texCoord;
varying vec2 lmCoord;
varying vec4 color;
varying vec3 viewPos;
varying vec3 worldNormal;

const vec3 SUN_WARM = vec3(1.10, 0.93, 0.72);   // mặt quay về phía mặt trời
const vec3 SUN_COOL = vec3(0.90, 0.80, 0.90);   // mặt khuất
const vec3 SHADOW_TINT = vec3(0.62, 0.56, 0.80);   // màu nhân vào chỗ bị che nắng

// Chiều này khoá ở giữa trưa: nghiêng đường đi mặt trời để nó nằm thấp về phía biển.
// Bóng đổ của Iris đi theo góc này. Đổi góc thì đổi cả LDN_SUN_POS trong lib/loi_di_nhap_sky.glsl.
const float sunPathRotation = 78.0;
const int shadowMapResolution = 4096;
const float shadowDistance = 96.0;

uniform sampler2D shadowtex0;
uniform mat4 gbufferModelViewInverse;
uniform mat4 shadowModelView;
uniform mat4 shadowProjection;

const int   SHADOW_TAPS = 16;
const float SHADOW_SOFTNESS = 3.0;    // độ mềm của mép bóng: tăng thì mờ hơn, giảm thì sắc hơn

// So sánh bốn điểm ảnh bóng gần nhất rồi trộn mượt theo vị trí
float shadowTap(vec2 uv, float reference) {
    float size = float(shadowMapResolution);
    vec2 texel = uv * size - 0.5;
    vec2 base = (floor(texel) + 0.5) / size;
    vec2 f = fract(texel);
    float a = step(reference, texture2D(shadowtex0, base).r);
    float b = step(reference, texture2D(shadowtex0, base + vec2(1.0 / size, 0.0)).r);
    float c = step(reference, texture2D(shadowtex0, base + vec2(0.0, 1.0 / size)).r);
    float d = step(reference, texture2D(shadowtex0, base + vec2(1.0 / size)).r);
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

// 1 = có nắng, 0 = bị che. Lấy nhiều điểm quanh vị trí để mép bóng mềm.
float sunVisibility(vec3 normal, float dist) {
    if (dist > shadowDistance * 0.95) return 1.0;

    vec3 player = (gbufferModelViewInverse * vec4(viewPos, 1.0)).xyz + normal * 0.15;
    vec3 coord = (shadowProjection * shadowModelView * vec4(player, 1.0)).xyz * 0.5 + 0.5;
    if (coord.x < 0.0 || coord.x > 1.0 || coord.y < 0.0 || coord.y > 1.0 || coord.z > 1.0) return 1.0;

    // Lấy mẫu rải đều trên một đĩa nhỏ, mỗi mẫu đã được trộn mượt nên mép mềm mà không thành bậc thang
    float reference = coord.z - 0.0004;
    float lit = 0.0;
    for (int i = 0; i < SHADOW_TAPS; i++) {
        float angle = float(i) * 2.39996;
        float radius = sqrt((float(i) + 0.5) / float(SHADOW_TAPS)) * SHADOW_SOFTNESS;
        lit += shadowTap(coord.xy + vec2(cos(angle), sin(angle)) * radius / float(shadowMapResolution), reference);
    }
    lit /= float(SHADOW_TAPS);
    return mix(lit, 1.0, smoothstep(shadowDistance * 0.75, shadowDistance * 0.95, dist));
}

void main() {
    vec4 albedo = texture2D(tex, texCoord);
    if (albedo.a < 0.1) discard;

    // Giữ nguyên 100% đồ họa, độ sáng, AO và màu sắc gốc của Vanilla Minecraft
    vec4 light = texture2D(lightmap, lmCoord);
    // Bóng phẳng: bớt chênh sáng giữa các mặt khối và bớt tối ở chỗ khuất
    float maxShade = max(max(color.r, color.g), max(color.b, 0.001));
    vec3 flatShade = mix(color.rgb, color.rgb / maxShade, 0.55);
    vec3 flatLight = mix(light.rgb, vec3(1.0), 0.50);
    // Nắng có hướng: phía mặt trời ấm vàng đào, phía khuất ngả tím xanh
    vec3 normal = normalize(worldNormal);
    float facing = dot(normal, normalize(LDN_SUN_POS));
    vec3 sunTint = mix(SUN_COOL, SUN_WARM, smoothstep(-0.5, 0.7, facing));

    // Mặt quay lưng với mặt trời đã có màu khuất, không tính bóng để tránh tự che chính nó
    if (facing > 0.02) sunTint *= mix(SHADOW_TINT, vec3(1.0), sunVisibility(normal, length(viewPos)));

    vec3 vanillaColor = clamp(albedo.rgb * flatShade * flatLight * sunTint, 0.0, 1.0);

    // Chỉ bổ sung sương mù mềm mại ở tầm xa để che phủ ranh giới chunk
    float dist = length(viewPos);
    float renderDist = far > 16.0 ? far : 192.0;
    float fogFactor = smoothstep(renderDist * 0.50, renderDist * 0.95, dist);

    vec3 finalColor = mix(vanillaColor, LDN_HORIZON, fogFactor);

    gl_FragData[0] = vec4(finalColor, albedo.a);
}
