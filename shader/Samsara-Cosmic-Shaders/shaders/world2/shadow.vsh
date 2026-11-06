#version 120

// Pass bóng đổ của Ký Ức Của Nam: vẽ thế giới từ phía mặt trời.
// Hình học được bẻ cong y hệt gbuffers (inceptionCurvePlayer) để bóng khớp với mặt đất cong.

#include "/lib/inception_curve.glsl"
#include "/lib/nam_memory_common.glsl"
#include "/lib/nam_memory_wind.glsl"

uniform mat4 shadowModelView;
uniform int renderStage;
uniform mat4 shadowModelViewInverse;
uniform mat4 shadowProjection;

varying vec2 texCoord;
varying vec4 color;

void main() {
    vec4 shadowView = gl_ModelViewMatrix * gl_Vertex;
    vec3 playerPos = (shadowModelViewInverse * shadowView).xyz;

    // Bóng của hoa cũng đung đưa theo gió (chỉ với block, không áp lên thực thể)
#if defined(MC_RENDER_STAGE_TERRAIN_CUTOUT) && defined(MC_RENDER_STAGE_TERRAIN_CUTOUT_MIPPED)
    if (renderStage == MC_RENDER_STAGE_TERRAIN_CUTOUT || renderStage == MC_RENDER_STAGE_TERRAIN_CUTOUT_MIPPED) {
        playerPos += nmWindOffset(playerPos, gl_Normal);
    }
#endif

    playerPos = inceptionCurvePlayer(playerPos);

    vec4 clip = shadowProjection * (shadowModelView * vec4(playerPos, 1.0));
    clip.xyz = nmDistortShadow(clip.xyz);
    gl_Position = clip;

    texCoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
    color = gl_Color;
}
