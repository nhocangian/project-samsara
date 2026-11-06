#version 120

/* DRAWBUFFERS:0 */

// Nước / kính / vật trong suốt của Ký Ức Của Nam.
// Giữ màu từ màu đỉnh (màu nước của mod), thêm nắng, bóng và vệt phản chiếu mặt trời trên mặt nước.

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
    if (albedo.a < 0.02) discard;

    vec3 n = normalize(normalPlayer);
    vec3 l = normalize(sunDirPlayer);
    float NdotL = dot(n, l);

    float shadow = 0.0;
    if (NdotL > 0.0) {
        shadow = nmShadow(playerPos, n, NdotL, gl_FragCoord.xy);
    }

    vec3 baseColor = albedo.rgb * color.rgb;
    vec3 lit = nmLighting(baseColor, lmCoord, NdotL, shadow, 0.0);

    // Vệt nắng lấp lánh (Blinn-Phong) — đủ sáng để bloom bắt được
    vec3 v = -normalize(playerPos);
    vec3 h = normalize(l + v);
    float spec = pow(max(dot(n, h), 0.0), 120.0) * shadow;
    lit += NM_SUN_COLOR * spec * 3.0;

    float renderDist = far > 16.0 ? far : 192.0;
    vec3 finalColor = nmApplyFog(lit, playerPos, l, renderDist);

    gl_FragData[0] = vec4(finalColor, albedo.a * color.a);
}
