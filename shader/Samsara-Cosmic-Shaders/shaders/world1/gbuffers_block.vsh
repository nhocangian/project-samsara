#version 120

varying vec2 texCoord;
varying vec2 lmCoord;
varying vec3 normal;
varying vec4 color;
varying vec3 viewPos;
varying float glitchMark;

uniform float frameTimeCounter;
uniform int blockEntityId;

#include "/lib/glitch_split.glsl"

void main() {
    // Đồ đạc phòng ngủ Bến Đò: rung nhẹ, giật mạnh theo đợt (lib/glitch_split.glsl)
    glitchMark = (blockEntityId == 10001) ? 1.0 : 0.0;
    vec4 vertex = gl_Vertex;
    if (glitchMark > 0.5) {
        float gt = frameTimeCounter;
        vertex.xyz += glitchShake(gt, 0.5 + 1.2 * glitchBurst(gt));
    }
    gl_Position = gl_ModelViewProjectionMatrix * vertex;
    texCoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
    lmCoord = (gl_TextureMatrix[1] * gl_MultiTexCoord1).xy;
    
    vec3 n = gl_NormalMatrix * gl_Normal;
    normal = length(n) > 0.001 ? normalize(n) : vec3(0.0, 1.0, 0.0);
    
    color = gl_Color;
    viewPos = (gl_ModelViewMatrix * vertex).xyz;
}
