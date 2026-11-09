#version 120

/* DRAWBUFFERS:02 */

// Mặt nước Đường Ống: chỉ là một lớp mỏng hơi đục để thấy gạch đáy.
// Không dùng texture nước của vanilla (nó gợn và lấp lánh) và không dùng màu biome (vàng rêu).
// Phản chiếu được vẽ trong composite.fsh; colortex2.r đánh dấu pixel là mặt nước.

#include "/lib/tang_duoi_light.glsl"

uniform sampler2D tex;
uniform sampler2D lightmap;
uniform vec3 fogColor;
uniform float far;

varying vec2 texCoord;
varying vec2 lmCoord;
varying vec4 color;
varying vec3 viewPos;
varying float isTopFace;

const vec3 WATER_TINT = vec3(0.60, 0.70, 0.68);
const float WATER_ALPHA = 0.26;

void main() {
    if (texture2D(tex, texCoord).a < 0.02) discard;

    vec3 light = coolBlockLight(texture2D(lightmap, lmCoord).rgb);
    vec3 waterColor = WATER_TINT * light;

    float dist = length(viewPos);
    float renderDist = far > 16.0 ? far : 192.0;
    float fogFactor = smoothstep(renderDist * 0.50, renderDist * 0.95, dist);

    gl_FragData[0] = vec4(mix(waterColor, fogColor, fogFactor), WATER_ALPHA);
    // Alpha = 1 để phép hoà trộn không làm loãng dấu hiệu
    gl_FragData[1] = vec4(isTopFace, lmCoord.x, 0.0, 1.0);   // g = ánh sáng block thật
}
