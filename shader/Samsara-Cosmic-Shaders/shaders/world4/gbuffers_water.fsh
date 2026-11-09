#version 120

/* DRAWBUFFERS:02 */

// Biển Lối đi nháp: xanh ngọc sáng có gợn sóng và vân nắng mảnh, nhìn xiên thì phản chiếu trời.
// Đục hoàn toàn (alpha = 1) để không bao giờ thấy đáy.

#include "/lib/loi_di_nhap_sky.glsl"

uniform sampler2D tex;
uniform mat4 gbufferModelViewInverse;
uniform vec3 cameraPosition;
uniform float far;
uniform float frameTimeCounter;
uniform int isEyeInWater;

varying vec2 texCoord;
varying vec2 lmCoord;
varying vec4 color;
varying vec3 viewPos;

const vec3 TURQ_BRIGHT  = vec3(0.01, 0.46, 0.50);
const vec3 TURQ_DEEP    = vec3(0.00, 0.27, 0.36);
const vec3 CAUSTIC_TINT = vec3(0.45, 0.85, 0.85);
const float WAVE_STRENGTH = 0.35;

float waveHeight(vec2 p, float t) {
    return ldnNoise(p * 0.35 + vec2(t * 0.12, t * 0.07)) * 0.60
         + ldnNoise(p * 0.90 - vec2(t * 0.20, -t * 0.11)) * 0.30
         + ldnNoise(p * 2.10 + vec2(-t * 0.15, t * 0.30)) * 0.10;
}

// Vân sáng dạng lưới mảnh, như nắng xuyên qua mặt nước
float causticLines(vec2 p, float t) {
    vec2 warp = vec2(ldnNoise(p * 0.45 + t * 0.06), ldnNoise(p * 0.45 + 31.0 - t * 0.05));
    float n = ldnNoise(p * 1.30 + warp * 3.0);
    return pow(1.0 - abs(2.0 * n - 1.0), 24.0);
}

void main() {
    if (texture2D(tex, texCoord).a < 0.02) discard;

    vec3 relative = (gbufferModelViewInverse * vec4(viewPos, 1.0)).xyz;
    vec3 worldPos = relative + cameraPosition;
    vec3 dir = normalize(relative);
    float dist = length(viewPos);
    float t = frameTimeCounter;

    // Gợn sóng yếu dần theo khoảng cách để xa không bị nhiễu hạt
    float e = 0.08;
    float h0 = waveHeight(worldPos.xz, t);
    vec2 slope = vec2(waveHeight(worldPos.xz + vec2(e, 0.0), t) - h0,
                      waveHeight(worldPos.xz + vec2(0.0, e), t) - h0) / e;
    float strength = WAVE_STRENGTH / (1.0 + dist * 0.04);
    vec3 normal = normalize(vec3(-slope.x * strength, 1.0, -slope.y * strength));

    float cosWave = clamp(dot(-dir, normal), 0.0, 1.0);
    float fresnel = 0.02 + 0.98 * pow(1.0 - cosWave, 5.5);

    // Màu thân nước: loang giữa hai sắc ngọc, thêm vân nắng mảnh
    vec3 body = mix(TURQ_DEEP, TURQ_BRIGHT, ldnNoise(worldPos.xz / 40.0));
    body += CAUSTIC_TINT * causticLines(worldPos.xz, t) * 0.14 / (1.0 + dist * 0.03);

    // Kéo ánh phản chiếu về màu đào: màu tím giữa trời trộn với xanh ngọc sẽ ra xám
    vec3 reflected = mix(ldnSkySoft(worldPos, reflect(dir, normal)), LDN_HORIZON, 0.65);
    vec3 col = mix(body, reflected, fresnel);

    // Vệt nắng lấp lánh trên mặt nước, kéo từ mặt trời về phía người nhìn
    float glint = max(dot(reflect(dir, normal), normalize(LDN_SUN_POS)), 0.0);
    col += vec3(1.0, 0.80, 0.55) * pow(glint, 120.0) * 0.55;

    // Từ dưới nước nhìn lên: chỉ thấy trời ở khoảng ngay trên đầu, xung quanh là mặt nước tối
    if (isEyeInWater == 1) {
        col = mix(TURQ_DEEP, LDN_HORIZON, smoothstep(0.55, 0.88, dir.y));
    }

    // Xa dần tan vào chân trời
    float renderDist = far > 16.0 ? far : 192.0;
    col = mix(col, LDN_HORIZON, smoothstep(renderDist * 0.60, renderDist * 1.00, dist));

    gl_FragData[0] = vec4(col, 1.0);
    // colortex2 cho phản chiếu trong composite: rg = độ nghiêng sóng, b = 1 là mặt nước nhìn từ trên
    gl_FragData[1] = vec4(clamp(normal.xz * 2.0 + 0.5, 0.0, 1.0), isEyeInWater == 1 ? 0.0 : 1.0, 1.0);
}
