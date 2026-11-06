#version 120

/* DRAWBUFFERS:0 */

// Tay cầm: cùng tông nắng với thế giới nhưng không tra bóng đổ (tránh bóng nhấp nháy trên tay).

#include "/lib/nam_memory_common.glsl"

uniform sampler2D tex;
uniform sampler2D lightmap;

varying vec2 texCoord;
varying vec2 lmCoord;
varying vec4 color;
varying vec3 normalView;
varying vec3 sunDirView;

void main() {
    vec4 albedo = texture2D(tex, texCoord);
    if (albedo.a < 0.1) discard;

    float NdotL = dot(normalize(normalView), normalize(sunDirView));
    // Nắng dịu hơn một chút và luôn có phần sáng tối thiểu để vật cầm tay dễ nhìn
    float soft = 0.35 + 0.65 * max(NdotL, 0.0);
    vec3 lit = nmLighting(albedo.rgb * color.rgb, lmCoord, soft, 0.85, 0.0);

    gl_FragData[0] = vec4(lit, albedo.a);
}
