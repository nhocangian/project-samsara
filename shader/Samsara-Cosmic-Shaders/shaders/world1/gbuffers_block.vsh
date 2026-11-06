#version 120

varying vec2 texCoord;
varying vec2 lmCoord;
varying vec3 normal;
varying vec4 color;
varying vec3 viewPos;

void main() {
    gl_Position = ftransform();
    texCoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
    lmCoord = (gl_TextureMatrix[1] * gl_MultiTexCoord1).xy;
    
    vec3 n = gl_NormalMatrix * gl_Normal;
    normal = length(n) > 0.001 ? normalize(n) : vec3(0.0, 1.0, 0.0);
    
    color = gl_Color;
    viewPos = (gl_ModelViewMatrix * gl_Vertex).xyz;
}
