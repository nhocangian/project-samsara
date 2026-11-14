#version 120

/* DRAWBUFFERS:05 */

uniform sampler2D tex;
uniform sampler2D lightmap;
uniform vec3 fogColor;
uniform float far;
uniform vec4 entityColor;

varying vec2 texCoord;
varying vec2 lmCoord;
varying vec4 color;
varying vec3 viewPos;
varying vec3 worldPos;

#include "/lib/dem_dau_tien_tear.glsl"

void main() {
    vec4 albedo = texture2D(tex, texCoord);
    if (albedo.a < 0.1) discard;

    float inTear = tearMask(worldPos);
    // Lớp lông của cừu đã bị đánh mang màu đánh dấu r = 255, g = b = 253 (SheepWoolFeatureRendererMixin):
    // chỉ hiện bên trong vết rách nhỏ quanh nó; ngoài đó con cừu trụi lông
    vec4 tint = color;
    if (color.r > 0.998 && abs(color.g - 253.0 / 255.0) < 0.002 && abs(color.b - 253.0 / 255.0) < 0.002) {
        if (inTear < 0.5) discard;
        tint = vec4(1.0);
    }

    vec4 light = texture2D(lightmap, lmCoord);
    vec3 vanillaColor = albedo.rgb * tint.rgb * light.rgb;
    vanillaColor = mix(vanillaColor, entityColor.rgb, entityColor.a);
    if (inTear > 0.5) vanillaColor *= vec3(0.86, 0.88, 0.94);

    float dist = length(viewPos);
    float renderDist = far > 16.0 ? far : 192.0;
    float fogFactor = smoothstep(renderDist * 0.50, renderDist * 0.95, dist);

    vec3 finalColor = mix(vanillaColor, fogColor, fogFactor);

    gl_FragData[0] = vec4(finalColor, albedo.a);
    // Thực thể trong vết rách cũng nhoè màu glitch như khối (final.fsh đọc colortex5)
    gl_FragData[1] = vec4(inTear, 0.0, 0.0, 1.0);
}
