#version 120

varying vec2 texCoord;
varying vec2 lmCoord;
varying vec4 color;
varying vec3 viewPos;
varying vec3 viewNormal;

void main() {
    gl_Position = ftransform();
    texCoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
    lmCoord = (gl_TextureMatrix[1] * gl_MultiTexCoord1).xy;
    color = gl_Color;
    viewPos = (gl_ModelViewMatrix * gl_Vertex).xyz;
    viewNormal = gl_NormalMatrix * gl_Normal;
}
