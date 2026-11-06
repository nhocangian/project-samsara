#version 120

// Tay cầm: không bẻ cong.

uniform vec3 shadowLightPosition;

varying vec2 texCoord;
varying vec2 lmCoord;
varying vec4 color;

// Thêm cho ánh sáng (không gian view)
varying vec3 normalView;
varying vec3 sunDirView;

void main() {
    gl_Position = ftransform();
    texCoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
    lmCoord = (gl_TextureMatrix[1] * gl_MultiTexCoord1).xy;
    color = gl_Color;

    normalView = normalize(gl_NormalMatrix * gl_Normal);
    sunDirView = normalize(shadowLightPosition);
}
