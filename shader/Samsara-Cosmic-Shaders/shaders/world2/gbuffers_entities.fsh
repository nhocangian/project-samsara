#version 120

/* DRAWBUFFERS:0 */

// Thực thể của Ký Ức Của Nam: cùng mô hình ánh sáng với gbuffers_terrain, cộng màu nháy sát thương.

const float sunPathRotation = -40.0; // phải khớp shadow.fsh

#include "/lib/nam_memory_common.glsl"
#include "/lib/nam_memory_shadow.glsl"

uniform sampler2D tex;
uniform sampler2D lightmap;
uniform vec3 fogColor;
uniform float far;
uniform vec4 entityColor;

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

    float shadow = 0.0;
    if (NdotL > 0.0) {
        shadow = nmShadow(playerPos, n, NdotL, gl_FragCoord.xy);
    }

    vec3 baseColor = albedo.rgb * color.rgb;
    baseColor = mix(baseColor, entityColor.rgb, entityColor.a);
    vec3 lit = nmLighting(baseColor, lmCoord, NdotL, shadow, 0.0);

    float renderDist = far > 16.0 ? far : 192.0;
    vec3 finalColor = nmApplyFog(lit, playerPos, l, renderDist);

    gl_FragData[0] = vec4(finalColor, albedo.a);
}
