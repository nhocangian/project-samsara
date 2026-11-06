#version 120

/* DRAWBUFFERS:0 */

// Bầu trời trưa của Ký Ức Của Nam: xanh đậm trong vắt ở đỉnh, nhạt dần về chân trời,
// dải nắng vàng sát chân trời và quầng quanh mặt trời. Bỏ qua màu trời vanilla.

#include "/lib/nam_memory_common.glsl"

uniform mat4 gbufferModelViewInverse;
uniform vec3 sunPosition;
uniform int renderStage;

varying vec4 starColor;
varying vec3 viewPosSky;

void main() {
#ifdef MC_RENDER_STAGE_STARS
    // Trưa vĩnh viễn: không vẽ sao
    if (renderStage == MC_RENDER_STAGE_STARS) discard;
#endif

    mat3 viewToPlayer = mat3(gbufferModelViewInverse);
    vec3 dir = normalize(viewToPlayer * viewPosSky);
    vec3 sunDir = normalize(viewToPlayer * sunPosition);

    vec3 sky = nmSkyColor(dir, sunDir);

    // Nhiễu rất nhẹ chống dải màu (banding) trên gradient
    float dither = fract(52.9829189 * fract(dot(gl_FragCoord.xy, vec2(0.06711056, 0.00583715)))) - 0.5;
    sky += dither * (1.5 / 255.0);

    gl_FragData[0] = vec4(sky, 1.0);
}
