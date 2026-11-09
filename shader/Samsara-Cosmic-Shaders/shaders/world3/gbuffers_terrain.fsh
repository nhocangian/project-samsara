#version 120

/* DRAWBUFFERS:02 */

#include "/lib/tang_duoi_light.glsl"
#include "/lib/tang_duoi_wet.glsl"

uniform sampler2D tex;
uniform sampler2D lightmap;
uniform vec3 fogColor;
uniform float far;
uniform mat4 gbufferModelView;
uniform mat4 gbufferModelViewInverse;
uniform vec3 cameraPosition;

varying vec2 texCoord;
varying vec2 lmCoord;
varying vec4 color;
varying vec3 viewPos;
varying vec3 viewNormal;

void main() {
    vec4 albedo = texture2D(tex, texCoord);
    if (albedo.a < 0.1) discard;

    // Giữ nguyên 100% đồ họa, độ sáng, AO và màu sắc gốc của Vanilla Minecraft
    vec4 light = texture2D(lightmap, lmCoord);
    vec3 vanillaColor = albedo.rgb * color.rgb * coolBlockLight(light.rgb);

    // Cống ẩm: tính trước sương mù để ở xa vẫn tan vào sương
    vec3 worldPos = (gbufferModelViewInverse * vec4(viewPos, 1.0)).xyz + cameraPosition;
    vec3 worldNormal = mat3(gbufferModelViewInverse) * normalize(viewNormal);
    vec3 viewUp = mat3(gbufferModelView) * vec3(0.0, 1.0, 0.0);
    float lightLevel = dot(light.rgb, vec3(0.299, 0.587, 0.114));
    float wet = wetMask(worldPos, worldNormal);
    vanillaColor = applyWetness(vanillaColor, viewPos, viewNormal, viewUp, wet, lightLevel);

    // Mặt khối (6 hướng) mã hoá vào g cho composite.fsh
    vec3 an = abs(worldNormal);
    float face = (an.x > an.y && an.x > an.z) ? (worldNormal.x > 0.0 ? 0.0 : 1.0)
               : (an.y > an.z) ? (worldNormal.y > 0.0 ? 2.0 : 3.0)
               : (worldNormal.z > 0.0 ? 4.0 : 5.0);

    // Chỉ bổ sung sương mù mềm mại ở tầm xa để che phủ ranh giới chunk
    float dist = length(viewPos);
    float renderDist = far > 16.0 ? far : 192.0;
    float fogFactor = smoothstep(renderDist * 0.50, renderDist * 0.95, dist);

    vec3 finalColor = mix(vanillaColor, fogColor, fogFactor);

    gl_FragData[0] = vec4(finalColor, albedo.a);
    gl_FragData[1] = vec4(0.0, (face + 0.5) / 6.0, wet, 1.0);
}
