#version 120

#include "/lib/inception_curve.glsl"

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
    vec4 curvedView = inceptionCurve(gl_ModelViewMatrix * gl_Vertex);
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
