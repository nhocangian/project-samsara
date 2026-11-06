#version 120

/* DRAWBUFFERS:0 */

// Block của Ký Ức Của Nam: nắng trưa vàng ấm + bóng đổ mềm + sương vàng ở xa.
// Kết quả là màu HDR; composite3 lo bloom, tonemap và chỉnh màu.

const float sunPathRotation = -40.0; // phải khớp shadow.fsh

#include "/lib/nam_memory_common.glsl"
#include "/lib/nam_memory_shadow.glsl"

uniform sampler2D tex;
uniform sampler2D lightmap;
uniform vec3 fogColor;
uniform float far;

varying vec2 texCoord;
varying vec2 lmCoord;
varying vec4 color;
varying vec3 viewPos;
varying vec3 playerPos;
varying vec3 normalPlayer;
varying vec3 sunDirPlayer;

void main() {
    vec4 albedo = texture2D(tex, texCoord);
    if (albedo.a < 0.1) discard;

    vec3 n = normalize(normalPlayer);
    vec3 l = normalize(sunDirPlayer);
    float NdotL = dot(n, l);
    float foliage = nmFoliageMask(n);

    // Cây cỏ hai mặt nhận bóng theo mặt hướng về nắng; khối đặc quay lưng thì khỏi tra bóng.
    float shadow = 0.0;
    if (NdotL > 0.0 || foliage > 0.01) {
        shadow = nmShadow(playerPos, n, NdotL, gl_FragCoord.xy);
    }

    vec3 baseColor = albedo.rgb * color.rgb;
    vec3 lit = nmLighting(baseColor, lmCoord, NdotL, shadow, foliage);

    float renderDist = far > 16.0 ? far : 192.0;
    vec3 finalColor = nmApplyFog(lit, playerPos, l, renderDist);

    gl_FragData[0] = vec4(finalColor, albedo.a);
}
