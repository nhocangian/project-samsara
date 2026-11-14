#version 120

uniform mat4 gbufferModelViewInverse;
uniform vec3 cameraPosition;

varying vec2 texCoord;
varying vec2 lmCoord;
varying vec4 color;
varying vec3 viewPos;
varying vec3 worldPos;

#include "/lib/dem_dau_tien_tear.glsl"

void main() {
    // Khối trong vết rách rung nhẹ cho cảm giác không ổn định; ngoài vết rách đứng yên
    vec3 rawView = (gl_ModelViewMatrix * gl_Vertex).xyz;
    vec3 rawWorld = (gbufferModelViewInverse * vec4(rawView, 1.0)).xyz + cameraPosition;
    vec4 vertex = gl_Vertex;
    vertex.xyz += tearShake(frameTimeCounter) * tearShakeWeight(rawWorld);
    gl_Position = gl_ModelViewProjectionMatrix * vertex;
    texCoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
    lmCoord = (gl_TextureMatrix[1] * gl_MultiTexCoord1).xy;
    color = gl_Color;
    viewPos = (gl_ModelViewMatrix * vertex).xyz;
    vec3 normal = mat3(gbufferModelViewInverse) * normalize(gl_NormalMatrix * gl_Normal);
    // Lùi vào trong mặt khối một chút để làm tròn theo pixel không chập chờn ngay trên mặt phẳng
    worldPos = (gbufferModelViewInverse * vec4(viewPos, 1.0)).xyz + cameraPosition - normal * 0.01;
}
