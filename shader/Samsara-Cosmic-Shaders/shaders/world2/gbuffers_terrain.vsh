#version 120

#include "/lib/inception_curve.glsl"
#include "/lib/nam_memory_wind.glsl"

uniform vec3 shadowLightPosition;

varying vec2 texCoord;
varying vec2 lmCoord;
varying vec4 color;
varying vec3 viewPos;

// Thêm cho ánh sáng / bóng đổ (không gian người chơi: gốc = camera, trục theo thế giới)
varying vec3 playerPos;
varying vec3 normalPlayer;
varying vec3 sunDirPlayer;

void main() {
    // Gió: dời đỉnh cây cỏ trong không gian người chơi TRƯỚC khi bẻ cong
    vec3 flatPlayer = (gbufferModelViewInverse * (gl_ModelViewMatrix * gl_Vertex)).xyz;
    flatPlayer += nmWindOffset(flatPlayer, gl_Normal);
    vec4 curvedView = inceptionCurve(gbufferModelView * vec4(flatPlayer, 1.0));
    gl_Position = gl_ProjectionMatrix * curvedView;
    texCoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
    lmCoord = (gl_TextureMatrix[1] * gl_MultiTexCoord1).xy;
    color = gl_Color;
    viewPos = curvedView.xyz;

    mat3 viewToPlayer = mat3(gbufferModelViewInverse);
    playerPos = (gbufferModelViewInverse * curvedView).xyz;
    normalPlayer = normalize(viewToPlayer * normalize(gl_NormalMatrix * gl_Normal));
    sunDirPlayer = normalize(viewToPlayer * normalize(shadowLightPosition));
}
